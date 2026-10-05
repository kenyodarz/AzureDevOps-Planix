package co.com.bancolombia.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para deserializar la representación de un Pipeline en Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineDTO {

    private Integer id;
    private String name;
    private String folder;
    private Integer revision;
    private String url;
}
