package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
    private co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase ingestPlanningSpecUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase searchPlanningSpecUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase managePlanningUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase devOpsDashboardUseCase;

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
    void shouldReturnDashboardDataWhenParametersAreProvided() {
        String mockResponse = "{\"metrics\":{},\"items\":[]}";
        when(devOpsDashboardUseCase.getDashboardData("Arkham", "Sprint3")).thenReturn(
                Mono.just(mockResponse));

        webTestClient.get()
                .uri("/api/devops/dashboard?cell=Arkham&sprint=Sprint3")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("metrics");
                });
    }

    @Test
    void shouldReturn400BadRequestWhenParametersAreMissing() {
        webTestClient.get()
                .uri("/api/devops/dashboard")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("La célula y el sprint son parámetros requeridos");
    }
}
