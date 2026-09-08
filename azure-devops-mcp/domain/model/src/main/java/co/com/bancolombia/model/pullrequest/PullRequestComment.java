package co.com.bancolombia.model.pullrequest;

import lombok.Builder;

/**
 * Entidad de dominio inmutable que representa un comentario o hilo de feedback en un Pull Request
 * de Azure DevOps.
 *
 * <p>Diseñada como un {@code record} inmutable puro sin dependencias de frameworks ni librerías de
 * serialización.
 *
 * @param id      identificador numérico del comentario o hilo en Azure DevOps
 * @param content contenido textual del comentario técnico o revisión de código
 * @param status  estado del hilo (ej. "active", "fixed", "wontFix", "closed", "byDesign")
 * @param author  nombre descriptivo o identificador del autor del comentario
 */
@Builder(toBuilder = true)
public record PullRequestComment(
        Integer id,
        String content,
        String status,
        String author) {

}
