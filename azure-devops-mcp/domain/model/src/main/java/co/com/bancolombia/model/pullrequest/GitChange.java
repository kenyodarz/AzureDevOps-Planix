package co.com.bancolombia.model.pullrequest;

import lombok.Builder;

/**
 * Objeto de valor inmutable que representa un cambio en un archivo dentro de un Pull Request.
 *
 * @param itemPath         ruta relativa del archivo en el repositorio
 * @param changeType       tipo de cambio aplicado (ej. "add", "edit", "delete")
 * @param originalObjectId hash o identificador del objeto Git antes del cambio
 * @param newObjectId      hash o identificador del objeto Git resultante del cambio
 */
@Builder(toBuilder = true)
public record GitChange(
        String itemPath,
        String changeType,
        String originalObjectId,
        String newObjectId) {

}
