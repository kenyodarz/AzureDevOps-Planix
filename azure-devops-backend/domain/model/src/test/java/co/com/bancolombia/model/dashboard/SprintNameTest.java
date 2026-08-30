package co.com.bancolombia.model.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del Value Object que reemplaza al {@code IterationPath} construido por concatenación.
 *
 * <p>La prueba que más importa es la que <b>no</b> está: ya no existe ninguna que verifique un
 * año. Ese era el fallo (D-19) y se cerró quitando el cálculo, no afinándolo.
 */
class SprintNameTest {

    @Test
    @DisplayName("GIVEN un sprint válido WHEN se construye THEN conserva el valor")
    void givenValidSprint_whenBuilt_thenKeepsValue() {
        // GIVEN / WHEN
        SprintName sprint = SprintName.of("Sprint 247");

        // THEN
        assertThat(sprint.value()).isEqualTo("Sprint 247");
        assertThat(sprint).hasToString("Sprint 247");
    }

    @Test
    @DisplayName("GIVEN un sprint con espacios WHEN se construye THEN los recorta")
    void givenPaddedSprint_whenBuilt_thenTrims() {
        // GIVEN / WHEN / THEN
        assertThat(SprintName.of("  Sprint 247  ").value()).isEqualTo("Sprint 247");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("GIVEN un sprint en blanco WHEN se construye THEN falla en el acto")
    void givenBlankSprint_whenBuilt_thenFailsFast(String raw) {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> SprintName.of(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre del sprint no puede estar vacío");
    }

    @ParameterizedTest
    @CsvSource({
            "Sprint 247, true",
            "Sprint 1, true",
            "'  Sprint 42  ', true",
            "Iteracion Beta, false",
            "Sprint, false",
            "Sprint 247 - Cierre, false",
            "sprint 247, false"
    })
    @DisplayName("GIVEN distintos nombres WHEN isNumbered THEN reconoce la convención 'Sprint N'")
    void givenSeveralNames_whenIsNumbered_thenRecognizesConvention(String raw, boolean expected) {
        // GIVEN / WHEN / THEN
        assertThat(SprintName.of(raw).isNumbered()).isEqualTo(expected);
    }

    @Test
    @DisplayName("GIVEN dos sprints equivalentes WHEN se comparan THEN son iguales")
    void givenEquivalentSprints_whenCompared_thenTheyAreEqual() {
        // GIVEN / WHEN / THEN
        assertThat(SprintName.of("Sprint 247")).isEqualTo(SprintName.of(" Sprint 247 "))
                .hasSameHashCodeAs(SprintName.of("Sprint 247"));
    }
}

