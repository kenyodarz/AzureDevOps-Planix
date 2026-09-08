package co.com.bancolombia.usecase.pullrequest;

import co.com.bancolombia.model.pullrequest.PullRequestComment;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para crear y publicar comentarios de feedback en hilos de Pull Requests de Azure
 * DevOps.
 *
 * <p>Aplica validaciones estrictas sobre los parámetros de la solicitud y el contenido del
 * comentario antes de delegar en el puerto {@link PullRequestPort}.
 */
@RequiredArgsConstructor
public class CreatePullRequestCommentUseCase {

    private final PullRequestPort pullRequestPort;

    /**
     * Publica un comentario en un Pull Request tras validar los argumentos.
     *
     * @param organization  nombre de la organización en Azure DevOps (no nula ni en blanco)
     * @param project       nombre o UUID del proyecto (no nulo ni en blanco)
     * @param repositoryId  nombre o UUID del repositorio Git (no nulo ni en blanco)
     * @param pullRequestId identificador numérico del Pull Request (debe ser mayor a 0)
     * @param comment       entidad de comentario con contenido no nulo ni en blanco
     * @param apiVersion    versión opcional de la API de Azure DevOps
     * @return {@link Mono} con la entidad {@link PullRequestComment} creada o error reactivo si los
     * parámetros son inválidos
     */
    public Mono<PullRequestComment> createComment(
            String organization,
            String project,
            String repositoryId,
            int pullRequestId,
            PullRequestComment comment,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Mono.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        if (repositoryId == null || repositoryId.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("repositoryId no puede ser nulo ni vacío"));
        }
        if (pullRequestId <= 0) {
            return Mono.error(new IllegalArgumentException("pullRequestId debe ser mayor a 0"));
        }
        if (comment == null) {
            return Mono.error(new IllegalArgumentException("comment no puede ser nulo"));
        }
        if (comment.content() == null || comment.content().isBlank()) {
            return Mono.error(
                    new IllegalArgumentException(
                            "el contenido del comentario no puede ser nulo ni vacío"));
        }

        return pullRequestPort.createComment(organization, project, repositoryId, pullRequestId,
                comment, apiVersion);
    }
}
