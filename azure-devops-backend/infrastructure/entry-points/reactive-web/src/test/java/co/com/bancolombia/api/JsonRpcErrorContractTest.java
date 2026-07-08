package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.util.List;
import java.util.Map;
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
class JsonRpcErrorContractTest {

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

    @Test
    void shouldReturnParseErrorWhenJsonIsMalformed() {
        String malformedJson = "{";

        webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(malformedJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.error.code").isEqualTo(-32700)
                .jsonPath("$.error.message").isEqualTo("Parse error");
    }

    @Test
    void shouldReturnInvalidRequestWhenMethodIsMissing() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", "req-100",
                "params", Map.of());

        webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jsonrpc").isEqualTo("2.0")
                .jsonPath("$.id").isEqualTo("req-100")
                .jsonPath("$.error.code").isEqualTo(-32600)
                .jsonPath("$.error.message").value(msg -> assertThat(String.valueOf(msg))
                        .contains("method is required"));
    }

    @Test
    void shouldReturnMethodNotFound() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", "req-101",
                "method", "Unknown",
                "params", Map.of());

        webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo(-32601);
    }

    @Test
    void shouldReturnInvalidParamsWhenMessageIsMissing() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", "req-102",
                "method", "message/send",
                "params", Map.of("unexpected", List.of("x")));

        webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo(-32602)
                .jsonPath("$.error.message").value(msg -> assertThat(String.valueOf(msg))
                        .contains("message is required"));
    }

    @Test
    void shouldReturnInternalErrorWhenUseCaseFails() {
        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", "req-103",
                "method", "message/send",
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", "hola")))));

        webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo(-32603)
                .jsonPath("$.error.message").value(msg -> assertThat(String.valueOf(msg))
                        .contains("Internal error"));
    }
}

