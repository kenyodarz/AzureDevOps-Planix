package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import reactor.core.publisher.Mono;

/**
 * Estrategia que atiende una intención concreta de la conversación.
 *
 * <p>Una implementación por {@link AgentIntent}: añadir una intención consiste en crear un handler
 * y registrarlo, sin modificar el orquestador (`spring-rules.md` §3-O).
 *
 * <p>El handler devuelve <b>el texto final para el usuario</b>. No construye la respuesta A2A ni
 * maneja errores genéricos: de eso se ocupa el caso de uso, en un único lugar.
 */
public interface ChatFlowHandler {

    /** Intención que este handler sabe atender. */
    AgentIntent supports();

    /**
     * Ejecuta el flujo.
     *
     * @param context datos de la conversación ya resueltos
     * @return el texto listo para presentar al usuario
     */
    Mono<String> handle(ChatFlowContext context);
}

