package co.com.bancolombia.model.a2a;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - AgentSkill. Describes a specific capability of the agent.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentSkill {

    private String id;
    private String name;
    private String description;
    private List<String> tags;
    private List<String> inputModes;
    private List<String> outputModes;
    private List<String> examples;
    private java.util.Map<String, Object> schema;
}
