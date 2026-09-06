package co.com.bancolombia.model.planning;

import java.util.List;
import java.util.Locale;
import lombok.Builder;

/**
 * Value Object inmutable que materializa la decisión DP-BFF-03 para encapsular la solicitud de
 * planeación macro y construir el comando canónico A2A despachado al Agente.
 *
 * @param quarter              trimestre objetivo en formato QX-YYYY (ej. "Q3-2026")
 * @param sprintCount          cantidad de sprints del período (estrictamente mayor a 0)
 * @param maxCapacityPerSprint capacidad máxima en puntos de historia por sprint (estrictamente
 *                             mayor a 0)
 * @param targetFronts         frentes o squads involucrados
 * @param objectives           objetivos específicos de la planeación
 * @param contextId            identificador de contexto de la sesión o tarea
 */
@Builder
public record ProgramPlanningCommand(
        String quarter,
        int sprintCount,
        int maxCapacityPerSprint,
        List<String> targetFronts,
        String objectives,
        String contextId
) {

    public ProgramPlanningCommand {
        if (quarter == null || !quarter.trim().matches("^[qQ][1-4]-\\d{4}$")) {
            throw new IllegalArgumentException(
                    "El trimestre debe cumplir el formato QX-YYYY (ej. Q3-2026)");
        }
        if (sprintCount <= 0) {
            throw new IllegalArgumentException("El número de sprints debe ser mayor a 0");
        }
        if (maxCapacityPerSprint <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor a 0");
        }
        quarter = quarter.trim().toUpperCase(Locale.ROOT);
        targetFronts = targetFronts == null ? List.of() : List.copyOf(targetFronts);
        objectives = objectives == null ? "" : objectives.trim();
        contextId = contextId == null ? "" : contextId.trim();
    }

    /**
     * Construye un comando a partir de una solicitud de planeación y un identificador de contexto.
     *
     * @param request   solicitud de planeación de entrada; no puede ser nula
     * @param contextId identificador de contexto de la tarea
     * @return comando de planeación validado
     */
    public static ProgramPlanningCommand fromRequest(ProgramPlanRequest request, String contextId) {
        if (request == null) {
            throw new IllegalArgumentException("El request de planeación no puede ser nulo");
        }
        return new ProgramPlanningCommand(
                request.quarter(),
                request.sprintCount(),
                request.maxCapacityPerSprint(),
                request.targetFronts(),
                request.objectives(),
                contextId
        );
    }

    /**
     * Construye un comando a partir de una solicitud de planeación sin identificador de contexto
     * inicial.
     *
     * @param request solicitud de planeación de entrada; no puede ser nula
     * @return comando de planeación validado
     */
    public static ProgramPlanningCommand fromRequest(ProgramPlanRequest request) {
        return fromRequest(request, null);
    }

    /**
     * Serializa los parámetros al comando canónico reconocido por el IntentResolver del Agente.
     * Ejemplo:
     * {@code /plan Q3-2026 6 sprints 34 sp [Canales, Core] Objetivos: Modernización Cloud}
     *
     * @return comando canónico formateado
     */
    public String toCanonicalCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("/plan ").append(quarter)
                .append(" ").append(sprintCount).append(" sprints")
                .append(" ").append(maxCapacityPerSprint).append(" sp");
        if (!targetFronts.isEmpty()) {
            sb.append(" [").append(String.join(", ", targetFronts)).append("]");
        }
        if (!objectives.isBlank()) {
            sb.append(" Objetivos: ").append(objectives);
        }
        return sb.toString();
    }

    /**
     * Alias de interoperabilidad hacia el prompt del agente.
     *
     * @return prompt formateado para el agente
     */
    public String toAgentPrompt() {
        return toCanonicalCommand();
    }
}
