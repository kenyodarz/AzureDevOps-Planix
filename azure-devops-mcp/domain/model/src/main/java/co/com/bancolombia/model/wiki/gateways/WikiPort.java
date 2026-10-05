package co.com.bancolombia.model.wiki.gateways;

import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.model.wiki.WikiSearchResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto gateway para interactuar con la Wiki de Azure DevOps.
 */
public interface WikiPort {

    /**
     * Consulta el contenido y metadatos de una página de Wiki.
     *
     * @param organization   nombre de la organización
     * @param project        nombre o UUID del proyecto
     * @param wikiIdentifier identificador o nombre de la Wiki
     * @param path           ruta de la página en la Wiki
     * @param includeContent si se debe incluir el cuerpo Markdown
     * @param apiVersion     versión opcional de la API
     * @return {@link Mono} con la página consultada
     */
    Mono<WikiPage> getWikiPage(
            String organization,
            String project,
            String wikiIdentifier,
            String path,
            boolean includeContent,
            String apiVersion);

    /**
     * Realiza una búsqueda de texto en las Wikis del proyecto.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param query        término de búsqueda
     * @param top          número máximo de resultados
     * @param apiVersion   versión opcional de la API
     * @return {@link Flux} con los resultados encontrados
     */
    Flux<WikiSearchResult> searchWiki(
            String organization,
            String project,
            String query,
            int top,
            String apiVersion);

    /**
     * Crea o actualiza una página de Wiki.
     *
     * @param organization   nombre de la organización
     * @param project        nombre o UUID del proyecto
     * @param wikiIdentifier identificador o nombre de la Wiki
     * @param path           ruta de la página
     * @param content        contenido en formato Markdown
     * @param comment        mensaje descriptivo del cambio o commit
     * @param apiVersion     versión opcional de la API
     * @return {@link Mono} con la página creada o actualizada
     */
    Mono<WikiPage> createOrUpdateWikiPage(
            String organization,
            String project,
            String wikiIdentifier,
            String path,
            String content,
            String comment,
            String apiVersion);
}
