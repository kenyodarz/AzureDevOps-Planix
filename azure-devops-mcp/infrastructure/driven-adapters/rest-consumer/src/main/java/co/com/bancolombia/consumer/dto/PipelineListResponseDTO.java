package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la lista de pipelines devuelta por la API de Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineListResponseDTO {

    private Integer count;
    private List<PipelineDTO> value;
}
