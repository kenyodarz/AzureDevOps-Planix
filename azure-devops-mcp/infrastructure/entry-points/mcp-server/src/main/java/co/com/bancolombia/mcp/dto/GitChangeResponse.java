package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta que representa un cambio en un archivo de un Pull Request hacia el cliente MCP.
 *
 * @param itemPath         ruta relativa del archivo en el repositorio Git
 * @param changeType       tipo de cambio aplicado (ej. "add", "edit", "delete")
 * @param originalObjectId hash o identificador del objeto Git antes del cambio
 * @param newObjectId      hash o identificador del objeto Git resultante del cambio
 */
public record GitChangeResponse(
        String itemPath,
        String changeType,
        String originalObjectId,
        String newObjectId) {

}
