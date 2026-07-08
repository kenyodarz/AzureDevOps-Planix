package co.com.bancolombia.model.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    private DashboardMetricsResponse metrics;
    private List<DashboardStoryItemResponse> items;

}

