package co.com.bancolombia.consumer;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamFieldValuesDTO {

    private String defaultValue;
    private List<TeamFieldValueDTO> values;
}

