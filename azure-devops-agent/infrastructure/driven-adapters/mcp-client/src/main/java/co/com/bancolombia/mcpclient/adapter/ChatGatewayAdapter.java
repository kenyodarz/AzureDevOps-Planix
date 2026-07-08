package co.com.bancolombia.mcpclient.adapter;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adaptador que implementa {@link ChatGateway} usando Spring AI {@link ChatClient}.
 * <p>
 * Envía mensajes al modelo de IA configurado (OpenAI-compatible) y filtra bloques de razonamiento
 * ({@code <think>...</think>}) de la respuesta.
 * Recupera e inyecta dinámicamente las herramientas de los clientes MCP activos en cada llamada.
 *
 * @see ChatGateway
 * @see ChatClient
 */
@Slf4j
@Component
public class ChatGatewayAdapter implements ChatGateway {

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final List<McpSyncClient> mcpClients;
    private final List<ToolCallback> mcpTools = new java.util.ArrayList<>();
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(8)
            .build();

    public ChatGatewayAdapter(ChatClient.Builder chatClientBuilder,
            @Value("${agent.system-prompt}") String systemPrompt,
            List<McpSyncClient> mcpClients) {
        log.info("=== Initializing ChatGatewayAdapter ===");
        this.chatClient = chatClientBuilder.build();
        this.systemPrompt = systemPrompt;
        this.mcpClients = mcpClients != null ? mcpClients : List.of();
        log.info("=== ChatClient configured. Found {} MCP clients ===", this.mcpClients.size());
        initializeMcpTools();
    }

    private void initializeMcpTools() {
        mcpTools.clear();
        for (McpSyncClient mcpClient : mcpClients) {
            try {
                log.info("Recuperando herramientas del MCP Client al arranque...");
                var provider = SyncMcpToolCallbackProvider.builder()
                        .mcpClients(mcpClient)
                        .build();
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null && callbacks.length > 0) {
                    mcpTools.addAll(List.of(callbacks));
                }
            } catch (Exception e) {
                log.warn(
                        "⚠️ No se pudieron registrar herramientas del MCP en el arranque (servidor offline o error): {}",
                        e.getMessage());
            }
        }
        log.info("✅ Cargadas {} herramientas del MCP exitosamente al inicio", mcpTools.size());
    }

    /**
     * Envía un mensaje al modelo de IA y retorna la respuesta filtrada.
     * <p>
     * Recupera en caliente las herramientas del servidor MCP e inyecta las mismas en el Prompt.
     * Si el servidor está caído, la llamada continúa de forma segura sin herramientas (no hay fallos en cascada).
     *
     * @param message texto del prompt a enviar al modelo
     * @return {@link Mono} con la respuesta del modelo, sin bloques de razonamiento
     * @throws RuntimeException si ocurre un error de comunicación con el modelo
     */
    @Override
    public Mono<String> sendMessage(String message, String contextId) {
        log.info("Received message for context {}: {}", contextId, message);
        return Mono.fromCallable(() -> {
                    log.debug("Building chat prompt with user message");

                    var promptSpec = chatClient.prompt()
                            .system(systemPrompt)
                            .user(message)
                            .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                            .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id",
                                    contextId));

                    if (!mcpTools.isEmpty()) {
                        promptSpec.tools(mcpTools);
                    }

                    String response = promptSpec.call().content();

                    log.info("Received response from AI model (length: {} chars)",
                            response != null ? response.length() : 0);
                    log.debug("Response content: {}", response);
                    return filterReasoning(response);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> log.error("Error during chat interaction", error));
    }


    private String filterReasoning(String response) {
        if (response == null) {
            return null;
        }
        // Remove <think>...</think> blocks (including newlines) using DOTALL mode (?s)
        String filtered = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (filtered.length() != response.length()) {
            log.info("Reasoning block filtered out. Original length: {}, Filtered length: {}",
                    response.length(), filtered.length());
        }
        return filtered;
    }
}
