package co.com.bancolombia.model.a2a;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - AgentCapabilities.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentCapabilities {

    @Builder.Default
    private boolean streaming = false;

    @Builder.Default
    private boolean pushNotifications = false;
}
