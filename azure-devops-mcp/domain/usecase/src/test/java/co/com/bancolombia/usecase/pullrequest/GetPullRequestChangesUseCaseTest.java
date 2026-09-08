package co.com.bancolombia.usecase.pullrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.pullrequest.GitChange;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetPullRequestChangesUseCaseTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final String REPO = "repoTest";
    private static final int PR_ID = 202;
    private static final String API_VERSION = "7.1";

    @Mock
    private PullRequestPort pullRequestPort;

    private GetPullRequestChangesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPullRequestChangesUseCase(pullRequestPort);
    }

    @Test
    @DisplayName("GIVEN valid arguments WHEN getChanges is called THEN delegates to port and returns changes Flux")
    void givenValidArguments_whenGetChanges_thenDelegatesAndReturnsChanges() {
        // Arrange
        GitChange change1 = GitChange.builder()
                .itemPath("/src/App.java")
                .changeType("edit")
                .originalObjectId("abc111")
                .newObjectId("abc222")
                .build();
        GitChange change2 = GitChange.builder()
                .itemPath("/README.md")
                .changeType("add")
                .originalObjectId(null)
                .newObjectId("def333")
                .build();

        when(pullRequestPort.getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Flux.just(change1, change2));

        // Act & Assert
        StepVerifier.create(useCase.getChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .expectNext(change1)
                .expectNext(change2)
                .verifyComplete();

        verify(pullRequestPort).getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN empty changes from port WHEN getChanges is called THEN returns empty Flux and completes")
    void givenEmptyChanges_whenGetChanges_thenReturnsEmptyFlux() {
        // Arrange
        when(pullRequestPort.getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(useCase.getChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .verifyComplete();

        verify(pullRequestPort).getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid organization WHEN getChanges is called THEN returns error and does not call port")
    void givenInvalidOrganization_whenGetChanges_thenReturnsError(String invalidOrg) {
        StepVerifier.create(useCase.getChanges(invalidOrg, PROJECT, REPO, PR_ID, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("organization"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestChanges(any(), any(), any(), anyInt(),
                any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid project WHEN getChanges is called THEN returns error and does not call port")
    void givenInvalidProject_whenGetChanges_thenReturnsError(String invalidProject) {
        StepVerifier.create(useCase.getChanges(ORG, invalidProject, REPO, PR_ID, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("project"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestChanges(any(), any(), any(), anyInt(),
                any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN invalid repositoryId WHEN getChanges is called THEN returns error and does not call port")
    void givenInvalidRepositoryId_whenGetChanges_thenReturnsError(String invalidRepo) {
        StepVerifier.create(useCase.getChanges(ORG, PROJECT, invalidRepo, PR_ID, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("repositoryId"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestChanges(any(), any(), any(), anyInt(),
                any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -99})
    @DisplayName("GIVEN non-positive pullRequestId WHEN getChanges is called THEN returns error and does not call port")
    void givenNonPositivePullRequestId_whenGetChanges_thenReturnsError(int invalidPrId) {
        StepVerifier.create(useCase.getChanges(ORG, PROJECT, REPO, invalidPrId, API_VERSION))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("pullRequestId"))
                .verify();

        verify(pullRequestPort, never()).getPullRequestChanges(any(), any(), any(), anyInt(),
                any());
    }

    @Test
    @DisplayName("GIVEN port error WHEN getChanges is called THEN propagates error")
    void givenPortError_whenGetChanges_thenPropagatesError() {
        // Arrange
        when(pullRequestPort.getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Flux.error(new RuntimeException("Simulated upstream error")));

        // Act & Assert
        StepVerifier.create(useCase.getChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .expectErrorMessage("Simulated upstream error")
                .verify();

        verify(pullRequestPort).getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION);
    }
}
