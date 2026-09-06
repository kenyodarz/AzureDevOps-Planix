package co.com.bancolombia.model.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ProgramPlanRequest - Solicitud de planeación trimestral macro")
class ProgramPlanRequestTest {

    private static final String QUARTER = "Q3-2026";
    private static final String OBJECTIVES = "Modernización Cloud y migración de microservicios";
    private static final List<String> FRONTS = List.of("Canales", "Core");
    private static final int SPRINT_COUNT = 6;
    private static final int CAPACITY = 34;

    @Test
    @DisplayName("Crea ProgramPlanRequest válido y preserva todos sus atributos")
    void givenValidParameters_whenCreateRequest_thenPreservesAllAttributes() {
        // WHEN
        ProgramPlanRequest request = new ProgramPlanRequest(QUARTER, OBJECTIVES, FRONTS,
                SPRINT_COUNT, CAPACITY);

        // THEN
        assertThat(request.quarter()).isEqualTo(QUARTER);
        assertThat(request.objectives()).isEqualTo(OBJECTIVES);
        assertThat(request.targetFronts()).containsExactly("Canales", "Core");
        assertThat(request.sprintCount()).isEqualTo(6);
        assertThat(request.maxCapacityPerSprint()).isEqualTo(34);
    }

    @Test
    @DisplayName("Crea ProgramPlanRequest utilizando el Builder de Lombok")
    void givenBuilder_whenBuildRequest_thenInstantiatesCorrectly() {
        // WHEN
        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .targetFronts(FRONTS)
                .sprintCount(SPRINT_COUNT)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        // THEN
        assertThat(request.quarter()).isEqualTo(QUARTER);
        assertThat(request.sprintCount()).isEqualTo(SPRINT_COUNT);
    }

    @Test
    @DisplayName("Garantiza inmutabilidad de la lista de frentes objetivo")
    void givenMutableFrontsList_whenCreateRequest_thenListIsImmutableAndDefensivelyCopied() {
        // GIVEN
        List<String> mutableFronts = new ArrayList<>();
        mutableFronts.add("Canales");

        // WHEN
        ProgramPlanRequest request = new ProgramPlanRequest(QUARTER, OBJECTIVES, mutableFronts,
                SPRINT_COUNT, CAPACITY);
        mutableFronts.add("Seguridad");

        // THEN
        assertThat(request.targetFronts()).containsExactly("Canales");
        assertThatThrownBy(() -> request.targetFronts().add("NuevoFrente"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Normaliza lista nula de frentes a lista inmutable vacía")
    void givenNullTargetFronts_whenCreateRequest_thenDefaultsToEmptyList() {
        // WHEN
        ProgramPlanRequest request = new ProgramPlanRequest(QUARTER, OBJECTIVES, null, SPRINT_COUNT,
                CAPACITY);

        // THEN
        assertThat(request.targetFronts()).isNotNull().isEmpty();
    }

    @ParameterizedTest(name = "quarter inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando el quarter es nulo o blanco")
    void givenBlankQuarter_whenCreateRequest_thenThrowsException(String invalidQuarter) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new ProgramPlanRequest(invalidQuarter, OBJECTIVES, FRONTS, SPRINT_COUNT,
                        CAPACITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El trimestre (quarter) es obligatorio");
    }

    @ParameterizedTest(name = "objectives inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando objectives es nulo o blanco")
    void givenBlankObjectives_whenCreateRequest_thenThrowsException(String invalidObjectives) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new ProgramPlanRequest(QUARTER, invalidObjectives, FRONTS, SPRINT_COUNT,
                        CAPACITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Los objetivos de planeación son obligatorios");
    }

    @ParameterizedTest(name = "sprintCount inválido: {0}")
    @ValueSource(ints = {0, -1, -6})
    @DisplayName("Lanza IllegalArgumentException cuando sprintCount es menor o igual a cero")
    void givenInvalidSprintCount_whenCreateRequest_thenThrowsException(int invalidSprintCount) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new ProgramPlanRequest(QUARTER, OBJECTIVES, FRONTS, invalidSprintCount,
                        CAPACITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad de sprints debe ser mayor a cero");
    }

    @ParameterizedTest(name = "maxCapacity inválida: {0}")
    @ValueSource(ints = {0, -1, -34})
    @DisplayName("Lanza IllegalArgumentException cuando maxCapacityPerSprint es menor o igual a cero")
    void givenInvalidCapacity_whenCreateRequest_thenThrowsException(int invalidCapacity) {
        // WHEN / THEN
        assertThatThrownBy(() -> new ProgramPlanRequest(QUARTER, OBJECTIVES, FRONTS, SPRINT_COUNT,
                invalidCapacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La capacidad máxima por sprint debe ser mayor a cero");
    }

    @Test
    @DisplayName("Comprueba igualdad estructural y hashCode en solicitudes idénticas")
    void givenSameValues_whenCheckEquality_thenAreEqual() {
        // GIVEN
        ProgramPlanRequest r1 = new ProgramPlanRequest(QUARTER, OBJECTIVES, FRONTS, SPRINT_COUNT,
                CAPACITY);
        ProgramPlanRequest r2 = new ProgramPlanRequest(QUARTER, OBJECTIVES, FRONTS, SPRINT_COUNT,
                CAPACITY);

        // THEN
        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
    }
}
