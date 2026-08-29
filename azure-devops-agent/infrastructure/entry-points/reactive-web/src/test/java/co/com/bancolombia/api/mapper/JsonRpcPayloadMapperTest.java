package co.com.bancolombia.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.a2a.SendMessageRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caracteriza el mapeo de payloads JSON-RPC hacia el modelo A2A, con especial atención a los
 * casos degenerados: la tolerancia actual a nulos y tipos inesperados es parte del contrato
 * público verificado por {@code JsonRpcErrorContractTest}.
 */
class JsonRpcPayloadMapperTest {

    private static Map<String, Object> minimalMessage() {
        Map<String, Object> params = new HashMap<>();
        params.put("message", Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "hola"))));
        return params;
    }

    @Test
    @DisplayName("GIVEN un payload completo WHEN se mapea THEN se traducen todos los campos")
    void givenFullPayload_whenToSendMessageRequest_thenAllFieldsAreMapped() {
        Map<String, Object> params = Map.of(
                "message", Map.of(
                        "role", "user",
                        "messageId", "msg-1",
                        "contextId", "ctx-1",
                        "referenceTaskIds", List.of("task-1", "task-2"),
                        "parts", List.of(Map.of(
                                "text", "hola",
                                "data", Map.of("k", "v"),
                                "url", "https://example.com/f.pdf",
                                "mediaType", "application/pdf",
                                "filename", "f.pdf",
                                "metadata", Map.of("m", 1)))),
                "configuration", Map.of(
                        "acceptedOutputModes", List.of("text"),
                        "historyLength", 5,
                        "blocking", true));

        SendMessageRequest request = JsonRpcPayloadMapper.toSendMessageRequest(params);

        assertThat(request.getMessage().getRole()).isEqualTo("user");
        assertThat(request.getMessage().getMessageId()).isEqualTo("msg-1");
        assertThat(request.getMessage().getContextId()).isEqualTo("ctx-1");
        assertThat(request.getMessage().getReferenceTaskIds())
                .containsExactly("task-1", "task-2");
        assertThat(request.getMessage().getParts()).hasSize(1);
        assertThat(request.getMessage().getParts().getFirst().getText()).isEqualTo("hola");
        assertThat(request.getMessage().getParts().getFirst().getData())
                .containsEntry("k", "v");
        assertThat(request.getMessage().getParts().getFirst().getUrl())
                .isEqualTo("https://example.com/f.pdf");
        assertThat(request.getMessage().getParts().getFirst().getMediaType())
                .isEqualTo("application/pdf");
        assertThat(request.getMessage().getParts().getFirst().getFilename()).isEqualTo("f.pdf");
        assertThat(request.getMessage().getParts().getFirst().getMetadata())
                .containsEntry("m", 1);
        assertThat(request.getConfiguration().getAcceptedOutputModes()).containsExactly("text");
        assertThat(request.getConfiguration().getHistoryLength()).isEqualTo(5);
        assertThat(request.getConfiguration().getBlocking()).isTrue();
    }

    @Test
    @DisplayName("GIVEN un payload mínimo WHEN se mapea THEN los opcionales quedan vacíos o nulos")
    void givenMinimalPayload_whenToSendMessageRequest_thenOptionalsAreEmpty() {
        SendMessageRequest request = JsonRpcPayloadMapper.toSendMessageRequest(minimalMessage());

        assertThat(request.getMessage().getMessageId()).isNull();
        assertThat(request.getMessage().getContextId()).isNull();
        assertThat(request.getMessage().getReferenceTaskIds()).isEmpty();
        assertThat(request.getMessage().getParts().getFirst().getData()).isEmpty();
        assertThat(request.getMessage().getParts().getFirst().getMetadata()).isEmpty();
        assertThat(request.getConfiguration()).isNull();
    }

    @Test
    @DisplayName("GIVEN params nulo WHEN se mapea THEN se exige que params sea un objeto")
    void givenNullParams_whenToSendMessageRequest_thenFails() {
        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("params must be a JSON object");
    }

    @Test
    @DisplayName("GIVEN params que no es objeto WHEN se mapea THEN se exige que params sea objeto")
    void givenNonObjectParams_whenToSendMessageRequest_thenFails() {
        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(List.of("x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("params must be a JSON object");
    }

    @Test
    @DisplayName("GIVEN params vacío WHEN se mapea THEN se exige el mensaje")
    void givenEmptyParams_whenToSendMessageRequest_thenMessageIsRequired() {
        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message is required and must be an object");
    }

    @Test
    @DisplayName("GIVEN claves no textuales WHEN se mapea THEN se descartan sin romper")
    void givenNonStringKeys_whenToSendMessageRequest_thenTheyAreIgnored() {
        Map<Object, Object> rawParams = new HashMap<>();
        rawParams.put(1, "ignorado");
        rawParams.put("message", Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "hola"))));

        SendMessageRequest request = JsonRpcPayloadMapper.toSendMessageRequest(rawParams);

        assertThat(request.getMessage().getRole()).isEqualTo("user");
    }

    @Test
    @DisplayName("GIVEN parts ausente WHEN se mapea THEN se exige un arreglo de partes")
    void givenMissingParts_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = Map.of("message", Map.of("role", "user"));

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message.parts is required and must be an array");
    }

    @Test
    @DisplayName("GIVEN una parte que no es objeto WHEN se mapea THEN falla")
    void givenNonObjectPart_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = Map.of("message", Map.of(
                "role", "user",
                "parts", List.of("texto suelto")));

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("each message part must be an object");
    }

    @Test
    @DisplayName("GIVEN una lista heterogénea con nulos WHEN se mapea THEN los nulos se conservan")
    void givenHeterogeneousList_whenToSendMessageRequest_thenNullsArePreserved() {
        List<Object> referenceTaskIds = new ArrayList<>();
        referenceTaskIds.add("task-1");
        referenceTaskIds.add(null);
        referenceTaskIds.add(42);
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("referenceTaskIds", referenceTaskIds);
        message.put("parts", List.of(Map.of("text", "hola")));

        SendMessageRequest request =
                JsonRpcPayloadMapper.toSendMessageRequest(Map.of("message", message));

        assertThat(request.getMessage().getReferenceTaskIds())
                .containsExactly("task-1", null, "42");
    }

    @Test
    @DisplayName("GIVEN referenceTaskIds que no es lista WHEN se mapea THEN falla")
    void givenNonListReferenceTaskIds_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = Map.of("message", Map.of(
                "role", "user",
                "referenceTaskIds", "task-1",
                "parts", List.of(Map.of("text", "hola"))));

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expected array value");
    }

    @Test
    @DisplayName("GIVEN data que no es objeto WHEN se mapea THEN falla")
    void givenNonObjectData_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = Map.of("message", Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "hola", "data", "no-es-objeto"))));

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expected object for map field");
    }

    @Test
    @DisplayName("GIVEN configuration que no es objeto WHEN se mapea THEN falla")
    void givenNonObjectConfiguration_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = new HashMap<>(minimalMessage());
        params.put("configuration", "no-es-objeto");

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("configuration must be an object when present");
    }

    @Test
    @DisplayName("GIVEN configuration vacía WHEN se mapea THEN blocking es falso por defecto")
    void givenEmptyConfiguration_whenToSendMessageRequest_thenBlockingDefaultsToFalse() {
        Map<String, Object> params = new HashMap<>(minimalMessage());
        params.put("configuration", Map.of());

        SendMessageRequest request = JsonRpcPayloadMapper.toSendMessageRequest(params);

        assertThat(request.getConfiguration().getBlocking()).isFalse();
        assertThat(request.getConfiguration().getHistoryLength()).isNull();
        assertThat(request.getConfiguration().getAcceptedOutputModes()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN historyLength decimal WHEN se mapea THEN se trunca a entero")
    void givenDecimalHistoryLength_whenToSendMessageRequest_thenIsTruncated() {
        Map<String, Object> params = new HashMap<>(minimalMessage());
        params.put("configuration", Map.of("historyLength", 5.9d));

        SendMessageRequest request = JsonRpcPayloadMapper.toSendMessageRequest(params);

        assertThat(request.getConfiguration().getHistoryLength()).isEqualTo(5);
    }

    @Test
    @DisplayName("GIVEN historyLength no numérico WHEN se mapea THEN falla")
    void givenNonNumericHistoryLength_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = new HashMap<>(minimalMessage());
        params.put("configuration", Map.of("historyLength", "cinco"));

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expected integer value");
    }

    @Test
    @DisplayName("GIVEN blocking no booleano WHEN se mapea THEN falla")
    void givenNonBooleanBlocking_whenToSendMessageRequest_thenFails() {
        Map<String, Object> params = new HashMap<>(minimalMessage());
        params.put("configuration", Map.of("blocking", "true"));

        assertThatThrownBy(() -> JsonRpcPayloadMapper.toSendMessageRequest(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expected boolean value");
    }

    @Test
    @DisplayName("GIVEN un taskId válido WHEN se extrae THEN se devuelve")
    void givenValidTaskId_whenToTaskId_thenReturnsIt() {
        assertThat(JsonRpcPayloadMapper.toTaskId(Map.of("taskId", "task-1")))
                .isEqualTo("task-1");
    }

    @Test
    @DisplayName("GIVEN params nulo WHEN se extrae el taskId THEN falla")
    void givenNullParams_whenToTaskId_thenFails() {
        assertThatThrownBy(() -> JsonRpcPayloadMapper.toTaskId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("params must be a JSON object");
    }

    @Test
    @DisplayName("GIVEN taskId ausente WHEN se extrae THEN se exige")
    void givenMissingTaskId_whenToTaskId_thenFails() {
        assertThatThrownBy(() -> JsonRpcPayloadMapper.toTaskId(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId is required");
    }

    @Test
    @DisplayName("GIVEN taskId en blanco WHEN se extrae THEN se exige")
    void givenBlankTaskId_whenToTaskId_thenFails() {
        assertThatThrownBy(() -> JsonRpcPayloadMapper.toTaskId(Map.of("taskId", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId is required");
    }
}

