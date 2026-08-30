package co.com.bancolombia.consumer.dto;

import lombok.Data;
import java.util.List;

@Data
public class WorkItemsBatchResponseDTO {
    private Integer count;
    private List<WorkItemDTO> value;
}
