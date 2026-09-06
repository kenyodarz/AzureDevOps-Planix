package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.IntentResolver;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import co.com.bancolombia.usecase.chat.handler.ChatFlowDispatcher;
import co.com.bancolombia.usecase.chat.handler.ChatFlowHandler;
import co.com.bancolombia.usecase.chat.handler.ProgramPlanningFlowHandler;
import co.com.bancolombia.usecase.planning.ProgramPlanningUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, Handler.class})
class RouterRestTest {

    private static final String LEGACY_ENDPOINT = "/message:send";
    private static final String JSON_RPC_ENDPOINT = "/";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AgentChatUseCase agentChatUseCase;

    @MockitoBean
    private co.com.bancolombia.model.chat.gateways.TaskStoreGateway taskStoreGateway;

    @BeforeEach
    void setUp() {
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
    }

    @Test
    void shouldReturn200WithSuccessStatusWhenRequestIsValid() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .task(Task.builder()
                        .id("task-123")
                        .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                        .build())
                .message(Message.builder().role("agent")
                        .parts(java.util.List.of(Part.ofText("Done"))).build())
                .build();

        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .message(Message.builder().role("user")
                        .parts(java.util.List.of(Part.ofText("hello"))).build())
                .build();

        webTestClient.post()
                .uri(LEGACY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(msgReq)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Deprecation", "true")
                .expectHeader().valueEquals("Sunset", "2026-06-30")
                .expectHeader().valueEquals("Link", "</>; rel=\"successor-version\"")
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(SendMessageResponse.class)
                .value(response -> {
                    assertThat(response.getTask().getStatus().getState())
                            .isEqualTo(TaskState.COMPLETED);
                });
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() {
        webTestClient.post()
                .uri(LEGACY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("Deprecation", "true")
                .expectHeader().valueEquals("Sunset", "2026-06-30");
    }

    @Test
    void shouldReturnJsonRpcEnvelopeWhenSendMessageViaRootEndpoint() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .task(Task.builder()
                        .id("task-123")
                        .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                        .build())
                .message(Message.builder().role("agent")
                        .parts(java.util.List.of(Part.ofText("Done"))).build())
                .build();

        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        Map<String, Object> jsonRpcRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "req-001",
                "method", "message/send",
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", "hello")),
                                "messageId", "msg-1",
                                "contextId", "ctx-1")));

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonRpcRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("req-001")
                .jsonPath("$.result.task.id").isEqualTo("task-123")
                .jsonPath("$.result.task.status.state").isEqualTo("completed");
    }

    @Test
    void shouldReturnJsonRpcMethodNotFoundWhenMethodIsUnknown() {
        Map<String, Object> invalidMethodRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "req-404",
                "method", "UnknownMethod",
                "params", Map.of());

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidMethodRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("req-404")
                .jsonPath("$.error.code").isEqualTo(-32601)
                .jsonPath("$.error.message").value(msg -> assertThat(String.valueOf(msg))
                        .contains("Method not found"));
    }

    @Test
    void shouldGetTaskAfterSendMessage() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .task(Task.builder()
                        .id("task-get-001")
                        .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                        .build())
                .message(Message.builder().role("agent")
                        .parts(List.of(Part.ofText("Done"))).build())
                .build();

        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.just(mockResponse));
        when(taskStoreGateway.findById("task-get-001"))
                .thenReturn(Mono.just(mockResponse.getTask()));

        Map<String, Object> sendRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "send-1",
                "method", "message/send",
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", "hola")))));

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendRequest)
                .exchange()
                .expectStatus().isOk();

        Map<String, Object> getTaskRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "get-1",
                "method", "tasks/get",
                "params", Map.of("taskId", "task-get-001"));

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(getTaskRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("get-1")
                .jsonPath("$.result.task.id").isEqualTo("task-get-001")
                .jsonPath("$.result.task.status.state").isEqualTo("completed");
    }

    @Test
    void shouldReturnJsonRpcErrorWhenTaskIsNotFound() {
        when(taskStoreGateway.findById("missing-task"))
                .thenReturn(Mono.empty());

        Map<String, Object> getTaskRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "get-2",
                "method", "tasks/get",
                "params", Map.of("taskId", "missing-task"));

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(getTaskRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("get-2")
                .jsonPath("$.error.code").isEqualTo(-32004)
                .jsonPath("$.error.message").value(msg -> assertThat(String.valueOf(msg))
                        .contains("Task not found"));
    }

    @Test
    void shouldCancelTaskAfterSendMessage() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .task(Task.builder()
                        .id("task-cancel-001")
                        .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                        .build())
                .message(Message.builder().role("agent")
                        .parts(List.of(Part.ofText("Done"))).build())
                .build();

        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.just(mockResponse));
        when(taskStoreGateway.findById("task-cancel-001"))
                .thenReturn(Mono.just(mockResponse.getTask()));

        Map<String, Object> sendRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "send-2",
                "method", "message/send",
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", "hola")))));

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendRequest)
                .exchange()
                .expectStatus().isOk();

        Map<String, Object> cancelTaskRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "cancel-1",
                "method", "tasks/cancel",
                "params", Map.of("taskId", "task-cancel-001"));

        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cancelTaskRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("cancel-1")
                .jsonPath("$.result.task.id").isEqualTo("task-cancel-001")
                .jsonPath("$.result.task.status.state").isEqualTo("canceled");
    }

    @Test
    void shouldReturnAgentCard() {
        webTestClient.get()
                .uri("/.well-known/agent-card.json")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.protocolVersion").isEqualTo("1.0")
                .jsonPath("$.name").isEqualTo("Financial Consumer Agent");
    }

    @Test
    void shouldReturnAgentCardViaAgentJson() {
        webTestClient.get()
                .uri("/.well-known/agent.json")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.protocolVersion").isEqualTo("1.0")
                .jsonPath("$.name").isEqualTo("Financial Consumer Agent");
    }

    @Test
    void shouldReturnAgentCardViaCard() {
        webTestClient.get()
                .uri("/card")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.protocolVersion").isEqualTo("1.0")
                .jsonPath("$.name").isEqualTo("Financial Consumer Agent");
    }

    @Test
    @DisplayName("GIVEN comando de planeación /plan Q3-2026 WHEN se envía vía JSON-RPC "
            + "THEN retorna 200 COMPLETED con tabla Markdown respetando DP-PL-02 y persiste spec maestro")
    void givenPlanningRequest_whenPostCommand_thenReturnsRoadmapMarkdown() {
        // GIVEN: Puertos de infraestructura simulados para el caso de uso real de planeación
        SpecStoragePort specStoragePort = mock(SpecStoragePort.class);
        PromptTemplatePort promptTemplatePort = mock(PromptTemplatePort.class);
        ChatGateway chatGateway = mock(ChatGateway.class);
        AgentResponseGateway agentResponseGateway = mock(AgentResponseGateway.class);

        SpecDocument specCanales = new SpecDocument("frente_canales.md",
                "# Especificación de Canales\n- Funcionalidades móviles prioritarias",
                "/specs/frente_canales.md");
        when(specStoragePort.getSpec("frente_canales.md")).thenReturn(Mono.just(specCanales));
        when(specStoragePort.saveSpec(eq("ideas_planning_q3_2026.md"), anyString()))
                .thenReturn(Mono.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt Renderizado para Q3-2026");

        String simulatedLlmRoadmap = """
                # Roadmap de Planeación — Q3-2026
                
                ## 1. Resumen Ejecutivo
                Plan de entregas estratégicas de Canales para Q3-2026.
                
                ## 2. Tabla de Roadmap por Sprints
                | Sprint | Tipo | Título | Story Points | Frente | Dependencias | Descripción / Criterio de Entrega |
                |:------:|:----:|:-------|:------------:|:-------|:-------------|:----------------------------------|
                | 1 | HU | Login Biométrico Móvil | 5 | Canales | Ninguna | Construcción y certificación en QA |
                | 1 | HA | Habilitación Pipeline HyMS | 3 | Canales | Ninguna | Runbook de despliegue y pase formal HyMS |
                | 2 | HU | Consulta de Saldos | 8 | Canales | Login Biométrico Móvil | Pruebas integradas en QA |
                
                ## 3. Especificaciones y Entregables por Frente
                - `ideas_planning_q3_2026.md`
                - `frente_canales.md`
                """;
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.just(simulatedLlmRoadmap));

        // Construir la cadena de casos de uso y handlers reales del dominio
        ProgramPlanningUseCase programPlanningUseCase = new ProgramPlanningUseCase(
                promptTemplatePort, specStoragePort, chatGateway);
        ProgramPlanningFlowHandler planningHandler = new ProgramPlanningFlowHandler(
                programPlanningUseCase);

        List<ChatFlowHandler> handlers = new ArrayList<>();
        handlers.add(planningHandler);
        for (AgentIntent intent : AgentIntent.values()) {
            if (intent != AgentIntent.PROGRAM_PLANNING) {
                ChatFlowHandler dummy = mock(ChatFlowHandler.class);
                when(dummy.supports()).thenReturn(intent);
                handlers.add(dummy);
            }
        }
        ChatFlowDispatcher dispatcher = new ChatFlowDispatcher(handlers);
        AgentChatUseCase realAgentChatUseCase = new AgentChatUseCase(
                agentResponseGateway, taskStoreGateway, new IntentResolver(), dispatcher);

        // Delegar la ejecución del mock inyectado al caso de uso real
        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenAnswer(invocation -> realAgentChatUseCase.chatAndRespond(
                        invocation.getArgument(0)));

        Map<String, Object> jsonRpcRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "req-plan-q3",
                "method", "message/send",
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts",
                                List.of(Map.of("text", "/plan Q3-2026 6 sprints 34 sp [Canales]")),
                                "messageId", "msg-plan-001",
                                "contextId", "ctx-plan-q3")));

        // WHEN & THEN: Enviar petición JSON-RPC al entry-point y verificar respuesta E2E
        webTestClient.post()
                .uri(JSON_RPC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonRpcRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("req-plan-q3")
                .jsonPath("$.result.task.status.state").isEqualTo("completed")
                .jsonPath("$.result.message.parts[0].text").value(text -> {
                    String body = String.valueOf(text);
                    assertThat(body)
                            .contains("# Roadmap de Planeación — Q3-2026")
                            .contains("## Resumen Ejecutivo")
                            .contains("Plan de entregas estratégicas de Canales para Q3-2026.")
                            .contains("## Métricas del Plan")
                            .contains("Total Story Points:** 16 pts")
                            .contains("HUs hasta QA")
                            .contains("HAs HyMS")
                            .contains("ideas_planning_q3_2026.md")
                            .contains("## Tabla de Asignaciones por Sprint")
                            .contains("Login Biométrico Móvil")
                            .contains("Habilitación Pipeline HyMS")
                            .contains("Consulta de Saldos");
                });

        // THEN: Verificar persistencia documental del artefacto maestro de planeación
        verify(specStoragePort).saveSpec(eq("ideas_planning_q3_2026.md"), anyString());
    }
}
