package co.com.bancolombia.model.workitem;

/**
 * Nombre de una célula (equipo) de Azure DevOps, ya normalizado.
 *
 * <p><b>Value Object inmutable.</b> Encapsula dos de los «comportamientos tolerantes» que
 * {@code CONTRATO-MCP.md} §2.4 declara <b>contrato de facto</b> y que hasta la Fase 04 vivían
 * incrustados en el entry-point (deuda <b>D-08</b>):
 *
 * <ol>
 *   <li>el nombre admite <b>barras dobles</b> ({@code \\}), que se colapsan a una;</li>
 *   <li>el nombre admite la <b>ruta completa</b>, en cuyo caso se toma el último tramo para
 *       preguntarle a Azure DevOps ({@link #shortName()}), pero se conserva la ruta entera para
 *       el repliegue por concatenación ({@link #value()}).</li>
 * </ol>
 *
 * <p>Esa distinción entre {@code value()} y {@code shortName()} <b>no es cosmética</b>: el código
 * original consultaba el gateway con el último tramo y fabricaba el {@code AreaPath} de repliegue
 * con la ruta completa. Fundir ambos cambiaría la sentencia WIQL en la rama de repliegue.
 */
public record TeamName(String value) {

    private static final String PATH_SEPARATOR = "\\";
    private static final String DOUBLE_PATH_SEPARATOR = "\\\\";

    public TeamName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la célula no puede ser nulo ni estar en blanco");
        }
    }

    /**
     * Construye el objeto de valor a partir del texto crudo recibido por la herramienta MCP,
     * colapsando las barras dobles.
     */
    public static TeamName of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("El nombre de la célula no puede ser nulo");
        }
        return new TeamName(raw.replace(DOUBLE_PATH_SEPARATOR, PATH_SEPARATOR));
    }

    /**
     * Último tramo de la ruta, que es lo que Azure DevOps espera como nombre de equipo.
     *
     * <p>Si el nombre no contiene separadores se devuelve tal cual, <b>sin recortar espacios</b>:
     * así se comportaba el entry-point y así debe seguir comportándose.
     */
    public String shortName() {
        if (value.contains(PATH_SEPARATOR)) {
            return value.substring(value.lastIndexOf(PATH_SEPARATOR) + 1).trim();
        }
        return value;
    }
}

