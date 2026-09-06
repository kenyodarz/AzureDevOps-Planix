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

@DisplayName("ProgramPlanResult - Resultado estructurado de Program Planning")
class ProgramPlanResultTest {

    private static final String QUARTER = "Q3-2026";
    private static final String SUMMARY = "Roadmap macro enfocado en migración a la nube.";

    private final SprintAllocation a1 = SprintAllocation.builder()
            .sprintNumber(1)
            .initiativeId("INIT-01")
            .title("Diseño de arquitectura")
            .storyPoints(5)
            .type(ActivityType.HA)
            .front("Core")
            .build();

    private final SprintAllocation a2 = SprintAllocation.builder()
            .sprintNumber(1)
            .initiativeId("INIT-01")
            .title("Implementación de login OAuth")
            .storyPoints(8)
            .type(ActivityType.HU)
            .front("Canales")
            .build();

    private final SprintAllocation a3 = SprintAllocation.builder()
            .sprintNumber(2)
            .initiativeId("INIT-02")
            .title("Integración de pasarela de pagos")
            .storyPoints(13)
            .type(ActivityType.HU)
            .front("Canales")
            .build();

    @Test
    @DisplayName("Crea ProgramPlanResult y preserva sus atributos")
    void givenValidData_whenCreateResult_thenPreservesAllAttributes() {
        // GIVEN
        List<SprintAllocation> allocations = List.of(a1, a2, a3);
        List<String> specs = List.of("ideas_planning_q3.md", "frente_canales.md");

        // WHEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, allocations, specs);

        // THEN
        assertThat(result.quarter()).isEqualTo(QUARTER);
        assertThat(result.executiveSummary()).isEqualTo(SUMMARY);
        assertThat(result.allocations()).hasSize(3);
        assertThat(result.generatedSpecNames()).containsExactly("ideas_planning_q3.md",
                "frente_canales.md");
    }

    @Test
    @DisplayName("Crea ProgramPlanResult con Builder de Lombok")
    void givenBuilder_whenBuildResult_thenInstantiatesCorrectly() {
        // WHEN
        ProgramPlanResult result = ProgramPlanResult.builder()
                .quarter(QUARTER)
                .executiveSummary(SUMMARY)
                .allocations(List.of(a1))
                .generatedSpecNames(List.of("ideas_planning_q3.md"))
                .build();

        // THEN
        assertThat(result.quarter()).isEqualTo(QUARTER);
        assertThat(result.allocations()).hasSize(1);
    }

    @Test
    @DisplayName("Garantiza inmutabilidad y copia defensiva en listas")
    void givenMutableLists_whenCreateResult_thenCollectionsAreImmutable() {
        // GIVEN
        List<SprintAllocation> mutableAllocations = new ArrayList<>();
        mutableAllocations.add(a1);
        List<String> mutableSpecs = new ArrayList<>();
        mutableSpecs.add("spec1.md");

        // WHEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, mutableAllocations,
                mutableSpecs);
        mutableAllocations.add(a2);
        mutableSpecs.add("spec2.md");

        // THEN
        assertThat(result.allocations()).hasSize(1);
        assertThat(result.generatedSpecNames()).hasSize(1);
        assertThatThrownBy(() -> result.allocations().add(a3))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Normaliza listas nulas a listas inmutables vacías")
    void givenNullLists_whenCreateResult_thenDefaultsToEmptyLists() {
        // WHEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, null, null);

        // THEN
        assertThat(result.allocations()).isNotNull().isEmpty();
        assertThat(result.generatedSpecNames()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("totalStoryPoints calcula correctamente la suma de story points")
    void givenAllocations_whenTotalStoryPoints_thenReturnsSum() {
        // GIVEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, List.of(a1, a2, a3),
                List.of());

        // WHEN / THEN (5 + 8 + 13 = 26)
        assertThat(result.totalStoryPoints()).isEqualTo(26);
    }

    @Test
    @DisplayName("totalStoryPoints retorna cero si no hay asignaciones")
    void givenEmptyAllocations_whenTotalStoryPoints_thenReturnsZero() {
        // GIVEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, List.of(), List.of());

        // WHEN / THEN
        assertThat(result.totalStoryPoints()).isZero();
    }

    @Test
    @DisplayName("allocationsForSprint filtra correctamente por sprint")
    void givenAllocations_whenFilterBySprint_thenReturnsMatchingAllocations() {
        // GIVEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, List.of(a1, a2, a3),
                List.of());

        // WHEN
        List<SprintAllocation> sprint1Allocations = result.allocationsForSprint(1);
        List<SprintAllocation> sprint2Allocations = result.allocationsForSprint(2);
        List<SprintAllocation> sprint3Allocations = result.allocationsForSprint(3);

        // THEN
        assertThat(sprint1Allocations).containsExactly(a1, a2);
        assertThat(sprint2Allocations).containsExactly(a3);
        assertThat(sprint3Allocations).isEmpty();
    }

    @Test
    @DisplayName("allocationsForType filtra correctamente por HU o HA")
    void givenAllocations_whenFilterByType_thenReturnsMatchingAllocations() {
        // GIVEN
        ProgramPlanResult result = new ProgramPlanResult(QUARTER, SUMMARY, List.of(a1, a2, a3),
                List.of());

        // WHEN
        List<SprintAllocation> hus = result.allocationsForType(ActivityType.HU);
        List<SprintAllocation> has = result.allocationsForType(ActivityType.HA);
        List<SprintAllocation> nullType = result.allocationsForType(null);

        // THEN
        assertThat(hus).containsExactly(a2, a3);
        assertThat(has).containsExactly(a1);
        assertThat(nullType).isEmpty();
    }

    @ParameterizedTest(name = "quarter inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando quarter es nulo o blanco")
    void givenBlankQuarter_whenCreateResult_thenThrowsException(String invalidQuarter) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new ProgramPlanResult(invalidQuarter, SUMMARY, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El trimestre (quarter) es obligatorio");
    }

    @ParameterizedTest(name = "resumen ejecutivo inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando executiveSummary es nulo o blanco")
    void givenBlankSummary_whenCreateResult_thenThrowsException(String invalidSummary) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new ProgramPlanResult(QUARTER, invalidSummary, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El resumen ejecutivo es obligatorio");
    }

    @Test
    @DisplayName("Comprueba igualdad estructural y hashCode en resultados idénticos")
    void givenSameValues_whenCheckEquality_thenAreEqual() {
        // GIVEN
        ProgramPlanResult r1 = new ProgramPlanResult(QUARTER, SUMMARY, List.of(a1),
                List.of("spec1.md"));
        ProgramPlanResult r2 = new ProgramPlanResult(QUARTER, SUMMARY, List.of(a1),
                List.of("spec1.md"));

        // THEN
        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
    }
}
