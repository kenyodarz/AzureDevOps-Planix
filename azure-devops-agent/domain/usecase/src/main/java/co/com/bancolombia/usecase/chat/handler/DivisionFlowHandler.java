package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.CorporateKnowledge;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Flujo de División: desglosa una historia que supera el máximo de Story Points admitido.
 */
@RequiredArgsConstructor
public class DivisionFlowHandler implements ChatFlowHandler {

    private static final String CONFIRM_CREATE_DIVIDED_STORIES =
            "\n\n¿Deseas proceder con el registro de estas historias divididas en Azure DevOps? "
                    + "Responde 'Crear' para registrar todas las historias con sus tareas "
                    + "correspondientes.";

    private final ChatGateway chatGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final CorporateKnowledge knowledge;

    @Override
    public AgentIntent supports() {
        return AgentIntent.DIVISION;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String prompt = promptTemplatePort.render(PromptTemplateId.STORY_DIVISION,
                Map.of(PromptVariables.AGILE_GUIDE, knowledge.agileGuide()));
        return chatGateway.sendMessage(prompt, context.contextId())
                .map(llmResponse -> llmResponse + CONFIRM_CREATE_DIVIDED_STORIES);
    }
}

