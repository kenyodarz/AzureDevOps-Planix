package co.com.bancolombia.model.pullrequest.gateways;

import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto gateway para consultar información y cambios de Pull Requests en Azure DevOps.
 *
 * <p>Define el contrato de salida del dominio para la interacción con repositorios Git de Azure
 * DevOps de forma reactiva no bloqueante.
 */
public interface PullRequestPort {

    /**
     * Consulta el detalle y metadatos de un Pull Request por su identificador.
     *
     * @param organization  nombre de la organización en Azure DevOps
     * @param project       nombre o UUID del proyecto
     * @param repositoryId  nombre o UUID del repositorio Git
     * @param pullRequestId identificador numérico único del PR
     * @param apiVersion    versión de la API de Azure DevOps (opcional)
     * @return {@link Mono} con la entidad {@link PullRequest}
     */
    Mono<PullRequest> getPullRequestById(
            String organization,
            String project,
            String repositoryId,
            int pullRequestId,
            String apiVersion);

    /**
     * Consulta la lista reactiva de cambios en archivos asociados a un Pull Request.
     *
     * @param organization  nombre de la organización en Azure DevOps
     * @param project       nombre o UUID del proyecto
     * @param repositoryId  nombre o UUID del repositorio Git
     * @param pullRequestId identificador numérico único del PR
     * @param apiVersion    versión de la API de Azure DevOps (opcional)
     * @return {@link Flux} con los {@link GitChange} del PR
     */
    Flux<GitChange> getPullRequestChanges(
            String organization,
            String project,
            String repositoryId,
            int pullRequestId,
            String apiVersion);
}
