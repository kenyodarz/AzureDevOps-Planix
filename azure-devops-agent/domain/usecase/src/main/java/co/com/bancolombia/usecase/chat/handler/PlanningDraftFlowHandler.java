package co.com.bancolombia.usecase.chat.handler;

import static java.util.logging.Level.SEVERE;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Flujo de Planificación: enriquece la idea del usuario con los fragmentos de planeación más
 * similares (RAG) antes de pedirle al modelo la propuesta inicial.
 */
@Log
@RequiredArgsConstructor
public class PlanningDraftFlowHandler implements ChatFlowHandler {

    /** Número de fragmentos de planeación que se recuperan del vector store. */
    private static final int RAG_MAX_RESULTS = 3;

    private static final String NO_RAG_CONTEXT = "No hay contexto de planeación adicional.";

    private final ChatGateway chatGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final PlanningVectorStorePort vectorStorePort;

    @Override
    public AgentIntent supports() {
        return AgentIntent.PLANNING_DRAFT;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String userText = context.userText();
        return vectorStorePort.searchSimilarity(userText, null, RAG_MAX_RESULTS)
                .collectList()
                // Regla de negocio del flujo, no manejo genérico de error: si el vector store cae,
                // la propuesta se genera igual, solo que sin contexto de planeación.
                .onErrorResume(error -> {
                    log.log(SEVERE, "Error searching planning vectors, continuing without context",
                            error);
                    return Mono.just(List.of());
                })
                .flatMap(chunks -> {
                    String prompt = promptTemplatePort.render(PromptTemplateId.PLANNING_DRAFT,
                            Map.of(PromptVariables.ORIGINAL_IDEA, userText,
                                    PromptVariables.RAG_CONTEXT, formatChunks(chunks)));
                    return chatGateway.sendMessage(prompt, context.contextId());
                });
    }

    private String formatChunks(List<PlanningChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return NO_RAG_CONTEXT;
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (PlanningChunk chunk : chunks) {
            contextBuilder.append("- Sección: ").append(chunk.sectionName()).append("\n")
                    .append("  Contenido: ").append(chunk.content()).append("\n\n");
        }
        return contextBuilder.toString();
    }
}

