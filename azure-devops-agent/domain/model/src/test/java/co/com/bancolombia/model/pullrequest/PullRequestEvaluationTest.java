package co.com.bancolombia.model.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Modelos de Pull Request Evaluation")
class PullRequestEvaluationTest {

    @Test
    @DisplayName("Creación exitosa de PullRequestEvaluation con todos sus atributos")
    void givenValidData_whenBuildPullRequestEvaluation_thenReturnsInstance() {
        EvaluationFinding finding = EvaluationFinding.builder()
                .filePath("src/main/java/Foo.java")
                .severity("HIGH")
                .category("CLEAN_ARCHITECTURE")
                .message("Violación de dependencia circular")
                .suggestion("Invertir dependencia mediante puerto")
                .build();

        PullRequestEvaluation evaluation = PullRequestEvaluation.builder()
                .pullRequestId(101)
                .repositoryId("repo-core")
                .verdict(EvaluationVerdict.CHANGES_REQUESTED)
                .summary("Se detectaron violaciones arquitecturales")
                .findings(List.of(finding))
                .recommendations(List.of("Mover puertos a domain/model"))
                .commentPublished(true)
                .publishedCommentId("comment-999")
                .build();

        assertThat(evaluation.getPullRequestId()).isEqualTo(101);
        assertThat(evaluation.getRepositoryId()).isEqualTo("repo-core");
        assertThat(evaluation.getVerdict()).isEqualTo(EvaluationVerdict.CHANGES_REQUESTED);
        assertThat(evaluation.getSummary()).contains("violaciones arquitecturales");
        assertThat(evaluation.getFindings()).hasSize(1);
        assertThat(evaluation.getFindings().getFirst().getFilePath()).isEqualTo(
                "src/main/java/Foo.java");
        assertThat(evaluation.getFindings().getFirst().getSeverity()).isEqualTo("HIGH");
        assertThat(evaluation.getFindings().getFirst().getCategory()).isEqualTo(
                "CLEAN_ARCHITECTURE");
        assertThat(evaluation.getRecommendations()).containsExactly("Mover puertos a domain/model");
        assertThat(evaluation.isCommentPublished()).isTrue();
        assertThat(evaluation.getPublishedCommentId()).isEqualTo("comment-999");
    }

    @Test
    @DisplayName("Creación exitosa de PullRequestInfo y PullRequestChangeInfo")
    void givenValidData_whenBuildPullRequestInfoAndChanges_thenReturnsInstances() {
        PullRequestInfo prInfo = PullRequestInfo.builder()
                .pullRequestId(42)
                .repositoryId("repo-git")
                .title("feat: nueva funcionalidad")
                .description("Agrega casos de uso")
                .sourceRefName("refs/heads/feature/nueva")
                .targetRefName("refs/heads/main")
                .status("active")
                .createdBy("dev@bancolombia.com.co")
                .build();

        PullRequestChangeInfo change = PullRequestChangeInfo.builder()
                .path("/src/main/java/co/com/bancolombia/App.java")
                .changeType("edit")
                .build();

        assertThat(prInfo.getPullRequestId()).isEqualTo(42);
        assertThat(prInfo.getTitle()).isEqualTo("feat: nueva funcionalidad");
        assertThat(prInfo.getSourceRefName()).isEqualTo("refs/heads/feature/nueva");
        assertThat(change.getPath()).isEqualTo("/src/main/java/co/com/bancolombia/App.java");
        assertThat(change.getChangeType()).isEqualTo("edit");
    }

    @Test
    @DisplayName("Creación de PullRequestEvaluationRequest")
    void givenValidData_whenBuildRequest_thenReturnsInstance() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("repo-test")
                .pullRequestId(200)
                .publishComment(true)
                .build();

        assertThat(request.getOrganization()).isEqualTo("grupobancolombia");
        assertThat(request.getProject()).isEqualTo("VST");
        assertThat(request.getRepositoryId()).isEqualTo("repo-test");
        assertThat(request.getPullRequestId()).isEqualTo(200);
        assertThat(request.isPublishComment()).isTrue();
    }

    @Test
    @DisplayName("Valores del enum EvaluationVerdict")
    void givenEnumValues_whenCheck_thenContainsExpectedConstants() {
        assertThat(EvaluationVerdict.values()).containsExactly(
                EvaluationVerdict.APPROVED,
                EvaluationVerdict.CHANGES_REQUESTED,
                EvaluationVerdict.REJECTED
        );
    }
}
