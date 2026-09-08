package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte para deserializar la respuesta de un hilo de comentarios en un Pull Request
 * retornado por la API REST de Azure DevOps Git.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitPullRequestCommentResponse {

    private Integer id;
    private String status;
    private List<CommentItemResponse> comments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentItemResponse {

        private Integer id;
        private Integer parentCommentId;
        private String content;
        private IdentityRefDTO author;
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
}
