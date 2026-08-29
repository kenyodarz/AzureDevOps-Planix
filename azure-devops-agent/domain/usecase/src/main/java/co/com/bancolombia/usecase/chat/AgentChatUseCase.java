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
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private static final String INFO_BLOCK_TEMPLATE = """
            
            
            ---
            📊 **Estimación de Complejidad (Story Points):** %d
            ⚠️ **Nivel de Incertidumbre:** %s
            🔍 **Justificación:** %s""";

    private static final String HIGH_COMPLEXITY_ALERT_TEMPLATE = """
            
            
            🚨 **Alerta de Complejidad:** Esta historia supera el estándar de la célula de 8 puntos. Sugiero dividirla. \
            Responde 'Dividir' para desglosarla automáticamente en historias de máximo 8 puntos, \
            o 'Crear' para registrarla completa.""";

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
        return text.trim().toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replaceAll("[.,;:!?]", "");
    }

    private int extractPuntosFromLlmResponse(String response) {
        if (response == null) {
            return 1;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"puntos\"\\s*:\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.log(java.util.logging.Level.WARNING,
                        "Failed to parse points from response: " + matcher.group(1), e);
            }
        }
        return 1;
    }

    private String extractJustificacion(String response) {
        if (response == null) {
            return "la complejidad del alcance propuesto";
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"incertidumbre_justificacion\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "la complejidad del alcance propuesto";
    }

    private String extractIncertidumbreNivel(String response) {
        if (response == null) {
            return "Media";
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"incertidumbre_nivel\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Media";
    }

    boolean shouldSearchPlanning(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() < 25) {
            return false;
        }
        String normalized = normalizeText(trimmed);

        List<String> commands = List.of("aprobado", "crear", "si", "no", "procede", "dividela",
                "divide", "de acuerdo", "dividir");
        return !commands.contains(normalized);
    }

    private String formatChunks(List<PlanningChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
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
                        || userText.toLowerCase().contains("lista")
                        || userText.toLowerCase().contains("busca")
                        || userText.toLowerCase().contains("consulta")
                        || userText.toLowerCase().contains("reporte");

        if (isGeneralFlow) {
            return runGeneralFlow(request, userText, contextId, startTime);
        }

        // --- DETECTAR FLUJO DE AUDITORÍA DE CALIDAD POR ID ---
        java.util.regex.Matcher auditMatcher = java.util.regex.Pattern.compile(
                "(?:audita|evalua|revisa|calidad).*?\\(ID:\\s*(\\d+)\\)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userText);
        if (auditMatcher.find()) {
            return runQualityAuditFlow(request, auditMatcher.group(1), contextId, startTime);
        }

        // --- DETECTAR FLUJO DE REFINAMIENTO DE HISTORIA POR ID ---
        java.util.regex.Matcher refineMatcher = java.util.regex.Pattern.compile(
                "\\(ID:\\s*(\\d+)\\)").matcher(userText);
        if (userText.toLowerCase().contains("analicemos y refinemos") && refineMatcher.find()) {
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
                    // Limpiar el JSON delimitado por AUDIT_JSON_START y AUDIT_JSON_END de la respuesta del usuario
                    String finalResponse = llmResponse
                            .replaceAll("(?s)AUDIT_JSON_START.*?AUDIT_JSON_END", "")
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
                    int puntos = extractPuntosFromLlmResponse(llmResponse);
                    String justificacion = extractJustificacion(llmResponse);
                    String nivelIncertidumbre = extractIncertidumbreNivel(llmResponse);
                    String finalResponse = llmResponse;

                    finalResponse = finalResponse.replaceAll(
                            "(?s)```json\\s*\\{\\s*\"puntos\"[^\\}]+\\}\\s*```", "").trim();
                    finalResponse = finalResponse.replaceAll("(?s)\\{\\s*\"puntos\"[^\\}]+\\}",
                                    "")
                            .trim();

                    String infoBlock = String.format(INFO_BLOCK_TEMPLATE,
                            puntos, nivelIncertidumbre, justificacion
                    );
                    finalResponse += infoBlock;

                    if (puntos >= 13) {
                        finalResponse += HIGH_COMPLEXITY_ALERT_TEMPLATE;
                    } else {
                        finalResponse += "\n\n¿Deseas actualizar el Work Item en Azure DevOps con esta versión refinada? Responde 'Aprobar' o 'Actualizar' para proceder.";
                    }

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
        return vectorStorePort.searchSimilarity(userText, null, 3)
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
        boolean isApproval =
                normalized.equals("aprobado") || normalized.equals("si") || normalized.equals(
                        "de acuerdo");
        boolean isDivision = normalized.equals("dividir") || normalized.equals("dividela")
                || normalized.equals("divide");

        if (isApproval) {
            String prompt = promptTemplatePort.render(PromptTemplateId.STRUCTURED_STORY, Map.of(
                    VAR_CORPORATE_TEMPLATE, templateMarkdown,
                    VAR_AGILE_GUIDE, agileGuideContent));
            return chatGateway.sendMessage(prompt, contextId)
                    .map(llmResponse -> {
                        int puntos = extractPuntosFromLlmResponse(llmResponse);
                        String justificacion = extractJustificacion(llmResponse);
                        String nivelIncertidumbre = extractIncertidumbreNivel(llmResponse);
                        String finalResponse = llmResponse;

                        // Remover el bloque JSON de la respuesta para que quede limpia la interfaz para el usuario
                        finalResponse = finalResponse.replaceAll(
                                "(?s)```json\\s*\\{\\s*\"puntos\"[^\\}]+\\}\\s*```", "").trim();
                        finalResponse = finalResponse.replaceAll("(?s)\\{\\s*\"puntos\"[^\\}]+\\}",
                                        "")
                                .trim();

                        // Añadir el bloque informativo de la estimación siempre
                        String infoBlock = String.format(INFO_BLOCK_TEMPLATE,
                                puntos, nivelIncertidumbre, justificacion
                        );
                        finalResponse += infoBlock;

                        if (puntos >= 13) {
                            String alert = HIGH_COMPLEXITY_ALERT_TEMPLATE;
                            finalResponse += alert;
                        } else {
                            String alert = "\n\n¿Deseas registrar esta Historia con sus tareas desglosadas en Azure DevOps? Responde 'Crear' para proceder.";
                            finalResponse += alert;
                        }

                        return createSuccessResponse(request, finalResponse,
                                System.currentTimeMillis() - startTime);
                    })
                    .onErrorResume(error -> {
                        log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                        return Mono.just(
                                createErrorResponse(request, startTime, error.getMessage()));
                    });
        } else if (isDivision) {
            String prompt = promptTemplatePort.render(PromptTemplateId.STORY_DIVISION,
                    Map.of(VAR_AGILE_GUIDE, agileGuideContent));
            return chatGateway.sendMessage(prompt, contextId)
                    .map(llmResponse -> {
                        String finalResponse = llmResponse
                                + "\n\n¿Deseas proceder con el registro de estas historias divididas en Azure DevOps? Responde 'Crear' para registrar todas las historias con sus tareas correspondientes.";
                        return createSuccessResponse(request, finalResponse,
                                System.currentTimeMillis() - startTime);
                    })
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
