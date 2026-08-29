package co.com.bancolombia.model.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cubre el comportamiento propio de {@link Message}: las factorías y la extracción de texto, que
 * es el punto por el que el caso de uso lee lo que escribió el usuario.
 */
@DisplayName("Message - Factorías y extracción de texto")
class MessageTest {

    @Test
    @DisplayName("GIVEN un texto WHEN se crea un mensaje de usuario THEN el rol es 'user' en minúscula")
    void givenText_whenUserText_thenRoleIsLowercaseUser() {
        Message message = Message.userText("hola");

        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getParts()).hasSize(1);
        assertThat(message.getParts().getFirst().getText()).isEqualTo("hola");
    }

    @Test
    @DisplayName("GIVEN un texto WHEN se crea un mensaje de agente THEN el rol es 'agent' en minúscula")
    void givenText_whenAgentText_thenRoleIsLowercaseAgent() {
        Message message = Message.agentText("respuesta");

        assertThat(message.getRole()).isEqualTo("agent");
        assertThat(message.getParts().getFirst().getText()).isEqualTo("respuesta");
    }

    @Test
    @DisplayName("GIVEN un mensaje con texto WHEN se extrae THEN devuelve el primer texto")
    void givenTextParts_whenExtractText_thenReturnsFirst() {
        Message message = Message.builder()
                .parts(List.of(Part.ofText("primero"), Part.ofText("segundo")))
                .build();

        assertThat(message.extractText()).isEqualTo("primero");
    }

    @Test
    @DisplayName("GIVEN partes sin texto antes del texto WHEN se extrae THEN las omite")
    void givenNonTextPartsFirst_whenExtractText_thenSkipsThem() {
        Message message = Message.builder()
                .parts(List.of(Part.ofData(java.util.Map.of("k", "v")), Part.ofText("el texto")))
                .build();

        assertThat(message.extractText()).isEqualTo("el texto");
    }

    @Test
    @DisplayName("GIVEN partes nulas WHEN se extrae THEN devuelve cadena vacía y no falla")
    void givenNullParts_whenExtractText_thenReturnsEmpty() {
        assertThat(new Message().extractText()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN una lista vacía de partes WHEN se extrae THEN devuelve cadena vacía")
    void givenEmptyParts_whenExtractText_thenReturnsEmpty() {
        Message message = Message.builder().parts(List.of()).build();

        assertThat(message.extractText()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN partes sin ningún texto WHEN se extrae THEN devuelve cadena vacía")
    void givenPartsWithoutText_whenExtractText_thenReturnsEmpty() {
        Message message = Message.builder()
                .parts(List.of(Part.ofUrl("https://x/y.pdf", "application/pdf")))
                .build();

        assertThat(message.extractText()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN una parte nula en la lista WHEN se extrae THEN no lanza excepción")
    void givenNullPartInList_whenExtractText_thenDoesNotThrow() {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.ofText("hola"));
        Message message = Message.builder().parts(parts).build();

        assertThat(message.extractText()).isEqualTo("hola");
    }

    @Test
    @DisplayName("GIVEN un mensaje WHEN se reconstruye con toBuilder THEN conserva el resto de campos")
    void givenMessage_whenToBuilder_thenKeepsOtherFields() {
        Message original = Message.builder()
                .role("user")
                .messageId("msg-1")
                .contextId("ctx-1")
                .referenceTaskIds(List.of("task-1"))
                .parts(List.of(Part.ofText("hola")))
                .build();

        Message copy = original.toBuilder().role("agent").build();

        assertThat(copy.getRole()).isEqualTo("agent");
        assertThat(copy.getMessageId()).isEqualTo("msg-1");
        assertThat(copy.getContextId()).isEqualTo("ctx-1");
        assertThat(copy.getReferenceTaskIds()).containsExactly("task-1");
        assertThat(copy.getParts()).isEqualTo(original.getParts());
    }
}

