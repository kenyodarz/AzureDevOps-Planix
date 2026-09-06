package co.com.bancolombia.model.planning;

import java.util.List;
import lombok.Builder;

/**
 * Modela la asignación de una actividad (Historia de Usuario o Habilitadora) a un sprint.
 *
 * <p>Record inmutable que valida sus invariantes en el constructor compacto y garantiza que las
 * dependencias sean inmutables y no nulas.
 *
 * @param sprintNumber número del sprint al que se asigna (mayor a cero)
 * @param type         tipo de actividad (USER_STORY o ENABLER)
 * @param title        título descriptivo de la actividad
 * @param description  descripción detallada o criterio de entrega
 * @param storyPoints  puntos de historia estimados (mayor o igual a cero)
 * @param front        frente o squad responsable
 * @param dependencies identificadores o nombres de actividades prerrequisito
 */
@Builder
public record SprintAllocation(
        int sprintNumber,
        ActivityType type,
        String title,
        String description,
        int storyPoints,
        String front,
        List<String> dependencies
) {

    public SprintAllocation {
        if (sprintNumber <= 0) {
            throw new IllegalArgumentException("El número de sprint debe ser mayor a cero");
        }
        if (type == null) {
            throw new IllegalArgumentException("El tipo de actividad es obligatorio");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título de la actividad es obligatorio");
        }
        if (storyPoints < 0) {
            throw new IllegalArgumentException("Los story points no pueden ser negativos");
        }
        title = title.trim();
        description = description == null ? "" : description.trim();
        front = front == null ? "" : front.trim();
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }
}
