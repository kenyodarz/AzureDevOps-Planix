package co.com.bancolombia.mcp.tools;

import co.com.bancolombia.mcp.dto.GitChangeResponse;
import co.com.bancolombia.mcp.dto.McpResponseMapper;
import co.com.bancolombia.mcp.dto.PullRequestCommentResponse;
import co.com.bancolombia.mcp.dto.PullRequestResponse;
import co.com.bancolombia.mcp.error.McpErrorTranslator;
import co.com.bancolombia.mcp.security.McpRoles;
import co.com.bancolombia.model.pullrequest.PullRequestComment;
import co.com.bancolombia.usecase.pullrequest.CreatePullRequestCommentUseCase;
import co.com.bancolombia.usecase.pullrequest.GetPullRequestChangesUseCase;
import co.com.bancolombia.usecase.pullrequest.GetPullRequestUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Herramientas MCP expuestas como beans de Spring AI para interactuar con Azure DevOps Git y Pull
 * Requests.
 *
 * <p><b>Cero lógica de negocio</b> (Clean Architecture Bancolombia, capa {@code entry-points}).
 * Cada método valida la entrada, traduce el protocolo MCP, delega en el caso de uso correspondiente
 * y canaliza la respuesta a través de {@link McpResponseMapper} y los errores a través de
 * {@link McpErrorTranslator}.
 *
 * <p>Conforme a la decisión de diseño <b>DP-PR-03</b>, esta clase se mantiene desacoplada de
 * {@code AzureDevOpsTools} (WIT & WIQL) para respetar el Principio de Responsabilidad Única.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureDevOpsGitTools {

    private static final String DEFAULT_COMMENT_STATUS = "active";

    private final GetPullRequestUseCase getPullRequestUseCase;
    private final GetPullRequestChangesUseCase getPullRequestChangesUseCase;
    private final CreatePullRequestCommentUseCase createPullRequestCommentUseCase;

    @McpTool(
            name = "getPullRequest",
            description = "Recupera los detalles y metadatos de un Pull Request en Azure DevOps utilizando su identificador numérico."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<PullRequestResponse> getPullRequest(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Nombre o UUID del repositorio Git en Azure DevOps", required = true) String repositoryId,
            @McpToolParam(description = "ID único numérico del Pull Request", required = true) int pullRequestId,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getPullRequest] ejecutada para PR ID: {}", pullRequestId);
        return getPullRequestUseCase.getPullRequest(organization, project, repositoryId,
                        pullRequestId, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("getPullRequest"));
    }

    @McpTool(
            name = "getPullRequestChanges",
            description = "Obtiene la lista de archivos modificados, agregados o eliminados dentro de un Pull Request en Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<List<GitChangeResponse>> getPullRequestChanges(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Nombre o UUID del repositorio Git en Azure DevOps", required = true) String repositoryId,
            @McpToolParam(description = "ID único numérico del Pull Request", required = true) int pullRequestId,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getPullRequestChanges] ejecutada para PR ID: {}", pullRequestId);
        return getPullRequestChangesUseCase.getChanges(organization, project, repositoryId,
                        pullRequestId, apiVersion)
                .collectList()
                .map(McpResponseMapper::toGitChangeResponses)
                .onErrorMap(McpErrorTranslator.forTool("getPullRequestChanges"));
    }

    @McpTool(
            name = "createPullRequestComment",
            description = "Publica un nuevo comentario técnico o hilo de feedback en un Pull Request de Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_WRITE)
    public Mono<PullRequestCommentResponse> createPullRequestComment(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Nombre o UUID del repositorio Git en Azure DevOps", required = true) String repositoryId,
            @McpToolParam(description = "ID único numérico del Pull Request", required = true) int pullRequestId,
            @McpToolParam(description = "Contenido textual del comentario o revisión técnica a publicar", required = true) String content,
            @McpToolParam(description = "Estado inicial del hilo de discusión (ej. active, closed; por defecto active)", required = false) String status,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [createPullRequestComment] ejecutada para PR ID: {}", pullRequestId);
        PullRequestComment comment = PullRequestComment.builder()
                .content(content)
                .status(status != null && !status.isBlank() ? status : DEFAULT_COMMENT_STATUS)
                .build();
        return createPullRequestCommentUseCase.createComment(organization, project, repositoryId,
                        pullRequestId,
                        comment, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("createPullRequestComment"));
    }
}
