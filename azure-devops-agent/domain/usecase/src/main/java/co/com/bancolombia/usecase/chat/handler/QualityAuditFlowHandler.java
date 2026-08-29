package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Flujo de Auditoría de Calidad de un Work Item existente.
 *
 * <p>Retira de la respuesta el bloque JSON técnico que el modelo delimita con
 * {@code AUDIT_JSON_START}/{@code AUDIT_JSON_END}: es información para máquinas, no para el usuario.
 */
@Log
@RequiredArgsConstructor
public class QualityAuditFlowHandler implements ChatFlowHandler {

    private static final Pattern AUDIT_JSON_BLOCK =
            Pattern.compile("(?s)AUDIT_JSON_START.*?AUDIT_JSON_END");

    private final ChatGateway chatGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final String defaultOrg;
    private final String defaultProject;
    private final String agileGuideContent;
    private final String qualityAuditContent;

    @Override
    public AgentIntent supports() {
        return AgentIntent.QUALITY_AUDIT;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String workItemId = context.requireWorkItemId();
        log.info("Ejecutando flujo de auditoría de calidad para Work Item ID: " + workItemId);
        String prompt = promptTemplatePort.render(PromptTemplateId.QUALITY_AUDIT, Map.of(
                PromptVariables.WORK_ITEM_ID, workItemId,
                PromptVariables.ORGANIZATION, defaultOrg,
                PromptVariables.PROJECT, defaultProject,
                PromptVariables.STANDARDS, agileGuideContent + "\n\n" + qualityAuditContent));
        return chatGateway.sendMessage(prompt, context.contextId())
                .map(llmResponse -> AUDIT_JSON_BLOCK.matcher(llmResponse).replaceAll("").trim());
    }
}

