package co.com.bancolombia.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para deserializar una corrida de pipeline devuelta por Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineRunDTO {

    private Integer id;
    private String name;
    private String status;
    private String result;
    private String createdDate;
    private String finishedDate;
    private PipelineRefDTO pipeline;
    private PipelineResourcesDTO resources;

    @JsonProperty("_links")
    private Map<String, LinkDTO> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineRefDTO {

        private Integer id;
        private String name;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineResourcesDTO {

        private Map<String, RepositoryResourceDTO> repositories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositoryResourceDTO {

        private String refName;
        private String version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkDTO {

        private String href;
    }
}
