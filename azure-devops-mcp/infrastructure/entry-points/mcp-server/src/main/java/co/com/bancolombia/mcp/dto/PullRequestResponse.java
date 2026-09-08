package co.com.bancolombia.mcp.dto;

import java.util.List;

/**
 * DTO de respuesta que representa un Pull Request hacia el cliente MCP.
 *
 * <p>Aisla el modelo de dominio interno {@code PullRequest} para que cambios en el dominio no
 * alteren el contrato JSON público expuesto al protocolo MCP.
 *
 * @param pullRequestId identificador numérico único del Pull Request
 * @param title         título descriptivo del Pull Request
 * @param description   descripción detallada del Pull Request
 * @param status        estado actual del PR (ej. "active", "completed", "abandoned")
 * @param sourceRefName nombre canónico de la rama origen (ej. "refs/heads/feature/xxx")
 * @param targetRefName nombre canónico de la rama destino (ej. "refs/heads/main")
 * @param repositoryId  nombre o UUID del repositorio Git
 * @param createdBy     nombre o identificador del autor del Pull Request
 * @param creationDate  fecha de creación del PR en formato ISO-8601
 * @param workItemIds   lista de IDs numéricos de Work Items vinculados al Pull Request
 */
public record PullRequestResponse(
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

}
