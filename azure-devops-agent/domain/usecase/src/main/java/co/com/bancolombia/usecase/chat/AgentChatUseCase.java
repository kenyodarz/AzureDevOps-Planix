package co.com.bancolombia.usecase.chat;

import static java.util.logging.Level.SEVERE;

import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.agent.IntentResolution;
import co.com.bancolombia.model.agent.IntentResolver;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.chat.handler.ChatFlowContext;
import co.com.bancolombia.usecase.chat.handler.ChatFlowDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Caso de uso que ejecuta la comunicación con el LLM a través de MCP tools.
 * <p>
 * Su responsabilidad se reduce a orquestar: validar el contenido, resolver la intención, despachar
 * al flujo correspondiente, envolver el resultado en la respuesta A2A y manejar el error en un
 * único lugar. El «qué hace» cada flujo vive en los {@code ChatFlowHandler} desde la Fase 04.
 * <p>
 * Soporta dos transportes:
 * <ul>
 *   <li><b>Kafka/async</b>: {@link #chat(SendMessageRequest)} – ejecuta y publica la respuesta vía gateway</li>
 *   <li><b>REST/sync</b>: {@link #chatAndRespond(SendMessageRequest)} – ejecuta y retorna directamente</li>
 * </ul>
 */
@Log
@RequiredArgsConstructor
public class AgentChatUseCase {

    private static final String NO_CONTENT = "No content provided";
    private static final String ERROR_EXECUTING_CHAT = "Error executing chat";

    private final AgentResponseGateway agentResponseGateway;
    private final TaskStoreGateway taskStoreGateway;
    private final IntentResolver intentResolver;
    private final ChatFlowDispatcher chatFlowDispatcher;


    public Mono<Void> chat(SendMessageRequest request) {
        return executeChat(request)
                .flatMap(response -> {
                    if (response.getTask() != null) {
                        return taskStoreGateway.save(response.getTask()).thenReturn(response);
                    }
                    return Mono.just(response);
                })
                .flatMap(agentResponseGateway::sendResponse);
    }

    public Mono<SendMessageResponse> chatAndRespond(SendMessageRequest request) {
        return executeChat(request);
    }

    /**
     * Valida el contenido, resuelve la intención en el dominio y despacha al flujo que corresponda.
     *
     * <p>El {@code onErrorResume} de este método es el <b>único</b> manejo genérico de error del
     * chat: los seis bloques idénticos que existían hasta la Fase 03 se unificaron aquí (D-02).
     */
    private Mono<SendMessageResponse> executeChat(SendMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String contextId = A2AResponseFactory.extractContextId(request);
        String userText = request != null && request.getMessage() != null
                ? request.getMessage().extractText()
                : null;

        if (userText == null || userText.trim().isEmpty()) {
            return Mono.just(A2AResponseFactory.success(request, NO_CONTENT, 0));
        }

        IntentResolution resolution = intentResolver.resolve(userText, contextId);
        return chatFlowDispatcher.dispatch(new ChatFlowContext(userText, contextId, resolution))
                .map(finalResponse -> A2AResponseFactory.success(request, finalResponse,
                        elapsedSince(startTime)))
                .onErrorResume(error -> {
                    log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                    return Mono.just(A2AResponseFactory.failure(request, elapsedSince(startTime),
                            error.getMessage()));
                });
    }

    private long elapsedSince(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
