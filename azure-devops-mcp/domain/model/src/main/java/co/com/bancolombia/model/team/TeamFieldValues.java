package co.com.bancolombia.model.team;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TeamFieldValues {

    private String defaultValue;
    private List<String> values;
}
