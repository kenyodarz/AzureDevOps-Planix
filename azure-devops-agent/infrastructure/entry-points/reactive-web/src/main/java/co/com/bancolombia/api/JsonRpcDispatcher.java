package co.com.bancolombia.api;

import co.com.bancolombia.api.mapper.JsonRpcPayloadMapper;
import co.com.bancolombia.api.mapper.JsonRpcResponseFactory;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.time.Instant;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * Despacha los métodos JSON-RPC 2.0 del agente hacia el dominio.
 * <p>
 * Colaborador interno de {@link Handler}: no es un bean de Spring, se construye con las
 * dependencias que el handler ya recibe por constructor. Separa el enrutamiento de métodos
 * del transporte HTTP.
 */
class JsonRpcDispatcher {

    private static final String INVALID_PARAMS_PREFIX = "Invalid params: ";
    private static final String TASK_NOT_FOUND_PREFIX = "Task not found: ";

    private final AgentChatUseCase agentChatUseCase;
    private final TaskStoreGateway taskStoreGateway;

    JsonRpcDispatcher(AgentChatUseCase agentChatUseCase, TaskStoreGateway taskStoreGateway) {
        this.agentChatUseCase = agentChatUseCase;
        this.taskStoreGateway = taskStoreGateway;
    }

    Mono<JsonRpcResponse> dispatch(JsonRpcRequest rpcRequest) {
        if (rpcRequest == null
                || !JsonRpcResponseFactory.JSONRPC_VERSION.equals(rpcRequest.getJsonrpc())) {
            return Mono.just(JsonRpcResponseFactory.error(JsonRpcResponseFactory.safeId(rpcRequest),
                    JsonRpcResponseFactory.INVALID_REQUEST,
                    "Invalid Request: jsonrpc must be '2.0'"));
        }
        if (rpcRequest.getMethod() == null || rpcRequest.getMethod().isBlank()) {
            return Mono.just(JsonRpcResponseFactory.error(rpcRequest.getId(),
                    JsonRpcResponseFactory.INVALID_REQUEST, "Invalid Request: method is required"));
        }

        return switch (rpcRequest.getMethod()) {
            case "message/send" -> sendMessage(rpcRequest);
            case "tasks/get" -> getTask(rpcRequest);
            case "tasks/cancel" -> cancelTask(rpcRequest);
            default -> Mono.just(JsonRpcResponseFactory.error(rpcRequest.getId(),
                    JsonRpcResponseFactory.METHOD_NOT_FOUND,
                    "Method not found: " + rpcRequest.getMethod()));
        };
    }

    private Mono<JsonRpcResponse> sendMessage(JsonRpcRequest rpcRequest) {
        if (rpcRequest.getParams() == null) {
            return Mono.just(invalidParams(rpcRequest, "params is required"));
        }

        SendMessageRequest sendMessageRequest;
        try {
            sendMessageRequest = JsonRpcPayloadMapper.toSendMessageRequest(rpcRequest.getParams());
        } catch (IllegalArgumentException ex) {
            return Mono.just(invalidParams(rpcRequest, ex.getMessage()));
        }

        return agentChatUseCase.chatAndRespond(sendMessageRequest)
                .flatMap(response -> persistTaskIfPresent(response.getTask())
                        .thenReturn(JsonRpcResponseFactory.success(rpcRequest.getId(), response)))
                .onErrorResume(ex -> Mono.just(JsonRpcResponseFactory.error(rpcRequest.getId(),
                        JsonRpcResponseFactory.INTERNAL_ERROR,
                        "Internal error: " + ex.getMessage())));
    }

    private Mono<?> persistTaskIfPresent(Task task) {
        if (task != null && task.getId() != null) {
            return taskStoreGateway.save(task);
        }
        return Mono.empty();
    }

    private Mono<JsonRpcResponse> getTask(JsonRpcRequest rpcRequest) {
        return onTask(rpcRequest, task -> Mono.just(
                JsonRpcResponseFactory.successWithTask(rpcRequest.getId(), task)));
    }

    private Mono<JsonRpcResponse> cancelTask(JsonRpcRequest rpcRequest) {
        return onTask(rpcRequest, task -> taskStoreGateway.save(canceled(task))
                .map(savedTask -> JsonRpcResponseFactory.successWithTask(rpcRequest.getId(),
                        savedTask)));
    }

    private Mono<JsonRpcResponse> onTask(JsonRpcRequest rpcRequest,
            Function<Task, Mono<JsonRpcResponse>> operation) {
        String taskId;
        try {
            taskId = JsonRpcPayloadMapper.toTaskId(rpcRequest.getParams());
        } catch (IllegalArgumentException ex) {
            return Mono.just(invalidParams(rpcRequest, ex.getMessage()));
        }
        return taskStoreGateway.findById(taskId)
                .flatMap(operation)
                .switchIfEmpty(Mono.defer(() -> Mono.just(
                        JsonRpcResponseFactory.error(rpcRequest.getId(),
                                JsonRpcResponseFactory.TASK_NOT_FOUND,
                                TASK_NOT_FOUND_PREFIX + taskId))));
    }

    private JsonRpcResponse invalidParams(JsonRpcRequest rpcRequest, String detail) {
        return JsonRpcResponseFactory.error(rpcRequest.getId(),
                JsonRpcResponseFactory.INVALID_PARAMS, INVALID_PARAMS_PREFIX + detail);
    }

    private Task canceled(Task task) {
        return task.toBuilder()
                .status(TaskStatus.builder()
                        .state(TaskState.CANCELED)
                        .timestamp(Instant.now().toString())
                        .build())
                .build();
    }
}

