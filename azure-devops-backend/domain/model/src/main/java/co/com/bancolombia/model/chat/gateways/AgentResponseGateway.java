package co.com.bancolombia.model.chat.gateways;

import co.com.bancolombia.model.a2a.SendMessageResponse;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para publicar respuestas del agente hacia un canal externo.
 * <p>
 * Abstrae el mecanismo de entrega de la respuesta (e.g. Kafka, REST callback). Se usa
 * principalmente en el flujo asíncrono del Consumer Agent donde la respuesta no se retorna
 * directamente al llamador HTTP.
 *
 * @see SendMessageResponse
 */
public interface AgentResponseGateway {

    /**
     * Publica la respuesta del agente al canal de salida configurado.
     * <p>
     * En la implementación actual, serializa el {@link SendMessageResponse} y lo produce al topic
     * Kafka configurado en {@code adapters.kafka.producer.topic}.
     *
     * @param response respuesta del agente con el {@link co.com.bancolombia.model.a2a.Task}
     *                 resultante (estado {@code COMPLETED} o {@code FAILED})
     * @return {@link Mono<Void>} que completa cuando la respuesta fue enviada exitosamente
     * @throws RuntimeException si el canal de mensajería no está disponible
     *
     *                          <pre>{@code
     *                                                                                                                                                                                // Uso típico desde AgentChatUseCase (flujo Kafka):
     *                                                                                                                                                                                agentResponseGateway.sendResponse(response)
     *                                                                                                                                                                                    .doOnSuccess(v -> log.info("Respuesta publicada en Kafka"))
     *                                                                                                                                                                                    .subscribe();
     *                                                                                                                                                                                }</pre>
     */
    Mono<Void> sendResponse(SendMessageResponse response);
}
