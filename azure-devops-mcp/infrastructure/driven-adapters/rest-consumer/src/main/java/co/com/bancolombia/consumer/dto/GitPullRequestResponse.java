package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte para deserializar la respuesta de la API REST de Azure DevOps correspondiente a
 * los detalles de un Pull Request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitPullRequestResponse {

    private Integer pullRequestId;
    private String title;
    private String description;
    private String status;
    private String sourceRefName;
    private String targetRefName;
    private GitRepositoryDTO repository;
    private IdentityRefDTO createdBy;
    private String creationDate;
    private List<ResourceRefDTO> workItemRefs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitRepositoryDTO {

        private String id;
        private String name;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityRefDTO {

        private String id;
        private String displayName;
        private String uniqueName;
        private String url;
        private String imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceRefDTO {

        private String id;
        private String url;
    }
}
