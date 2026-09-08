package co.com.bancolombia.model.pullrequest;

import lombok.Builder;
import lombok.Getter;

/**
 * Metadatos inmutables de un Pull Request consultado desde Azure DevOps.
 */
@Getter
@Builder(toBuilder = true)
public class PullRequestInfo {

    private final int pullRequestId;
    private final String repositoryId;
    private final String title;
    private final String description;
    private final String sourceRefName;
    private final String targetRefName;
    private final String status;
    private final String createdBy;
}
