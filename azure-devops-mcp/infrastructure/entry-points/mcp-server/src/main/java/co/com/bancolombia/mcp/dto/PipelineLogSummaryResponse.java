package co.com.bancolombia.mcp.dto;

import java.util.List;

/**
 * DTO de respuesta para el resumen de logs y errores de Pipeline hacia el cliente MCP.
 */
public record PipelineLogSummaryResponse(
        Integer runId,
        boolean hasErrors,
        int errorCount,
        int warningCount,
        List<String> errorLines) {
}
