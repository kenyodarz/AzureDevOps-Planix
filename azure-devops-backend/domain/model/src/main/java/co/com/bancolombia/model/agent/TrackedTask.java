package co.com.bancolombia.model.agent;

import co.com.bancolombia.model.a2a.Task;
import java.util.Objects;

/**
 * Una tarea junto con su procedencia.
 *
 * <p>Se introduce deliberadamente <b>en lugar de añadir un campo a {@link Task}</b>: {@code Task}
 * es el modelo A2A compartido con el agente y debe seguir siendo fiel al protocolo. El origen es
 * una necesidad del BFF, no del contrato A2A, y por eso se modela envolviéndola.
 *
 * @param task   tarea tal como la conoce su productor; obligatoria
 * @param origin quién la originó; obligatorio
 */
public record TrackedTask(Task task, TaskOrigin origin) {

    public TrackedTask {
        Objects.requireNonNull(task, "La tarea rastreada no puede ser nula");
        Objects.requireNonNull(origin, "El origen de la tarea es obligatorio");
    }

    /**
     * Etiqueta una tarea como propia del agente.
     *
     * @param task tarea recibida del agente
     * @return la tarea con su origen
     */
    public static TrackedTask fromAgent(Task task) {
        return new TrackedTask(task, TaskOrigin.AGENT);
    }

    /**
     * Etiqueta una tarea como propia del BFF.
     *
     * @param task tarea creada por el propio BFF
     * @return la tarea con su origen
     */
    public static TrackedTask fromBff(Task task) {
        return new TrackedTask(task, TaskOrigin.BFF);
    }

    /**
     * Identificador de la tarea, usado para deduplicar el listado combinado.
     *
     * @return el identificador; puede ser {@code null} si el productor no lo informó
     */
    public String id() {
        return task.getId();
    }
}

