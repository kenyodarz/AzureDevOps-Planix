package co.com.bancolombia.usecase.searchplanning;

import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Caso de uso encargado de buscar fragmentos de planeación relevantes por similitud semántica.
 */
@RequiredArgsConstructor
public class SearchPlanningSpecUseCase {

    private final PlanningVectorStorePort vectorStorePort;

    public Flux<PlanningChunk> search(String query, String initiativeId, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return Flux.error(
                    new IllegalArgumentException("La consulta de búsqueda no puede estar vacía"));
        }

        int limit = maxResults > 0 ? maxResults : 3;

        return vectorStorePort.searchSimilarity(query, initiativeId, limit);
    }
}
