package co.com.bancolombia.api.dto;

import co.com.bancolombia.model.planning.ProgramPlanRequest;
import java.util.List;

/**
 * Payload HTTP de entrada para solicitar la planeación macro trimestral (Program Planning).
 *
 * <p>Mapea al modelo de dominio inmutable {@link ProgramPlanRequest}.
 *
 * @param quarter              Trimestre objetivo (ej. "Q3-2026")
 * @param objectives           Objetivos funcionales y técnicos del período
 * @param targetFronts         Lista de frentes o squads que participan en la planeación
 * @param sprintCount          Cantidad de sprints en el período
 * @param maxCapacityPerSprint Capacidad estimada por sprint en puntos de historia
 * @param contextId            Identificador de sesión o conversación opcional para trazabilidad
 */
public record ProgramPlanRequestDTO(
        String quarter,
        String objectives,
        List<String> targetFronts,
        Integer sprintCount,
        Integer maxCapacityPerSprint,
        String contextId
) {

    public ProgramPlanRequestDTO {
        targetFronts = targetFronts == null ? List.of() : List.copyOf(targetFronts);
    }

    /**
     * Convierte el DTO HTTP en el modelo de dominio {@link ProgramPlanRequest}.
     *
     * @return instancia inmutable del modelo de dominio
     */
    public ProgramPlanRequest toDomain() {
        return ProgramPlanRequest.builder()
                .quarter(quarter)
                .objectives(objectives)
                .targetFronts(targetFronts)
                .sprintCount(sprintCount != null ? sprintCount : 0)
                .maxCapacityPerSprint(maxCapacityPerSprint != null ? maxCapacityPerSprint : 0)
                .build();
    }
}
