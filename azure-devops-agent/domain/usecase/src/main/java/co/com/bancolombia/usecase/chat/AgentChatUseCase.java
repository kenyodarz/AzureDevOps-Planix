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

    private static final String INFO_BLOCK_TEMPLATE = """
            
            
            ---
            📊 **Estimación de Complejidad (Story Points):** %d
            ⚠️ **Nivel de Incertidumbre:** %s
            🔍 **Justificación:** %s""";

    private static final String HIGH_COMPLEXITY_ALERT_TEMPLATE = """
            
            
            🚨 **Alerta de Complejidad:** Esta historia supera el estándar de la célula de 8 puntos. Sugiero dividirla. \
            Responde 'Dividir' para desglosarla automáticamente en historias de máximo 8 puntos, \
            o 'Crear' para registrarla completa.""";

    private static final String FASE1_PROMPT_TEMPLATE = """
            Eres un Scrum Master y Product Owner Técnico de Bancolombia. \
            Analiza la siguiente idea de desarrollo y genera una propuesta de Título y Descripción inicial en prosa.
            
            Sigue estrictamente estas reglas:
            1. Genera una propuesta de Título con el formato `<Equipo/Frente> | <Nombre de la HU/HA>`.
               - Si en la idea original no se especifica el equipo o frente de trabajo, solicita amablemente en tu respuesta que se proporcione. Si no se suministra, usa "Marketplace - Tools" por defecto.
            2. Genera una propuesta de Descripción clara y fluida en prosa describiendo el objetivo de la HU/HA.
            3. NO incluyas Criterios de Aceptación, Tareas, DoR, DoD, estimaciones de Story Points ni incertidumbre en esta etapa.
            4. Pregunta al usuario si aprueba esta propuesta de título y descripción.
            
            Idea original:
            %s
            
            === CONTEXTO ADICIONAL (RAG) ===
            %s""";

    private static final String FASE2_PROMPT_TEMPLATE = """
            Genera el borrador estructurado en Markdown de la Historia de Usuario (HU) o Historia Habilitadora (HA) \
            siguiendo estrictamente la Plantilla Corporativa y la Guía de Agilidad.
            
            Usa esta plantilla:
            %s
            
            Guía de agilidad:
            %s
            
            Reglas de Formato e Inyección de Estimación:
            1. Presenta la HU/HA completa en formato Markdown encerrada estrictamente dentro de una única caja de código de formato Markdown (delimitada por ```markdown y ```).
            2. En el listado de "Definition of Done (DoD)" de la plantilla, incluye obligatoriamente: `- [ ] Cumple todos los criterios de aceptación`. Asegúrate de no duplicar corchetes ni el formato de lista.
            3. Evalúa la complejidad en Story Points (Fibonacci) y el nivel de incertidumbre de forma REALISTA.
               - **No juegues a ser 'The Wizard of Master Code'**. Estima pensando en el esfuerzo real de un desarrollador humano (escribir código, pruebas unitarias y de integración, documentar, pasar pipelines).
               - **Regla de Mínimos**: Cualquier desarrollo técnico que involucre el desarrollo de múltiples endpoints (CRUD/BFF), base de datos, seguridad (JWT/RBAC) o integraciones de APIs no puede ser estimado en menos de **5 puntos**. 1 y 2 puntos quedan estrictamente reservados para cambios de configuración triviales o ajustes estéticos simples.
            4. Al final de tu respuesta (FUERA de la caja de código), debes retornar obligatoriamente un bloque JSON estructurado con el siguiente formato exacto:
            ```json
            {
              "puntos": X,
              "incertidumbre_nivel": "Nula/Baja/Media/Alta/Critica",
              "incertidumbre_justificacion": "..."
            }
            ```
            No agregues texto explicativo después del bloque JSON.""";

    private static final String DIVISION_PROMPT_TEMPLATE = """
            La propuesta actual de Historia de Usuario supera los 8 puntos de complejidad. \
            Divide esta idea de desarrollo en múltiples Historias de Usuario individuales y coherentes, \
            de manera que ninguna de las nuevas historias supere los 8 puntos de complejidad (escala de Fibonacci: 1, 2, 3, 5, 8).
            
            Para cada una de las nuevas Historias de Usuario, genera:
            1. Título con formato `<Equipo/Frente> | <Nombre de la HU/HA>`.
            2. Descripción fluida en prosa redactada en formato oficial: "Yo como [rol] requiero [necesidad] para [beneficio]".
            3. Estimación en Story Points y una justificación clara basada en la complejidad.
            
            Muestra el desglose de las nuevas historias de usuario estimadas para aprobación.
            
            === GUÍA DE AGILIDAD (HISTORIA_USUARIO.md) ===
            %s""";

    private static final String REFINEMENT_PROMPT_TEMPLATE = """
            Eres un Scrum Master y Product Owner Técnico de Bancolombia.
            Tu objetivo es analizar y proponer mejoras de refinamiento para una Historia de Usuario o Historia Habilitadora existente en Azure DevOps.
            
            Paso 1: Usa la herramienta 'getWorkItem' para obtener los detalles del Work Item con ID "%s" (organización: "%s", proyecto: "%s").
            
            Paso 2: Una vez que obtengas la descripción y criterios del Work Item:
               - Analiza la descripción y criterios de aceptación actuales según la Guía de Agilidad corporativa.
               - Propón en una única caja de código Markdown (delimitada por ```markdown y ```) la descripción y criterios de aceptación refinados en el formato oficial, y el DoD.
               - IMPORTANTE: No incluyas ni evalúes el DoR (Definition of Ready), ya que este ha sido retirado. Solo concéntrate en los Criterios de Aceptación y el Definition of Done (DoD).
               - Evalúa la complejidad en Story Points (Fibonacci) y el nivel de incertidumbre (Nula/Baja/Media/Alta/Critica) con su respectiva justificación de forma realista.
            
            Paso 3: Al final de tu respuesta (FUERA de la caja de código), retorna obligatoriamente este bloque JSON con el formato exacto:
            ```json
            {
              "puntos": X,
              "incertidumbre_nivel": "...",
              "incertidumbre_justificacion": "..."
            }
            ```
            Pregunta al usuario si aprueba esta propuesta de refinamiento.
            
            === GUÍA DE AGILIDAD (HISTORIA_USUARIO.md) ===
            %s
            """;

    private static final String AUDIT_QUALITY_PROMPT_TEMPLATE = """
            Eres un Agile Coach y Quality Analyst de Bancolombia especializado en requisitos ágiles.
            
            Usa la herramienta 'getWorkItem' para obtener el Work Item con ID "%s"
            (organización: "%s", proyecto: "%s").
            
            Evalúa la historia usando los estándares corporativos de Bancolombia:
            
            === ESTÁNDARES CORPORATIVOS ===
            %s
            ==============================
            
            Genera un reporte en Markdown con estas secciones:
            
            ## 📋 Auditoría de Calidad — Work Item #%s
            
            ### 1. Título
            ¿Cumple el formato `<Equipo/Frente> | <Nombre de la HU/HA>`?
            Diagnóstico: [✅ Cumple / ⚠️ Parcial / ❌ No cumple] — Justificación.
            
            ### 2. Descripción
            ¿Sigue el formato "Yo como [rol], requiero [necesidad] para [beneficio]"?
            ¿Es clara y sin ambigüedades?
            Diagnóstico: [✅ / ⚠️ / ❌] — Justificación.
            
            ### 3. Criterios de Aceptación
            ¿Están definidos como checklist medible?
            ¿Permiten al PO aceptar/rechazar objetivamente?
            Diagnóstico: [✅ / ⚠️ / ❌] — Cuáles faltan o son ambiguos.
            
            ### 4. Definition of Done (DoD)
            ¿Incluye los 7 ítems obligatorios corporativos?
            (Desarrollo ✓ Pruebas unitarias ✓ Integración ✓ Documentación ✓ Seguridad ✓ CA cumplidos ✓ Merge)
            Diagnóstico: [✅ / ⚠️ / ❌] — Lista los ítems faltantes.
            
            ### 5. Estimación (Story Points)
            ¿El SP asignado pertenece a la escala Fibonacci (1,2,3,5,8)? ¿Es una estimación realista del esfuerzo requerido?
            Recuerda: la guía corporativa NO establece mínimos obligatorios de Story Points para tareas de bases de datos, integraciones o APIs. Por lo tanto, estimar 1, 2 o 3 SP es totalmente válido si el esfuerzo es bajo y está justificado.
            Diagnóstico: [✅ / ⚠️ / ❌] — Justificación basada exclusivamente en la escala de Fibonacci y la coherencia del esfuerzo.
            
            ### 6. Estado y Asignación
            ¿El estado (New/Active/Impedimento/Closed) es correcto dado el contexto?
            ¿Tiene responsable asignado?
            Diagnóstico: [✅ / ⚠️ / ❌]
            
            ### 7. Puntaje Global
            
            | Dimensión               | Puntaje | Peso |
            |-------------------------|---------|------|
            | Título                  | X/10    | 10%% |
            | Descripción             | X/20    | 20%% |
            | Criterios de Aceptación | X/25    | 25%% |
            | DoD                     | X/25    | 25%% |
            | Estimación              | X/10    | 10%% |
            | Estado/Asignación       | X/10    | 10%% |
            | **TOTAL**               | **X/100** |    |
            
            ### 8. Errores Críticos
            (omitir si no aplica)
            
            ### 9. Sugerencias de Mejora
            Redacción accionable para que el colaborador pueda mejorar la historia.
            
            Al final de tu respuesta, fuera del reporte en Markdown, debes incluir obligatoriamente este bloque JSON delimitado exactamente por las etiquetas AUDIT_JSON_START y AUDIT_JSON_END:
            AUDIT_JSON_START
            {
              "workItemId": "%s",
              "qualityScore": X,
              "criticalErrors": ["..."],
              "canBeWorkedOn": true
            }
            AUDIT_JSON_END
            """;

    private final ChatGateway chatGateway;
    private final AgentResponseGateway agentResponseGateway;
    private final TaskStoreGateway taskStoreGateway;
    private final String templateMarkdown;
    private final PlanningVectorStorePort vectorStorePort;
    private final String agileGuideContent;
    private final String defaultOrg;
    private final String defaultProject;
    private final String qualityAuditContent;

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
        String prompt = String.format(AUDIT_QUALITY_PROMPT_TEMPLATE,
                workItemId, defaultOrg, defaultProject,
                agileGuideContent + "\n\n" + qualityAuditContent,
                workItemId,
                workItemId);
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
        String prompt = String.format(REFINEMENT_PROMPT_TEMPLATE, workItemId, defaultOrg,
                defaultProject, agileGuideContent);
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
                    String prompt = String.format(FASE1_PROMPT_TEMPLATE, userText, formattedChunks);
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
            String prompt = String.format(FASE2_PROMPT_TEMPLATE, templateMarkdown,
                    agileGuideContent);
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
            String prompt = String.format(DIVISION_PROMPT_TEMPLATE, agileGuideContent);
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
            String prompt = buildPrompt(request);
            return chatGateway.sendMessage(prompt, contextId)
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
