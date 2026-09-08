package co.com.bancolombia.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.mcp.dto.GitChangeResponse;
import co.com.bancolombia.mcp.dto.PullRequestCommentResponse;
import co.com.bancolombia.mcp.dto.PullRequestResponse;
import co.com.bancolombia.mcp.error.McpToolExecutionException;
import co.com.bancolombia.model.exception.AzureDevOpsException;
import co.com.bancolombia.model.exception.AzureDevOpsUnauthorizedException;
import co.com.bancolombia.model.exception.WorkItemNotFoundException;
import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;
import co.com.bancolombia.model.pullrequest.PullRequestComment;
import co.com.bancolombia.usecase.pullrequest.CreatePullRequestCommentUseCase;
import co.com.bancolombia.usecase.pullrequest.GetPullRequestChangesUseCase;
import co.com.bancolombia.usecase.pullrequest.GetPullRequestUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AzureDevOpsGitToolsTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnologia";
    private static final String REPO = "repo-core-payments";
    private static final int PR_ID = 42;
    private static final String API_VERSION = "7.1";

    @Mock
    private GetPullRequestUseCase getPullRequestUseCase;
    @Mock
    private GetPullRequestChangesUseCase getPullRequestChangesUseCase;
    @Mock
    private CreatePullRequestCommentUseCase createPullRequestCommentUseCase;

    @InjectMocks
    private AzureDevOpsGitTools gitTools;

    // ---------------------------------------------------------------- getPullRequest

    @Test
    @DisplayName("GIVEN parametros validos WHEN getPullRequest THEN delega al caso de uso y mapea la respuesta")
    void givenValidParameters_whenGetPullRequest_thenDelegatesAndMapsResponse() {
        // Arrange
        PullRequest pr = PullRequest.builder()
                .pullRequestId(PR_ID)
                .title("Feature branch PR")
                .status("active")
                .sourceRefName("refs/heads/feature/mcp")
                .targetRefName("refs/heads/main")
                .repositoryId(REPO)
                .build();

        when(getPullRequestUseCase.getPullRequest(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Mono.just(pr));

        // Act
        Mono<PullRequestResponse> result = gitTools.getPullRequest(ORG, PROJECT, REPO, PR_ID,
                API_VERSION);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(PR_ID, response.pullRequestId());
                    assertEquals("Feature branch PR", response.title());
                    assertEquals("active", response.status());
                    assertEquals("refs/heads/feature/mcp", response.sourceRefName());
                    assertEquals("refs/heads/main", response.targetRefName());
                    assertEquals(REPO, response.repositoryId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN fallo de dominio WHEN getPullRequest THEN traduce el error con McpErrorTranslator")
    void givenDomainException_whenGetPullRequest_thenTranslatesError() {
        // Arrange
        AzureDevOpsException domainException =
                new WorkItemNotFoundException("Pull Request 42 no encontrado");

        when(getPullRequestUseCase.getPullRequest(anyString(), anyString(), anyString(), anyInt(),
                any()))
                .thenReturn(Mono.error(domainException));

        // Act & Assert
        StepVerifier.create(gitTools.getPullRequest(ORG, PROJECT, REPO, PR_ID, null))
                .expectErrorMatches(e -> e instanceof McpToolExecutionException
                        && e.getMessage().contains("AZDO_NOT_FOUND"))
                .verify();
    }

    // ---------------------------------------------------------------- getPullRequestChanges

    @Test
    @DisplayName("GIVEN parametros validos WHEN getPullRequestChanges THEN delega, recolecta la lista y mapea")
    void givenValidParameters_whenGetPullRequestChanges_thenCollectsListAndMaps() {
        // Arrange
        GitChange change1 = GitChange.builder().itemPath("/src/App.java").changeType("edit")
                .build();
        GitChange change2 = GitChange.builder().itemPath("/pom.xml").changeType("edit").build();

        when(getPullRequestChangesUseCase.getChanges(ORG, PROJECT, REPO, PR_ID, API_VERSION))
                .thenReturn(Flux.just(change1, change2));

        // Act
        Mono<List<GitChangeResponse>> result = gitTools.getPullRequestChanges(ORG, PROJECT, REPO,
                PR_ID, API_VERSION);

        // Assert
        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(2, list.size());
                    assertEquals("/src/App.java", list.get(0).itemPath());
                    assertEquals("edit", list.get(0).changeType());
                    assertEquals("/pom.xml", list.get(1).itemPath());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN fallo no controlado WHEN getPullRequestChanges THEN traduce a error interno")
    void givenUnexpectedException_whenGetPullRequestChanges_thenTranslatesToInternalError() {
        // Arrange
        when(getPullRequestChangesUseCase.getChanges(anyString(), anyString(), anyString(),
                anyInt(), any()))
                .thenReturn(Flux.error(new RuntimeException("Connection timeout")));

        // Act & Assert
        StepVerifier.create(gitTools.getPullRequestChanges(ORG, PROJECT, REPO, PR_ID, null))
                .expectErrorMatches(e -> e instanceof McpToolExecutionException
                        && e.getMessage().contains("MCP_INTERNAL_ERROR"))
                .verify();
    }

    // ---------------------------------------------------------------- createPullRequestComment

    @Test
    @DisplayName("GIVEN contenido y status explicito WHEN createPullRequestComment THEN delega con el comentario construido")
    void givenContentAndExplicitStatus_whenCreateComment_thenDelegatesWithComment() {
        // Arrange
        PullRequestComment created = PullRequestComment.builder()
                .id(1001)
                .content("Por favor revisar cobertura de pruebas")
                .status("active")
                .author("Antigravity Agent")
                .build();

        when(createPullRequestCommentUseCase.createComment(eq(ORG), eq(PROJECT), eq(REPO),
                eq(PR_ID), any(), eq(API_VERSION)))
                .thenReturn(Mono.just(created));

        // Act
        Mono<PullRequestCommentResponse> result = gitTools.createPullRequestComment(
                ORG, PROJECT, REPO, PR_ID, "Por favor revisar cobertura de pruebas", "active",
                API_VERSION);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(1001, response.id());
                    assertEquals("Por favor revisar cobertura de pruebas", response.content());
                    assertEquals("active", response.status());
                    assertEquals("Antigravity Agent", response.author());
                })
                .verifyComplete();

        ArgumentCaptor<PullRequestComment> commentCaptor = ArgumentCaptor.forClass(
                PullRequestComment.class);
        verify(createPullRequestCommentUseCase).createComment(eq(ORG), eq(PROJECT), eq(REPO),
                eq(PR_ID),
                commentCaptor.capture(), eq(API_VERSION));
        assertEquals("Por favor revisar cobertura de pruebas", commentCaptor.getValue().content());
        assertEquals("active", commentCaptor.getValue().status());
    }

    @Test
    @DisplayName("GIVEN status nulo o en blanco WHEN createPullRequestComment THEN asigna active por defecto")
    void givenBlankStatus_whenCreateComment_thenDefaultsToActive() {
        // Arrange
        PullRequestComment created = PullRequestComment.builder()
                .id(1002)
                .content("Comentario con status por defecto")
                .status("active")
                .build();

        when(createPullRequestCommentUseCase.createComment(eq(ORG), eq(PROJECT), eq(REPO),
                eq(PR_ID), any(), any()))
                .thenReturn(Mono.just(created));

        // Act
        Mono<PullRequestCommentResponse> result = gitTools.createPullRequestComment(
                ORG, PROJECT, REPO, PR_ID, "Comentario con status por defecto", "   ", null);

        // Assert
        StepVerifier.create(result).expectNextCount(1).verifyComplete();

        ArgumentCaptor<PullRequestComment> commentCaptor = ArgumentCaptor.forClass(
                PullRequestComment.class);
        verify(createPullRequestCommentUseCase).createComment(eq(ORG), eq(PROJECT), eq(REPO),
                eq(PR_ID),
                commentCaptor.capture(), any());
        assertEquals("active", commentCaptor.getValue().status());
    }

    @Test
    @DisplayName("GIVEN fallo de dominio WHEN createPullRequestComment THEN traduce con McpErrorTranslator")
    void givenDomainException_whenCreateComment_thenTranslatesError() {
        // Arrange
        AzureDevOpsException domainException =
                new AzureDevOpsUnauthorizedException("No autorizado para comentar en este PR");

        when(createPullRequestCommentUseCase.createComment(anyString(), anyString(), anyString(),
                anyInt(), any(), any()))
                .thenReturn(Mono.error(domainException));

        // Act & Assert
        StepVerifier.create(
                        gitTools.createPullRequestComment(ORG, PROJECT, REPO, PR_ID, "Comentario", null,
                                null))
                .expectErrorMatches(e -> e instanceof McpToolExecutionException
                        && e.getMessage().contains("AZDO_UNAUTHORIZED"))
                .verify();
    }
}
