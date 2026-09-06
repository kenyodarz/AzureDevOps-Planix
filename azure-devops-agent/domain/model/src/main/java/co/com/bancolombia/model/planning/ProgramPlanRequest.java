package co.com.bancolombia.model.planning;

import java.util.List;
import lombok.Builder;

/**
 * Parámetros de entrada para la planeación macro trimestral (Program Planning).
 *
 * @param quarter              trimestre objetivo (ej. "Q3-2026")
 * @param objectives           objetivos estratégicos del trimestre
 * @param targetFronts         frentes o squads involucrados en la planeación
 * @param sprintCount          cantidad de sprints en el período (mayor a cero)
 * @param maxCapacityPerSprint capacidad máxima estimada en puntos de historia por sprint (mayor a
 *                             cero)
 */
@Builder
public record ProgramPlanRequest(
        String quarter,
        String objectives,
        List<String> targetFronts,
        int sprintCount,
        int maxCapacityPerSprint
) {

    public ProgramPlanRequest {
        if (quarter == null || quarter.isBlank()) {
            throw new IllegalArgumentException("El trimestre (quarter) es obligatorio");
        }
        if (objectives == null || objectives.isBlank()) {
            throw new IllegalArgumentException("Los objetivos de planeación son obligatorios");
        }
        if (sprintCount <= 0) {
            throw new IllegalArgumentException("La cantidad de sprints debe ser mayor a cero");
        }
        if (maxCapacityPerSprint <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad máxima por sprint debe ser mayor a cero");
        }
        quarter = quarter.trim();
        objectives = objectives.trim();
        targetFronts = targetFronts == null ? List.of() : List.copyOf(targetFronts);
    }
}
