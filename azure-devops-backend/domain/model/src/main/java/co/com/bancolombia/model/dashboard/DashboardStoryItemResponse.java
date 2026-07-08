package co.com.bancolombia.model.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

