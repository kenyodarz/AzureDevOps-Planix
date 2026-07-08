package co.com.bancolombia.model.a2a;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - TaskStatus. Encapsulates state + inline response message.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatus {

    private TaskState state;
    private Message message;
    private String timestamp;
}
