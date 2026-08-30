package co.com.bancolombia.model.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del Value Object que reemplaza al {@code AreaPath} construido por concatenación (D-09).
 */
class TeamNameTest {

    private static final String CELL = "EQU1096 - EXODIA";

    @Test
    @DisplayName("GIVEN un nombre válido WHEN se construye THEN conserva el valor")
    void givenValidName_whenBuilt_thenKeepsValue() {
        // GIVEN / WHEN
        TeamName team = TeamName.of(CELL);

        // THEN
        assertThat(team.value()).isEqualTo(CELL);
        assertThat(team).hasToString(CELL);
    }

    @Test
    @DisplayName("GIVEN un nombre con espacios WHEN se construye THEN los recorta")
    void givenPaddedName_whenBuilt_thenTrims() {
        // GIVEN / WHEN / THEN
        assertThat(TeamName.of("   " + CELL + "\t").value()).isEqualTo(CELL);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("GIVEN un nombre en blanco WHEN se construye THEN falla en el acto")
    void givenBlankName_whenBuilt_thenFailsFast(String raw) {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> TeamName.of(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre de la célula no puede estar vacío");
    }

    @Test
    @DisplayName("GIVEN dos nombres equivalentes WHEN se comparan THEN son iguales")
    void givenEquivalentNames_whenCompared_thenTheyAreEqual() {
        // GIVEN / WHEN / THEN
        assertThat(TeamName.of(CELL)).isEqualTo(TeamName.of("  " + CELL + "  "))
                .hasSameHashCodeAs(TeamName.of(CELL));
    }
}

