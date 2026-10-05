package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta para una ejecución de Pipeline hacia el cliente MCP.
 */
public record PipelineRunResponse(
        Integer id,
        String name,
        String status,
        String result,
        String createdDate,
        String finishedDate,
        Integer pipelineId,
        String pipelineName,
        String sourceBranch,
        String sourceCommit,
        String webUrl) {
}
