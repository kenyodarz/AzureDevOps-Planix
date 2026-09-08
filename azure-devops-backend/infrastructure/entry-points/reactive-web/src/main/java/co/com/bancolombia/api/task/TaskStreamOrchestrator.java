package co.com.bancolombia.api.task;

import co.com.bancolombia.api.dto.event.TaskUpdateEvent;
import co.com.bancolombia.api.dto.task.TaskDtoMapper;
import co.com.bancolombia.api.dto.task.TaskResponse;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Orquesta el stream SSE de sincronización de tareas entre el BFF/Agente y el frontend.
 *
 * <p>Sustituye la deuda técnica del sondeo recurrente (polling cada 5 segundos) por un flujo
 * reactivo
 * push. Al suscribirse, el cliente recibe el estado actual ({@code INITIAL}). Cuando se registra o
 * modifica una tarea se emite {@code TASKS_UPDATE}. Si existen tareas activas en estado
 * {@code submitted} o {@code working}, un ticker reactivo sondea en servidor hasta que todas
 * completan o fallan, momento en el cual el flujo entra en reposo absoluto (cero consultas a la
 * base de datos).
 *
 * <p>Emite además un evento de {@code HEARTBEAT} periódico para evitar que firewalls o proxies
 * cierren la conexión por inactividad.
 */
@Slf4j
@Component
public class TaskStreamOrchestrator {

    private final TrackAgentTaskUseCase trackAgentTaskUseCase;
    private final Duration activePollInterval;
    private final Duration heartbeatInterval;
    private final Sinks.Many<String> changeTrigger;
    private final AtomicBoolean hasActiveTasks;

    public TaskStreamOrchestrator(
            TrackAgentTaskUseCase trackAgentTaskUseCase,
            @Value("${tasks.stream.active-interval-ms:5000}") long activeIntervalMs,
            @Value("${tasks.stream.heartbeat-interval-seconds:25}") long heartbeatSeconds) {
        this.trackAgentTaskUseCase = trackAgentTaskUseCase;
        this.activePollInterval = Duration.ofMillis(activeIntervalMs);
        this.heartbeatInterval = Duration.ofSeconds(heartbeatSeconds);
        this.changeTrigger = Sinks.many().multicast().directBestEffort();
        this.hasActiveTasks = new AtomicBoolean(false);
    }

    private static ServerSentEvent<TaskUpdateEvent> asServerSentEvent(TaskUpdateEvent update) {
        return ServerSentEvent.<TaskUpdateEvent>builder()
                .event(update.getEvent())
                .data(update)
                .build();
    }

    /**
     * Notifica un cambio en las tareas (creación, cancelación, etc.) para despertar el stream.
     */
    public void notifyTaskChange() {
        changeTrigger.tryEmitNext("CHANGE");
    }

    /**
     * Retorna el stream SSE de tareas para un cliente conectado.
     */
    public Flux<ServerSentEvent<TaskUpdateEvent>> stream() {
        Flux<TaskUpdateEvent> initialFlux = fetchCurrentTasks(TaskUpdateEvent.EVENT_INITIAL).flux();

        Flux<TaskUpdateEvent> triggerFlux = changeTrigger.asFlux()
                .flatMap(trigger -> fetchCurrentTasks(TaskUpdateEvent.EVENT_TASKS_UPDATE));

        Flux<TaskUpdateEvent> activeTasksTicker = Flux.interval(activePollInterval)
                .filter(tick -> hasActiveTasks.get())
                .flatMap(tick -> fetchCurrentTasks(TaskUpdateEvent.EVENT_TASKS_UPDATE));

        Flux<TaskUpdateEvent> heartbeatFlux = Flux.interval(heartbeatInterval)
                .map(tick -> new TaskUpdateEvent(TaskUpdateEvent.EVENT_HEARTBEAT, List.of(),
                        "keep-alive"));

        return Flux.merge(initialFlux, triggerFlux, activeTasksTicker, heartbeatFlux)
                .map(TaskStreamOrchestrator::asServerSentEvent)
                .onErrorResume(error -> {
                    log.error("Error en stream SSE de tareas", error);
                    return Flux.just(
                            asServerSentEvent(TaskUpdateEvent.ofError(error.getMessage())));
                });
    }

    private Mono<TaskUpdateEvent> fetchCurrentTasks(String eventType) {
        return trackAgentTaskUseCase.listTasks()
                .map(TaskDtoMapper::toResponse)
                .collectList()
                .map(tasks -> {
                    boolean active = tasks.stream().anyMatch(this::isActive);
                    hasActiveTasks.set(active);
                    return new TaskUpdateEvent(eventType, tasks);
                })
                .onErrorResume(e -> {
                    log.warn("Error consultando tareas para stream SSE: {}", e.getMessage());
                    return Mono.just(new TaskUpdateEvent(eventType, List.of(), e.getMessage()));
                });
    }

    private boolean isActive(TaskResponse task) {
        if (task == null || task.status() == null || task.status().state() == null) {
            return false;
        }
        String state = task.status().state();
        return "submitted".equalsIgnoreCase(state) || "working".equalsIgnoreCase(state);
    }
}
