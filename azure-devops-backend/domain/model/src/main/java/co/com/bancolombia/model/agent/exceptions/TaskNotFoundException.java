package co.com.bancolombia.model.agent.exceptions;

/**
 * La tarea consultada o cancelada no existe en el agente.
 *
 * <p>Traduce el código JSON-RPC específico de A2A {@code -32004}. El entry-point debe convertirla
 * en un <b>404</b>, no en el 400 genérico que hoy devuelve cualquier fallo (DP-06, Fase 07).
 */
public class TaskNotFoundException extends AgentException {

    private final String taskId;

    public TaskNotFoundException(String taskId) {
        super("El agente no conoce la tarea " + taskId);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}

