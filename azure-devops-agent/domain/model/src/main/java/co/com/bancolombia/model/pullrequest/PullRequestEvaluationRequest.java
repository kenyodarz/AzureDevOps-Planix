package co.com.bancolombia.model.pullrequest;

import lombok.Builder;
import lombok.Getter;

/**
 * Petición de evaluación automatizada de un Pull Request.
 */
@Getter
@Builder(toBuilder = true)
public class PullRequestEvaluationRequest {

    private final String organization;
    private final String project;
    private final String repositoryId;
    private final int pullRequestId;
    private final boolean publishComment;
}
