package co.com.bancolombia.api.dto.task;

/**
 * Estado de una tarea tal como lo espera la interfaz TypeScript {@code AgentTaskStatus}.
 *
 * @param state     valor A2A en minúsculas ({@code working}, {@code input-required}…), nunca el
 *                  nombre del enumerado de Java
 * @param message   texto plano extraído del mensaje embebido en el estado
 * @param timestamp marca de tiempo informada por el productor de la tarea
 */
public record TaskStatusResponse(String state, String message, String timestamp) {

}

