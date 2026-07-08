package co.com.bancolombia.model.workitem;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkItem {
    private Integer id;
    private Integer rev;
    private Map<String, Object> fields;
    private List<WorkItemRelation> relations;
    private String url;
}
