package co.com.bancolombia.usecase.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.pullrequest.EvaluationFinding;
import co.com.bancolombia.model.pullrequest.EvaluationVerdict;
import co.com.bancolombia.model.pullrequest.PullRequestChangeInfo;
import co.com.bancolombia.model.pullrequest.PullRequestEvaluationRequest;
import co.com.bancolombia.model.pullrequest.PullRequestInfo;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestMcpPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluatePullRequestUseCase - Orquestación de Evaluación")
class EvaluatePullRequestUseCaseTest {

    @Mock
    private PullRequestMcpPort pullRequestMcpPort;

    @Mock
    private PromptTemplatePort promptTemplatePort;

    @Mock
    private ChatGateway chatGateway;

    private EvaluatePullRequestUseCase useCase;

    private PullRequestInfo samplePrInfo;
    private List<PullRequestChangeInfo> sampleChanges;

    @BeforeEach
    void setUp() {
        useCase = new EvaluatePullRequestUseCase(pullRequestMcpPort, promptTemplatePort,
                chatGateway);

        samplePrInfo = PullRequestInfo.builder()
                .pullRequestId(42)
                .repositoryId("azure-devops-core")
                .title("feat: implementar casos de uso")
                .description("Agrega casos de uso de Git")
                .sourceRefName("refs/heads/feature/git-usecases")
                .targetRefName("refs/heads/main")
                .status("active")
                .createdBy("dev@bancolombia.com.co")
                .build();

        sampleChanges = List.of(
                PullRequestChangeInfo.builder().path("/domain/usecase/Foo.java").changeType("add")
                        .build(),
                PullRequestChangeInfo.builder().path("/domain/model/Bar.java").changeType("edit")
                        .build()
        );
    }

    @Test
    @DisplayName("Falla si la solicitud es nula o tiene parámetros inválidos")
    void givenInvalidRequests_whenEvaluate_thenThrowsException() {
        assertThatThrownBy(() -> useCase.evaluate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("solicitud de evaluación es obligatoria");

        PullRequestEvaluationRequest noRepo = PullRequestEvaluationRequest.builder()
                .organization("org")
                .project("proj")
                .pullRequestId(10)
                .build();
        assertThatThrownBy(() -> useCase.evaluate(noRepo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repositoryId es obligatorio");

        PullRequestEvaluationRequest invalidId = PullRequestEvaluationRequest.builder()
                .organization("org")
                .project("proj")
                .repositoryId("repo")
                .pullRequestId(0)
                .build();
        assertThatThrownBy(() -> useCase.evaluate(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pullRequestId debe ser mayor a 0");

        PullRequestEvaluationRequest noOrg = PullRequestEvaluationRequest.builder()
                .project("proj")
                .repositoryId("repo")
                .pullRequestId(10)
                .build();
        assertThatThrownBy(() -> useCase.evaluate(noOrg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organización es obligatoria");

        PullRequestEvaluationRequest noProj = PullRequestEvaluationRequest.builder()
                .organization("org")
                .repositoryId("repo")
                .pullRequestId(10)
                .build();
        assertThatThrownBy(() -> useCase.evaluate(noProj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proyecto es obligatorio");
    }

    @Test
    @DisplayName("Evaluación exitosa con veredicto APPROVED y bloque JSON estructurado sin publicar comentario")
    void givenApprovedPr_whenEvaluateWithoutPublish_thenReturnsApprovedEvaluation() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("azure-devops-core")
                .pullRequestId(42)
                .publishComment(false)
                .build();

        String llmResponse = """
                # Informe de Evaluación
                Veredicto General: APPROVED
                Código cumple con estándares.
                
                EVALUATION_JSON_START
                {
                  "pullRequestId": 42,
                  "verdict": "APPROVED",
                  "summary": "Excelente implementación siguiendo Clean Architecture pura.",
                  "findings": [],
                  "recommendations": ["Añadir documentación adicional en JavaDoc"]
                }
                EVALUATION_JSON_END
                """;

        when(pullRequestMcpPort.getPullRequest("grupobancolombia", "VST", "azure-devops-core", 42))
                .thenReturn(Mono.just(samplePrInfo));
        when(pullRequestMcpPort.getPullRequestChanges("grupobancolombia", "VST",
                "azure-devops-core", 42))
                .thenReturn(Flux.fromIterable(sampleChanges));
        when(promptTemplatePort.render(eq(PromptTemplateId.EVALUATE_PULL_REQUEST), anyMap()))
                .thenReturn("PROMPT RENDERIZADO");
        when(chatGateway.sendMessage("PROMPT RENDERIZADO", "pr-eval-azure-devops-core-42"))
                .thenReturn(Mono.just(llmResponse));

        StepVerifier.create(useCase.evaluate(request))
                .assertNext(evaluation -> {
                    assertThat(evaluation.getPullRequestId()).isEqualTo(42);
                    assertThat(evaluation.getRepositoryId()).isEqualTo("azure-devops-core");
                    assertThat(evaluation.getVerdict()).isEqualTo(EvaluationVerdict.APPROVED);
                    assertThat(evaluation.getSummary()).contains("Excelente implementación");
                    assertThat(evaluation.getFindings()).isEmpty();
                    assertThat(evaluation.getRecommendations()).containsExactly(
                            "Añadir documentación adicional en JavaDoc");
                    assertThat(evaluation.isCommentPublished()).isFalse();
                    assertThat(evaluation.getPublishedCommentId()).isNull();
                })
                .verifyComplete();

        verify(pullRequestMcpPort, never()).createPullRequestComment(anyString(), anyString(),
                anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Evaluación con veredicto CHANGES_REQUESTED, hallazgos técnicos y publicación de comentario")
    void givenChangesRequestedPr_whenEvaluateWithPublish_thenPublishesCommentAndReturnsEvaluation() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("azure-devops-core")
                .pullRequestId(42)
                .publishComment(true)
                .build();

        String llmResponse = """
                Informe de revisión técnica.
                EVALUATION_JSON_START
                {
                  "pullRequestId": 42,
                  "verdict": "CHANGES_REQUESTED",
                  "summary": "Se detectaron violaciones de Clean Architecture en el dominio.",
                  "findings": [
                    {
                      "filePath": "domain/usecase/Foo.java",
                      "severity": "HIGH",
                      "category": "CLEAN_ARCHITECTURE",
                      "message": "Uso indebido de Spring @Component en usecase",
                      "suggestion": "Remover anotación y declarar en UseCasesConfig"
                    }
                  ],
                  "recommendations": [
                    "Remover dependencias de frameworks del dominio",
                    "Asegurar 100% mutaciones eliminadas"
                  ]
                }
                EVALUATION_JSON_END
                """;

        when(pullRequestMcpPort.getPullRequest("grupobancolombia", "VST", "azure-devops-core", 42))
                .thenReturn(Mono.just(samplePrInfo));
        when(pullRequestMcpPort.getPullRequestChanges("grupobancolombia", "VST",
                "azure-devops-core", 42))
                .thenReturn(Flux.fromIterable(sampleChanges));
        when(promptTemplatePort.render(eq(PromptTemplateId.EVALUATE_PULL_REQUEST), anyMap()))
                .thenReturn("PROMPT RENDERIZADO");
        when(chatGateway.sendMessage("PROMPT RENDERIZADO", "pr-eval-azure-devops-core-42"))
                .thenReturn(Mono.just(llmResponse));
        when(pullRequestMcpPort.createPullRequestComment(
                eq("grupobancolombia"), eq("VST"), eq("azure-devops-core"), eq(42), anyString()))
                .thenReturn(Mono.just("comment-thread-101"));

        StepVerifier.create(useCase.evaluate(request))
                .assertNext(evaluation -> {
                    assertThat(evaluation.getPullRequestId()).isEqualTo(42);
                    assertThat(evaluation.getVerdict()).isEqualTo(
                            EvaluationVerdict.CHANGES_REQUESTED);
                    assertThat(evaluation.getFindings()).hasSize(1);
                    EvaluationFinding finding = evaluation.getFindings().getFirst();
                    assertThat(finding.getFilePath()).isEqualTo("domain/usecase/Foo.java");
                    assertThat(finding.getSeverity()).isEqualTo("HIGH");
                    assertThat(finding.getCategory()).isEqualTo("CLEAN_ARCHITECTURE");
                    assertThat(finding.getMessage()).contains("Uso indebido de Spring");
                    assertThat(evaluation.getRecommendations()).hasSize(2);
                    assertThat(evaluation.isCommentPublished()).isTrue();
                    assertThat(evaluation.getPublishedCommentId()).isEqualTo("comment-thread-101");
                })
                .verifyComplete();

        verify(pullRequestMcpPort).createPullRequestComment(
                eq("grupobancolombia"), eq("VST"), eq("azure-devops-core"), eq(42), anyString());
    }

    @Test
    @DisplayName("Si falla la publicación de comentario, no aborta la evaluación y retorna commentPublished en false")
    void givenCommentPublishFailure_whenEvaluate_thenHandlesGracefully() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("azure-devops-core")
                .pullRequestId(42)
                .publishComment(true)
                .build();

        String llmResponse = """
                EVALUATION_JSON_START
                {
                  "pullRequestId": 42,
                  "verdict": "APPROVED",
                  "summary": "Todo correcto",
                  "findings": [],
                  "recommendations": []
                }
                EVALUATION_JSON_END
                """;

        when(pullRequestMcpPort.getPullRequest("grupobancolombia", "VST", "azure-devops-core", 42))
                .thenReturn(Mono.just(samplePrInfo));
        when(pullRequestMcpPort.getPullRequestChanges("grupobancolombia", "VST",
                "azure-devops-core", 42))
                .thenReturn(Flux.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.EVALUATE_PULL_REQUEST), anyMap()))
                .thenReturn("PROMPT RENDERIZADO");
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.just(llmResponse));
        when(pullRequestMcpPort.createPullRequestComment(anyString(), anyString(), anyString(),
                anyInt(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de red publicando comentario")));

        StepVerifier.create(useCase.evaluate(request))
                .assertNext(evaluation -> {
                    assertThat(evaluation.getVerdict()).isEqualTo(EvaluationVerdict.APPROVED);
                    assertThat(evaluation.isCommentPublished()).isFalse();
                    assertThat(evaluation.getPublishedCommentId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Fallback cuando el LLM responde en texto plano sin bloque JSON")
    void givenPlainTextLlmResponse_whenEvaluate_thenUsesFallbackParser() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("azure-devops-core")
                .pullRequestId(42)
                .publishComment(false)
                .build();

        String plainTextResponse = "El PR contiene violaciones graves de seguridad. VERDICT: REJECTED.";

        when(pullRequestMcpPort.getPullRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(samplePrInfo));
        when(pullRequestMcpPort.getPullRequestChanges(anyString(), anyString(), anyString(),
                anyInt()))
                .thenReturn(Flux.fromIterable(sampleChanges));
        when(promptTemplatePort.render(any(), anyMap()))
                .thenReturn("PROMPT");
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.just(plainTextResponse));

        StepVerifier.create(useCase.evaluate(request))
                .assertNext(evaluation -> {
                    assertThat(evaluation.getVerdict()).isEqualTo(EvaluationVerdict.REJECTED);
                    assertThat(evaluation.getSummary()).contains("violaciones graves de seguridad");
                    assertThat(evaluation.getFindings()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Maneja respuesta nula o vacía del LLM")
    void givenEmptyLlmResponse_whenEvaluate_thenReturnsChangesRequestedDefault() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("azure-devops-core")
                .pullRequestId(42)
                .publishComment(false)
                .build();

        when(pullRequestMcpPort.getPullRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(samplePrInfo));
        when(pullRequestMcpPort.getPullRequestChanges(anyString(), anyString(), anyString(),
                anyInt()))
                .thenReturn(Flux.empty());
        when(promptTemplatePort.render(any(), anyMap()))
                .thenReturn("PROMPT");
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.just("   "));

        StepVerifier.create(useCase.evaluate(request))
                .assertNext(evaluation -> {
                    assertThat(evaluation.getVerdict()).isEqualTo(
                            EvaluationVerdict.CHANGES_REQUESTED);
                    assertThat(evaluation.getSummary()).contains("No se recibió respuesta válida");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Si el veredicto en JSON es inválido, asigna CHANGES_REQUESTED por defecto")
    void givenUnknownVerdictInJson_whenEvaluate_thenDefaultsToChangesRequested() {
        PullRequestEvaluationRequest request = PullRequestEvaluationRequest.builder()
                .organization("grupobancolombia")
                .project("VST")
                .repositoryId("azure-devops-core")
                .pullRequestId(42)
                .publishComment(false)
                .build();

        String llmResponse = """
                EVALUATION_JSON_START
                {
                  "pullRequestId": 42,
                  "verdict": "UNKNOWN_CUSTOM_STATUS",
                  "summary": "Resumen",
                  "findings": [],
                  "recommendations": []
                }
                EVALUATION_JSON_END
                """;

        when(pullRequestMcpPort.getPullRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(samplePrInfo));
        when(pullRequestMcpPort.getPullRequestChanges(anyString(), anyString(), anyString(),
                anyInt()))
                .thenReturn(Flux.empty());
        when(promptTemplatePort.render(any(), anyMap()))
                .thenReturn("PROMPT");
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.just(llmResponse));

        StepVerifier.create(useCase.evaluate(request))
                .assertNext(evaluation -> {
                    assertThat(evaluation.getVerdict()).isEqualTo(
                            EvaluationVerdict.CHANGES_REQUESTED);
                    assertThat(evaluation.getSummary()).isEqualTo("Resumen");
                })
                .verifyComplete();
    }
}
