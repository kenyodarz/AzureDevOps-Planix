package co.com.bancolombia.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.api.JsonRpcRequest;
import co.com.bancolombia.api.JsonRpcResponse;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.server.ServerWebInputException;

/**
 * Verifica la forma del sobre JSON-RPC 2.0 y los códigos de error del contrato público.
 */
class JsonRpcResponseFactoryTest {

    private static Task completedTask() {
        return Task.builder()
                .id("task-1")
                .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                .build();
    }

    @Test
    @DisplayName("GIVEN una respuesta completa WHEN se arma el éxito THEN incluye tarea y mensaje")
    void givenFullResponse_whenSuccess_thenIncludesTaskAndMessage() {
        SendMessageResponse response = SendMessageResponse.builder()
                .task(completedTask())
                .message(Message.builder().role("agent").parts(List.of(Part.ofText("ok"))).build())
                .build();

        JsonRpcResponse envelope = JsonRpcResponseFactory.success("req-1", response);

        assertThat(envelope.getJsonrpc()).isEqualTo("2.0");
        assertThat(envelope.getId()).isEqualTo("req-1");
        assertThat(envelope.getError()).isNull();
        assertThat(envelope.getResult())
                .containsKeys("task", "message");
    }

    @Test
    @DisplayName("GIVEN una respuesta nula WHEN se arma el éxito THEN el resultado queda vacío")
    void givenNullResponse_whenSuccess_thenResultIsEmpty() {
        JsonRpcResponse envelope = JsonRpcResponseFactory.success("req-2", null);

        assertThat(envelope.getResult()).isEmpty();
        assertThat(envelope.getError()).isNull();
    }

    @Test
    @DisplayName("GIVEN una respuesta sin tarea WHEN se arma el éxito THEN solo lleva el mensaje")
    void givenResponseWithoutTask_whenSuccess_thenOnlyMessage() {
        SendMessageResponse response = SendMessageResponse.builder()
                .message(Message.builder().role("agent").parts(List.of(Part.ofText("ok"))).build())
                .build();

        JsonRpcResponse envelope = JsonRpcResponseFactory.success("req-3", response);

        assertThat(envelope.getResult()).containsOnlyKeys("message");
    }

    @Test
    @DisplayName("GIVEN una tarea WHEN se arma el éxito con tarea THEN el resultado la contiene")
    void givenTask_whenSuccessWithTask_thenResultContainsIt() {
        JsonRpcResponse envelope =
                JsonRpcResponseFactory.successWithTask("req-4", completedTask());

        assertThat(envelope.getJsonrpc()).isEqualTo("2.0");
        assertThat(envelope.getResult()).containsOnlyKeys("task");
        assertThat(envelope.getResult()).containsEntry("task", completedTask());
    }

    @Test
    @DisplayName("GIVEN un código y mensaje WHEN se arma el error THEN el sobre lo refleja")
    void givenCodeAndMessage_whenError_thenEnvelopeIsBuilt() {
        JsonRpcResponse envelope = JsonRpcResponseFactory.error("req-5",
                JsonRpcResponseFactory.METHOD_NOT_FOUND, "Method not found: x");

        assertThat(envelope.getJsonrpc()).isEqualTo("2.0");
        assertThat(envelope.getId()).isEqualTo("req-5");
        assertThat(envelope.getResult()).isNull();
        assertThat(envelope.getError().getCode()).isEqualTo(-32601);
        assertThat(envelope.getError().getMessage()).isEqualTo("Method not found: x");
    }

    @Test
    @DisplayName("GIVEN los códigos del contrato THEN conservan sus valores históricos")
    void givenContractCodes_thenValuesAreStable() {
        assertThat(JsonRpcResponseFactory.PARSE_ERROR).isEqualTo(-32700);
        assertThat(JsonRpcResponseFactory.INVALID_REQUEST).isEqualTo(-32600);
        assertThat(JsonRpcResponseFactory.METHOD_NOT_FOUND).isEqualTo(-32601);
        assertThat(JsonRpcResponseFactory.INVALID_PARAMS).isEqualTo(-32602);
        assertThat(JsonRpcResponseFactory.INTERNAL_ERROR).isEqualTo(-32603);
        assertThat(JsonRpcResponseFactory.TASK_NOT_FOUND).isEqualTo(-32004);
    }

    @Test
    @DisplayName("GIVEN un fallo de deserialización WHEN se mapea el sobre THEN es Parse error")
    void givenDecodingException_whenFromEnvelopeError_thenParseError() {
        JsonRpcResponse envelope = JsonRpcResponseFactory
                .fromEnvelopeError(new DecodingException("roto"));

        assertThat(envelope.getId()).isNull();
        assertThat(envelope.getError().getCode()).isEqualTo(-32700);
        assertThat(envelope.getError().getMessage()).isEqualTo("Parse error");
    }

    @Test
    @DisplayName("GIVEN un fallo de entrada anidado WHEN se mapea el sobre THEN es Parse error")
    void givenNestedInputException_whenFromEnvelopeError_thenParseError() {
        Throwable nested = new IllegalStateException("wrapper",
                new ServerWebInputException("entrada inválida"));

        JsonRpcResponse envelope = JsonRpcResponseFactory.fromEnvelopeError(nested);

        assertThat(envelope.getError().getCode()).isEqualTo(-32700);
    }

    @Test
    @DisplayName("GIVEN un fallo genérico WHEN se mapea el sobre THEN es Invalid Request")
    void givenGenericError_whenFromEnvelopeError_thenInvalidRequest() {
        JsonRpcResponse envelope = JsonRpcResponseFactory
                .fromEnvelopeError(new IllegalArgumentException("JSON-RPC body is required"));

        assertThat(envelope.getError().getCode()).isEqualTo(-32600);
        assertThat(envelope.getError().getMessage())
                .isEqualTo("Invalid Request: JSON-RPC body is required");
    }

    @Test
    @DisplayName("GIVEN un fallo nulo WHEN se mapea el sobre THEN el motivo es desconocido")
    void givenNullError_whenFromEnvelopeError_thenUnknownReason() {
        JsonRpcResponse envelope = JsonRpcResponseFactory.fromEnvelopeError(null);

        assertThat(envelope.getError().getCode()).isEqualTo(-32600);
        assertThat(envelope.getError().getMessage()).isEqualTo("Invalid Request: unknown");
    }

    @Test
    @DisplayName("GIVEN una petición nula WHEN se pide el id THEN se devuelve nulo")
    void givenNullRequest_whenSafeId_thenReturnsNull() {
        assertThat(JsonRpcResponseFactory.safeId(null)).isNull();
    }

    @Test
    @DisplayName("GIVEN una petición WHEN se pide el id THEN se devuelve el suyo")
    void givenRequest_whenSafeId_thenReturnsItsId() {
        JsonRpcRequest request = JsonRpcRequest.builder().id("req-6").build();

        assertThat(JsonRpcResponseFactory.safeId(request)).isEqualTo("req-6");
    }
}

