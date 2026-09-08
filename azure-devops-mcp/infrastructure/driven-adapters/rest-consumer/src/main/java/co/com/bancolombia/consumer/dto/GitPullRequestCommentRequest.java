package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte para la solicitud de creación de un hilo de comentarios en un Pull Request de
 * Azure DevOps Git API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitPullRequestCommentRequest {

    private List<CommentItemRequest> comments;
    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentItemRequest {

        private Integer parentCommentId;
        private String content;
        private Integer commentType;
    }
}
