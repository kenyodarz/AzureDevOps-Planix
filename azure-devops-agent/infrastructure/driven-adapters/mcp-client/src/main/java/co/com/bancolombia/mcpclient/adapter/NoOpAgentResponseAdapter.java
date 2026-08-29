package co.com.bancolombia.mcpclient.adapter;

import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementación <i>Null Object</i> de {@link AgentResponseGateway}: acepta la respuesta y no hace
 * nada con ella.
 *
 * <p><b>Es deliberada, no un pendiente</b> (DP-05, Opción A). En la plataforma del Banco el
 * protocolo A2A viaja sobre Kafka; mientras la mensajería no esté cableada en este agente, el
 * transporte asíncrono {@code AgentChatUseCase.chat(...)} necesita un puerto que resuelva sin
 * efectos laterales. Este adaptador cumple esa función y actúa como interruptor: el camino
 * asíncrono queda apagado <b>sin necesidad de un feature flag</b>, porque publicar en
 * {@link Mono#empty()} no produce ningún efecto observable.
 *
 * <p><b>Para activar A2A sobre Kafka:</b> sustituir este bean por un adaptador productor que
 * implemente {@link AgentResponseGateway} y publique en el topic configurado. No hay que tocar el
 * dominio, el caso de uso ni la configuración de flujos: el punto de extensión es este bean.
 *
 * @see AgentResponseGateway
 */
@Component
public class NoOpAgentResponseAdapter implements AgentResponseGateway {

    /**
     * Descarta la respuesta sin publicarla.
     *
     * @param response respuesta del agente, ignorada por diseño
     * @return {@link Mono#empty()}, que completa de inmediato y sin efectos laterales
     */
    @Override
    public Mono<Void> sendResponse(SendMessageResponse response) {
        return Mono.empty();
    }
}
