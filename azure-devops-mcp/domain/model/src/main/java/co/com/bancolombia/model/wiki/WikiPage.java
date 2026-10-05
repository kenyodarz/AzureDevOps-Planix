package co.com.bancolombia.model.wiki;

import lombok.Builder;

/**
 * Entidad de dominio inmutable que representa una página de Wiki en Azure DevOps.
 *
 * @param id        identificador numérico de la página
 * @param path      ruta jerárquica de la página dentro de la Wiki (ej. "/arquitectura/lineamientos")
 * @param content   contenido textual en formato Markdown
 * @param remoteUrl URL web para visualizar la página en el navegador
 * @param version   etiqueta de versión o eTag de la página
 */
@Builder(toBuilder = true)
public record WikiPage(
        Integer id,
        String path,
        String content,
        String remoteUrl,
        String version) {
}
