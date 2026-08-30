package co.com.bancolombia.agentclient;

import java.util.Map;

/**
 * Sobres del protocolo JSON-RPC 2.0 hacia el agente.
 *
 * <p>Son <b>detalle del adaptador</b>, no del dominio ni del entry-point. El BFF actúa como
 * cliente, de modo que estas clases modelan lo que se pone en el cable y nada más. Sustituyen a los
 * {@code JsonRpcRequest/Response/Error} que vivían en {@code reactive-web}, donde servían al rol
 * contrario —el de servidor— que el BFF ya no tiene.
 *
 * <p>Los códigos de error replican los que el agente declara en su {@code JsonRpcResponseFactory}
 * y marca como intocables.
 */
final class JsonRpcEnvelope {

    /**
     * Versión del protocolo declarada en cada sobre emitido.
     */
    static final String VERSION = "2.0";

    /**
     * El sobre no cumple la forma exigida por JSON-RPC 2.0.
     */
    static final int INVALID_REQUEST = -32600;
    /**
     * El método solicitado no existe en el agente.
     */
    static final int METHOD_NOT_FOUND = -32601;
    /**
     * El {@code params} enviado no es válido para el método invocado.
     */
    static final int INVALID_PARAMS = -32602;
    /**
     * Fallo no controlado durante la ejecución del método en el agente.
     */
    static final int INTERNAL_ERROR = -32603;
    /**
     * Código específico A2A: la tarea consultada no existe.
     */
    static final int TASK_NOT_FOUND = -32004;

    private JsonRpcEnvelope() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Construye la petición JSON-RPC 2.0 que viaja en el cuerpo del {@code POST /}.
     *
     * @param id     identificador de correlación de la petición
     * @param method método A2A a invocar
     * @param params carga útil del método
     * @return el mapa listo para serializar
     */
    static Map<String, Object> request(String id, String method, Map<String, Object> params) {
        return Map.of(
                "jsonrpc", VERSION,
                "id", id,
                "method", method,
                "params", params);
    }

    /**
     * Respuesta cruda del agente, tal como llega por el cable.
     */
    record Response(String jsonrpc, String id, Map<String, Object> result, Error error) {

        boolean hasError() {
            return error != null;
        }
    }

    /**
     * Bloque de error del sobre JSON-RPC.
     */
    record Error(int code, String message) {

    }
}

