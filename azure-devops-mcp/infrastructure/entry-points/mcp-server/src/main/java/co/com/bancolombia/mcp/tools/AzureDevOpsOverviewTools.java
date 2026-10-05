package co.com.bancolombia.mcp.tools;

import co.com.bancolombia.mcp.dto.McpResponseMapper;
import co.com.bancolombia.mcp.dto.ProjectOverviewResponse;
import co.com.bancolombia.mcp.dto.WikiPageResponse;
import co.com.bancolombia.mcp.dto.WikiSearchResultResponse;
import co.com.bancolombia.mcp.error.McpErrorTranslator;
import co.com.bancolombia.mcp.security.McpRoles;
import co.com.bancolombia.usecase.overview.GetProjectOverviewUseCase;
import co.com.bancolombia.usecase.wiki.CreateOrUpdateWikiPageUseCase;
import co.com.bancolombia.usecase.wiki.GetWikiPageUseCase;
import co.com.bancolombia.usecase.wiki.SearchWikiUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Herramientas MCP expuestas como beans de Spring AI para interactuar con Overview y Wikis de Azure DevOps.
 *
 * <p>Cero lógica de negocio (Clean Architecture Bancolombia, capa {@code entry-points}).
 * Cada método valida la entrada, traduce el protocolo MCP, delega en el caso de uso correspondiente
 * y canaliza la respuesta a través de {@link McpResponseMapper} y los errores a través de {@link McpErrorTranslator}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureDevOpsOverviewTools {

    private final GetProjectOverviewUseCase getProjectOverviewUseCase;
    private final GetWikiPageUseCase getWikiPageUseCase;
    private final SearchWikiUseCase searchWikiUseCase;
    private final CreateOrUpdateWikiPageUseCase createOrUpdateWikiPageUseCase;

    @McpTool(
            name = "getProjectOverview",
            description = "Recupera la visión general, metadatos y estado de un proyecto en Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<ProjectOverviewResponse> getProjectOverview(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getProjectOverview] ejecutada para proyecto: {}", project);
        return getProjectOverviewUseCase.getProjectOverview(organization, project, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("getProjectOverview"));
    }

    @McpTool(
            name = "getWikiPage",
            description = "Recupera el contenido en Markdown y metadatos de una página específica de la Wiki en Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<WikiPageResponse> getWikiPage(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Identificador o nombre de la Wiki (ej. 'ProjectName.wiki')", required = true) String wikiIdentifier,
            @McpToolParam(description = "Ruta jerárquica de la página dentro de la Wiki (ej. '/arquitectura/lineamientos')", required = true) String path,
            @McpToolParam(description = "Indica si se debe incluir el cuerpo en Markdown (por defecto true)", required = false) Boolean includeContent,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getWikiPage] ejecutada para ruta: {}", path);
        boolean withContent = includeContent == null || includeContent;
        return getWikiPageUseCase.getWikiPage(organization, project, wikiIdentifier, path, withContent, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("getWikiPage"));
    }

    @McpTool(
            name = "searchWiki",
            description = "Busca páginas en la Wiki de Azure DevOps a partir de palabras clave o consultas de texto."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<List<WikiSearchResultResponse>> searchWiki(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Término o consulta de texto a buscar", required = true) String query,
            @McpToolParam(description = "Número máximo de resultados a retornar (por defecto 5)", required = false) Integer top,
            @McpToolParam(description = "Versión de la API de Azure DevOps", required = false) String apiVersion) {
        log.info("MCP Tool [searchWiki] ejecutada para query: {}", query);
        return searchWikiUseCase.searchWiki(organization, project, query, top, apiVersion)
                .map(McpResponseMapper::toWikiSearchResultResponses)
                .onErrorMap(McpErrorTranslator.forTool("searchWiki"));
    }

    @McpTool(
            name = "createOrUpdateWikiPage",
            description = "Crea o actualiza una página de Wiki en Azure DevOps con contenido en formato Markdown."
    )
    @PreAuthorize(McpRoles.HAS_WRITE)
    public Mono<WikiPageResponse> createOrUpdateWikiPage(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Identificador o nombre de la Wiki (ej. 'ProjectName.wiki')", required = true) String wikiIdentifier,
            @McpToolParam(description = "Ruta jerárquica de la página a crear o actualizar", required = true) String path,
            @McpToolParam(description = "Contenido en texto con formato Markdown", required = true) String content,
            @McpToolParam(description = "Mensaje descriptivo del cambio o commit", required = false) String comment,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [createOrUpdateWikiPage] ejecutada para ruta: {}", path);
        return createOrUpdateWikiPageUseCase.createOrUpdateWikiPage(
                        organization, project, wikiIdentifier, path, content, comment, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("createOrUpdateWikiPage"));
    }
}
