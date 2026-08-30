package co.com.bancolombia.api.dto.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Métricas del tablero en su forma serializable. Ver {@link DashboardResponse}.
 */
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

