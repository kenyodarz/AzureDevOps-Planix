package co.com.bancolombia.api.dto.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoryUpdateEvent {

    private String id;
    private boolean hasAcceptanceCriteria;
    private boolean hasDoD;
    private int qualityScore;
    private int linkedTasksCount;
    private String feedback;

}

