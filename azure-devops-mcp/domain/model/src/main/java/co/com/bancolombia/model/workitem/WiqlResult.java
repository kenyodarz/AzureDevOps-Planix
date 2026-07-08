package co.com.bancolombia.model.workitem;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WiqlResult {
    private String queryType;
    private String queryResultType;
    private String asOf;
    private List<WorkItemReference> workItems;
}
