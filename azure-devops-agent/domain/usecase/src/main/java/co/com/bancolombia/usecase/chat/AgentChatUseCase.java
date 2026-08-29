package co.com.bancolombia.usecase.chat;

import static java.util.logging.Level.SEVERE;

import co.com.bancolombia.model.a2a.Artifact;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.ComplexityEstimation;
import co.com.bancolombia.model.agent.EstimationSummary;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Caso de uso que ejecuta la comunicación con el LLM a través de MCP tools.
 * <p>
 * Soporta dos transportes:
 * <ul>
 *   <li><b>Kafka/async</b>: {@link #chat(SendMessageRequest)} – ejecuta y publica la respuesta vía gateway</li>
 *   <li><b>REST/sync</b>: {@link #chatAndRespond(SendMessageRequest)} – ejecuta y retorna directamente</li>
 * </ul>
 */
@Log
@RequiredArgsConstructor
public class AgentChatUseCase {

    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String NO_CONTENT = "No content provided";
    private static final String KEY_EXECUTION_TIME_MS = "executionTimeMs";
    private static final String ERROR_EXECUTING_CHAT = "Error executing chat";

    // ─── Nombres de las variables que declaran las plantillas de prompt ────────
    private static final String VAR_WORK_ITEM_ID = "workItemId";
    private static final String VAR_ORGANIZATION = "organizacion";
    private static final String VAR_PROJECT = "proyecto";
    private static final String VAR_AGILE_GUIDE = "guiaAgilidad";
    private static final String VAR_STANDARDS = "estandares";
    private static final String VAR_ORIGINAL_IDEA = "ideaOriginal";
    private static final String VAR_RAG_CONTEXT = "contextoRag";
    private static final String VAR_CORPORATE_TEMPLATE = "plantillaCorporativa";

    // ─── Enrutamiento ──────────────────────────────────────────────────────────
    /** Longitud mínima para considerar un texto como idea de planeación y no como comando. */
    private static final int MIN_PLANNING_TEXT_LENGTH = 25;

    /** Número de fragmentos de planeación que se recuperan del vector store. */
    private static final int RAG_MAX_RESULTS = 3;

    private static final String REFINEMENT_MARKER = "analicemos y refinemos";

    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{M}");

    private static final Pattern AUDIT_REQUEST_PATTERN = Pattern.compile(
            "(?:audita|evalua|revisa|calidad).*?\\(ID:\\s*(\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern WORK_ITEM_ID_PATTERN = Pattern.compile("\\(ID:\\s*(\\d+)\\)");

    private static final Pattern AUDIT_JSON_BLOCK =
            Pattern.compile("(?s)AUDIT_JSON_START.*?AUDIT_JSON_END");

    /**
     * Comandos cortos que nunca deben interpretarse como una idea de planeación.
     * Fuente única de verdad, compartida por el enrutamiento y el manejo de comandos.
     */
    private static final List<String> SHORT_COMMANDS = List.of("aprobado", "crear", "si", "no",
            "procede", "dividela", "divide", "de acuerdo", "dividir");

    private static final List<String> APPROVAL_COMMANDS = List.of("aprobado", "si", "de acuerdo");

    private static final List<String> DIVISION_COMMANDS = List.of("dividir", "dividela", "divide");

    /** Palabras que redirigen la conversación al flujo general de consulta. */
    private static final List<String> GENERAL_KEYWORDS =
            List.of("lista", "busca", "consulta", "reporte");

    // ─── Llamadas a la acción de cada flujo ────────────────────────────────────
    private static final String CONFIRM_CREATE_STORY =
            "\n\n¿Deseas registrar esta Historia con sus tareas desglosadas en Azure DevOps? "
                    + "Responde 'Crear' para proceder.";

    private static final String CONFIRM_UPDATE_STORY =
            "\n\n¿Deseas actualizar el Work Item en Azure DevOps con esta versión refinada? "
                    + "Responde 'Aprobar' o 'Actualizar' para proceder.";

    private static final String CONFIRM_CREATE_DIVIDED_STORIES =
            "\n\n¿Deseas proceder con el registro de estas historias divididas en Azure DevOps? "
                    + "Responde 'Crear' para registrar todas las historias con sus tareas "
                    + "correspondientes.";

    private final ChatGateway chatGateway;
    private final AgentResponseGateway agentResponseGateway;
    private final TaskStoreGateway taskStoreGateway;
    private final String templateMarkdown;
    private final PlanningVectorStorePort vectorStorePort;
    private final String agileGuideContent;
    private final String defaultOrg;
    private final String defaultProject;
    private final String qualityAuditContent;
    // TODO Fase 05: agrupar los String de configuración en Value Objects para reducir el constructor
    private final PromptTemplatePort promptTemplatePort;

    private static String extractContextId(SendMessageRequest request) {
        return request != null && request.getMessage() != null
                ? request.getMessage().getContextId()
                : null;
    }

    private static Message buildAgentMessage(String contextId, String text) {
        return Message.builder()
                .role("agent")
                .contextId(contextId)
                .messageId(UUID.randomUUID().toString())
                .parts(List.of(Part.ofText(text)))
                .build();
    }

    private static String nowIso() {
        return Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    public Mono<Void> chat(SendMessageRequest request) {
        return executeChat(request)
                .flatMap(response -> {
                    if (response.getTask() != null) {
                        return taskStoreGateway.save(response.getTask()).thenReturn(response);
                    }
                    return Mono.just(response);
                })
                .flatMap(agentResponseGateway::sendResponse);
    }

    public Mono<SendMessageResponse> chatAndRespond(SendMessageRequest request) {
        return executeChat(request);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return DIACRITICAL_MARKS.matcher(decomposed).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.,;:!?]", "");
    }

    boolean shouldSearchPlanning(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() < MIN_PLANNING_TEXT_LENGTH) {
            return false;
        }
        return !SHORT_COMMANDS.contains(normalizeText(trimmed));
    }

    /**
     * Determina si el texto contiene alguna palabra que redirige al flujo general de consulta.
     *
     * <p>Conserva deliberadamente la semántica de subcadena vigente. La detección por palabra
     * completa forma parte de la corrección de DP-01, planificada para la Fase 03.
     */
    private boolean containsGeneralKeyword(String userText) {
        String lowered = userText.toLowerCase(Locale.ROOT);
        return GENERAL_KEYWORDS.stream().anyMatch(lowered::contains);
    }

    private String formatChunks(List<PlanningChunk> chunks) {        if (chunks == null || chunks.isEmpty()) {
            return "No hay contexto de planeación adicional.";
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (PlanningChunk chunk : chunks) {
            contextBuilder.append("- Sección: ").append(chunk.sectionName()).append("\n")
                    .append("  Contenido: ").append(chunk.content()).append("\n\n");
        }
        return contextBuilder.toString();
    }

    private Mono<SendMessageResponse> executeChat(SendMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String contextId = extractContextId(request);
        String userText = request != null && request.getMessage() != null ? request.getMessage().extractText() : null;

        if (userText == null || userText.trim().isEmpty()) {
            return Mono.just(createSuccessResponse(request, NO_CONTENT, 0));
        }

        boolean isGeneralFlow =
                (contextId != null && (contextId.startsWith("general") || contextId.startsWith(
                        "dashboard")))
                        || containsGeneralKeyword(userText);

        if (isGeneralFlow) {
            return runGeneralFlow(request, userText, contextId, startTime);
        }

        // --- DETECTAR FLUJO DE AUDITORÍA DE CALIDAD POR ID ---
        Matcher auditMatcher = AUDIT_REQUEST_PATTERN.matcher(userText);
        if (auditMatcher.find()) {
            return runQualityAuditFlow(request, auditMatcher.group(1), contextId, startTime);
        }

        // --- DETECTAR FLUJO DE REFINAMIENTO DE HISTORIA POR ID ---
        Matcher refineMatcher = WORK_ITEM_ID_PATTERN.matcher(userText);
        if (userText.toLowerCase(Locale.ROOT).contains(REFINEMENT_MARKER)
                && refineMatcher.find()) {
            return runRefinementFlow(request, refineMatcher.group(1), contextId, startTime);
        }

        if (!shouldSearchPlanning(userText)) {
            return handleSpecialCommands(request, userText, contextId, startTime);
        }

        return runPlanningSimilarityFlow(request, userText, contextId, startTime);
    }

    private Mono<SendMessageResponse> runGeneralFlow(SendMessageRequest request, String userText,
            String contextId, long startTime) {
        return chatGateway.sendMessage(userText, contextId)
                .map(llmResponse -> createSuccessResponse(request, llmResponse,
                        System.currentTimeMillis() - startTime))
                .onErrorResume(error -> {
                    log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                    return Mono.just(
                            createErrorResponse(request, startTime, error.getMessage()));
                });
    }

    private Mono<SendMessageResponse> runQualityAuditFlow(SendMessageRequest request,
            String workItemId, String contextId, long startTime) {
        log.info("Ejecutando flujo de auditoría de calidad para Work Item ID: " + workItemId);
        String prompt = promptTemplatePort.render(PromptTemplateId.QUALITY_AUDIT, Map.of(
                VAR_WORK_ITEM_ID, workItemId,
                VAR_ORGANIZATION, defaultOrg,
                VAR_PROJECT, defaultProject,
                VAR_STANDARDS, agileGuideContent + "\n\n" + qualityAuditContent));
        return chatGateway.sendMessage(prompt, contextId)
                .map(llmResponse -> {
                    // Se retira el bloque JSON técnico delimitado por AUDIT_JSON_START/END
                    String finalResponse = AUDIT_JSON_BLOCK.matcher(llmResponse)
                            .replaceAll("")
                            .trim();
                    return createSuccessResponse(request, finalResponse,
                            System.currentTimeMillis() - startTime);
                })
                .onErrorResume(error -> {
                    log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                    return Mono.just(createErrorResponse(request, startTime, error.getMessage()));
                });
    }

    private Mono<SendMessageResponse> runRefinementFlow(SendMessageRequest request,
            String workItemId, String contextId, long startTime) {
        String prompt = promptTemplatePort.render(PromptTemplateId.STORY_REFINEMENT, Map.of(
                VAR_WORK_ITEM_ID, workItemId,
                VAR_ORGANIZATION, defaultOrg,
                VAR_PROJECT, defaultProject,
                VAR_AGILE_GUIDE, agileGuideContent));
        return chatGateway.sendMessage(prompt, contextId)
                .map(llmResponse -> {
                    String finalResponse = EstimationSummary.compose(llmResponse,
                            ComplexityEstimation.parseFrom(llmResponse), CONFIRM_UPDATE_STORY);
                    return createSuccessResponse(request, finalResponse,
                            System.currentTimeMillis() - startTime);
                })
                .onErrorResume(error -> {
                    log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                    return Mono.just(
                            createErrorResponse(request, startTime, error.getMessage()));
                });
    }

    private Mono<SendMessageResponse> runPlanningSimilarityFlow(SendMessageRequest request,
            String userText, String contextId, long startTime) {
        return vectorStorePort.searchSimilarity(userText, null, RAG_MAX_RESULTS)
                .collectList()
                .onErrorResume(error -> {
                    log.log(SEVERE, "Error searching planning vectors, continuing without context",
                            error);
                    return Mono.just(List.of());
                })
                .flatMap(chunks -> {
                    String formattedChunks = formatChunks(chunks);
                    String prompt = promptTemplatePort.render(PromptTemplateId.PLANNING_DRAFT,
                            Map.of(VAR_ORIGINAL_IDEA, userText, VAR_RAG_CONTEXT, formattedChunks));
                    return chatGateway.sendMessage(prompt, contextId);
                })
                .map(llmResponse -> createSuccessResponse(request, llmResponse,
                        System.currentTimeMillis() - startTime))
                .onErrorResume(error -> {
                    log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                    return Mono.just(createErrorResponse(request, startTime, error.getMessage()));
                });
    }

    private Mono<SendMessageResponse> handleSpecialCommands(SendMessageRequest request,
            String userText,
            String contextId, long startTime) {
        String normalized = normalizeText(userText);

        if (APPROVAL_COMMANDS.contains(normalized)) {
            String prompt = promptTemplatePort.render(PromptTemplateId.STRUCTURED_STORY, Map.of(
                    VAR_CORPORATE_TEMPLATE, templateMarkdown,
                    VAR_AGILE_GUIDE, agileGuideContent));
            return chatGateway.sendMessage(prompt, contextId)
                    .map(llmResponse -> {
                        String finalResponse = EstimationSummary.compose(llmResponse,
                                ComplexityEstimation.parseFrom(llmResponse), CONFIRM_CREATE_STORY);
                        return createSuccessResponse(request, finalResponse,
                                System.currentTimeMillis() - startTime);
                    })
                    .onErrorResume(error -> {
                        log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                        return Mono.just(
                                createErrorResponse(request, startTime, error.getMessage()));
                    });
        } else if (DIVISION_COMMANDS.contains(normalized)) {
            String prompt = promptTemplatePort.render(PromptTemplateId.STORY_DIVISION,
                    Map.of(VAR_AGILE_GUIDE, agileGuideContent));
            return chatGateway.sendMessage(prompt, contextId)
                    .map(llmResponse -> createSuccessResponse(request,
                            llmResponse + CONFIRM_CREATE_DIVIDED_STORIES,
                            System.currentTimeMillis() - startTime))
                    .onErrorResume(error -> {
                        log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                        return Mono.just(
                                createErrorResponse(request, startTime, error.getMessage()));
                    });
        } else {
            // El texto ya fue validado como no vacío en executeChat, se envía tal cual al modelo
            return chatGateway.sendMessage(userText, contextId)
                    .map(llmResponse -> createSuccessResponse(request, llmResponse,
                            System.currentTimeMillis() - startTime))
                    .onErrorResume(error -> {
                        log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                        return Mono.just(
                                createErrorResponse(request, startTime, error.getMessage()));
                    });
        }
    }

    private String buildPrompt(SendMessageRequest request) {
        if (request == null || request.getMessage() == null) {
            return NO_CONTENT;
        }
        String content = request.getMessage().extractText();
        if (content == null || content.isBlank()) {
            return NO_CONTENT;
        }
        if ("CREATE_STRUCTURED_USER_STORY".equals(content) || content.contains(
                "\"intent\":\"CREATE_STRUCTURED_USER_STORY\"")) {
            return String.format("""
                    Debes crear una Historia de Usuario estructurada en Azure DevOps siguiendo la plantilla de HU/HA corporativa.
                    
                    Plantilla corporativa a utilizar:
                    %s
                    
                    PASO 1 - Crear Historia de Usuario:
                    Ejecuta la herramienta 'createWorkItem' con el tipo 'User Story' (o el tipo correspondiente) y las operaciones JSON Patch para definir el título, descripción con el formato completo de la plantilla Markdown (incluyendo DoR, DoD y Criterios), organización y proyecto.
                    
                    PASO 2 - Crear y Vincular Tareas Hijas (Desglose):
                    Para cada una de las tareas listadas bajo '🛠️ Tareas' de la plantilla, ejecuta la herramienta 'createWorkItem' con el tipo 'Task'.
                    Posteriormente, por cada tarea hija creada, ejecuta la herramienta 'updateWorkItem' sobre el ID de la tarea para agregar un enlace jerárquico tipo 'parent' (relación 'System.LinkTypes.Hierarchy-Reverse' apuntando a la URL de la Historia de Usuario creada en el Paso 1).
                    
                    Al finalizar, retorna un resumen en español de la Historia de Usuario creada con sus enlaces a las tareas hijas en Azure DevOps.
                    """, templateMarkdown);
        }
        return content;
    }

    private SendMessageResponse createSuccessResponse(SendMessageRequest request,
            String llmResponse, long executionTimeMs) {
        String contextId = extractContextId(request);
        Message agentMessage = buildAgentMessage(contextId, llmResponse);

        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .contextId(contextId)
                .status(TaskStatus.builder()
                        .state(TaskState.COMPLETED)
                        .message(agentMessage)
                        .timestamp(nowIso())
                        .build())
                .artifacts(List.of(
                        Artifact.builder()
                                .artifactId(UUID.randomUUID().toString())
                                .name("agent-response")
                                .description("Primary response generated by the consumer agent")
                                .parts(List.of(Part.ofData(Map.of(
                                        "response", llmResponse,
                                        KEY_EXECUTION_TIME_MS, executionTimeMs))))
                                .build()))
                .history(List.of(agentMessage))
                .metadata(Map.of(KEY_EXECUTION_TIME_MS, executionTimeMs))
                .build();

        return SendMessageResponse.builder().message(agentMessage).task(task).build();
    }

    private SendMessageResponse createErrorResponse(SendMessageRequest request, long startTime,
            String errorMessage) {
        String contextId = extractContextId(request);
        Message agentMessage = buildAgentMessage(contextId,
                "No pude completar la operación: " + errorMessage);
        long executionTimeMs = System.currentTimeMillis() - startTime;

        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .contextId(contextId)
                .status(TaskStatus.builder()
                        .state(TaskState.FAILED)
                        .message(agentMessage)
                        .timestamp(nowIso())
                        .build())
                .history(List.of(agentMessage))
                .metadata(Map.of(
                        KEY_EXECUTION_TIME_MS, executionTimeMs,
                        "errorCode", INTERNAL_ERROR,
                        "errorMessage", errorMessage == null ? "unknown" : errorMessage))
                .build();

        return SendMessageResponse.builder().message(agentMessage).task(task).build();
    }
}
