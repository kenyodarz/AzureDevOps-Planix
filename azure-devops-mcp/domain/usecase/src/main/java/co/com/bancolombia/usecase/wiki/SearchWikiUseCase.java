package co.com.bancolombia.usecase.wiki;

import co.com.bancolombia.model.wiki.WikiSearchResult;
import co.com.bancolombia.model.wiki.gateways.WikiPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para realizar búsquedas textuales dentro de las Wikis de Azure DevOps.
 */
@RequiredArgsConstructor
public class SearchWikiUseCase {

    private static final int DEFAULT_TOP = 5;
    private static final int MAX_TOP = 25;

    private final WikiPort wikiPort;

    /**
     * Busca páginas en la Wiki que coincidan con la consulta.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param query        término de búsqueda
     * @param top          número máximo de resultados
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la lista de resultados
     */
    public Mono<List<WikiSearchResult>> searchWiki(
            String organization,
            String project,
            String query,
            Integer top,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Mono.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("query no puede ser nula ni vacía"));
        }
        int effectiveTop = (top == null || top <= 0) ? DEFAULT_TOP : Math.min(top, MAX_TOP);
        return wikiPort.searchWiki(organization, project, query, effectiveTop, apiVersion)
                .collectList();
    }
}
