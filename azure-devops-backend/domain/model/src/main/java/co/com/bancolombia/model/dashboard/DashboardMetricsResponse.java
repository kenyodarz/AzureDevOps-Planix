package co.com.bancolombia.model.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class DashboardMetricsResponse {

    private int totalPoints;
    private int completedPoints;
    private int completedPercentage;
    private int avgQualityScore;
    private int undocumentedCount;

}

