package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta para la visión general de un proyecto hacia el cliente MCP.
 */
public record ProjectOverviewResponse(
        String id,
        String name,
        String description,
        String state,
        String visibility,
        String url) {
}
