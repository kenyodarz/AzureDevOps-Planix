package co.com.bancolombia.usecase.chat.handler;

import static java.util.logging.Level.WARNING;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Flujo de Planificación: enriquece la idea del usuario con el documento Markdown de especificación
 * correspondiente antes de solicitarle al modelo la propuesta inicial.
 */
@Log
@RequiredArgsConstructor
public class PlanningDraftFlowHandler implements ChatFlowHandler {

    private static final Pattern FRONT_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final String DEFAULT_SPEC_FILE = "ideas_planning_q3.md";
    private static final String NO_SPEC_CONTEXT = "No hay contexto de planeación adicional.";

    private final ChatGateway chatGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final SpecStoragePort specStoragePort;

    @Override
    public AgentIntent supports() {
        return AgentIntent.PLANNING_DRAFT;
    }

    private static String normalizeFrontName(String frontName) {
        String replaced = NON_ALPHANUMERIC.matcher(frontName).replaceAll("_");
        int start = 0;
        while (start < replaced.length() && replaced.charAt(start) == '_') {
            start++;
        }
        int end = replaced.length();
        while (end > start && replaced.charAt(end - 1) == '_') {
            end--;
        }
        return replaced.substring(start, end);
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String userText = context.userText();
        String specFileName = resolveSpecFileName(userText);
        return specStoragePort.getSpec(specFileName)
                .map(SpecDocument::content)
                // Regla de negocio del flujo: si el spec no existe o falla su lectura,
                // la propuesta se genera de todos modos sin interrumpir la experiencia.
                .onErrorResume(error -> {
                    log.log(WARNING,
                            "No se pudo cargar el spec [{0}], continuando sin contexto adicional: {1}",
                            new Object[]{specFileName, error.getMessage()});
                    return Mono.just(NO_SPEC_CONTEXT);
                })
                .defaultIfEmpty(NO_SPEC_CONTEXT)
                .flatMap(content -> {
                    String effectiveContent =
                            (content == null || content.isBlank()) ? NO_SPEC_CONTEXT : content;
                    String prompt = promptTemplatePort.render(PromptTemplateId.PLANNING_DRAFT,
                            Map.of(PromptVariables.ORIGINAL_IDEA, userText,
                                    PromptVariables.RAG_CONTEXT, effectiveContent));
                    return chatGateway.sendMessage(prompt, context.contextId());
                });
    }

    private String resolveSpecFileName(String userText) {
        if (userText == null || userText.isBlank()) {
            return DEFAULT_SPEC_FILE;
        }
        Matcher matcher = FRONT_PATTERN.matcher(userText);
        if (matcher.find()) {
            String frontName = matcher.group(1).trim().toLowerCase();
            String normalized = normalizeFrontName(frontName);
            if (!normalized.isEmpty()) {
                return normalized + ".md";
            }
        }
        return DEFAULT_SPEC_FILE;
    }
}

