package co.com.bancolombia.mcp.dto;

import java.util.Map;

/**
 * DTO de <b>respuesta</b>: la relación de un elemento de trabajo tal y como viaja hacia el cliente
 * MCP.
 *
 * <p>Nace en la Fase 07 (DP-07 §0.2(a)) al cerrar la <b>frontera de salida hacia el cliente MCP</b>,
 * la última que quedaba abierta: la Fase 03 cerró la de entrada y la de salida hacia Azure DevOps,
 * pero el retorno seguía serializando {@code domain/model} tal cual ({@code CONTRATO-MCP.md} §3.4).
 *
 * <p>⚠️ <b>Los tres nombres de campo —{@code rel}, {@code url}, {@code attributes}— son contrato
 * público</b> y están congelados por {@code McpResponsePayloadCharacterizationTest}. Renombrar la
 * clase es seguro; renombrar un componente no lo es nunca.
 */
public record WorkItemRelationResponse(String rel, String url, Map<String, Object> attributes) {
}

