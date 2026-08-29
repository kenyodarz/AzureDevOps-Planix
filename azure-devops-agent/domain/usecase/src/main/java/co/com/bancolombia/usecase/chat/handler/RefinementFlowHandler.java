package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.AzureDevOpsScope;
import co.com.bancolombia.model.agent.ComplexityEstimation;
import co.com.bancolombia.model.agent.CorporateKnowledge;
import co.com.bancolombia.model.agent.EstimationSummary;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Flujo de Refinamiento de una historia existente, identificada por {@code (ID: n)}.
 */
@RequiredArgsConstructor
public class RefinementFlowHandler implements ChatFlowHandler {

    private static final String CONFIRM_UPDATE_STORY =
            "\n\n¿Deseas actualizar el Work Item en Azure DevOps con esta versión refinada? "
                    + "Responde 'Aprobar' o 'Actualizar' para proceder.";

    private final ChatGateway chatGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final AzureDevOpsScope scope;
    private final CorporateKnowledge knowledge;

    @Override
    public AgentIntent supports() {
        return AgentIntent.REFINEMENT;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String prompt = promptTemplatePort.render(PromptTemplateId.STORY_REFINEMENT, Map.of(
                PromptVariables.WORK_ITEM_ID, context.requireWorkItemId(),
                PromptVariables.ORGANIZATION, scope.organization(),
                PromptVariables.PROJECT, scope.project(),
                PromptVariables.AGILE_GUIDE, knowledge.agileGuide()));
        return chatGateway.sendMessage(prompt, context.contextId())
                .map(llmResponse -> EstimationSummary.compose(llmResponse,
                        ComplexityEstimation.parseFrom(llmResponse), CONFIRM_UPDATE_STORY));
    }
}

