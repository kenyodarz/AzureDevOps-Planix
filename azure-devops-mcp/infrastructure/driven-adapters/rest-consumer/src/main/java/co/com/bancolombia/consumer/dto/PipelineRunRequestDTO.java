package co.com.bancolombia.consumer.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de disparo de una nueva corrida de pipeline en Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineRunRequestDTO {

    private PipelineResourcesRequestDTO resources;
    private Map<String, VariableValueDTO> variables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineResourcesRequestDTO {

        private Map<String, RepositoryRefRequestDTO> repositories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositoryRefRequestDTO {

        private String refName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableValueDTO {

        private String value;
    }
}
