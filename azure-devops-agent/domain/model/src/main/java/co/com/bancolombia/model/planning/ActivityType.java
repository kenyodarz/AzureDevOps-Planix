package co.com.bancolombia.model.planning;

import java.util.Locale;

/**
 * Tipifica las actividades planificadas en el marco de trabajo ágil según DP-PL-02.
 *
 * <ul>
 *   <li><b>HU (USER_STORY):</b> Historia de Usuario orientada a construir funcionalidad desde diseño
 *       hasta pruebas en ambiente QA.</li>
 *   <li><b>HA (ENABLER):</b> Historia Habilitadora orientada a setups técnicos previos, ciberseguridad,
 *       observabilidad y el proceso formal de Paso a Producción bajo el marco HyMS (Runbook y soporte).</li>
 * </ul>
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-PL-02
 */
public enum ActivityType {

    /**
     * Historia de Usuario funcional (construcción hasta ambiente QA).
     */
    USER_STORY("HU"),

    /**
     * Historia Habilitadora técnica o de paso a producción HyMS.
     */
    ENABLER("HA");

    private final String tag;

    ActivityType(String tag) {
        this.tag = tag;
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
        for (ActivityType type : values()) {
            if (type.tag.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tag de actividad no reconocido: " + tag);
    }

    /**
     * Retorna el identificador corto de la actividad (HU o HA).
     *
     * @return tag de la actividad
     */
    public String tag() {
        return tag;
    }

    /**
     * Alias JavaBeans para interoperabilidad.
     *
     * @return tag de la actividad
     */
    public String getTag() {
        return tag;
    }
}
