package co.com.bancolombia.model.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias para el modelo inmutable {@link SpecDocument} y {@link SpecNotFoundException}.
 */
@DisplayName("SpecDocument - Modelo de documento de especificación")
class SpecDocumentTest {

    private static final String SPEC_NAME = "ideas_planning_q3.md";
    private static final String SPEC_CONTENT = "# Plan Q3 2026\nContenido de especificación...";
    private static final String SPEC_PATH = "specs/ideas_planning_q3.md";

    @Test
    @DisplayName("Crea SpecDocument válido con todos los atributos informados")
    void givenValidData_whenCreateSpecDocument_thenPreservesAllAttributes() {
        // WHEN
        SpecDocument document = new SpecDocument(SPEC_NAME, SPEC_CONTENT, SPEC_PATH);

        // THEN
        assertThat(document.name()).isEqualTo(SPEC_NAME);
        assertThat(document.content()).isEqualTo(SPEC_CONTENT);
        assertThat(document.path()).isEqualTo(SPEC_PATH);
    }

    @Test
    @DisplayName("Normaliza contenido nulo a cadena vacía")
    void givenNullContent_whenCreateSpecDocument_thenContentDefaultsToEmptyString() {
        // WHEN
        SpecDocument document = new SpecDocument(SPEC_NAME, null, SPEC_PATH);

        // THEN
        assertThat(document.name()).isEqualTo(SPEC_NAME);
        assertThat(document.content()).isEmpty();
        assertThat(document.path()).isEqualTo(SPEC_PATH);
    }

    @ParameterizedTest(name = "nombre inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando el nombre es nulo, vacío o sólo espacios")
    void givenBlankOrNullName_whenCreateSpecDocument_thenThrowsIllegalArgumentException(
            String invalidName) {
        // WHEN / THEN
        assertThatThrownBy(() -> new SpecDocument(invalidName, SPEC_CONTENT, SPEC_PATH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre del spec no puede ser nulo ni vacío");
    }

    @Test
    @DisplayName("Comprueba igualdad estructural y hashCode en records idénticos")
    void givenSameValues_whenCheckEquality_thenAreEqualAndHaveSameHashCode() {
        // GIVEN
        SpecDocument doc1 = new SpecDocument(SPEC_NAME, SPEC_CONTENT, SPEC_PATH);
        SpecDocument doc2 = new SpecDocument(SPEC_NAME, SPEC_CONTENT, SPEC_PATH);

        // THEN
        assertThat(doc1)
                .isEqualTo(doc2)
                .hasSameHashCodeAs(doc2);
        assertThat(doc1.toString()).contains(SPEC_NAME, SPEC_PATH);
    }

    @Test
    @DisplayName("SpecNotFoundException contiene el nombre del spec y extiende RuntimeException")
    void givenSpecName_whenCreateSpecNotFoundException_thenHasDescriptiveMessage() {
        // WHEN
        SpecNotFoundException exception = new SpecNotFoundException(SPEC_NAME);

        // THEN
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No se encontró el documento de especificación: " + SPEC_NAME);
    }
}
