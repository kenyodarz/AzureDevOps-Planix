package co.com.bancolombia.model.workitem;

import java.util.Arrays;
import java.util.List;

/**
 * Tipos de elemento de trabajo por los que se filtra una consulta WIQL, ya normalizados,
 * traducidos y entrecomillados.
 *
 * <p><b>Value Object inmutable.</b> Reúne las tres reglas de Azure DevOps que la deuda <b>D-08</b>
 * señalaba como incrustadas en el entry-point y que <b>DP-04 §0.2 resolvió como regla de
 * dominio</b> —no como configuración—, en línea con §4.1 del plan maestro:
 *
 * <ol>
 *   <li>los tipos por defecto son {@code 'Historia de Usuario'} y {@code 'Habilitador'};</li>
 *   <li>{@code User Story} se traduce a {@code Historia de Usuario} (sin distinguir mayúsculas);</li>
 *   <li>cada tipo se entrecomilla para el WIQL, <b>sin duplicar</b> las comillas si ya las trae.</li>
 * </ol>
 *
 * <p><b>Deliberadamente no deduplica.</b> §4.1 del plan maestro sugería «normaliza y deduplica»,
 * pero el código original no deduplicaba: hacerlo aquí cambiaría la sentencia WIQL emitida para una
 * entrada con tipos repetidos, y la regla de oro de la Fase 04 es que la sentencia salga idéntica
 * carácter a carácter.
 */
public record WorkItemTypes(List<String> values) {

    private static final String QUOTE = "'";
    private static final String SEPARATOR = ",";
    private static final String USER_STORY = "User Story";
    private static final String HISTORIA_DE_USUARIO = "Historia de Usuario";
    private static final String HABILITADOR = "Habilitador";

    public WorkItemTypes {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    "La lista de tipos de elemento de trabajo no puede ser nula ni estar vacía");
        }
        values = List.copyOf(values);
    }

    /**
     * Tipos por defecto cuando la herramienta MCP no recibe ninguno.
     *
     * @return {@code 'Historia de Usuario','Habilitador'}
     */
    public static WorkItemTypes defaults() {
        return new WorkItemTypes(
                List.of(quote(HISTORIA_DE_USUARIO), quote(HABILITADOR)));
    }

    /**
     * Interpreta la lista separada por comas que llega por el cable.
     *
     * @param csv lista separada por comas; si es nula o está en blanco se usan {@link #defaults()}
     */
    public static WorkItemTypes parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return defaults();
        }
        return new WorkItemTypes(Arrays.stream(csv.split(SEPARATOR))
                .map(String::trim)
                .map(WorkItemTypes::normalize)
                .map(WorkItemTypes::quote)
                .toList());
    }

    /**
     * Fragmento listo para insertarse en la cláusula {@code IN (...)} de la sentencia WIQL.
     */
    public String toWiqlList() {
        return String.join(SEPARATOR, values);
    }

    private static String normalize(String type) {
        return USER_STORY.equalsIgnoreCase(type) ? HISTORIA_DE_USUARIO : type;
    }

    private static String quote(String type) {
        if (type.startsWith(QUOTE) && type.endsWith(QUOTE)) {
            return type;
        }
        return QUOTE + type + QUOTE;
    }
}

