package co.com.bancolombia.usecase.manageplanning;

import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Caso de uso encargado de la gestión de iniciativas vectorizadas.
 */
@RequiredArgsConstructor
public class ManagePlanningUseCase {

    private static final String INVALID_INITIATIVE_ID_MESSAGE = "El ID de la iniciativa no puede ser nulo o vacío";

    private final PlanningVectorStorePort vectorStorePort;

    public Flux<Map<String, Object>> getInitiatives() {
        return vectorStorePort.findAllInitiatives();
    }

    public Mono<Void> deleteInitiative(String initiativeId) {
        if (initiativeId == null || initiativeId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(INVALID_INITIATIVE_ID_MESSAGE));
        }
        return vectorStorePort.deleteInitiative(initiativeId);
    }

    public Mono<Void> updateCell(String initiativeId, String cell) {
        if (initiativeId == null || initiativeId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(INVALID_INITIATIVE_ID_MESSAGE));
        }
        String cellValue = cell != null ? cell : "";
        return vectorStorePort.updateInitiativeCell(initiativeId, cellValue);
    }

    public Flux<PlanningChunk> getInitiativeChunks(String initiativeId) {
        if (initiativeId == null || initiativeId.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException(INVALID_INITIATIVE_ID_MESSAGE));
        }
        return vectorStorePort.findChunksByInitiative(initiativeId);
    }
}
