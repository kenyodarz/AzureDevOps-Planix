package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.mapper.JsonRpcResponseFactory;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Verifica el enrutamiento de métodos JSON-RPC y la validación previa del sobre, sin levantar
 * el contexto web.
 */
@ExtendWith(MockitoExtension.class)
class JsonRpcDispatcherTest {

    @Mock
    private AgentChatUseCase agentChatUseCase;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    private JsonRpcDispatcher dispatcher;

    private static Object validParams() {
        return Map.of("message", Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "hola"))));
    }

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher(agentChatUseCase, taskStoreGateway);
    }

    @Test
    @DisplayName("GIVEN una petición nula WHEN se despacha THEN es una petición inválida")
    void givenNullRequest_whenDispatch_thenInvalidRequest() {
        JsonRpcResponse response = dispatcher.dispatch(null).block();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getError().getCode())
                .isEqualTo(JsonRpcResponseFactory.INVALID_REQUEST);
    }

    @Test
    @DisplayName("GIVEN una versión distinta de 2.0 WHEN se despacha THEN es una petición inválida")
    void givenWrongVersion_whenDispatch_thenInvalidRequest() {
        JsonRpcRequest request = JsonRpcRequest.builder()
                .jsonrpc("1.0").id("req-1").method("message/send").build();

        JsonRpcResponse response = dispatcher.dispatch(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("req-1");
        assertThat(response.getError().getMessage()).contains("jsonrpc must be '2.0'");
    }

    @Test
    @DisplayName("GIVEN un método en blanco WHEN se despacha THEN se exige el método")
    void givenBlankMethod_whenDispatch_thenMethodIsRequired() {
        JsonRpcRequest request = JsonRpcRequest.builder()
                .jsonrpc("2.0").id("req-2").method("   ").build();

        JsonRpcResponse response = dispatcher.dispatch(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getError().getMessage()).contains("method is required");
    }

    @Test
    @DisplayName("GIVEN message/send sin params WHEN se despacha THEN los params son inválidos")
    void givenSendMessageWithoutParams_whenDispatch_thenInvalidParams() {
        JsonRpcRequest request = JsonRpcRequest.builder()
                .jsonrpc("2.0").id("req-3").method("message/send").build();

        JsonRpcResponse response = dispatcher.dispatch(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getError().getCode())
                .isEqualTo(JsonRpcResponseFactory.INVALID_PARAMS);
        assertThat(response.getError().getMessage()).contains("params is required");
    }

    @Test
    @DisplayName("GIVEN una respuesta sin tarea WHEN se despacha THEN no se persiste nada")
    void givenResponseWithoutTask_whenDispatch_thenNothingIsPersisted() {
        when(agentChatUseCase.chatAndRespond(any(SendMessageRequest.class)))
                .thenReturn(Mono.just(SendMessageResponse.builder()
                        .message(Message.builder().role("agent")
                                .parts(List.of(Part.ofText("ok"))).build())
                        .build()));
        JsonRpcRequest request = JsonRpcRequest.builder()
                .jsonrpc("2.0").id("req-4").method("message/send").params(validParams()).build();

        JsonRpcResponse response = dispatcher.dispatch(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getError()).isNull();
        assertThat(response.getResult()).containsOnlyKeys("message");
        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN tasks/get sin taskId WHEN se despacha THEN los params son inválidos")
    void givenGetTaskWithoutTaskId_whenDispatch_thenInvalidParams() {
        JsonRpcRequest request = JsonRpcRequest.builder()
                .jsonrpc("2.0").id("req-5").method("tasks/get").params(Map.of()).build();

        JsonRpcResponse response = dispatcher.dispatch(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getError().getCode())
                .isEqualTo(JsonRpcResponseFactory.INVALID_PARAMS);
        assertThat(response.getError().getMessage()).contains("taskId is required");
    }

    @Test
    @DisplayName("GIVEN tasks/cancel sin taskId WHEN se despacha THEN los params son inválidos")
    void givenCancelTaskWithoutTaskId_whenDispatch_thenInvalidParams() {
        JsonRpcRequest request = JsonRpcRequest.builder()
                .jsonrpc("2.0").id("req-6").method("tasks/cancel").params("no-es-objeto").build();

        JsonRpcResponse response = dispatcher.dispatch(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getError().getCode())
                .isEqualTo(JsonRpcResponseFactory.INVALID_PARAMS);
        assertThat(response.getError().getMessage()).contains("params must be a JSON object");
    }
}

