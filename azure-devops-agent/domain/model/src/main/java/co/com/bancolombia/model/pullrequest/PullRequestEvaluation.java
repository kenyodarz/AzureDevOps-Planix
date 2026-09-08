package co.com.bancolombia.model.pullrequest;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Resultado integral e inmutable de la evaluación técnica y arquitectural de un Pull Request.
 */
@Getter
@Builder(toBuilder = true)
public class PullRequestEvaluation {

    private final int pullRequestId;
    private final String repositoryId;
    private final EvaluationVerdict verdict;
    private final String summary;
    private final List<EvaluationFinding> findings;
    private final List<String> recommendations;
    private final boolean commentPublished;
    private final String publishedCommentId;
}
