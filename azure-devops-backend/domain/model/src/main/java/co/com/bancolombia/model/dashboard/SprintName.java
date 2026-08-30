package co.com.bancolombia.model.dashboard;

/**
 * Nombre del sprint o iteración cuyo backlog se audita, por ejemplo {@code "Sprint 247"}.
 *
 * <p><b>Fase 05 (D-19, D-09, B-01).</b> Aquí vivía el peor fallo del BFF. El código anterior
 * detectaba el patrón {@code "Sprint N"} y fabricaba la ruta
 * {@code proyecto\LocalDate.now().getYear()\Sprint N}. Pero un sprint que va de diciembre de 2025 a
 * enero de 2026 <b>es de 2025</b>, porque así se llama su {@code IterationPath}, no porque lo diga
 * el calendario. Consultado en enero, el año calculado no coincidía, Azure DevOps devolvía una
 * iteración vacía y el tablero salía sin ítems: <b>sin error, sin aviso y sin log</b>. El usuario
 * no podía distinguir «este sprint no tiene historias» de «preguntamos por un sprint que no
 * existe».
 *
 * <p>El arreglo no fue calcular mejor el año: fue <b>dejar de calcularlo</b>. Quien sabe a qué
 * iteración corresponde un sprint es Azure DevOps, así que el BFF envía el nombre y la resolución
 * ocurre en el MCP, que ya tenía ese patrón implementado para el {@code AreaPath} (B-01 opción C).
 *
 * <p>Este tipo conserva {@link #isNumbered()} porque el patrón {@code "Sprint N"} sigue siendo
 * vocabulario del negocio; lo que ya no hace es derivar de él una ruta ni un año.
 */
public record SprintName(String value) {

    private static final String BLANK_MESSAGE = "El nombre del sprint no puede estar vacío";
    private static final String NUMBERED_PATTERN = "Sprint \\d+";

    public SprintName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(BLANK_MESSAGE);
        }
        value = value.trim();
    }

    public static SprintName of(String raw) {
        return new SprintName(raw);
    }

    /**
     * Indica si el sprint sigue la convención numerada {@code "Sprint N"}.
     */
    public boolean isNumbered() {
        return value.matches(NUMBERED_PATTERN);
    }

    @Override
    public String toString() {
        return value;
    }
}

