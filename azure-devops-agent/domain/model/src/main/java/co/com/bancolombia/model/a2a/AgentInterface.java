package co.com.bancolombia.model.a2a;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - AgentInterface. Protocol+URL describing where the agent listens.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentInterface {

    /**
     * Transport protocol: "jsonrpc", "grpc", "rest"
     */
    private String protocol;
    /**
     * Base URL of the A2A endpoint.
     */
    private String url;
}
