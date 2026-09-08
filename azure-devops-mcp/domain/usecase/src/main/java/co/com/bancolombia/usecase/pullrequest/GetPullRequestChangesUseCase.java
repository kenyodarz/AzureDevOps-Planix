package co.com.bancolombia.usecase.pullrequest;

import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Caso de uso puro para consultar la secuencia reactiva de cambios de archivos en un Pull Request.
 *
 * <p>Aplica validaciones sobre los parámetros requeridos antes de delegar en el puerto
 * {@link PullRequestPort}, retornando un error reactivo en caso de inconsistencia.
 */
@RequiredArgsConstructor
public class GetPullRequestChangesUseCase {

    private final PullRequestPort pullRequestPort;

    /**
     * Obtiene la lista reactiva de cambios en archivos de un Pull Request.
     *
     * @param organization  nombre de la organización en Azure DevOps (no nula ni en blanco)
     * @param project       nombre o UUID del proyecto (no nulo ni en blanco)
     * @param repositoryId  nombre o UUID del repositorio Git (no nulo ni en blanco)
     * @param pullRequestId identificador numérico del Pull Request (debe ser mayor a 0)
     * @param apiVersion    versión opcional de la API de Azure DevOps
     * @return {@link Flux} con los {@link GitChange} o error reactivo si los parámetros son
     * inválidos
     */
    public Flux<GitChange> getChanges(
            String organization,
            String project,
            String repositoryId,
            int pullRequestId,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Flux.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Flux.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        if (repositoryId == null || repositoryId.isBlank()) {
            return Flux.error(
                    new IllegalArgumentException("repositoryId no puede ser nulo ni vacío"));
        }
        if (pullRequestId <= 0) {
            return Flux.error(new IllegalArgumentException("pullRequestId debe ser mayor a 0"));
        }
        return pullRequestPort.getPullRequestChanges(organization, project, repositoryId,
                pullRequestId, apiVersion);
    }
}
