package co.com.bancolombia.mcp.dto;

import java.util.List;

/**
 * DTO de <b>respuesta</b>: el resultado de una consulta WIQL tal y como viaja hacia el cliente MCP.
 *
 * <p><b>Es el retorno de {@code listWorkItemsByTeamAndSprint}, la tool más usada del sistema.</b>
 * Por eso su forma es la más delicada de toda la Fase 07: hasta ahora la producía directamente
 * {@code co.com.bancolombia.model.workitem.WiqlResult}, un modelo de dominio
 * ({@code CONTRATO-MCP.md} §3.4).
 *
 * <p>⚠️ <b>Los cuatro nombres de campo —{@code queryType}, {@code queryResultType}, {@code asOf} y
 * {@code workItems}— son contrato público</b> y están congelados por
 * {@code McpResponsePayloadCharacterizationTest}.
 */
public record WiqlResultResponse(
        String queryType,
        String queryResultType,
        String asOf,
        List<WorkItemReferenceResponse> workItems) {
}

