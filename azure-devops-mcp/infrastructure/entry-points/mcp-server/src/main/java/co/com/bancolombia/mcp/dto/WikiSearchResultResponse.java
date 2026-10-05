package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta para un resultado de búsqueda en la Wiki hacia el cliente MCP.
 */
public record WikiSearchResultResponse(
        String path,
        String wikiName,
        String summary,
        String url) {
}
