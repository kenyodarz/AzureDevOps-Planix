package co.com.bancolombia.model.a2a;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - Task Stateful unit of work initiated by a client agent. Lifecycle: submitted
 * → working → completed | failed | canceled | rejected | input-required
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    /**
     * Unique identifier for this task. Server-generated.
     */
    private String id;

    /**
     * Groups related tasks within the same conversation/session.
     */
    private String contextId;

    /**
     * Current lifecycle status (state + optional inline response message).
     */
    private TaskStatus status;

    /**
     * Tangible outputs produced by the agent during this task.
     */
    private List<Artifact> artifacts;

    /**
     * Message history for this task (conversation turns).
     */
    private List<Message> history;

    /**
     * Arbitrary metadata for extension/tracing.
     */
    @SuppressWarnings("java:S1948")
    private Map<String, Object> metadata;
}
