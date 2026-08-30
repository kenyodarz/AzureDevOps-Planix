package co.com.bancolombia.model.workitem;

/**
 * Nombre de un sprint (iteración) de Azure DevOps, ya normalizado.
 *
 * <p><b>Value Object inmutable.</b> Igual que {@link TeamName}, colapsa las <b>barras dobles</b>
 * que el cliente MCP puede enviar ({@code CONTRATO-MCP.md} §2.4, punto 1).
 *
 * <p>A diferencia de {@link TeamName}, aquí <b>no</b> se recorta el último tramo: el entry-point
 * pasaba el sprint completo tanto al caso de uso de iteraciones —que ya se queda con el último
 * tramo por su cuenta— como al repliegue por concatenación, que necesita la ruta entera para
 * decidir si ya empieza por el nombre del proyecto.
 */
public record SprintName(String value) {

    private static final String PATH_SEPARATOR = "\\";
    private static final String DOUBLE_PATH_SEPARATOR = "\\\\";

    public SprintName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del sprint no puede ser nulo ni estar en blanco");
        }
    }

    /**
     * Construye el objeto de valor a partir del texto crudo recibido por la herramienta MCP,
     * colapsando las barras dobles.
     */
    public static SprintName of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("El nombre del sprint no puede ser nulo");
        }
        return new SprintName(raw.replace(DOUBLE_PATH_SEPARATOR, PATH_SEPARATOR));
    }
}

