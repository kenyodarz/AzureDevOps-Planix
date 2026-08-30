package co.com.bancolombia.usecase.agent;

import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.TrackedTask;
import co.com.bancolombia.model.agent.gateways.AgentGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Delega trabajo en el agente y proyecta el estado de sus tareas hacia el frontend.
 *
 * <p>Es la pieza que da sentido a la misión del BFF: cuando el usuario pide «analiza este sprint»,
 * el agente abre una tarea y este caso de uso se asegura de que el frontend pueda seguirla.
 *
 * <p><b>Propiedad de las tareas (D-24 saldada).</b> Las tareas del agente son del agente: el BFF
 * <b>ya no las escribe</b> en la base de datos. La réplica local que existía mientras duró la
 * integración por base de datos compartida se retiró en la Fase 03; la fuente de verdad es el
 * propio agente y se consulta cada vez. {@code taskStoreGateway} queda reservado para las tareas
 * <i>propias</i> del BFF, las que crea el flujo del dashboard.
 *
 * <p><b>Modo no bloqueante (DP-08).</b> {@link #sendAndTrack} no espera a que el agente termine:
 * devuelve la interacción en cuanto el agente acusa recibo, típicamente con la tarea en
 * {@code submitted} o {@code working}. El progreso se consulta después con {@link #refreshTask}.
 */
@RequiredArgsConstructor
public class TrackAgentTaskUseCase {

    private final AgentGateway agentGateway;
    private final TaskStoreGateway taskStoreGateway;

    /**
     * Envía una orden al agente y devuelve su respuesta junto con la tarea abierta.
     *
     * @param command orden a ejecutar
     * @return la respuesta del agente junto con la tarea que el frontend podrá seguir
     */
    public Mono<AgentInteraction> sendAndTrack(AgentCommand command) {
        return agentGateway.sendMessage(command);
    }

    /**
     * Consulta al agente el estado actual de una tarea.
     *
     * <p>Es la operación sobre la que se apoya el sondeo del frontend. No hay copia local que
     * actualizar: la respuesta del agente <b>es</b> el estado.
     *
     * @param taskId identificador de la tarea
     * @return la tarea con su estado más reciente
     */
    public Mono<Task> refreshTask(String taskId) {
        return requireTaskId(taskId).flatMap(agentGateway::getTask);
    }

    /**
     * Solicita al agente la cancelación de una tarea.
     *
     * @param taskId identificador de la tarea
     * @return la tarea ya cancelada
     */
    public Mono<Task> cancelTask(String taskId) {
        return requireTaskId(taskId).flatMap(agentGateway::cancelTask);
    }

    /**
     * Lista todas las tareas que el frontend debe ver, etiquetadas con su origen (DP-07-bis).
     *
     * <p>Combina las dos fuentes que existen —el agente y el propio BFF— porque el panel de
     * progreso necesita la foto completa. Dos decisiones deliberadas:
     *
     * <ul>
     *   <li><b>Degradación ante un agente caído</b>: si el agente no responde, el listado no se
     *       rompe, se queda con las tareas del BFF. Que el panel entero desaparezca por un agente
     *       caído sería peor que mostrar datos parciales.</li>
     *   <li><b>Deduplicación por identificador</b>: mientras la tabla siga siendo física y
     *       compartida (DP-05, Fase 07), la misma tarea puede llegar por ambas vías. Prevalece la
     *       del agente, que se emite primero y es la fuente de verdad.</li>
     * </ul>
     *
     * @return las tareas conocidas, sin duplicados
     */
    public Flux<TrackedTask> listTasks() {
        Flux<TrackedTask> fromAgent = agentGateway.listTasks()
                .map(TrackedTask::fromAgent)
                .onErrorResume(error -> Flux.empty());

        Flux<TrackedTask> fromBff = taskStoreGateway.findAll()
                .map(TrackedTask::fromBff);

        return Flux.concat(fromAgent, fromBff)
                .distinct(TrackedTask::id);
    }

    /**
     * Publica las capacidades del agente para que el frontend las descubra a través del BFF.
     *
     * @return la tarjeta de presentación del agente
     */
    public Mono<AgentCard> getAgentCard() {
        return agentGateway.getAgentCard();
    }

    private Mono<String> requireTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("El identificador de la tarea es obligatorio"));
        }
        return Mono.just(taskId);
    }
}
