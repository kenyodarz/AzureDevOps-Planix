package co.com.bancolombia.consumer;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class WorkItemDTO {
    private Integer id;
    private Integer rev;
    private Map<String, Object> fields;
    private List<WorkItemRelationDTO> relations;
    private String url;
}
