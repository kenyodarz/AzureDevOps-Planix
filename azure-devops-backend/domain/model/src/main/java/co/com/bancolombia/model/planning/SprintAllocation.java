package co.com.bancolombia.model.planning;

import lombok.Builder;

/**
 * Modela la asignación inmutable de una actividad (Historia de Usuario o Habilitador) a un sprint.
 *
 * @param sprintNumber número del sprint al que se asigna (estrictamente mayor a cero)
 * @param initiativeId identificador de la iniciativa macro a la que pertenece
 * @param title        título descriptivo de la actividad
 * @param storyPoints  estimación en puntos de historia (estrictamente mayor a cero)
 * @param type         tipo de actividad (HU o HA)
 * @param front        frente o squad responsable de la entrega
 */
@Builder
public record SprintAllocation(
        int sprintNumber,
        String initiativeId,
        String title,
        int storyPoints,
        ActivityType type,
        String front
) {

    public SprintAllocation {
        if (sprintNumber <= 0) {
            throw new IllegalArgumentException("El número de sprint debe ser mayor a cero");
        }
        if (storyPoints <= 0) {
            throw new IllegalArgumentException("Los story points deben ser mayores a cero");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título de la actividad es obligatorio");
        }
        if (type == null) {
            throw new IllegalArgumentException("El tipo de actividad es obligatorio");
        }
        title = title.trim();
        initiativeId = initiativeId == null ? "" : initiativeId.trim();
        front = front == null ? "" : front.trim();
    }
}
