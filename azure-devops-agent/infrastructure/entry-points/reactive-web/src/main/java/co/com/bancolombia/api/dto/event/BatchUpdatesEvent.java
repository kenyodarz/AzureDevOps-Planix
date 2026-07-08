package co.com.bancolombia.api.dto.event;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class BatchUpdatesEvent {

    private List<StoryUpdateEvent> updates;

}

