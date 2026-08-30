package co.com.bancolombia.api.dto.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Historia del tablero en su forma serializable. Ver {@link DashboardResponse}.
 */
@Setter
@Getter
@NoArgsConstructor
public class DashboardStoryItemResponse {

    private String id;
    private String title;
    private int points;
    private String state;
    private String assignedMember;
    private boolean hasAcceptanceCriteria;
    private boolean hasDoD;
    private int qualityScore;
    private int linkedTasksCount;
    private String feedback;

}

