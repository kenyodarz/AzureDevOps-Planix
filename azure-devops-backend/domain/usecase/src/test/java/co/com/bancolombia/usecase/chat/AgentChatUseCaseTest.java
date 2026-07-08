package co.com.bancolombia.usecase.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AgentChatUseCaseTest {

    private static final int MAX_RESULTS_LIMIT = 3;
    private static final String TEMPLATE_MARKDOWN = "Template content";
    private static final String AGILE_GUIDE = "Contenido de la guia de agilidad";

    private ChatGateway chatGateway;
    private AgentResponseGateway agentResponseGateway;
    private TaskStoreGateway taskStoreGateway;
    private PlanningVectorStorePort vectorStorePort;
    private AgentChatUseCase agentChatUseCase;

    @BeforeEach
    void setUp() {
        chatGateway = mock(ChatGateway.class);
        agentResponseGateway = mock(AgentResponseGateway.class);
        taskStoreGateway = mock(TaskStoreGateway.class);
        vectorStorePort = mock(PlanningVectorStorePort.class);
        agentChatUseCase = new AgentChatUseCase(chatGateway, agentResponseGateway, taskStoreGateway,
                TEMPLATE_MARKDOWN, vectorStorePort, AGILE_GUIDE, "grupobancolombia",
                "Vicepresidencia Servicios de Tecnología");
    }

    @Test
    void shouldGenerateTitleAndDescriptionInProseWithoutEstimationInPhase1() {
        // GIVEN
        String userIdea = "implementar notificaciones push para movil";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-phase1")
                        .parts(List.of(Part.ofText(userIdea)))
                        .build())
                .build();

        PlanningChunk mockChunk = PlanningChunk.builder()
                .id("chunk-1")
                .initiativeId("init-123")
                .sectionName("Requerimientos de Notificación")
                .content("Las notificaciones deben ser push y enviarse al dispositivo móvil.")
                .metadata(Map.of())
                .build();

        when(vectorStorePort.searchSimilarity(userIdea, null, MAX_RESULTS_LIMIT))
                .thenReturn(Flux.just(mockChunk));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatGateway.sendMessage(promptCaptor.capture(), eq("ctx-phase1")))
                .thenReturn(
                        Mono.just("Propuesta de Titulo y Descripcion en prosa sin estimaciones"));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("Propuesta de Titulo y Descripcion en prosa sin estimaciones",
                            response.getMessage().extractText());
                })
                .verifyComplete();

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains(userIdea));
        assertTrue(capturedPrompt.contains(
                "Analiza la siguiente idea de desarrollo y genera una propuesta de Título y Descripción inicial en prosa."));
        assertTrue(capturedPrompt.contains(
                "NO incluyas Criterios de Aceptación, Tareas, DoR, DoD, estimaciones de Story Points ni incertidumbre"));
        assertTrue(capturedPrompt.contains("Requerimientos de Notificación"));
        assertTrue(capturedPrompt.contains("Las notificaciones deben ser push"));
    }

    @Test
    void shouldGenerateDraftAndAppendRegistrationPromptInPhase2WhenPointsAreLessThan13() {
        // GIVEN
        String approvalMessage = "aprobado";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-phase2-low")
                        .parts(List.of(Part.ofText(approvalMessage)))
                        .build())
                .build();

        String llmResponseWithJson = """
                ```markdown
                ### Mi HU
                Como usuario quiero ingresar al sistema para ver mi cuenta.
                ```
                ```json
                {
                  "puntos": 5,
                  "incertidumbre_nivel": "Baja",
                  "incertidumbre_justificacion": "Ya se ha realizado antes"
                }
                ```
                """;

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatGateway.sendMessage(promptCaptor.capture(), eq("ctx-phase2-low")))
                .thenReturn(Mono.just(llmResponseWithJson));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    String resultText = response.getMessage().extractText();
                    assertTrue(resultText.contains("### Mi HU"));
                    assertTrue(resultText.contains(
                            "¿Deseas registrar esta Historia con sus tareas desglosadas en Azure DevOps? Responde 'Crear' para proceder."));
                })
                .verifyComplete();

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains(
                "Genera el borrador estructurado en Markdown de la Historia de Usuario"));
        assertTrue(capturedPrompt.contains("puntos"));
    }

    @Test
    void shouldGenerateDraftAndAppendDivisionAlertInPhase2WhenPointsAre13OrMore() {
        // GIVEN
        String approvalMessage = "si";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-phase2-high")
                        .parts(List.of(Part.ofText(approvalMessage)))
                        .build())
                .build();

        String llmResponseWithJson = """
                ```markdown
                ### Mi Macro HU Compleja
                Como usuario quiero un modulo de auditoria general.
                ```
                ```json
                {
                  "puntos": 13,
                  "incertidumbre_nivel": "Alta",
                  "incertidumbre_justificacion": "alta complejidad en integraciones"
                }
                ```
                """;

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatGateway.sendMessage(promptCaptor.capture(), eq("ctx-phase2-high")))
                .thenReturn(Mono.just(llmResponseWithJson));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    String resultText = response.getMessage().extractText();
                    assertTrue(resultText.contains("### Mi Macro HU Compleja"));
                    assertTrue(resultText.contains(
                            "📊 **Estimación de Complejidad (Story Points):** 13"));
                    assertTrue(resultText.contains("⚠️ **Nivel de Incertidumbre:** Alta"));
                    assertTrue(resultText.contains(
                            "🔍 **Justificación:** alta complejidad en integraciones"));
                    assertTrue(resultText.contains(
                            "🚨 **Alerta de Complejidad:** Esta historia supera el estándar de la célula de 8 puntos."));
                })
                .verifyComplete();
    }

    @Test
    void shouldExecuteDivisionWhenMessageIsDividir() {
        // GIVEN
        String divisionMessage = "Dividir";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-division")
                        .parts(List.of(Part.ofText(divisionMessage)))
                        .build())
                .build();

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatGateway.sendMessage(promptCaptor.capture(), eq("ctx-division")))
                .thenReturn(Mono.just("Desglose de historias de maximo 8 puntos"));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    String resultText = response.getMessage().extractText();
                    assertTrue(resultText.contains("Desglose de historias de maximo 8 puntos"));
                    assertTrue(resultText.contains(
                            "¿Deseas proceder con el registro de estas historias divididas en Azure DevOps?"));
                })
                .verifyComplete();

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains(
                "La propuesta actual de Historia de Usuario supera los 8 puntos de complejidad."));
        assertTrue(capturedPrompt.contains(
                "Divide esta idea de desarrollo en múltiples Historias de Usuario"));
    }

    @Test
    void shouldExecuteCreationWhenMessageIsCrear() {
        // GIVEN
        String creationMessage = "Crear";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-creation")
                        .parts(List.of(Part.ofText(creationMessage)))
                        .build())
                .build();

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatGateway.sendMessage(promptCaptor.capture(), eq("ctx-creation")))
                .thenReturn(Mono.just("Registro en Azure DevOps completado exitosamente"));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("Registro en Azure DevOps completado exitosamente",
                            response.getMessage().extractText());
                })
                .verifyComplete();

        String capturedPrompt = promptCaptor.getValue();
        assertEquals("Crear", capturedPrompt);
    }

    @Test
    void shouldRefineExistingWorkItemWhenRequested() {
        // GIVEN
        String refinementMsg = "Asistente, quiero que analicemos y refinemos la Historia de Usuario: \"Mi HU de pruebas\" (ID: 7583198). Ayúdame a revisar sus criterios de aceptación y calidad de documentación.";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-refinement")
                        .parts(List.of(Part.ofText(refinementMsg)))
                        .build())
                .build();

        String llmResponseWithJson = """
                ### Refinamiento Propuesto para HU 7583198
                - Criterios de Aceptación mejorados
                - DoD validado
                ```json
                {
                  "puntos": 5,
                  "incertidumbre_nivel": "Baja",
                  "incertidumbre_justificacion": "Se cuenta con un API similar"
                }
                ```
                """;

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatGateway.sendMessage(promptCaptor.capture(), eq("ctx-refinement")))
                .thenReturn(Mono.just(llmResponseWithJson));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    String resultText = response.getMessage().extractText();
                    assertTrue(resultText.contains("### Refinamiento Propuesto para HU 7583198"));
                    assertTrue(resultText.contains(
                            "📊 **Estimación de Complejidad (Story Points):** 5"));
                    assertTrue(resultText.contains("⚠️ **Nivel de Incertidumbre:** Baja"));
                    assertTrue(resultText.contains(
                            "🔍 **Justificación:** Se cuenta con un API similar"));
                    assertTrue(resultText.contains(
                            "¿Deseas actualizar el Work Item en Azure DevOps con esta versión refinada? Responde 'Aprobar' o 'Actualizar' para proceder."));
                })
                .verifyComplete();

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains("7583198"));
        assertTrue(capturedPrompt.contains("getWorkItem"));
        assertTrue(capturedPrompt.contains("grupobancolombia"));
    }

    @Test
    void shouldBypassToFreeFormLlmWhenQueryIntentDetected() {
        // GIVEN
        String userQuery = "lista los items de la célula EQU1096 - EXODIA para el Sprint 247";
        SendMessageRequest request = SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-query")
                        .parts(List.of(Part.ofText(userQuery)))
                        .build())
                .build();

        when(chatGateway.sendMessage(userQuery, "ctx-query"))
                .thenReturn(Mono.just("Respuesta de la consulta con ítems"));

        // WHEN
        Mono<SendMessageResponse> resultMono = agentChatUseCase.chatAndRespond(request);

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("Respuesta de la consulta con ítems",
                            response.getMessage().extractText());
                })
                .verifyComplete();
    }
}