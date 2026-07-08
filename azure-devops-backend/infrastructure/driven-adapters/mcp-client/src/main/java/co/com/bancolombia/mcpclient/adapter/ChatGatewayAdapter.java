package co.com.bancolombia.mcpclient.adapter;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptador que implementa {@link ChatGateway} delegando la ejecución del modelo de IA,
 * el prompt del sistema y las herramientas del MCP directamente en el Agente de IA autónomo.
 * <p>
 * Esto garantiza que el BFF se mantenga 100% desacoplado de las llamadas de IA síncronas
 * y de las dependencias directas del protocolo MCP.
 */
@Slf4j
@Component
public class ChatGatewayAdapter implements ChatGateway {

    private final WebClient webClient;

    public ChatGatewayAdapter(WebClient.Builder webClientBuilder,
            @Value("${agent.url:http://localhost:8082}") String agentUrl) {
        log.info("=== Initializing Desacoplado ChatGatewayAdapter ===");
        log.info("BFF delegará las interacciones de chat al Agente en: {}", agentUrl);
        this.webClient = webClientBuilder.baseUrl(agentUrl).build();
    }

    /**
     * Envía un mensaje al Agente Autónomo mediante protocolo A2A para que sea procesado
     * mediante su LLM y las herramientas MCP mapeadas.
     *
     * @param message prompt a enviar al modelo a través del Agente
     * @param contextId sesión sobre la cual agrupar o auditar
     * @return Mono con la respuesta limpia del Agente
     */
    @Override
    public Mono<String> sendMessage(String message, String contextId) {
        log.info("BFF delegando mensaje al Agente autónomo (ContextId: {})", contextId);

        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId(contextId)
                        .parts(List.of(Part.ofText(message)))
                        .build())
                .build();

        return webClient.post()
                .uri("/message:send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SendMessageResponse.class)
                .map(response -> {
                    if (response != null && response.getMessage() != null) {
                        String botResponse = response.getMessage().extractText();
                        log.debug("Respuesta recibida del Agente (longitud: {} chars)", botResponse.length());
                        return filterReasoning(botResponse);
                    }
                    return "";
                })
                .doOnError(error -> log.error("Fallo al delegar la interacción de chat al Agente", error));
    }

    private String filterReasoning(String response) {
        if (response == null) {
            return null;
        }
        String filtered = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (filtered.length() != response.length()) {
            log.info("Context block filtered out. Original length: {}, Filtered length: {}",
                    response.length(), filtered.length());
        }
        return filtered;
    }
}
