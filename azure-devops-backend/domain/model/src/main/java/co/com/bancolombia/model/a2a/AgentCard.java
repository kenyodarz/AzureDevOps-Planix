package co.com.bancolombia.model.a2a;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - AgentCard
 * <p>
 * Digital "business card" for agent discovery. Served at GET /.well-known/agent-card.json
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentCard {

    /**
     * A2A protocol version this agent conforms to.
     */
    private String protocolVersion;

    /**
     * Human-readable name of the agent.
     */
    private String name;

    /**
     * Human-readable description of what this agent does.
     */
    private String description;

    /**
     * Semantic version of the agent implementation.
     */
    private String version;

    /**
     * Organization info for this agent.
     */
    private AgentProvider provider;

    /**
     * List of transport interfaces exposed by this agent (protocol + URL).
     */
    private List<AgentInterface> supportedInterfaces;

    /**
     * Streaming and push-notification capabilities.
     */
    private AgentCapabilities capabilities;

    /**
     * Supported input content types (e.g., "text", "data").
     */
    private List<String> defaultInputModes;

    /**
     * Supported output content types (e.g., "text", "data").
     */
    private List<String> defaultOutputModes;

    /**
     * Skills this agent can perform.
     */
    private List<AgentSkill> skills;

    /**
     * Security schemes required to invoke this agent. Key = scheme name (e.g., "oauth2"), value =
     * scheme definition object.
     */
    @SuppressWarnings("java:S1948")
    private Map<String, Object> securitySchemes;

    /**
     * Tags for indexing/discovery in registries.
     */
    private List<String> tags;
}
