package co.com.bancolombia.mcp.tools;

import co.com.bancolombia.mcp.dto.McpResponseMapper;
import co.com.bancolombia.mcp.dto.PipelineLogSummaryResponse;
import co.com.bancolombia.mcp.dto.PipelineRunResponse;
import co.com.bancolombia.mcp.dto.PipelineSummaryResponse;
import co.com.bancolombia.mcp.error.McpErrorTranslator;
import co.com.bancolombia.mcp.security.McpRoles;
import co.com.bancolombia.usecase.pipeline.GetPipelineRunLogsUseCase;
import co.com.bancolombia.usecase.pipeline.GetPipelineRunUseCase;
import co.com.bancolombia.usecase.pipeline.ListPipelinesUseCase;
import co.com.bancolombia.usecase.pipeline.TriggerPipelineRunUseCase;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Herramientas MCP expuestas como beans de Spring AI para interactuar con Pipelines de Azure DevOps.
 *
 * <p>Cero lógica de negocio (Clean Architecture Bancolombia, capa {@code entry-points}).
 * Cada método valida la entrada, traduce el protocolo MCP, delega en el caso de uso correspondiente
 * y canaliza la respuesta a través de {@link McpResponseMapper} y los errores a través de {@link McpErrorTranslator}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureDevOpsPipelineTools {

    private final ListPipelinesUseCase listPipelinesUseCase;
    private final GetPipelineRunUseCase getPipelineRunUseCase;
    private final GetPipelineRunLogsUseCase getPipelineRunLogsUseCase;
    private final TriggerPipelineRunUseCase triggerPipelineRunUseCase;

    @McpTool(
            name = "listPipelines",
            description = "Lista los pipelines de compilación y despliegue disponibles en un proyecto de Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<List<PipelineSummaryResponse>> listPipelines(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Número máximo de pipelines a retornar (por defecto 10)", required = false) Integer top,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [listPipelines] ejecutada para proyecto: {}", project);
        return listPipelinesUseCase.listPipelines(organization, project, top, apiVersion)
                .map(McpResponseMapper::toPipelineSummaryResponses)
                .onErrorMap(McpErrorTranslator.forTool("listPipelines"));
    }

    @McpTool(
            name = "getPipelineRun",
            description = "Recupera los detalles y estado de ejecución de una corrida específica de un pipeline en Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<PipelineRunResponse> getPipelineRun(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Identificador numérico del pipeline", required = true) int pipelineId,
            @McpToolParam(description = "Identificador numérico de la corrida (run ID)", required = true) int runId,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getPipelineRun] ejecutada para pipeline {} y run {}", pipelineId, runId);
        return getPipelineRunUseCase.getPipelineRun(organization, project, pipelineId, runId, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("getPipelineRun"));
    }

    @McpTool(
            name = "getPipelineRunLogs",
            description = "Recupera un resumen de los logs y errores de una corrida de pipeline, filtrando las fallas principales sin sobrecargar el contexto."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<PipelineLogSummaryResponse> getPipelineRunLogs(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Identificador numérico del pipeline", required = true) int pipelineId,
            @McpToolParam(description = "Identificador numérico de la corrida (run ID)", required = true) int runId,
            @McpToolParam(description = "Indica si solo se deben extraer errores (por defecto true)", required = false) Boolean onlyErrors,
            @McpToolParam(description = "Límite máximo de líneas a incluir (por defecto 100)", required = false) Integer maxLines,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getPipelineRunLogs] ejecutada para pipeline {} y run {}", pipelineId, runId);
        return getPipelineRunLogsUseCase.getPipelineRunLogs(organization, project, pipelineId, runId, onlyErrors, maxLines, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("getPipelineRunLogs"));
    }

    @McpTool(
            name = "triggerPipelineRun",
            description = "Dispara una nueva ejecución (build/run) de un pipeline en Azure DevOps sobre una rama específica."
    )
    @PreAuthorize(McpRoles.HAS_WRITE)
    public Mono<PipelineRunResponse> triggerPipelineRun(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Identificador numérico del pipeline a ejecutar", required = true) int pipelineId,
            @McpToolParam(description = "Rama sobre la cual ejecutar (ej. 'main' o 'develop')", required = false) String branch,
            @McpToolParam(description = "Variables opcionales de ejecución en formato clave-valor", required = false) Map<String, String> variables,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [triggerPipelineRun] ejecutada para pipeline {} en rama {}", pipelineId, branch);
        return triggerPipelineRunUseCase.triggerPipelineRun(organization, project, pipelineId, branch, variables, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("triggerPipelineRun"));
    }
}
