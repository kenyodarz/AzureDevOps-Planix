package co.com.bancolombia.model.chat.gateways;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para la comunicación con el modelo de lenguaje (LLM).
 * <p>
 * Define el contrato que deben implementar los adaptadores de IA (e.g. Spring AI ChatClient) para
 * enviar prompts y recibir respuestas. Siguiendo Clean Architecture, el dominio solo conoce esta
 * interfaz, no la implementación concreta.
 */
public interface ChatGateway {

    /**
     * Envía un mensaje (prompt) al modelo de IA y retorna la respuesta generada.
     *
     * @param message texto del prompt completo a enviar al LLM; nunca {@code null}
     * @return {@link Mono} con la respuesta en texto plano del modelo. Si el modelo retorna bloques
     * de razonamiento ({@code <think>}), la implementación debe filtrarlos antes de emitir.
     * @throws RuntimeException si ocurre un error de red o de autenticación con el servicio de IA
     *
     *                          <pre>{@code
     *                                                                                                                                                                                // Ejemplo de uso desde un caso de uso:
     *                                                                                                                                                                                chatGateway.sendMessage("¿Cuáles son los términos para cliente CC 123?")
     *                                                                                                                                                                                    .subscribe(respuesta -> log.info("LLM respondió: {}", respuesta));
     *                                                                                                                                                                                }</pre>
     */
    Mono<String> sendMessage(String message, String contextId);
}
