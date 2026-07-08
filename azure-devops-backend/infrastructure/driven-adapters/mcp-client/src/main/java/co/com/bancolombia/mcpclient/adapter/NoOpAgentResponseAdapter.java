package co.com.bancolombia.mcpclient.adapter;

import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class NoOpAgentResponseAdapter implements AgentResponseGateway {

    @Override
    public Mono<Void> sendResponse(SendMessageResponse response) {
        // No-Op: Kafka está deshabilitado en esta versión ágil del agente.
        return Mono.empty();
    }
}
