package co.com.bancolombia.api;

import co.com.bancolombia.model.a2a.AgentCapabilities;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.AgentInterface;
import co.com.bancolombia.model.a2a.AgentProvider;
import co.com.bancolombia.model.a2a.AgentSkill;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.MessageSendConfiguration;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Manejador HTTP del agente consumidor A2A.
 * <p>
 * Implementa el protocolo JSON-RPC 2.0 para comunicación A2A y expone:
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

    private static final String JSONRPC_VERSION = "2.0";
    private static final String INVALID_PARAMS_PREFIX = "Invalid params: ";
    private static final int JSON_RPC_PARSE_ERROR = -32700;
    private static final int JSON_RPC_INVALID_REQUEST = -32600;
    private static final int JSON_RPC_METHOD_NOT_FOUND = -32601;
    private static final int JSON_RPC_INVALID_PARAMS = -32602;
    private static final int JSON_RPC_INTERNAL_ERROR = -32603;
    private static final int JSON_RPC_TASK_NOT_FOUND = -32004;

    private static final String PARAMS_MUST_BE_OBJECT = "params must be a JSON object";
    private static final String TASK_NOT_FOUND_PREFIX = "Task not found: ";
    private static final String SCHEMA_TYPE = "type";
    private static final String SCHEMA_TYPE_STRING = "string";
    private static final String SCHEMA_DESCRIPTION = "description";
    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_KEY = "error";
    private static final String ROLE_AGENT = "agent";

    private final AgentChatUseCase agentChatUseCase;
    private final String legacySunsetDate;
    private final String legacySuccessorUrl;
    private final co.com.bancolombia.model.chat.gateways.TaskStoreGateway taskStoreGateway;
    private final JsonMapper jsonMapper;

    public Handler(AgentChatUseCase agentChatUseCase,
            co.com.bancolombia.model.chat.gateways.TaskStoreGateway taskStoreGateway,
            @Value("${a2a.legacy.sunset-date:2026-06-30}") String legacySunsetDate,
            @Value("${a2a.legacy.successor-url:/}") String legacySuccessorUrl,
            JsonMapper jsonMapper) {
        this.agentChatUseCase = agentChatUseCase;
        this.taskStoreGateway = taskStoreGateway;
        this.legacySunsetDate = legacySunsetDate;
        this.legacySuccessorUrl = legacySuccessorUrl;
        this.jsonMapper = jsonMapper;
    }

    private static String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static Integer asInteger(Object value) {
        return switch (value) {
            case null -> null;
            case Integer i -> i;
            case Number n -> n.intValue();
            default -> throw new IllegalArgumentException("expected integer value");
        };
    }

    private static Map<String, Object> toStringKeyedMap(Map<?, ?> rawMap) {
        Map<String, Object> result = LinkedHashMap.newLinkedHashMap(rawMap.size());
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    public Mono<ServerResponse> handleJsonRpc(ServerRequest serverRequest) {
        log.info("A2A POST / JSON-RPC command received");

        return serverRequest.bodyToMono(JsonRpcRequest.class)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("JSON-RPC body is required")))
                .flatMap(this::dispatchJsonRpc)
                .flatMap(rpcResponse -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(rpcResponse))
                .onErrorResume(this::mapEnvelopeErrorToResponse);
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

    private ServerResponse.BodyBuilder withDeprecationHeaders(ServerResponse.BodyBuilder builder) {
        return builder
                .header("Deprecation", "true")
                .header("Sunset", legacySunsetDate)
                .header("Link", "<" + legacySuccessorUrl + ">; rel=\"successor-version\"");
    }

    private Mono<ServerResponse> mapEnvelopeErrorToResponse(Throwable error) {
        int code = isParseError(error) ? JSON_RPC_PARSE_ERROR : JSON_RPC_INVALID_REQUEST;
        String errorMessage = error != null && error.getMessage() != null
                ? error.getMessage() : "unknown";
        String message = code == JSON_RPC_PARSE_ERROR
                ? "Parse error"
                : "Invalid Request: " + errorMessage;

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonRpcError(null, code, message));
    }

    private boolean isParseError(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof DecodingException
                    || cursor instanceof ServerWebInputException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private Mono<JsonRpcResponse> dispatchJsonRpc(JsonRpcRequest rpcRequest) {
        if (rpcRequest == null || !JSONRPC_VERSION.equals(rpcRequest.getJsonrpc())) {
            return Mono.just(jsonRpcError(safeId(rpcRequest), JSON_RPC_INVALID_REQUEST,
                    "Invalid Request: jsonrpc must be '2.0'"));
        }

        if (rpcRequest.getMethod() == null || rpcRequest.getMethod().isBlank()) {
            return Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_INVALID_REQUEST,
                    "Invalid Request: method is required"));
        }

        return switch (rpcRequest.getMethod()) {
            case "message/send" -> dispatchSendMessage(rpcRequest);
            case "tasks/get" -> dispatchGetTask(rpcRequest);
            case "tasks/cancel" -> dispatchCancelTask(rpcRequest);
            default -> Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_METHOD_NOT_FOUND,
                    "Method not found: " + rpcRequest.getMethod()));
        };
    }

    private Mono<JsonRpcResponse> dispatchSendMessage(JsonRpcRequest rpcRequest) {
        if (rpcRequest.getParams() == null) {
            return Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_INVALID_PARAMS,
                    INVALID_PARAMS_PREFIX + "params is required"));
        }

        SendMessageRequest sendMessageRequest;
        try {
            sendMessageRequest = mapSendMessageRequest(rpcRequest.getParams());
        } catch (IllegalArgumentException ex) {
            return Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_INVALID_PARAMS,
                    INVALID_PARAMS_PREFIX + ex.getMessage()));
        }

        return agentChatUseCase.chatAndRespond(sendMessageRequest)
                .flatMap(sendMessageResponse -> {
                    if (sendMessageResponse.getTask() != null
                            && sendMessageResponse.getTask().getId() != null) {
                        return taskStoreGateway.save(sendMessageResponse.getTask())
                                .thenReturn(
                                        jsonRpcSuccess(rpcRequest.getId(), sendMessageResponse));
                    }
                    return Mono.just(jsonRpcSuccess(rpcRequest.getId(), sendMessageResponse));
                })
                .onErrorResume(
                        ex -> Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_INTERNAL_ERROR,
                                "Internal error: " + ex.getMessage())));
    }

    private Mono<JsonRpcResponse> dispatchGetTask(JsonRpcRequest rpcRequest) {
        return handleTaskOperation(rpcRequest,
                task -> Mono.just(jsonRpcSuccessWithTask(rpcRequest.getId(), task)));
    }

    private Mono<JsonRpcResponse> dispatchCancelTask(JsonRpcRequest rpcRequest) {
        return handleTaskOperation(rpcRequest, task -> {
            Task canceledTask = cancelTask(task);
            return taskStoreGateway.save(canceledTask)
                    .map(savedTask -> jsonRpcSuccessWithTask(rpcRequest.getId(), savedTask));
        });
    }

    private Mono<JsonRpcResponse> handleTaskOperation(JsonRpcRequest rpcRequest,
            Function<Task, Mono<JsonRpcResponse>> operation) {
        String taskId;
        try {
            taskId = mapTaskId(rpcRequest.getParams());
        } catch (IllegalArgumentException ex) {
            return Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_INVALID_PARAMS,
                    INVALID_PARAMS_PREFIX + ex.getMessage()));
        }
        return taskStoreGateway.findById(taskId)
                .flatMap(operation)
                .switchIfEmpty(Mono.defer(
                        () -> Mono.just(jsonRpcError(rpcRequest.getId(), JSON_RPC_TASK_NOT_FOUND,
                                TASK_NOT_FOUND_PREFIX + taskId))));
    }

    private String mapTaskId(Object paramsObject) {
        if (!(paramsObject instanceof Map<?, ?> rawParams)) {
            throw new IllegalArgumentException(PARAMS_MUST_BE_OBJECT);
        }
        Map<String, Object> params = toStringKeyedMap(rawParams);
        String taskId = asString(params.get("taskId"));
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId is required");
        }
        return taskId;
    }

    private JsonRpcResponse jsonRpcSuccessWithTask(String id, Task task) {
        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        return JsonRpcResponse.builder()
                .jsonrpc(JSONRPC_VERSION)
                .id(id)
                .result(result)
                .build();
    }

    private JsonRpcResponse jsonRpcSuccess(String id, SendMessageResponse sendMessageResponse) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (sendMessageResponse != null && sendMessageResponse.getTask() != null) {
            result.put("task", sendMessageResponse.getTask());
        }
        if (sendMessageResponse != null && sendMessageResponse.getMessage() != null) {
            result.put(MESSAGE_KEY, sendMessageResponse.getMessage());
        }

        return JsonRpcResponse.builder()
                .jsonrpc(JSONRPC_VERSION)
                .id(id)
                .result(result)
                .build();
    }

    private JsonRpcResponse jsonRpcError(String id, int code, String message) {
        return JsonRpcResponse.builder()
                .jsonrpc(JSONRPC_VERSION)
                .id(id)
                .error(JsonRpcError.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    private String safeId(JsonRpcRequest request) {
        return request != null ? request.getId() : null;
    }

    private SendMessageRequest mapSendMessageRequest(Object paramsObject) {
        if (!(paramsObject instanceof Map<?, ?> rawParams)) {
            throw new IllegalArgumentException(PARAMS_MUST_BE_OBJECT);
        }

        Map<String, Object> params = toStringKeyedMap(rawParams);
        Object messageObject = params.get(MESSAGE_KEY);
        if (!(messageObject instanceof Map<?, ?> rawMessage)) {
            throw new IllegalArgumentException("message is required and must be an object");
        }

        Map<String, Object> messageMap = toStringKeyedMap(rawMessage);
        Message message = Message.builder()
                .role(asString(messageMap.get("role")))
                .messageId(asString(messageMap.get("messageId")))
                .contextId(asString(messageMap.get("contextId")))
                .referenceTaskIds(asStringList(messageMap.get("referenceTaskIds")))
                .parts(asParts(messageMap.get("parts")))
                .build();

        MessageSendConfiguration configuration = mapConfiguration(params.get("configuration"));

        return SendMessageRequest.builder()
                .message(message)
                .configuration(configuration)
                .build();
    }

    private MessageSendConfiguration mapConfiguration(Object configObject) {
        if (configObject == null) {
            return null;
        }
        if (!(configObject instanceof Map<?, ?> rawConfig)) {
            throw new IllegalArgumentException("configuration must be an object when present");
        }

        Map<String, Object> configMap = toStringKeyedMap(rawConfig);
        return MessageSendConfiguration.builder()
                .acceptedOutputModes(asStringList(configMap.get("acceptedOutputModes")))
                .historyLength(asInteger(configMap.get("historyLength")))
                .blocking(asBoolean(configMap.get("blocking")))
                .build();
    }

    private List<Part> asParts(Object partsObject) {
        if (!(partsObject instanceof List<?> rawParts)) {
            throw new IllegalArgumentException("message.parts is required and must be an array");
        }

        List<Part> parts = new ArrayList<>();
        for (Object item : rawParts) {
            if (!(item instanceof Map<?, ?> rawPart)) {
                throw new IllegalArgumentException("each message part must be an object");
            }
            Map<String, Object> partMap = toStringKeyedMap(rawPart);
            parts.add(Part.builder()
                    .text(asString(partMap.get("text")))
                    .data(asMap(partMap.get("data")))
                    .url(asString(partMap.get("url")))
                    .mediaType(asString(partMap.get("mediaType")))
                    .filename(asString(partMap.get("filename")))
                    .metadata(asMap(partMap.get("metadata")))
                    .build());
        }
        return parts;
    }

    private boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalArgumentException("expected boolean value");
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("expected array value");
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(asString(item));
        }
        return result;
    }

    private Map<String, Object> asMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("expected object for map field");
        }
        return toStringKeyedMap(rawMap);
    }

    private Task cancelTask(Task task) {
        TaskStatus canceledStatus = TaskStatus.builder()
                .state(TaskState.CANCELED)
                .timestamp(Instant.now().toString())
                .build();
        return task.toBuilder()
                .status(canceledStatus)
                .build();
    }

    public Mono<ServerResponse> handleAgentCard() {
        AgentCard card = AgentCard.builder()
                .protocolVersion("1.0")
                .name("Financial Consumer Agent")
                .version("1.0.0")
                .description(
                        "Agente integrador A2A para consulta de clientes y términos financieros")
                .provider(AgentProvider.builder()
                        .organization("Bancolombia")
                        .url("https://www.bancolombia.com")
                        .build())
                .supportedInterfaces(List.of(
                        AgentInterface.builder()
                                .protocol("jsonrpc")
                                .url("http://localhost:8082")
                                .build()))
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text", "data"))
                .defaultOutputModes(List.of("text", "data"))
                .skills(List.of(
                        AgentSkill.builder()
                                .id("search-clients-terms")
                                .name("Búsqueda de clientes y términos")
                                .description(
                                        "Busca información de clientes y términos por nacionalidad")
                                .tags(List.of("finance", "clients", "terms"))
                                .inputModes(List.of("text", "data"))
                                .outputModes(List.of("text", "data"))
                                .schema(Map.of(
                                        SCHEMA_TYPE, "object",
                                        "properties", Map.of(
                                                "name", Map.of(SCHEMA_TYPE, SCHEMA_TYPE_STRING,
                                                        SCHEMA_DESCRIPTION,
                                                        "Nombre o parte del nombre del cliente"),
                                                "documentType",
                                                Map.of(SCHEMA_TYPE, SCHEMA_TYPE_STRING,
                                                        SCHEMA_DESCRIPTION,
                                                        "Tipo de documento (CC, NIT, CIP, SSN, etc.)"),
                                                "documentNumber",
                                                Map.of(SCHEMA_TYPE, SCHEMA_TYPE_STRING,
                                                        SCHEMA_DESCRIPTION,
                                                        "Número de documento de identidad")
                                        ),
                                        "required",
                                        List.of("name", "documentType", "documentNumber")
                                 ))
                                .examples(List.of(
                                        "Busca el cliente con CC 12345 y dame sus términos",
                                        "Cuales son las condiciones para clientes colombianos?"))
                                .build()))
                .securitySchemes(Map.of(
                        "oauth2", Map.of(
                                "type", "oauth2",
                                "flows", Map.of(
                                        "clientCredentials", Map.of(
                                                "tokenUrl",
                                                "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token",
                                                "scopes", Map.of("api://{client-id}/.default",
                                                        "Access to agent"))))))
                .tags(List.of("finance", "a2a", "clients", "terms"))
                .build();

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(card);
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
}
