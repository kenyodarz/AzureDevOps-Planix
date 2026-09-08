package co.com.bancolombia.usecase.pullrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.pullrequest.PullRequestComment;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreatePullRequestCommentUseCaseTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final String REPO = "repoTest";
    private static final int PR_ID = 202;
    private static final String API_VERSION = "7.1";

    @Mock
    private PullRequestPort pullRequestPort;

    private CreatePullRequestCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePullRequestCommentUseCase(pullRequestPort);
    }

    @Test
    @DisplayName("GIVEN valid arguments WHEN createComment is called THEN delegates to port and returns comment")
    void givenValidArguments_whenCreateComment_thenDelegatesAndReturnsComment() {
        PullRequestComment inputComment = PullRequestComment.builder()
                .content("Sugerencia de refactor: extraer método auxiliar")
                .status("active")
                .build();

        PullRequestComment expectedComment = inputComment.toBuilder()
                .id(999)
                .author("Bot Evaluador")
                .build();

        when(pullRequestPort.createComment(ORG, PROJECT, REPO, PR_ID, inputComment, API_VERSION))
                .thenReturn(Mono.just(expectedComment));

        StepVerifier.create(
                        useCase.createComment(ORG, PROJECT, REPO, PR_ID, inputComment, API_VERSION))
                .expectNext(expectedComment)
                .verifyComplete();

        verify(pullRequestPort).createComment(ORG, PROJECT, REPO, PR_ID, inputComment, API_VERSION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid organization WHEN createComment is called THEN returns error and does not call port")
    void givenInvalidOrganization_whenCreateComment_thenReturnsError(String invalidOrg) {
        PullRequestComment comment = PullRequestComment.builder().content("Contenido válido")
                .build();

        StepVerifier.create(
                        useCase.createComment(invalidOrg, PROJECT, REPO, PR_ID, comment, API_VERSION))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().contains("organization"))
                .verify();

        verify(pullRequestPort, never()).createComment(any(), any(), any(), anyInt(), any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid project WHEN createComment is called THEN returns error and does not call port")
    void givenInvalidProject_whenCreateComment_thenReturnsError(String invalidProject) {
        PullRequestComment comment = PullRequestComment.builder().content("Contenido válido")
                .build();

        StepVerifier.create(
                        useCase.createComment(ORG, invalidProject, REPO, PR_ID, comment, API_VERSION))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().contains("project"))
                .verify();

        verify(pullRequestPort, never()).createComment(any(), any(), any(), anyInt(), any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid repositoryId WHEN createComment is called THEN returns error and does not call port")
    void givenInvalidRepositoryId_whenCreateComment_thenReturnsError(String invalidRepo) {
        PullRequestComment comment = PullRequestComment.builder().content("Contenido válido")
                .build();

        StepVerifier.create(
                        useCase.createComment(ORG, PROJECT, invalidRepo, PR_ID, comment, API_VERSION))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().contains("repositoryId"))
                .verify();

        verify(pullRequestPort, never()).createComment(any(), any(), any(), anyInt(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("GIVEN non-positive pullRequestId WHEN createComment is called THEN returns error and does not call port")
    void givenNonPositivePullRequestId_whenCreateComment_thenReturnsError(int invalidPrId) {
        PullRequestComment comment = PullRequestComment.builder().content("Contenido válido")
                .build();

        StepVerifier.create(
                        useCase.createComment(ORG, PROJECT, REPO, invalidPrId, comment, API_VERSION))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().contains("pullRequestId"))
                .verify();

        verify(pullRequestPort, never()).createComment(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("GIVEN null comment WHEN createComment is called THEN returns error and does not call port")
    void givenNullComment_whenCreateComment_thenReturnsError() {
        StepVerifier.create(useCase.createComment(ORG, PROJECT, REPO, PR_ID, null, API_VERSION))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().contains("comment no puede ser nulo"))
                .verify();

        verify(pullRequestPort, never()).createComment(any(), any(), any(), anyInt(), any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN comment with null or blank content WHEN createComment is called THEN returns error and does not call port")
    void givenBlankCommentContent_whenCreateComment_thenReturnsError(String invalidContent) {
        PullRequestComment comment = PullRequestComment.builder()
                .content(invalidContent)
                .build();

        StepVerifier.create(useCase.createComment(ORG, PROJECT, REPO, PR_ID, comment, API_VERSION))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().contains("el contenido del comentario"))
                .verify();

        verify(pullRequestPort, never()).createComment(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("GIVEN port error WHEN createComment is called THEN propagates error")
    void givenPortError_whenCreateComment_thenPropagatesError() {
        PullRequestComment comment = PullRequestComment.builder().content("Contenido válido")
                .build();

        when(pullRequestPort.createComment(ORG, PROJECT, REPO, PR_ID, comment, API_VERSION))
                .thenReturn(Mono.error(new RuntimeException("Simulated error in port")));

        StepVerifier.create(useCase.createComment(ORG, PROJECT, REPO, PR_ID, comment, API_VERSION))
                .expectErrorMessage("Simulated error in port")
                .verify();

        verify(pullRequestPort).createComment(ORG, PROJECT, REPO, PR_ID, comment, API_VERSION);
    }
}
