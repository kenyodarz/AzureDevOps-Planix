package co.com.bancolombia.mcp.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO de <b>respuesta</b>: el elemento de trabajo tal y como viaja hacia el cliente MCP.
 *
 * <p>Es el retorno de {@code getWorkItem}, {@code createWorkItem}, {@code updateWorkItem} y —dentro
 * de una lista— {@code getWorkItemsBatch}. Nace en la Fase 07 (DP-07 §0.2(a)) al cerrar la frontera
 * de salida hacia el cliente MCP.
 *
 * <p><b>{@code fields} sigue siendo un mapa abierto — DP-07 §0.2(d).</b> Es lo más anémico del
 * repositorio, pero también lo que permite que el cliente pida <i>cualquier</i> campo de Azure
 * DevOps sin cambiar el servidor. Tiparlo habría cambiado la forma del JSON, que es precisamente lo
 * que esta fase no puede hacer.
 *
 * <p>⚠️ <b>Los cinco nombres de campo son contrato público</b> y están congelados por
 * {@code McpResponsePayloadCharacterizationTest}, incluida la emisión de <b>nulos</b>: un
 * {@code fields} nulo se sigue serializando como {@code null} y no como {@code {}}.
 */
public record WorkItemResponse(
        Integer id,
        Integer rev,
        Map<String, Object> fields,
        List<WorkItemRelationResponse> relations,
        String url) {
}

