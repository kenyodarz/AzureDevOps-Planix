package co.com.bancolombia.api.dto.task;

import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.TaskOrigin;
import co.com.bancolombia.model.agent.TrackedTask;

/**
 * Traduce el modelo de dominio a los DTO que viajan por HTTP.
 *
 * <p>Cumple la regla de que el entry-point no expone el dominio: aquí se decide qué se publica y
 * en qué forma, y un cambio en {@code Task} no arrastra al frontend.
 */
public final class TaskDtoMapper {

    private TaskDtoMapper() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Convierte una tarea etiquetada con su origen.
     *
     * @param tracked tarea con procedencia
     * @return el DTO listo para serializar
     */
    public static TaskResponse toResponse(TrackedTask tracked) {
        return toResponse(tracked.task(), tracked.origin());
    }

    /**
     * Convierte una tarea del agente. Las rutas {@code tasks/get} y {@code tasks/cancel} solo
     * hablan con el agente, de modo que el origen es siempre {@link TaskOrigin#AGENT}.
     *
     * @param task tarea del agente
     * @return el DTO listo para serializar
     */
    public static TaskResponse toResponse(Task task) {
        return toResponse(task, TaskOrigin.AGENT);
    }

    /**
     * Convierte la interacción completa devuelta por {@code POST /api/chat/messages}.
     *
     * @param interaction respuesta del agente, con o sin tarea
     * @return el DTO listo para serializar
     */
    public static ChatMessageResponse toResponse(AgentInteraction interaction) {
        return new ChatMessageResponse(
                interaction.reply(),
                interaction.taskAsOptional().map(TaskDtoMapper::toResponse).orElse(null));
    }

    private static TaskResponse toResponse(Task task, TaskOrigin origin) {
        if (task == null) {
            return null;
        }
        return new TaskResponse(
                task.getId(),
                task.getContextId(),
                origin.name(),
                toStatusResponse(task.getStatus()));
    }

    /**
     * El estado se publica con el <b>valor A2A en minúsculas</b> ({@code input-required}), que es
     * lo que el frontend ya interpreta, y no con el nombre del enumerado de Java
     * ({@code INPUT_REQUIRED}).
     */
    private static TaskStatusResponse toStatusResponse(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return new TaskStatusResponse(
                status.getState() != null ? status.getState().getValue() : null,
                status.getMessage() != null ? status.getMessage().extractText() : null,
                status.getTimestamp());
    }
}

