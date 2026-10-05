package co.com.bancolombia.model.wiki;

import lombok.Builder;

/**
 * Objeto de valor que representa un resultado de búsqueda dentro de la Wiki de Azure DevOps.
 *
 * @param path     ruta de la página encontrada
 * @param wikiName nombre de la Wiki que contiene la página
 * @param summary  fragmento de texto o resumen con la coincidencia
 * @param url      enlace a la página encontrada
 */
@Builder(toBuilder = true)
public record WikiSearchResult(
        String path,
        String wikiName,
        String summary,
        String url) {
}
