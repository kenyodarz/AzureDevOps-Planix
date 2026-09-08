package co.com.bancolombia.model.pullrequest.gateways;

import co.com.bancolombia.model.pullrequest.PullRequestChangeInfo;
import co.com.bancolombia.model.pullrequest.PullRequestInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto reactivo de salida para interactuar con Azure DevOps MCP Server en operaciones de Pull
 * Request.
 *
 * <p>Permite al agente consultar metadatos de un Pull Request, listar los archivos modificados
 * y publicar comentarios o feedback de revisión técnica.
 */
public interface PullRequestMcpPort {

    /**
     * Consulta los metadatos y estado de un Pull Request específico.
     *
     * @param organization  organización en Azure DevOps
     * @param project       proyecto en Azure DevOps
     * @param repositoryId  identificador o nombre del repositorio
     * @param pullRequestId identificador numérico del PR
     * @return Mono con la información estructurada del PR
     */
    Mono<PullRequestInfo> getPullRequest(String organization, String project, String repositoryId,
            int pullRequestId);

    /**
     * Consulta el listado de archivos y cambios realizados en el Pull Request.
     *
     * @param organization  organización en Azure DevOps
     * @param project       proyecto en Azure DevOps
     * @param repositoryId  identificador o nombre del repositorio
     * @param pullRequestId identificador numérico del PR
     * @return Flux con los cambios de archivos detectados
     */
    Flux<PullRequestChangeInfo> getPullRequestChanges(String organization, String project,
            String repositoryId, int pullRequestId);

    /**
     * Publica un comentario técnico o resumen de evaluación en el hilo del Pull Request.
     *
     * @param organization  organización en Azure DevOps
     * @param project       proyecto en Azure DevOps
     * @param repositoryId  identificador o nombre del repositorio
     * @param pullRequestId identificador numérico del PR
     * @param comment       contenido textual en Markdown del comentario
     * @return Mono con el identificador del comentario o confirmación generada
     */
    Mono<String> createPullRequestComment(String organization, String project, String repositoryId,
            int pullRequestId, String comment);
}
