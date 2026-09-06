package co.com.bancolombia.model.planning;

import java.util.Locale;

/**
 * Tipifica las actividades planificadas en el marco de trabajo ágil.
 *
 * <ul>
 *   <li><b>HU:</b> Historia de Usuario orientada a construir funcionalidad de negocio desde diseño
 *       hasta pruebas en ambiente QA.</li>
 *   <li><b>HA:</b> Historia Habilitadora de Arquitectura orientada a setups técnicos previos,
 *       ciberseguridad, observabilidad y paso a producción bajo marco HyMS.</li>
 * </ul>
 */
public enum ActivityType {

    /**
     * Historia de Usuario funcional.
     */
    HU("HU", "Historia de Usuario"),

    /**
     * Habilitador de Arquitectura o historia técnica.
     */
    HA("HA", "Habilitador de Arquitectura");

    private final String code;
    private final String description;

    ActivityType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Resuelve el tipo de actividad a partir de su tag corto ("HU", "HA") o de su nombre.
     *
     * @param tag identificador a resolver; no puede ser nulo ni vacío
     * @return el tipo de actividad correspondiente
     * @throws IllegalArgumentException si el tag es nulo, está en blanco o no es reconocido
     */
    public static ActivityType fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("El tag de actividad es obligatorio");
        }
        String normalized = tag.trim().toUpperCase(Locale.ROOT);
        if ("USER_STORY".equals(normalized) || "HU".equals(normalized)
                || "HISTORIA DE USUARIO".equals(normalized)) {
            return HU;
        }
        if ("ENABLER".equals(normalized) || "HA".equals(normalized)
                || "HABILITADOR DE ARQUITECTURA".equals(normalized)
                || "HABILITADOR".equals(normalized)) {
            return HA;
        }
        throw new IllegalArgumentException("Tag de actividad no reconocido: " + tag);
    }

    /**
     * Alias de resolución tolerante a fallos.
     *
     * @param value valor a parsear
     * @return tipo de actividad
     */
    public static ActivityType parse(String value) {
        return fromTag(value);
    }

    /**
     * Alias de resolución tolerante a fallos para interoperabilidad.
     *
     * @param value valor a parsear
     * @return tipo de actividad
     */
    public static ActivityType fromString(String value) {
        return fromTag(value);
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public String tag() {
        return code;
    }

    public String getTag() {
        return code;
    }

    public String description() {
        return description;
    }

    public String getDescription() {
        return description;
    }
}
