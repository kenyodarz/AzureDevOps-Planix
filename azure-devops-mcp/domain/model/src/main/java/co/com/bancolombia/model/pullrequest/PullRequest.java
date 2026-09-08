package co.com.bancolombia.model.pullrequest;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.List;
import lombok.Builder;

/**
 * Entidad de dominio inmutable que representa un Pull Request de Azure DevOps.
 *
 * <p>Diseñada como un {@code record} inmutable con soporte de {@link Builder}. La lista de
 * identificadores de Work Items asociados se resguarda mediante una copia defensiva inmodificable
 * que preserva el orden y los nulos sin generar mutaciones colaterales.
 *
 * @param pullRequestId identificador numérico único del Pull Request
 * @param title         título descriptivo del Pull Request
 * @param description   descripción o detalle del Pull Request
 * @param status        estado actual del PR (ej. "active", "completed", "abandoned")
 * @param sourceRefName nombre canónico de la rama origen (ej. "refs/heads/feature/xxx")
 * @param targetRefName nombre canónico de la rama destino (ej. "refs/heads/main")
 * @param repositoryId  nombre o UUID del repositorio Git
 * @param createdBy     nombre o identificador del autor del Pull Request
 * @param creationDate  fecha de creación del PR en formato ISO-8601
 * @param workItemIds   lista de IDs numéricos de Work Items vinculados al Pull Request
 */
@Builder(toBuilder = true)
public record PullRequest(
        Integer pullRequestId,
        String title,
        String description,
        String status,
        String sourceRefName,
        String targetRefName,
        String repositoryId,
        String createdBy,
        String creationDate,
        List<Integer> workItemIds) {

    public PullRequest {
        workItemIds = DomainCollections.immutableCopy(workItemIds);
    }
}
