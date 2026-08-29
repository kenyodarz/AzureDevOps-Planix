package co.com.bancolombia.api;

import co.com.bancolombia.api.mapper.JsonRpcResponseFactory;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Manejador HTTP del agente consumidor A2A.
 * <p>
 * Su única responsabilidad es traducir entre el transporte HTTP y el dominio: recibe el cuerpo,
 * delega y publica la respuesta. El enrutamiento de métodos vive en {@link JsonRpcDispatcher},
 * el mapeo de payloads en {@code JsonRpcPayloadMapper}, la forma de los sobres en
 * {@link JsonRpcResponseFactory} y la tarjeta de capacidades en {@link AgentCardProvider}.
 * <ul>
 *   <li>{@code POST /} – JSON-RPC 2.0 (métodos: message/send, tasks/get, tasks/cancel)</li>
 *   <li>{@code POST /message:send} – endpoint legacy REST (deprecated)</li>
 *   <li>{@code GET /.well-known/agent-card.json} – descubrimiento de capacidades del agente</li>
 * </ul>
 *
 * @see AgentChatUseCase
 */
@Log4j2
@Component
public class Handler {

    private static final String ERROR_KEY = "error";

    private final AgentChatUseCase agentChatUseCase;
    private final TaskStoreGateway taskStoreGateway;
    private final JsonRpcDispatcher jsonRpcDispatcher;
    private final String legacySunsetDate;
    private final String legacySuccessorUrl;

    public Handler(AgentChatUseCase agentChatUseCase,
            TaskStoreGateway taskStoreGateway,
            @Value("${a2a.legacy.sunset-date:2026-06-30}") String legacySunsetDate,
            @Value("${a2a.legacy.successor-url:/}") String legacySuccessorUrl) {
        this.agentChatUseCase = agentChatUseCase;
        this.taskStoreGateway = taskStoreGateway;
        this.jsonRpcDispatcher = new JsonRpcDispatcher(agentChatUseCase, taskStoreGateway);
        this.legacySunsetDate = legacySunsetDate;
        this.legacySuccessorUrl = legacySuccessorUrl;
    }

    public Mono<ServerResponse> handleJsonRpc(ServerRequest serverRequest) {
        log.info("A2A POST / JSON-RPC command received");

        return serverRequest.bodyToMono(JsonRpcRequest.class)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("JSON-RPC body is required")))
                .flatMap(jsonRpcDispatcher::dispatch)
                .flatMap(this::okJson)
                .onErrorResume(error -> okJson(JsonRpcResponseFactory.fromEnvelopeError(error)));
    }

    public Mono<ServerResponse> handleSendMessage(ServerRequest serverRequest) {
        log.info("A2A POST /message:send command received");

        return serverRequest.bodyToMono(SendMessageRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Request body is required")))
                .flatMap(agentChatUseCase::chatAndRespond)
                .flatMap(response -> withDeprecationHeaders(ServerResponse.ok())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(e -> {
                    log.error("Error processing A2A REST command", e);
                    return withDeprecationHeaders(ServerResponse.badRequest())
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue("{\"error\":\"" + e.getMessage() + "\"}");
                });
    }

    public Mono<ServerResponse> handleAgentCard() {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(AgentCardProvider.agentCard());
    }

    public Mono<ServerResponse> handleListTasks() {
        return taskStoreGateway.findAll()
                .collectList()
                .flatMap(tasks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(tasks))
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    private Mono<ServerResponse> okJson(JsonRpcResponse rpcResponse) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rpcResponse);
    }

    private ServerResponse.BodyBuilder withDeprecationHeaders(ServerResponse.BodyBuilder builder) {
        return builder
                .header("Deprecation", "true")
                .header("Sunset", legacySunsetDate)
                .header("Link", "<" + legacySuccessorUrl + ">; rel=\"successor-version\"");
    }
}

