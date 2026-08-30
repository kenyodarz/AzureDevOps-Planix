package co.com.bancolombia.model.agent;

/**
 * Quién originó la tarea que el frontend ve en su panel de progreso.
 *
 * <p>Existen <b>dos productores</b> de tareas en el sistema y el panel debe mostrar la foto
 * completa, pero distinguible (DP-07-bis, opción B2):
 *
 * <ul>
 *   <li>{@link #AGENT}: el agente autónomo, al procesar {@code message/send}. Ejemplo: «refina
 *       esta historia».</li>
 *   <li>{@link #BFF}: el propio BFF, en el stream del dashboard. Ejemplo: «analiza el sprint
 *       247».</li>
 * </ul>
 *
 * <p>Etiquetar el origen es lo que permite al BFF <b>dejar de escribir</b> las tareas ajenas sin
 * que el frontend pierda visibilidad (deuda D-24).
 */
public enum TaskOrigin {

    /**
     * Tarea abierta y gobernada por el agente autónomo.
     */
    AGENT,

    /**
     * Tarea propia del BFF, creada por sus flujos internos.
     */
    BFF
}

