package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta para un resumen de Pipeline hacia el cliente MCP.
 */
public record PipelineSummaryResponse(
        Integer id,
        String name,
        String folder,
        Integer revision,
        String url) {
}
