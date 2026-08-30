package co.com.bancolombia.api.dto.task;

/**
 * Representación HTTP de una tarea hacia el frontend.
 *
 * <p>Existe para que el entry-point <b>deje de devolver el modelo de dominio</b> (deuda D-15).
 * Sus campos son exactamente los que consume la interfaz TypeScript {@code AgentTask}: ni más —el
 * {@code history} y los {@code artifacts} de la {@code Task} A2A no se usan en el panel— ni menos.
 *
 * @param id        identificador de la tarea
 * @param contextId conversación o sesión a la que pertenece
 * @param origin    quién la originó: {@code AGENT} o {@code BFF} (DP-07-bis)
 * @param status    estado actual
 */
public record TaskResponse(String id, String contextId, String origin, TaskStatusResponse status) {

}

