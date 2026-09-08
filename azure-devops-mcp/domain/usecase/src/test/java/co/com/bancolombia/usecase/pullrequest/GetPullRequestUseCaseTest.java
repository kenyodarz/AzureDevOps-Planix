package co.com.bancolombia.usecase.pullrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.pullrequest.PullRequest;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestPort;
import java.util.List;
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
class GetPullRequestUseCaseTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final String REPO = "repoTest";
    private static final int PR_ID = 101;
    private static final String API_VERSION = "7.1";

    @Mock
    private PullRequestPort pullRequestPort;

    private GetPullRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPullRequestUseCase(pullRequestPort);
    }

    @Test
    @DisplayName("GIVEN valid arguments WHEN getPullRequest is called THEN delegates to port and returns PullRequest")
    void givenValidArguments_whenGetPullRequest_thenDelegatesAndReturnsPullRequest() {
        // Arrange
        PullRequest expected = PullRequest.builder()
                .pullRequestId(PR_ID)
                .title("Feature branch")
                .description("PR Description")
                .status("active")
                .sourceRefName("refs/heads/feature/login")
                .targetRefName("refs/heads/main")
                .repositoryId(REPO)
                .createdBy("developer@test.com")
                .creationDate("2026-09-08T10:00:00Z")
                .workItemIds(List.of(12345))
                .build();

        when(pullRequestPort.getPullRequestById(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Mono.just(expected));

        // Act & Assert
        StepVerifier.create(useCase.getPullRequest(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .expectNext(expected)
                .verifyComplete();

        verify(pullRequestPort).getPullRequestById(ORG, PROJECT, REPO, PR_ID, API_VERSION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid organization WHEN getPullRequest is called THEN returns error and does not call port")
    void givenInvalidOrganization_whenGetPullRequest_thenReturnsError(String invalidOrg) {
        StepVerifier.create(useCase.getPullRequest(invalidOrg, PROJECT, REPO, PR_ID, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("organization"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestById(any(), any(), any(), anyInt(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid project WHEN getPullRequest is called THEN returns error and does not call port")
    void givenInvalidProject_whenGetPullRequest_thenReturnsError(String invalidProject) {
        StepVerifier.create(useCase.getPullRequest(ORG, invalidProject, REPO, PR_ID, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("project"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestById(any(), any(), any(), anyInt(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid repositoryId WHEN getPullRequest is called THEN returns error and does not call port")
    void givenInvalidRepositoryId_whenGetPullRequest_thenReturnsError(String invalidRepo) {
        StepVerifier.create(useCase.getPullRequest(ORG, PROJECT, invalidRepo, PR_ID, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("repositoryId"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestById(any(), any(), any(), anyInt(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -99})
    @DisplayName("GIVEN non-positive pullRequestId WHEN getPullRequest is called THEN returns error and does not call port")
    void givenNonPositivePullRequestId_whenGetPullRequest_thenReturnsError(int invalidPrId) {
        StepVerifier.create(useCase.getPullRequest(ORG, PROJECT, REPO, invalidPrId, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("pullRequestId"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestById(any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("GIVEN port error WHEN getPullRequest is called THEN propagates error")
    void givenPortError_whenGetPullRequest_thenPropagatesError() {
        // Arrange
        when(pullRequestPort.getPullRequestById(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Mono.error(new RuntimeException("Simulated upstream error")));

        // Act & Assert
        StepVerifier.create(useCase.getPullRequest(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .expectErrorMessage("Simulated upstream error")
                .verify();

        verify(pullRequestPort).getPullRequestById(ORG, PROJECT, REPO, PR_ID, API_VERSION);
    }
}
