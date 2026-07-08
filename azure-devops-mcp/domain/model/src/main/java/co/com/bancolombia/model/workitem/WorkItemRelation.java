package co.com.bancolombia.model.workitem;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkItemRelation {
    private String rel;
    private String url;
    private Map<String, Object> attributes;
}
