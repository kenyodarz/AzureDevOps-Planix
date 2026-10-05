package co.com.bancolombia.mcp.dto;

import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;
import co.com.bancolombia.model.pullrequest.PullRequestComment;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemReference;
import co.com.bancolombia.model.workitem.WorkItemRelation;
import java.util.List;

/**
 * Mapper de la <b>frontera de salida hacia el cliente MCP</b>: traduce los modelos de dominio a los
 * DTOs de respuesta que Jackson serializa en el protocolo.
 *
 * <p><b>Es la mitad que faltaba.</b> La Fase 03 interpuso {@link McpToolDtoMapper} en la entrada y
 * cinco mappers en la salida hacia Azure DevOps, pero dejó el <b>retorno</b> sin frontera: cuatro
 * modelos de {@code domain/model} se serializaban tal cual hacia el cliente
 * ({@code CONTRATO-MCP.md} §3.4, «queda anotada para la Fase 07»). Ese momento es éste, y lo
 * autorizó <b>DP-07 §0.2(a)</b>.
 *
 * <p><b>Qué gana el dominio con esto.</b> Mientras el modelo <i>era</i> el contrato de respuesta, no
 * podía enriquecerse sin arriesgar el JSON: convertirlo en {@code record} habría renombrado los
 * accesores y, con ellos, los nombres de campo emitidos. Con esta frontera en medio, la forma del
 * dominio y la forma del cable son <b>independientes</b>, que es exactamente lo que pide DP-03.
 *
 * <p>⚠️ <b>Traducción y nada más.</b> Sin Spring, sin lógica de negocio y —deliberadamente— <b>sin
 * normalizar los nulos</b>: si {@code relations} llega nulo, sale nulo. Convertirlo en lista vacía
 * sería más limpio, pero cambiaría el JSON que ve el cliente, y eso no lo decide un refactor
 * (mismo criterio que el javadoc de {@code TeamScope} aplicó a las cadenas en blanco).
 */
@SuppressWarnings("java:S1168")
// Se preserva deliberadamente el retorno nulo para respetar la serialización del contrato JSON congelado (DP-07)
public final class McpResponseMapper {

    private McpResponseMapper() {
        // Mapper estático: no se instancia.
    }

    public static WorkItemResponse toResponse(WorkItem workItem) {
        if (workItem == null) {
            return null;
        }
        return new WorkItemResponse(
                workItem.id(),
                workItem.rev(),
                workItem.fields(),
                toRelationResponses(workItem.relations()),
                workItem.url());
    }

    public static List<WorkItemResponse> toResponses(List<WorkItem> workItems) {
        if (workItems == null) {
            return null;
        }
        return workItems.stream().map(McpResponseMapper::toResponse).toList();
    }

    public static WorkItemRelationResponse toResponse(WorkItemRelation relation) {
        if (relation == null) {
            return null;
        }
        return new WorkItemRelationResponse(relation.rel(), relation.url(), relation.attributes());
    }

    public static WiqlResultResponse toResponse(WiqlResult result) {
        if (result == null) {
            return null;
        }
        return new WiqlResultResponse(
                result.queryType(),
                result.queryResultType(),
                result.asOf(),
                toReferenceResponses(result.workItems()));
    }

    public static WorkItemReferenceResponse toResponse(WorkItemReference reference) {
        if (reference == null) {
            return null;
        }
        return new WorkItemReferenceResponse(reference.id(), reference.url());
    }

    private static List<WorkItemRelationResponse> toRelationResponses(List<WorkItemRelation> relations) {
        if (relations == null) {
            return null;
        }
        return relations.stream().map(McpResponseMapper::toResponse).toList();
    }

    private static List<WorkItemReferenceResponse> toReferenceResponses(List<WorkItemReference> references) {
        if (references == null) {
            return null;
        }
        return references.stream().map(McpResponseMapper::toResponse).toList();
    }

    public static PullRequestResponse toResponse(PullRequest pullRequest) {
        if (pullRequest == null) {
            return null;
        }
        return new PullRequestResponse(
                pullRequest.pullRequestId(),
                pullRequest.title(),
                pullRequest.description(),
                pullRequest.status(),
                pullRequest.sourceRefName(),
                pullRequest.targetRefName(),
                pullRequest.repositoryId(),
                pullRequest.createdBy(),
                pullRequest.creationDate(),
                pullRequest.workItemIds());
    }

    public static GitChangeResponse toResponse(GitChange change) {
        if (change == null) {
            return null;
        }
        return new GitChangeResponse(
                change.itemPath(),
                change.changeType(),
                change.originalObjectId(),
                change.newObjectId());
    }

    public static List<GitChangeResponse> toGitChangeResponses(List<GitChange> changes) {
        if (changes == null) {
            return null;
        }
        return changes.stream().map(McpResponseMapper::toResponse).toList();
    }

    public static PullRequestCommentResponse toResponse(PullRequestComment comment) {
        if (comment == null) {
            return null;
        }
        return new PullRequestCommentResponse(
                comment.id(),
                comment.content(),
                comment.status(),
                comment.author());
    }

    public static ProjectOverviewResponse toResponse(co.com.bancolombia.model.overview.ProjectOverview overview) {
        if (overview == null) {
            return null;
        }
        return new ProjectOverviewResponse(
                overview.id(),
                overview.name(),
                overview.description(),
                overview.state(),
                overview.visibility(),
                overview.url());
    }

    public static WikiPageResponse toResponse(co.com.bancolombia.model.wiki.WikiPage page) {
        if (page == null) {
            return null;
        }
        return new WikiPageResponse(
                page.id(),
                page.path(),
                page.content(),
                page.remoteUrl(),
                page.version());
    }

    public static WikiSearchResultResponse toResponse(co.com.bancolombia.model.wiki.WikiSearchResult result) {
        if (result == null) {
            return null;
        }
        return new WikiSearchResultResponse(
                result.path(),
                result.wikiName(),
                result.summary(),
                result.url());
    }

    public static List<WikiSearchResultResponse> toWikiSearchResultResponses(List<co.com.bancolombia.model.wiki.WikiSearchResult> results) {
        if (results == null) {
            return null;
        }
        return results.stream().map(McpResponseMapper::toResponse).toList();
    }

    public static PipelineSummaryResponse toResponse(co.com.bancolombia.model.pipeline.PipelineSummary summary) {
        if (summary == null) {
            return null;
        }
        return new PipelineSummaryResponse(
                summary.id(),
                summary.name(),
                summary.folder(),
                summary.revision(),
                summary.url());
    }

    public static List<PipelineSummaryResponse> toPipelineSummaryResponses(List<co.com.bancolombia.model.pipeline.PipelineSummary> summaries) {
        if (summaries == null) {
            return null;
        }
        return summaries.stream().map(McpResponseMapper::toResponse).toList();
    }

    public static PipelineRunResponse toResponse(co.com.bancolombia.model.pipeline.PipelineRun run) {
        if (run == null) {
            return null;
        }
        return new PipelineRunResponse(
                run.id(),
                run.name(),
                run.status(),
                run.result(),
                run.createdDate(),
                run.finishedDate(),
                run.pipelineId(),
                run.pipelineName(),
                run.sourceBranch(),
                run.sourceCommit(),
                run.webUrl());
    }

    public static PipelineLogSummaryResponse toResponse(co.com.bancolombia.model.pipeline.PipelineLogSummary logSummary) {
        if (logSummary == null) {
            return null;
        }
        return new PipelineLogSummaryResponse(
                logSummary.runId(),
                logSummary.hasErrors(),
                logSummary.errorCount(),
                logSummary.warningCount(),
                logSummary.errorLines());
    }
}

