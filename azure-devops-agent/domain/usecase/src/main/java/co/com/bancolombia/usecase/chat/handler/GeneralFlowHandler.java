package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Flujo General: el único que no usa plantilla. El texto del usuario viaja al modelo tal cual.
 */
@RequiredArgsConstructor
public class GeneralFlowHandler implements ChatFlowHandler {

    private final ChatGateway chatGateway;

    @Override
    public AgentIntent supports() {
        return AgentIntent.GENERAL;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        return chatGateway.sendMessage(context.userText(), context.contextId());
    }
}

