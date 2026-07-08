package co.com.bancolombia.model.planning.gateways;

import co.com.bancolombia.model.planning.PlanningChunk;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para interactuar con la base de datos vectorial de planeaciones.
 */
public interface PlanningVectorStorePort {

    /**
     * Guarda y vectoriza los fragmentos de planeación en el vector store.
     */
    Mono<Void> saveChunks(List<PlanningChunk> chunks);

    /**
     * Busca por similitud semántica dentro de los fragmentos asociados a una iniciativa
     * específica. Si el initiativeId es nulo o vacío, realiza una búsqueda global.
     */
    Flux<PlanningChunk> searchSimilarity(String query, String initiativeId, int maxResults);

    /**
     * Elimina todos los vectores indexados de una iniciativa.
     */
    Mono<Void> deleteInitiative(String initiativeId);

    /**
     * Obtiene la lista de iniciativas únicas (initiative_id, initiative_title, cell).
     */
    Flux<Map<String, Object>> findAllInitiatives();

    /**
     * Obtiene todos los chunks pertenecientes a una iniciativa.
     */
    Flux<PlanningChunk> findChunksByInitiative(String initiativeId);

    /**
     * Actualiza la propiedad cell en la metadata JSONB de todos los chunks de la iniciativa.
     */
    Mono<Void> updateInitiativeCell(String initiativeId, String cell);
}
