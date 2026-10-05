package co.com.bancolombia.usecase.wiki;

import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.model.wiki.gateways.WikiPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para consultar el contenido y metadatos de una página en la Wiki de Azure DevOps.
 */
@RequiredArgsConstructor
public class GetWikiPageUseCase {

    private final WikiPort wikiPort;

    /**
     * Consulta una página de Wiki por su ruta jerárquica.
     *
     * @param organization   nombre de la organización
     * @param project        nombre o UUID del proyecto
     * @param wikiIdentifier identificador o nombre de la Wiki
     * @param path           ruta de la página (ej. "/arquitectura/lineamientos")
     * @param includeContent si debe retornar el cuerpo en Markdown
     * @param apiVersion     versión opcional de la API
     * @return {@link Mono} con la entidad {@link WikiPage}
     */
    public Mono<WikiPage> getWikiPage(
            String organization,
            String project,
            String wikiIdentifier,
            String path,
            boolean includeContent,
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
        return wikiPort.getWikiPage(
                organization, project, wikiIdentifier, path, includeContent, apiVersion);
    }
}
