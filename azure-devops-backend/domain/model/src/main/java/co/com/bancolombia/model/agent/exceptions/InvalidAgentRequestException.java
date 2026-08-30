package co.com.bancolombia.model.agent.exceptions;

/**
 * El agente rechazó la petición por estar mal formada.
 *
 * <p>Traduce los códigos JSON-RPC {@code -32600} (Invalid Request), {@code -32601} (Method not
 * found) y {@code -32602} (Invalid params). Los tres significan lo mismo desde la perspectiva del
 * BFF: <b>la culpa es del emisor</b>, no del agente, así que reintentar no serviría de nada.
 *
 * <p>Que esta excepción llegue a producción indica una divergencia de contrato entre el BFF y el
 * agente, y debe tratarse como un defecto del BFF.
 */
public class InvalidAgentRequestException extends AgentException {

    public InvalidAgentRequestException(String detail) {
        super("El agente rechazó la petición: " + detail);
    }
}

