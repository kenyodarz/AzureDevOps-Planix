package co.com.bancolombia.model.agent.exceptions;

/**
 * Raíz de los fallos que el BFF puede sufrir al delegar en el agente.
 *
 * <p>Existe para que los casos de uso y el entry-point puedan distinguir «falló el agente» de
 * cualquier otro error, sin conocer el transporte. El adaptador traduce a estas excepciones los
 * códigos de error JSON-RPC y los fallos de red, de modo que ningún detalle de infraestructura
 * (`WebClientResponseException`, códigos numéricos) se filtra al dominio.
 */
public abstract class AgentException extends RuntimeException {

    protected AgentException(String message) {
        super(message);
    }

    protected AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}

