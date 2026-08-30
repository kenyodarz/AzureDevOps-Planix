package co.com.bancolombia.api.dto.planning;

import co.com.bancolombia.model.planning.PlanningChunk;

/**
 * Traduce el modelo de planificación a los DTO que viajan por HTTP (D-15).
 *
 * <p>Sigue el precedente de {@code DashboardDtoMapper} (Fase 05) y de {@code TaskDtoMapper}
 * (Fase 03): el entry-point no expone el dominio.
 */
public final class PlanningDtoMapper {

    private PlanningDtoMapper() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Convierte un fragmento de dominio en su respuesta HTTP.
     *
     * @param chunk fragmento de dominio; puede ser {@code null}
     * @return el DTO listo para serializar, o {@code null} si no había fragmento
     */
    public static PlanningChunkResponse toResponse(PlanningChunk chunk) {
        if (chunk == null) {
            return null;
        }
        return new PlanningChunkResponse(
                chunk.id(),
                chunk.initiativeId(),
                chunk.sectionName(),
                chunk.content(),
                chunk.metadata());
    }
}

