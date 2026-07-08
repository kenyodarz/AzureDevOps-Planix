package co.com.bancolombia.consumer;

import lombok.Data;
import java.util.List;

@Data
public class WiqlResultDTO {
    private String queryType;
    private String queryResultType;
    private String asOf;
    private List<WorkItemReferenceDTO> workItems;
}
