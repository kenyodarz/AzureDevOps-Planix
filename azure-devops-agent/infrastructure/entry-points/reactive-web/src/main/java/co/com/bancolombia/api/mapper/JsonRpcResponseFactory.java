package co.com.bancolombia.api.mapper;

import co.com.bancolombia.api.JsonRpcError;
import co.com.bancolombia.api.JsonRpcRequest;
import co.com.bancolombia.api.JsonRpcResponse;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.server.ServerWebInputException;

/**
 * Construye los sobres de respuesta JSON-RPC 2.0 del agente.
 * <p>
 * Concentra además los códigos de error del protocolo, que antes viajaban como literales
 * numéricos dentro del entry-point. Los valores son los del contrato vigente y están cubiertos
 * por {@code JsonRpcErrorContractTest}: no deben cambiarse.
 */
public final class JsonRpcResponseFactory {

    /** Versión del protocolo declarada en todo sobre emitido. */
    public static final String JSONRPC_VERSION = "2.0";

    /** JSON inválido: el cuerpo no pudo deserializarse. */
    public static final int PARSE_ERROR = -32700;
    /** El sobre no cumple la forma exigida por JSON-RPC 2.0. */
    public static final int INVALID_REQUEST = -32600;
    /** El método solicitado no existe en el agente. */
    public static final int METHOD_NOT_FOUND = -32601;
    /** El {@code params} recibido no es válido para el método invocado. */
    public static final int INVALID_PARAMS = -32602;
    /** Fallo no controlado durante la ejecución del método. */
    public static final int INTERNAL_ERROR = -32603;
    /** Código específico A2A: la tarea consultada no existe. */
    public static final int TASK_NOT_FOUND = -32004;

    private static final String MESSAGE_KEY = "message";
    private static final String TASK_KEY = "task";

    private JsonRpcResponseFactory() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Sobre de éxito de {@code message/send}.
     *
     * @param id identificador de la petición original
     * @param sendMessageResponse respuesta del caso de uso, puede ser nula
     * @return el sobre JSON-RPC con {@code result}
     */
    public static JsonRpcResponse success(String id, SendMessageResponse sendMessageResponse) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (sendMessageResponse != null && sendMessageResponse.getTask() != null) {
            result.put(TASK_KEY, sendMessageResponse.getTask());
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

    /**
     * Sobre de éxito de {@code tasks/get} y {@code tasks/cancel}.
     *
     * @param id identificador de la petición original
     * @param task tarea a publicar en el resultado
     * @return el sobre JSON-RPC con {@code result.task}
     */
    public static JsonRpcResponse successWithTask(String id, Task task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(TASK_KEY, task);
        return JsonRpcResponse.builder()
                .jsonrpc(JSONRPC_VERSION)
                .id(id)
                .result(result)
                .build();
    }

    /**
     * Sobre de error.
     *
     * @param id identificador de la petición original, puede ser nulo
     * @param code código de error JSON-RPC
     * @param message descripción legible del error
     * @return el sobre JSON-RPC con {@code error}
     */
    public static JsonRpcResponse error(String id, int code, String message) {
        return JsonRpcResponse.builder()
                .jsonrpc(JSONRPC_VERSION)
                .id(id)
                .error(JsonRpcError.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    /**
     * Traduce un fallo ocurrido antes de poder interpretar el sobre —cuerpo ausente o JSON
     * malformado— al error JSON-RPC correspondiente.
     *
     * @param error causa del fallo, puede ser nula
     * @return sobre de error sin {@code id}, porque aún no se conocía
     */
    public static JsonRpcResponse fromEnvelopeError(Throwable error) {
        int code = isParseError(error) ? PARSE_ERROR : INVALID_REQUEST;
        String errorMessage = error != null && error.getMessage() != null
                ? error.getMessage() : "unknown";
        String message = code == PARSE_ERROR
                ? "Parse error"
                : "Invalid Request: " + errorMessage;
        return error(null, code, message);
    }

    /**
     * Obtiene el {@code id} de una petición que puede no haberse podido construir.
     *
     * @param request petición JSON-RPC, puede ser nula
     * @return el identificador o {@code null} si no hay petición
     */
    public static String safeId(JsonRpcRequest request) {
        return request != null ? request.getId() : null;
    }

    private static boolean isParseError(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof DecodingException || cursor instanceof ServerWebInputException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}

