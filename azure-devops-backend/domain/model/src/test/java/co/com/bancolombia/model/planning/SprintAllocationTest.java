package co.com.bancolombia.model.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SprintAllocation - Asignación inmutable de actividades a sprints")
class SprintAllocationTest {

    @Test
    @DisplayName("Crea SprintAllocation válido con constructor y preserva atributos")
    void givenValidData_whenCreateSprintAllocation_thenPreservesAllAttributes() {
        // WHEN
        SprintAllocation allocation = new SprintAllocation(
                1,
                "INIT-01",
                "Integrar motor de reglas",
                8,
                ActivityType.HU,
                "Canales"
        );

        // THEN
        assertThat(allocation.sprintNumber()).isEqualTo(1);
        assertThat(allocation.initiativeId()).isEqualTo("INIT-01");
        assertThat(allocation.title()).isEqualTo("Integrar motor de reglas");
        assertThat(allocation.storyPoints()).isEqualTo(8);
        assertThat(allocation.type()).isEqualTo(ActivityType.HU);
        assertThat(allocation.front()).isEqualTo("Canales");
    }

    @Test
    @DisplayName("Crea SprintAllocation mediante Builder de Lombok")
    void givenBuilder_whenBuildSprintAllocation_thenInstantiatesCorrectly() {
        // WHEN
        SprintAllocation allocation = SprintAllocation.builder()
                .sprintNumber(2)
                .initiativeId("INIT-02")
                .title("Setup observabilidad OpenTelemetry")
                .storyPoints(5)
                .type(ActivityType.HA)
                .front("Core")
                .build();

        // THEN
        assertThat(allocation.sprintNumber()).isEqualTo(2);
        assertThat(allocation.initiativeId()).isEqualTo("INIT-02");
        assertThat(allocation.title()).isEqualTo("Setup observabilidad OpenTelemetry");
        assertThat(allocation.storyPoints()).isEqualTo(5);
        assertThat(allocation.type()).isEqualTo(ActivityType.HA);
        assertThat(allocation.front()).isEqualTo("Core");
    }

    @Test
    @DisplayName("Normaliza campos nulos opcionales a cadenas vacías y realiza trim")
    void givenNullOptionalFields_whenCreateSprintAllocation_thenNormalizesToEmptyStrings() {
        // WHEN
        SprintAllocation allocation = new SprintAllocation(
                1,
                null,
                "  Título con espacios  ",
                3,
                ActivityType.HU,
                null
        );

        // THEN
        assertThat(allocation.initiativeId()).isEmpty();
        assertThat(allocation.title()).isEqualTo("Título con espacios");
        assertThat(allocation.front()).isEmpty();
    }

    @ParameterizedTest(name = "sprint inválido: {0}")
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Lanza IllegalArgumentException cuando el sprintNumber es menor o igual a cero")
    void givenInvalidSprintNumber_whenCreateSprintAllocation_thenThrowsException(
            int invalidSprint) {
        // WHEN / THEN
        assertThatThrownBy(() -> new SprintAllocation(
                invalidSprint,
                "INIT-01",
                "Actividad",
                5,
                ActivityType.HU,
                "Frente"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El número de sprint debe ser mayor a cero");
    }

    @ParameterizedTest(name = "storyPoints inválidos: {0}")
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("Lanza IllegalArgumentException cuando storyPoints es menor o igual a cero")
    void givenInvalidStoryPoints_whenCreateSprintAllocation_thenThrowsException(int invalidSp) {
        // WHEN / THEN
        assertThatThrownBy(() -> new SprintAllocation(
                1,
                "INIT-01",
                "Actividad",
                invalidSp,
                ActivityType.HU,
                "Frente"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Los story points deben ser mayores a cero");
    }

    @ParameterizedTest(name = "título inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando el título es nulo o blanco")
    void givenBlankTitle_whenCreateSprintAllocation_thenThrowsException(String invalidTitle) {
        // WHEN / THEN
        assertThatThrownBy(() -> new SprintAllocation(
                1,
                "INIT-01",
                invalidTitle,
                5,
                ActivityType.HU,
                "Frente"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El título de la actividad es obligatorio");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException cuando el tipo de actividad es nulo")
    void givenNullType_whenCreateSprintAllocation_thenThrowsException() {
        // WHEN / THEN
        assertThatThrownBy(() -> new SprintAllocation(
                1,
                "INIT-01",
                "Actividad",
                5,
                null,
                "Frente"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tipo de actividad es obligatorio");
    }

    @Test
    @DisplayName("Comprueba igualdad estructural y hashCode en asignaciones idénticas")
    void givenSameValues_whenCheckEquality_thenAreEqual() {
        // GIVEN
        SprintAllocation a1 = new SprintAllocation(1, "INIT-01", "Actividad", 5, ActivityType.HU,
                "Frente");
        SprintAllocation a2 = new SprintAllocation(1, "INIT-01", "Actividad", 5, ActivityType.HU,
                "Frente");

        // THEN
        assertThat(a1).isEqualTo(a2).hasSameHashCodeAs(a2);
    }
}
