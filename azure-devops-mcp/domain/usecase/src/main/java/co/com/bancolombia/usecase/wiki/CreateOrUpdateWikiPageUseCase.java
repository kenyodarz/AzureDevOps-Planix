package co.com.bancolombia.usecase.wiki;

import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.model.wiki.gateways.WikiPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para crear o actualizar el contenido de una página de Wiki en Azure DevOps.
 */
@RequiredArgsConstructor
public class CreateOrUpdateWikiPageUseCase {

    private final WikiPort wikiPort;

    /**
     * Crea o actualiza una página en la Wiki.
     *
     * @param organization   nombre de la organización
     * @param project        nombre o UUID del proyecto
     * @param wikiIdentifier identificador o nombre de la Wiki
     * @param path           ruta de la página
     * @param content        contenido en formato Markdown
     * @param comment        mensaje del cambio
     * @param apiVersion     versión opcional de la API
     * @return {@link Mono} con la página creada o actualizada
     */
    public Mono<WikiPage> createOrUpdateWikiPage(
            String organization,
            String project,
            String wikiIdentifier,
            String path,
            String content,
            String comment,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Mono.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        if (wikiIdentifier == null || wikiIdentifier.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("wikiIdentifier no puede ser nulo ni vacío"));
        }
        if (path == null || path.isBlank()) {
            return Mono.error(new IllegalArgumentException("path no puede ser nulo ni vacío"));
        }
        if (content == null) {
            return Mono.error(new IllegalArgumentException("content no puede ser nulo"));
        }
        return wikiPort.createOrUpdateWikiPage(
                organization, project, wikiIdentifier, path, content, comment, apiVersion);
    }
}
