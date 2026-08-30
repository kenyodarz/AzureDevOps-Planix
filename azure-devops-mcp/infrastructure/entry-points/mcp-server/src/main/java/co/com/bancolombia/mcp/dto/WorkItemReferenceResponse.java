package co.com.bancolombia.mcp.dto;

/**
 * DTO de <b>respuesta</b>: la referencia mínima a un elemento de trabajo —{@code id} y {@code url}—
 * que devuelve una consulta WIQL.
 *
 * <p>Es cada uno de los elementos del array {@code workItems} de {@link WiqlResultResponse}, o sea,
 * la carga útil real de {@code listWorkItemsByTeamAndSprint}. Nace en la Fase 07 (DP-07 §0.2(a)).
 *
 * <p>⚠️ <b>{@code id} y {@code url} son contrato público</b>, congelados por
 * {@code McpResponsePayloadCharacterizationTest}.
 */
public record WorkItemReferenceResponse(Integer id, String url) {
}

