package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.ComplexityEstimation;
import co.com.bancolombia.model.agent.EstimationSummary;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Flujo de Aprobación: el usuario confirma la propuesta y se genera la historia estructurada
 * siguiendo la plantilla corporativa.
 */
@RequiredArgsConstructor
public class ApprovalFlowHandler implements ChatFlowHandler {

    private static final String CONFIRM_CREATE_STORY =
            "\n\n¿Deseas registrar esta Historia con sus tareas desglosadas en Azure DevOps? "
                    + "Responde 'Crear' para proceder.";

    private final ChatGateway chatGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final String templateMarkdown;
    private final String agileGuideContent;

    @Override
    public AgentIntent supports() {
        return AgentIntent.APPROVAL;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String prompt = promptTemplatePort.render(PromptTemplateId.STRUCTURED_STORY, Map.of(
                PromptVariables.CORPORATE_TEMPLATE, templateMarkdown,
                PromptVariables.AGILE_GUIDE, agileGuideContent));
        return chatGateway.sendMessage(prompt, context.contextId())
                .map(llmResponse -> EstimationSummary.compose(llmResponse,
                        ComplexityEstimation.parseFrom(llmResponse), CONFIRM_CREATE_STORY));
    }
}

