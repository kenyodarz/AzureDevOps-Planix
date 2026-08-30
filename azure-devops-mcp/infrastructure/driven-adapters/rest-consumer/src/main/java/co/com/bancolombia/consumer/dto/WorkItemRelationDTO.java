package co.com.bancolombia.consumer.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WorkItemRelationDTO {
    private String rel;
    private String url;
    private Map<String, Object> attributes;
}
