package co.com.bancolombia.model.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cubre las factorías de {@link Part}. La especificación A2A exige que un {@code Part} lleve una
 * sola clase de contenido: estas pruebas fijan que cada factoría deja el resto de campos vacío.
 */
@DisplayName("Part - Factorías de contenido")
class PartTest {

    @Test
    @DisplayName("GIVEN un texto WHEN se crea la parte THEN solo lleva texto")
    void givenText_whenOfText_thenOnlyTextIsSet() {
        Part part = Part.ofText("hola");

        assertThat(part.getText()).isEqualTo("hola");
        assertThat(part.getData()).isNull();
        assertThat(part.getUrl()).isNull();
        assertThat(part.getMediaType()).isNull();
        assertThat(part.getFilename()).isNull();
        assertThat(part.getMetadata()).isNull();
    }

    @Test
    @DisplayName("GIVEN datos estructurados WHEN se crea la parte THEN solo lleva datos")
    void givenData_whenOfData_thenOnlyDataIsSet() {
        Part part = Part.ofData(Map.of("documentType", "CC"));

        assertThat(part.getData()).containsEntry("documentType", "CC");
        assertThat(part.getText()).isNull();
        assertThat(part.getUrl()).isNull();
    }

    @Test
    @DisplayName("GIVEN una URL y su tipo WHEN se crea la parte THEN lleva ambos y nada más")
    void givenUrl_whenOfUrl_thenUrlAndMediaTypeAreSet() {
        Part part = Part.ofUrl("https://example.com/f.pdf", "application/pdf");

        assertThat(part.getUrl()).isEqualTo("https://example.com/f.pdf");
        assertThat(part.getMediaType()).isEqualTo("application/pdf");
        assertThat(part.getText()).isNull();
        assertThat(part.getData()).isNull();
    }

    @Test
    @DisplayName("GIVEN una parte WHEN se reconstruye con toBuilder THEN conserva lo no modificado")
    void givenPart_whenToBuilder_thenKeepsUntouchedFields() {
        Part original = Part.builder()
                .text("hola")
                .filename("f.txt")
                .metadata(Map.of("m", 1))
                .build();

        Part copy = original.toBuilder().text("adiós").build();

        assertThat(copy.getText()).isEqualTo("adiós");
        assertThat(copy.getFilename()).isEqualTo("f.txt");
        assertThat(copy.getMetadata()).containsEntry("m", 1);
    }

    @Test
    @DisplayName("GIVEN dos partes con el mismo contenido THEN son iguales")
    void givenSameContent_whenCompared_thenAreEqual() {
        assertThat(Part.ofText("hola"))
                .isEqualTo(Part.ofText("hola"))
                .hasSameHashCodeAs(Part.ofText("hola"));
    }
}

