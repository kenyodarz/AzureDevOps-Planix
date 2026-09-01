/**
 * Parámetros de ajuste de la aplicación.
 *
 * DP-06 — El usuario confirmó (2026-08-31) que estos intervalos **no** se definieron para una
 * actualización en vivo del tablero y que quedan registrados como deuda técnica reconocida. Se
 * aplica la acción por defecto documentada en `DECISIONES_PENDIENTES.md`: los valores se trasladan
 * LITERALMENTE desde `devops-agent-state.service.ts` (30000 y 5000), sin modificarlos y **sin
 * añadir topes de reintentos**. Cualquier ajuste requiere reabrir DP-06.
 */

/** Sondeo de tareas en reposo (sin tareas activas). Origen: `devops-agent-state.service.ts`. */
export const POLLING_INTERVAL_IDLE_MS = 30000;

/** Sondeo de tareas con al menos una tarea `submitted` o `working`. */
export const POLLING_INTERVAL_ACTIVE_MS = 5000;
