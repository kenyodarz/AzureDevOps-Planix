package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta para una página de Wiki hacia el cliente MCP.
 */
public record WikiPageResponse(
        Integer id,
        String path,
        String content,
        String remoteUrl,
        String version) {
}
