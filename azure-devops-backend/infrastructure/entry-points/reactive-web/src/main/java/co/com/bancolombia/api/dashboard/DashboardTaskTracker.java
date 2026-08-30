package co.com.bancolombia.api.dashboard;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Ciclo de vida de la {@code Task} A2A que acompaña a una auditoría del backlog.
 *
 * <p><b>D-05.</b> Hasta la Fase 05 esto vivía dentro de
 * {@code Handler.handleDevOpsDashboardStream}, mezclado con el parseo, el mapeo y la orquestación
 * del SSE: cuatro responsabilidades en una cadena de unas noventa líneas. Aquí queda una sola, y se
 * puede probar sin levantar un servidor.
 *
 * <p><b>Fase 08 (D-42, B-13).</b> Antes decía aquí que «no es un bean de Spring a propósito». No
 * era cierto por completo: el motivo real no era de diseño, sino que declararlo obligaba a añadirlo
 * al {@code @ContextConfiguration} de tres pruebas, y ninguna fase anterior tuvo autorización para
 * tocarlas. Es un andamio, y esta fase es la que los jubila. Ahora es un {@code @Component} como
 * cualquier otro colaborador del entry-point, y el {@code Handler} deja de construir a mano las
 * dependencias de sus dependencias.
 */
@Component
public class DashboardTaskTracker {

    private static final String ROLE_AGENT = "agent";

    private final TaskStoreGateway taskStoreGateway;

    public DashboardTaskTracker(TaskStoreGateway taskStoreGateway) {
        this.taskStoreGateway = taskStoreGateway;
    }

    /**
     * Crea la tarea en {@code WORKING} y la guarda.
     */
    public Mono<Task> start(String sessionKey, String description) {
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .contextId(sessionKey)
                .status(statusOf(TaskState.WORKING, sessionKey, description))
                .build();
        return taskStoreGateway.save(task);
    }

    /**
     * Marca la tarea como {@code COMPLETED}.
     */
    public Mono<Void> complete(Task task) {
        return transition(task, TaskState.COMPLETED,
                "Auditoría de backlog finalizada con éxito. Reporte generado.");
    }

    /**
     * Marca la tarea como {@code FAILED}, conservando el detalle del error.
     */
    public Mono<Void> fail(Task task, Throwable error) {
        return transition(task, TaskState.FAILED, "Fallo en auditoría: " + error.getMessage());
    }

    /**
     * <b>Ojo con el {@code defer}.</b> Sin él, {@code taskStoreGateway.save(...)} se ejecutaría al
     * <i>construir</i> la cadena y no al recorrerla: la tarea se guardaría aunque el paso nunca
     * llegara a suscribirse, y la marca de tiempo sería la del ensamblado. Es exactamente lo que
     * ocurría al extraer este código del {@code Handler}, donde el {@code Mono.defer} original lo
     * evitaba sin que se notara.
     */
    private Mono<Void> transition(Task task, TaskState state, String description) {
        return Mono.defer(() -> {
            Task updated = task.toBuilder()
                    .status(statusOf(state, task.getContextId(), description))
                    .build();
            return taskStoreGateway.save(updated).then();
        });
    }

    private TaskStatus statusOf(TaskState state, String sessionKey, String description) {
        return TaskStatus.builder()
                .state(state)
                .message(Message.builder()
                        .role(ROLE_AGENT)
                        .contextId(sessionKey)
                        .messageId(UUID.randomUUID().toString())
                        .parts(List.of(Part.ofText(description)))
                        .build())
                .timestamp(Instant.now().toString())
                .build();
    }
}

