package co.com.bancolombia.api.dto.event;

import co.com.bancolombia.api.dto.task.TaskResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento que viaja por el stream SSE de tareas del agente y del BFF.
 *
 * <p>Permite al frontend recibir actualizaciones en tiempo real (INITIAL, TASKS_UPDATE,
 * HEARTBEAT, ERROR) sin recurrir al sondeo periódico por HTTP (polling).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateEvent {

    public static final String EVENT_INITIAL = "INITIAL";
    public static final String EVENT_TASKS_UPDATE = "TASKS_UPDATE";
    public static final String EVENT_HEARTBEAT = "HEARTBEAT";
    public static final String EVENT_ERROR = "ERROR";

    private String event;
    private List<TaskResponse> data;
    private String message;

    public TaskUpdateEvent(String event, List<TaskResponse> data) {
        this(event, data, null);
    }

    public static TaskUpdateEvent ofError(String message) {
        return new TaskUpdateEvent(EVENT_ERROR, null, message);
    }
}
