package co.com.bancolombia.usecase.pullrequest;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.pullrequest.EvaluationFinding;
import co.com.bancolombia.model.pullrequest.EvaluationVerdict;
import co.com.bancolombia.model.pullrequest.PullRequestChangeInfo;
import co.com.bancolombia.model.pullrequest.PullRequestEvaluation;
import co.com.bancolombia.model.pullrequest.PullRequestEvaluationRequest;
import co.com.bancolombia.model.pullrequest.PullRequestInfo;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestMcpPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro de orquestación para la evaluación automatizada de Pull Requests.
 *
 * <p>Flujo de ejecución:
 * <ol>
 *   <li>Consulta concurrentemente los metadatos y cambios del PR vía {@link PullRequestMcpPort}.</li>
 *   <li>Renderiza la plantilla especializada {@link PromptTemplateId#EVALUATE_PULL_REQUEST} vía {@link PromptTemplatePort}.</li>
 *   <li>Solicita el análisis al modelo de lenguaje mediante {@link ChatGateway}.</li>
 *   <li>Estructura el veredicto y hallazgos en una entidad {@link PullRequestEvaluation}.</li>
 *   <li>Si la petición lo requiere, publica el comentario de retroalimentación vía {@link PullRequestMcpPort}.</li>
 * </ol>
 */
@Log
@RequiredArgsConstructor
public class EvaluatePullRequestUseCase {

    private static final String DEFAULT_STANDARDS =
            "Estándares Corporativos Bancolombia: Clean Architecture pura, Java 25, Project Reactor no bloqueante, cero secretos.";
    private static final Pattern JSON_BLOCK_PATTERN =
            Pattern.compile("(?s)EVALUATION_JSON_START(.*?)EVALUATION_JSON_END");
    private static final Pattern VERDICT_PATTERN =
            Pattern.compile("\"verdict\"\\s*:\\s*\"([A-Z_]+)\"");
    private static final Pattern SUMMARY_PATTERN =
            Pattern.compile("\"summary\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern FINDING_BLOCK_PATTERN =
            Pattern.compile("(?s)\\{[^{}\"]*\"filePath\"[^{}]*?\\}");
    private static final Pattern FIELD_FILE_PATH =
            Pattern.compile("\"filePath\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern FIELD_SEVERITY =
            Pattern.compile("\"severity\"\\s*:\\s*\"([A-Z_]+)\"");
    private static final Pattern FIELD_CATEGORY =
            Pattern.compile("\"category\"\\s*:\\s*\"([A-Z_]+)\"");
    private static final Pattern FIELD_MESSAGE =
            Pattern.compile("\"message\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern FIELD_SUGGESTION =
            Pattern.compile("\"suggestion\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern RECOMMENDATIONS_ARRAY_PATTERN =
            Pattern.compile("(?s)\"recommendations\"\\s*:\\s*\\[(.*?)\\]");
    private static final Pattern STRING_ITEM_PATTERN =
            Pattern.compile("\"((?:\\\\\"|[^\"])*)\"");

    private final PullRequestMcpPort pullRequestMcpPort;
    private final PromptTemplatePort promptTemplatePort;
    private final ChatGateway chatGateway;

    /**
     * Ejecuta la evaluación integral del Pull Request.
     *
     * @param request parámetros de la solicitud de evaluación
     * @return Mono emitiendo el resultado inmutable de la evaluación
     */
    public Mono<PullRequestEvaluation> evaluate(PullRequestEvaluationRequest request) {
        validateRequest(request);

        String organization = request.getOrganization();
        String project = request.getProject();
        String repositoryId = request.getRepositoryId();
        int pullRequestId = request.getPullRequestId();
        String contextId = "pr-eval-" + repositoryId + "-" + pullRequestId;

        return Mono.zip(
                pullRequestMcpPort.getPullRequest(organization, project, repositoryId,
                        pullRequestId),
                pullRequestMcpPort.getPullRequestChanges(organization, project, repositoryId,
                        pullRequestId).collectList()
        ).flatMap(tuple -> {
            PullRequestInfo prInfo = tuple.getT1();
            List<PullRequestChangeInfo> changes = tuple.getT2();

            String prompt = renderEvaluationPrompt(prInfo, changes);
            return chatGateway.sendMessage(prompt, contextId)
                    .flatMap(llmResponse -> processLlmResponse(request, prInfo, llmResponse));
        });
    }

    private void validateRequest(PullRequestEvaluationRequest request) {
        Objects.requireNonNull(request, "La solicitud de evaluación es obligatoria");
        if (request.getRepositoryId() == null || request.getRepositoryId().isBlank()) {
            throw new IllegalArgumentException("El repositoryId es obligatorio");
        }
        if (request.getPullRequestId() <= 0) {
            throw new IllegalArgumentException("El pullRequestId debe ser mayor a 0");
        }
        if (request.getOrganization() == null || request.getOrganization().isBlank()) {
            throw new IllegalArgumentException("La organización es obligatoria");
        }
        if (request.getProject() == null || request.getProject().isBlank()) {
            throw new IllegalArgumentException("El proyecto es obligatorio");
        }
    }

    private String renderEvaluationPrompt(PullRequestInfo prInfo,
            List<PullRequestChangeInfo> changes) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("pullRequestId", prInfo.getPullRequestId());
        variables.put("repositoryId",
                prInfo.getRepositoryId() != null ? prInfo.getRepositoryId() : "");
        variables.put("title", prInfo.getTitle() != null ? prInfo.getTitle() : "");
        variables.put("description",
                prInfo.getDescription() != null ? prInfo.getDescription() : "Sin descripción");
        variables.put("sourceBranch",
                prInfo.getSourceRefName() != null ? prInfo.getSourceRefName() : "");
        variables.put("targetBranch",
                prInfo.getTargetRefName() != null ? prInfo.getTargetRefName() : "");
        variables.put("changesList", formatChanges(changes));
        variables.put("architectureStandards", DEFAULT_STANDARDS);

        return promptTemplatePort.render(PromptTemplateId.EVALUATE_PULL_REQUEST, variables);
    }

    private String formatChanges(List<PullRequestChangeInfo> changes) {
        if (changes == null || changes.isEmpty()) {
            return "No se reportaron archivos modificados en este Pull Request.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("| Tipo de Cambio | Ruta del Archivo |\n");
        sb.append("|:--------------:|:-----------------|\n");
        for (PullRequestChangeInfo change : changes) {
            sb.append("| ").append(change.getChangeType()).append(" | ")
                    .append(change.getPath()).append(" |\n");
        }
        return sb.toString();
    }

    private Mono<PullRequestEvaluation> processLlmResponse(
            PullRequestEvaluationRequest request, PullRequestInfo prInfo, String llmResponse) {
        ParsedEvaluation parsed = parseLlmResponse(llmResponse);

        if (request.isPublishComment()) {
            String commentMarkdown = buildFeedbackComment(parsed, prInfo);
            return pullRequestMcpPort.createPullRequestComment(
                            request.getOrganization(),
                            request.getProject(),
                            request.getRepositoryId(),
                            request.getPullRequestId(),
                            commentMarkdown)
                    .map(commentId -> buildEvaluationResult(request, parsed, true, commentId))
                    .onErrorResume(e -> {
                        log.warning(
                                "No se pudo publicar el comentario en el PR: " + e.getMessage());
                        return Mono.just(buildEvaluationResult(request, parsed, false, null));
                    });
        }

        return Mono.just(buildEvaluationResult(request, parsed, false, null));
    }

    private PullRequestEvaluation buildEvaluationResult(
            PullRequestEvaluationRequest request, ParsedEvaluation parsed, boolean published,
            String commentId) {
        return PullRequestEvaluation.builder()
                .pullRequestId(request.getPullRequestId())
                .repositoryId(request.getRepositoryId())
                .verdict(parsed.verdict())
                .summary(parsed.summary())
                .findings(parsed.findings())
                .recommendations(parsed.recommendations())
                .commentPublished(published)
                .publishedCommentId(commentId)
                .build();
    }

    private ParsedEvaluation parseLlmResponse(String response) {
        if (response == null || response.isBlank()) {
            return new ParsedEvaluation(
                    EvaluationVerdict.CHANGES_REQUESTED,
                    "No se recibió respuesta válida del modelo de evaluación.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        Matcher jsonBlockMatcher = JSON_BLOCK_PATTERN.matcher(response);
        if (jsonBlockMatcher.find()) {
            String jsonContent = jsonBlockMatcher.group(1);
            return parseJsonBlock(jsonContent);
        }

        // Fallback: parsear veredicto del texto libre
        EvaluationVerdict verdict = extractVerdictFromText(response);
        return new ParsedEvaluation(
                verdict,
                response.trim(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private ParsedEvaluation parseJsonBlock(String json) {
        return new ParsedEvaluation(
                extractVerdictFromJson(json),
                extractSummaryFromJson(json),
                extractFindingsFromJson(json),
                extractRecommendationsFromJson(json)
        );
    }

    private EvaluationVerdict extractVerdictFromJson(String json) {
        Matcher verdictMatcher = VERDICT_PATTERN.matcher(json);
        if (verdictMatcher.find()) {
            try {
                return EvaluationVerdict.valueOf(verdictMatcher.group(1));
            } catch (IllegalArgumentException _) {
                // Mantiene el valor por defecto CHANGES_REQUESTED
            }
        }
        return EvaluationVerdict.CHANGES_REQUESTED;
    }

    private String extractSummaryFromJson(String json) {
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(json);
        if (summaryMatcher.find()) {
            return summaryMatcher.group(1).replace("\\\"", "\"");
        }
        return "Evaluación completada";
    }

    private List<EvaluationFinding> extractFindingsFromJson(String json) {
        List<EvaluationFinding> findings = new ArrayList<>();
        Matcher findingMatcher = FINDING_BLOCK_PATTERN.matcher(json);
        while (findingMatcher.find()) {
            String block = findingMatcher.group();
            String path = extractRegexGroup(FIELD_FILE_PATH, block);
            String severity = extractRegexGroup(FIELD_SEVERITY, block);
            String category = extractRegexGroup(FIELD_CATEGORY, block);
            String message = extractRegexGroup(FIELD_MESSAGE, block);
            String suggestion = extractRegexGroup(FIELD_SUGGESTION, block);

            findings.add(EvaluationFinding.builder()
                    .filePath(path != null ? path : "general")
                    .severity(severity != null ? severity : "INFO")
                    .category(category != null ? category : "CODE_QUALITY")
                    .message(message != null ? message : "Observación técnica")
                    .suggestion(suggestion != null ? suggestion : "")
                    .build());
        }
        return findings;
    }

    private List<String> extractRecommendationsFromJson(String json) {
        List<String> recommendations = new ArrayList<>();
        Matcher recArrayMatcher = RECOMMENDATIONS_ARRAY_PATTERN.matcher(json);
        if (recArrayMatcher.find()) {
            String arrayContent = recArrayMatcher.group(1);
            Matcher strMatcher = STRING_ITEM_PATTERN.matcher(arrayContent);
            while (strMatcher.find()) {
                recommendations.add(strMatcher.group(1).replace("\\\"", "\""));
            }
        }
        return recommendations;
    }

    private String extractRegexGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).replace("\\\"", "\"") : null;
    }

    private EvaluationVerdict extractVerdictFromText(String text) {
        String upper = text.toUpperCase();
        if (upper.contains("VERDICT: APPROVED") || upper.contains("VEREDICTO: APPROVED")
                || upper.contains("VEREDICTO GENERAL: APPROVED")) {
            return EvaluationVerdict.APPROVED;
        }
        if (upper.contains("REJECTED")) {
            return EvaluationVerdict.REJECTED;
        }
        return EvaluationVerdict.CHANGES_REQUESTED;
    }

    private String buildFeedbackComment(ParsedEvaluation parsed, PullRequestInfo prInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 Evaluación Automatizada de Pull Request #")
                .append(prInfo.getPullRequestId()).append("\n\n");
        sb.append("**Veredicto:** `").append(parsed.verdict()).append("`\n\n");
        sb.append("**Resumen:** ").append(parsed.summary()).append("\n\n");

        if (!parsed.findings().isEmpty()) {
            sb.append("#### Hallazgos y Observaciones Técnicas\n\n");
            sb.append("| Severidad | Categoría | Archivo | Observación | Sugerencia |\n");
            sb.append("|:---------:|:---------:|:--------|:------------|:-----------|\n");
            for (EvaluationFinding f : parsed.findings()) {
                sb.append("| ").append(f.getSeverity()).append(" | ")
                        .append(f.getCategory()).append(" | `")
                        .append(f.getFilePath()).append("` | ")
                        .append(f.getMessage()).append(" | ")
                        .append(f.getSuggestion()).append(" |\n");
            }
            sb.append("\n");
        }

        if (!parsed.recommendations().isEmpty()) {
            sb.append("#### Recomendaciones\n\n");
            for (String rec : parsed.recommendations()) {
                sb.append("- ").append(rec).append("\n");
            }
            sb.append("\n");
        }

        sb.append(
                "---\n*Revisión generada por Azure DevOps Agent siguiendo estándares de Clean Architecture Bancolombia.*");
        return sb.toString();
    }

    private record ParsedEvaluation(
            EvaluationVerdict verdict,
            String summary,
            List<EvaluationFinding> findings,
            List<String> recommendations
    ) {

    }
}
