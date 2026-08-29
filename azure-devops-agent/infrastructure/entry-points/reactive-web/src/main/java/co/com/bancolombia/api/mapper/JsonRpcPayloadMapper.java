package co.com.bancolombia.api.mapper;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.MessageSendConfiguration;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce el {@code params} crudo de una petición JSON-RPC 2.0 al modelo A2A del dominio.
 * <p>
 * Es deliberadamente tolerante con los payloads mal formados: ante un tipo inesperado lanza
 * {@link IllegalArgumentException} con un mensaje que el entry-point convierte en un error
 * JSON-RPC {@code -32602 (Invalid params)}. Esa tolerancia es parte del contrato público y no
 * debe endurecerse sin una decisión explícita.
 */
public final class JsonRpcPayloadMapper {

    private static final String PARAMS_MUST_BE_OBJECT = "params must be a JSON object";
    private static final String MESSAGE_KEY = "message";

    private JsonRpcPayloadMapper() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Convierte el {@code params} de {@code message/send} en una {@link SendMessageRequest}.
     *
     * @param paramsObject valor crudo deserializado del campo {@code params}
     * @return la petición A2A equivalente
     * @throws IllegalArgumentException si el payload no cumple la forma esperada
     */
    public static SendMessageRequest toSendMessageRequest(Object paramsObject) {
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

        return SendMessageRequest.builder()
                .message(message)
                .configuration(mapConfiguration(params.get("configuration")))
                .build();
    }

    /**
     * Extrae el identificador de tarea del {@code params} de {@code tasks/get} y
     * {@code tasks/cancel}.
     *
     * @param paramsObject valor crudo deserializado del campo {@code params}
     * @return el identificador de tarea, nunca vacío
     * @throws IllegalArgumentException si falta o está en blanco
     */
    public static String toTaskId(Object paramsObject) {
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

    private static MessageSendConfiguration mapConfiguration(Object configObject) {
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

    private static List<Part> asParts(Object partsObject) {
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

    private static boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalArgumentException("expected boolean value");
    }

    private static List<String> asStringList(Object value) {
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

    private static Map<String, Object> asMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("expected object for map field");
        }
        return toStringKeyedMap(rawMap);
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
}

