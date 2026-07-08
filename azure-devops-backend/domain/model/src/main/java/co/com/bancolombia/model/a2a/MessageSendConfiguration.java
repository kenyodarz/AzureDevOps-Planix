package co.com.bancolombia.model.a2a;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - Optional configuration hints sent alongside a SendMessage call.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendConfiguration {

    /**
     * Output content modes the client can accept (e.g., "text", "data").
     */
    private List<String> acceptedOutputModes;
    /**
     * Number of history messages to include in the response Task.
     */
    private Integer historyLength;
    /**
     * If true, the request blocks until task is terminal (no streaming).
     */
    private Boolean blocking;
}
