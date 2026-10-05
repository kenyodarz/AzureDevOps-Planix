package co.com.bancolombia.model.pipeline;

import lombok.Builder;

/**
 * Entidad de dominio inmutable que representa un resumen de Pipeline en Azure DevOps.
 *
 * @param id       identificador numérico del pipeline
 * @param name     nombre del pipeline
 * @param folder   carpeta o ruta organizativa del pipeline
 * @param revision revisión del pipeline
 * @param url      URL canónica del pipeline
 */
@Builder(toBuilder = true)
public record PipelineSummary(
        Integer id,
        String name,
        String folder,
        Integer revision,
        String url) {
}
