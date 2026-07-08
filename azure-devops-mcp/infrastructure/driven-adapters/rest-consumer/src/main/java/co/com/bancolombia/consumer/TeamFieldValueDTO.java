package co.com.bancolombia.consumer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamFieldValueDTO {

    private String value;
    private boolean includeChildren;
}

