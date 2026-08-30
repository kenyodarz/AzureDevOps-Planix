package co.com.bancolombia.model.agent.exceptions;

/**
 * El agente falló al ejecutar la tarea.
 *
 * <p>Traduce el código JSON-RPC {@code -32603} (Internal error) y cualquier otro código no
 * contemplado. La petición era válida; lo que falló fue el trabajo del agente —normalmente el LLM o
 * una herramienta MCP—, así que reintentar puede tener sentido.
 */
public class AgentExecutionException extends AgentException {

    public AgentExecutionException(String detail) {
        super("El agente no pudo completar la tarea: " + detail);
    }
}

