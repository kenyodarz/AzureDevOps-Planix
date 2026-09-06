package co.com.bancolombia.api.dto;

import co.com.bancolombia.api.dto.task.TaskDtoMapper;
import co.com.bancolombia.api.dto.task.TaskResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.agent.AgentInteraction;

/**
 * Representación HTTP de respuesta tras activar la planeación macro trimestral.
 *
 * <p>Proporciona el resumen del mensaje, la tarea asíncrona registrada en el agente (para
 * seguimiento
 * en la UI) y el identificador de contexto.
 *
 * @param quarter   Trimestre solicitado
 * @param message   Respuesta textual emitida por el agente o el caso de uso
 * @param task      Información de la tarea asíncrona para rastreo de progreso
 * @param contextId Identificador de contexto de la sesión
 */
public record ProgramPlanResponseDTO(
        String quarter,
        String message,
        TaskResponse task,
        String contextId
) {

    /**
     * Construye la respuesta HTTP a partir del resultado de la interacción del agente.
     *
     * @param quarter     trimestre objetivo
     * @param interaction interacción producida por el caso de uso
     * @return DTO listo para serializar
     */
    public static ProgramPlanResponseDTO from(String quarter, AgentInteraction interaction) {
        if (interaction == null) {
            return new ProgramPlanResponseDTO(quarter, null, null, null);
        }
        TaskResponse taskResponse = interaction.taskAsOptional()
                .map(TaskDtoMapper::toResponse)
                .orElse(null);
        String contextId = interaction.taskAsOptional()
                .map(Task::getContextId)
                .orElse(null);

        return new ProgramPlanResponseDTO(
                quarter,
                interaction.reply(),
                taskResponse,
                contextId
        );
    }
}
