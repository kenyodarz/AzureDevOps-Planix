package co.com.bancolombia.api;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, Handler.class})
@TestPropertySource(properties = "a2a.legacy-message-send-enabled=false")
class RouterRestLegacyToggleTest {

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
    void shouldDisableLegacyEndpointWhenFeatureFlagIsFalse() {
        webTestClient.post()
                .uri("/message:send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldKeepJsonRpcEndpointEnabledWhenLegacyIsDisabled() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .task(Task.builder()
                        .id("task-legacy-off")
                        .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                        .build())
                .message(Message.builder()
                        .role("agent")
                        .parts(List.of(Part.ofText("ok")))
                        .build())
                .build();

        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        Map<String, Object> jsonRpcRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "req-legacy-off",
                "method", "message/send",
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", "hola")))));

        webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonRpcRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("req-legacy-off")
                .jsonPath("$.result.task.id").isEqualTo("task-legacy-off");
    }
}

