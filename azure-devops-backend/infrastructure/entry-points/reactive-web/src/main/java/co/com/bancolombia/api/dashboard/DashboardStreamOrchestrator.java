package co.com.bancolombia.api.dashboard;

import co.com.bancolombia.api.dto.dashboard.DashboardDtoMapper;
import co.com.bancolombia.api.dto.dashboard.DashboardResponse;
import co.com.bancolombia.api.dto.event.BatchUpdatesEvent;
import co.com.bancolombia.api.dto.event.DashboardUpdateEvent;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.model.dashboard.StoryQuality;
import co.com.bancolombia.model.dashboard.StoryUpdate;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Orquesta el stream SSE de la auditoría del backlog.
 *
 * <p><b>D-05.</b> Es lo que antes hacía {@code Handler.handleDevOpsDashboardStream} además de
 * traducir HTTP. El entry-point vuelve a ser lo que debe ser —leer parámetros y elegir el código de
 * respuesta— y esta clase se ocupa del flujo.
 *
 * <p><b>D-31 y B-06.</b> El stream <b>nunca termina en error</b>: cualquier fallo se convierte en
 * un evento {@link DashboardUpdateEvent#ERROR} y el flujo se completa. Ésa es la única forma de que
 * WebFlux llegue a escribir la cabecera {@code text/event-stream}, y por tanto de que el frontend
 * pueda distinguir «no hay datos» de «el agente se cayó».
 *
 * <p><b>T-06.</b> {@link DashboardDtoMapper} se invoca <b>solo desde aquí</b>: es el único sitio
 * del BFF donde el JSON del agente se convierte en dominio y el dominio vuelve a salir como JSON.
 */
@Slf4j
@Component
public class DashboardStreamOrchestrator {

    private static final String EVENT_INITIAL = "INITIAL";
    private static final String EVENT_BATCH_UPDATE = "BATCH_UPDATE";
    private static final String UNKNOWN_ERROR = "No fue posible completar la auditoría del backlog";

    private final DevOpsDashboardUseCase devOpsDashboardUseCase;
    private final DashboardTaskTracker taskTracker;
    private final JsonMapper jsonMapper;
    private final int auditBatchSize;
    private final int auditConcurrency;

    /**
     * <b>Fase 08 (D-42, B-13).</b> Los dos parámetros de configuración se leen aquí, que es donde
     * se usan. Antes los recibía el {@code Handler}, que no hacía nada con ellos salvo pasárselos a
     * esta clase al construirla con {@code new}: el entry-point declaraba configuración ajena para
     * poder montar a mano un colaborador que Spring sabe montar solo.
     */
    public DashboardStreamOrchestrator(DevOpsDashboardUseCase devOpsDashboardUseCase,
            DashboardTaskTracker taskTracker,
            JsonMapper jsonMapper,
            @Value("${audit.batch-size:10}") int auditBatchSize,
            @Value("${audit.concurrency:2}") int auditConcurrency) {
        this.devOpsDashboardUseCase = devOpsDashboardUseCase;
        this.taskTracker = taskTracker;
        this.jsonMapper = jsonMapper;
        this.auditBatchSize = auditBatchSize;
        this.auditConcurrency = auditConcurrency;
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? UNKNOWN_ERROR : message;
    }

    private static ServerSentEvent<DashboardUpdateEvent> asServerSentEvent(
            DashboardUpdateEvent update) {
        return ServerSentEvent.<DashboardUpdateEvent>builder()
                .event(update.getEvent())
                .data(update)
                .build();
    }

    public Flux<ServerSentEvent<DashboardUpdateEvent>> stream(String cell, String sprint) {
        String sessionKey = "dashboard-" + UUID.randomUUID();

        /*
         * Estado acumulado de la auditoría (D-06). Aquí nunca se modifica una auditoría: se
         * reemplaza la referencia por una auditoría nueva, y `updateAndGet` garantiza que con
         * varios lotes resolviéndose a la vez no se pierda ninguna actualización.
         */
        AtomicReference<BacklogAudit> auditState = new AtomicReference<>();

        return taskTracker.start(sessionKey, "Analizando calidad del backlog: Célula " + cell
                        + " | Sprint " + sprint)
                .flatMapMany(task -> auditEvents(cell, sprint, sessionKey, auditState)
                        .concatWith(closeSuccessfully(task, auditState, cell, sprint))
                        .onErrorResume(error -> reportFailure(task, error)))
                .map(DashboardStreamOrchestrator::asServerSentEvent);
    }

    /**
     * El {@code INITIAL} con lo que devuelve el agente y un {@code BATCH_UPDATE} por lote.
     */
    private Flux<DashboardUpdateEvent> auditEvents(String cell, String sprint, String sessionKey,
            AtomicReference<BacklogAudit> auditState) {
        return devOpsDashboardUseCase.getDashboardInitialData(cell, sprint, sessionKey)
                .map(response -> jsonMapper.readValue(response, DashboardResponse.class))
                .flatMapMany(initialDto -> {
                    BacklogAudit initialAudit = DashboardDtoMapper.toDomain(initialDto);
                    auditState.set(initialAudit);
                    return Flux.concat(
                            Flux.just(new DashboardUpdateEvent(EVENT_INITIAL, initialDto)),
                            batchUpdates(initialAudit, sessionKey, auditState));
                });
    }

    private Flux<DashboardUpdateEvent> batchUpdates(BacklogAudit initialAudit, String sessionKey,
            AtomicReference<BacklogAudit> auditState) {
        return Flux.fromIterable(initialAudit.partition(auditBatchSize))
                .flatMapSequential(batch -> auditBatch(batch, sessionKey), auditConcurrency)
                .map(updates -> new DashboardUpdateEvent(EVENT_BATCH_UPDATE,
                        DashboardDtoMapper.toDto(
                                auditState.updateAndGet(current -> current.withUpdates(updates)))));
    }

    /**
     * Cierre feliz: se guarda el reporte y la tarea pasa a {@code COMPLETED}. No emite eventos.
     *
     * <p><b>B-05.</b> Si el reporte no se puede guardar, se registra y el stream continúa: el
     * tablero ya se entregó y perder el reporte no debe invalidar la consulta.
     */
    private Flux<DashboardUpdateEvent> closeSuccessfully(Task task,
            AtomicReference<BacklogAudit> auditState, String cell, String sprint) {
        return Mono.defer(() -> devOpsDashboardUseCase
                        .saveDashboardReport(auditState.get(), cell, sprint))
                .doOnError(error -> log.error(
                        "No se pudo guardar el reporte del tablero; el stream continúa.", error))
                .onErrorComplete()
                .then(taskTracker.complete(task))
                .thenMany(Flux.empty());
    }

    /**
     * Marca la tarea como fallida y cuenta el fallo por el propio stream (D-31, B-06).
     */
    private Flux<DashboardUpdateEvent> reportFailure(Task task, Throwable error) {
        log.warn("Error en el stream del tablero; se emite un evento ERROR.", error);
        return taskTracker.fail(task, error)
                .thenMany(Flux.just(DashboardUpdateEvent.ofError(describe(error))));
    }

    /**
     * Pide al agente la auditoría de un lote y devuelve las actualizaciones que haya entendido.
     *
     * <p>Nunca falla: si el agente no responde o su JSON no se puede leer, devuelve una lista
     * vacía. Aplicar una lista vacía deja la auditoría intacta. Un lote roto no debe tumbar el
     * stream.
     */
    private Mono<List<StoryUpdate>> auditBatch(List<StoryQuality> batch, String sessionKey) {
        return devOpsDashboardUseCase
                .getBatchAudit(batch.size(), BacklogAudit.idsCsv(batch), sessionKey)
                .map(this::parseUpdates)
                .onErrorResume(error -> {
                    log.warn("Falló la auditoría de un lote; se conserva el estado actual.", error);
                    return Mono.just(List.of());
                });
    }

    private List<StoryUpdate> parseUpdates(String auditResponse) {
        try {
            BatchUpdatesEvent batchUpdates = jsonMapper.readValue(auditResponse,
                    BatchUpdatesEvent.class);
            return DashboardDtoMapper.toDomainUpdates(batchUpdates.getUpdates());
        } catch (Exception e) {
            log.warn("Error al leer la auditoría del lote; se conserva el estado actual.", e);
            return List.of();
        }
    }
}

