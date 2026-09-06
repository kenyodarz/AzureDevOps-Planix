package co.com.bancolombia.model.planning;

import java.util.List;
import lombok.Builder;

/**
 * Resultado estructurado del proceso de Program Planning.
 *
 * <p>Contiene el resumen ejecutivo del trimestre, la distribución de actividades en los sprints y
 * la lista de especificaciones documentales generadas por frente.
 *
 * @param quarter            trimestre planificado (ej. "Q3-2026")
 * @param executiveSummary   resumen estratégico de la planeación
 * @param allocations        distribución de HUs y HAs a lo largo de los sprints
 * @param generatedSpecNames nombres de los archivos de spec generados o actualizados
 */
@Builder
public record ProgramPlanResult(
        String quarter,
        String executiveSummary,
        List<SprintAllocation> allocations,
        List<String> generatedSpecNames
) {

    public ProgramPlanResult {
        if (quarter == null || quarter.isBlank()) {
            throw new IllegalArgumentException("El trimestre (quarter) es obligatorio");
        }
        if (executiveSummary == null || executiveSummary.isBlank()) {
            throw new IllegalArgumentException("El resumen ejecutivo es obligatorio");
        }
        quarter = quarter.trim();
        executiveSummary = executiveSummary.trim();
        allocations = allocations == null ? List.of() : List.copyOf(allocations);
        generatedSpecNames =
                generatedSpecNames == null ? List.of() : List.copyOf(generatedSpecNames);
    }

    /**
     * Calcula el total acumulado de puntos de historia en todas las asignaciones.
     *
     * @return suma de story points
     */
    public int totalStoryPoints() {
        return allocations.stream()
                .mapToInt(SprintAllocation::storyPoints)
                .sum();
    }

    /**
     * Filtra las asignaciones correspondientes a un sprint específico.
     *
     * @param sprintNumber número del sprint
     * @return lista inmutable de asignaciones del sprint
     */
    public List<SprintAllocation> allocationsForSprint(int sprintNumber) {
        return allocations.stream()
                .filter(allocation -> allocation.sprintNumber() == sprintNumber)
                .toList();
    }

    /**
     * Filtra las asignaciones por tipo de actividad (HU o HA).
     *
     * @param type tipo de actividad deseado
     * @return lista inmutable de asignaciones de ese tipo
     */
    public List<SprintAllocation> allocationsForType(ActivityType type) {
        if (type == null) {
            return List.of();
        }
        return allocations.stream()
                .filter(allocation -> allocation.type() == type)
                .toList();
    }
}
