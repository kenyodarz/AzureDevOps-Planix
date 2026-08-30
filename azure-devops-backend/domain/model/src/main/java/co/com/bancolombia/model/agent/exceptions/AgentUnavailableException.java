package co.com.bancolombia.model.agent.exceptions;

/**
 * No se pudo hablar con el agente: está caído, inaccesible o agotó el tiempo de espera.
 *
 * <p>Se distingue de {@link AgentExecutionException} porque aquí el agente <b>ni siquiera llegó a
 * intentarlo</b>. El entry-point debería traducirla a un <b>503</b> (DP-06, Fase 07), no al 400
 * genérico de hoy, para que el frontend pueda mostrar «el asistente no está disponible» en lugar de
 * insinuar que el usuario se equivocó.
 */
public class AgentUnavailableException extends AgentException {

    public AgentUnavailableException(String detail, Throwable cause) {
        super("No fue posible contactar con el agente: " + detail, cause);
    }
}

