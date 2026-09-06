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

@DisplayName("ProgramPlanningCommand - Value Object para invocación al Agente (DP-BFF-03)")
class ProgramPlanningCommandTest {

    private static final String QUARTER = "Q3-2026";
    private static final int SPRINTS = 6;
    private static final int CAPACITY = 34;
    private static final List<String> FRONTS = List.of("Canales", "Core");
    private static final String OBJECTIVES = "Modernización Cloud";
    private static final String CONTEXT_ID = "ctx-12345";

    @Test
    @DisplayName("Crea ProgramPlanningCommand válido y preserva todos sus atributos")
    void givenValidData_whenCreateCommand_thenPreservesAllAttributes() {
        // WHEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                QUARTER, SPRINTS, CAPACITY, FRONTS, OBJECTIVES, CONTEXT_ID);

        // THEN
        assertThat(command.quarter()).isEqualTo("Q3-2026");
        assertThat(command.sprintCount()).isEqualTo(6);
        assertThat(command.maxCapacityPerSprint()).isEqualTo(34);
        assertThat(command.targetFronts()).containsExactly("Canales", "Core");
        assertThat(command.objectives()).isEqualTo(OBJECTIVES);
        assertThat(command.contextId()).isEqualTo(CONTEXT_ID);
    }

    @Test
    @DisplayName("Crea ProgramPlanningCommand con Builder de Lombok")
    void givenBuilder_whenBuildCommand_thenInstantiatesCorrectly() {
        // WHEN
        ProgramPlanningCommand command = ProgramPlanningCommand.builder()
                .quarter(QUARTER)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .targetFronts(FRONTS)
                .objectives(OBJECTIVES)
                .contextId(CONTEXT_ID)
                .build();

        // THEN
        assertThat(command.quarter()).isEqualTo("Q3-2026");
        assertThat(command.sprintCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("Normaliza quarter en minúsculas a mayúsculas")
    void givenLowerCaseQuarter_whenCreateCommand_thenNormalizesToUpperCase() {
        // WHEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                "q2-2025", SPRINTS, CAPACITY, FRONTS, OBJECTIVES, CONTEXT_ID);

        // THEN
        assertThat(command.quarter()).isEqualTo("Q2-2025");
    }

    @Test
    @DisplayName("Garantiza inmutabilidad y copia defensiva en targetFronts")
    void givenMutableFrontsList_whenCreateCommand_thenDefensivelyCopiedAndImmutable() {
        // GIVEN
        List<String> mutableList = new ArrayList<>();
        mutableList.add("Canales");

        // WHEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                QUARTER, SPRINTS, CAPACITY, mutableList, OBJECTIVES, CONTEXT_ID);
        mutableList.add("Seguridad");

        // THEN
        assertThat(command.targetFronts()).containsExactly("Canales");
        assertThatThrownBy(() -> command.targetFronts().add("NuevoFrente"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Normaliza campos nulos opcionales (targetFronts, objectives, contextId)")
    void givenNullOptionalFields_whenCreateCommand_thenNormalizesToDefaults() {
        // WHEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                QUARTER, SPRINTS, CAPACITY, null, null, null);

        // THEN
        assertThat(command.targetFronts()).isNotNull().isEmpty();
        assertThat(command.objectives()).isEmpty();
        assertThat(command.contextId()).isEmpty();
    }

    @ParameterizedTest(name = "quarter válido: \"{0}\"")
    @ValueSource(strings = {"Q1-2026", "q2-2025", "Q3-2027", "q4-2030", "  q3-2026  "})
    @DisplayName("Acepta formatos válidos de trimestre QX-YYYY")
    void givenValidQuarterFormats_whenCreateCommand_thenSucceeds(String validQuarter) {
        // WHEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                validQuarter, SPRINTS, CAPACITY, FRONTS, OBJECTIVES, CONTEXT_ID);

        // THEN
        assertThat(command.quarter()).isEqualTo(validQuarter.trim().toUpperCase());
    }

    @ParameterizedTest(name = "quarter inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"Q5-2026", "Q0-2026", "2026-Q3", "Q3", "2026", "Q3/2026", "inv", "   "})
    @DisplayName("Lanza IllegalArgumentException cuando el quarter no cumple formato QX-YYYY")
    void givenInvalidQuarter_whenCreateCommand_thenThrowsException(String invalidQuarter) {
        // WHEN / THEN
        assertThatThrownBy(() -> new ProgramPlanningCommand(
                invalidQuarter, SPRINTS, CAPACITY, FRONTS, OBJECTIVES, CONTEXT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El trimestre debe cumplir el formato QX-YYYY (ej. Q3-2026)");
    }

    @ParameterizedTest(name = "sprintCount inválido: {0}")
    @ValueSource(ints = {0, -1, -6})
    @DisplayName("Lanza IllegalArgumentException cuando sprintCount es menor o igual a cero")
    void givenInvalidSprintCount_whenCreateCommand_thenThrowsException(int invalidSprintCount) {
        // WHEN / THEN
        assertThatThrownBy(() -> new ProgramPlanningCommand(
                QUARTER, invalidSprintCount, CAPACITY, FRONTS, OBJECTIVES, CONTEXT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El número de sprints debe ser mayor a 0");
    }

    @ParameterizedTest(name = "capacidad máxima inválida: {0}")
    @ValueSource(ints = {0, -1, -34})
    @DisplayName("Lanza IllegalArgumentException cuando maxCapacityPerSprint es menor o igual a cero")
    void givenInvalidCapacity_whenCreateCommand_thenThrowsException(int invalidCapacity) {
        // WHEN / THEN
        assertThatThrownBy(() -> new ProgramPlanningCommand(
                QUARTER, SPRINTS, invalidCapacity, FRONTS, OBJECTIVES, CONTEXT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La capacidad máxima debe ser mayor a 0");
    }

    @Test
    @DisplayName("toCanonicalCommand y toAgentPrompt generan formato canónico completo con frentes y objetivos")
    void givenCompleteCommand_whenToCanonicalCommand_thenProducesExpectedPrompt() {
        // GIVEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                QUARTER, SPRINTS, CAPACITY, FRONTS, OBJECTIVES, CONTEXT_ID);

        // WHEN
        String canonical = command.toCanonicalCommand();
        String prompt = command.toAgentPrompt();

        // THEN
        String expected = "/plan Q3-2026 6 sprints 34 sp [Canales, Core] Objetivos: Modernización Cloud";
        assertThat(canonical).isEqualTo(expected);
        assertThat(prompt).isEqualTo(expected);
    }

    @Test
    @DisplayName("toCanonicalCommand omite frentes y objetivos si están vacíos")
    void givenMinimalCommand_whenToCanonicalCommand_thenProducesMinimalPrompt() {
        // GIVEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                QUARTER, SPRINTS, CAPACITY, List.of(), "", CONTEXT_ID);

        // WHEN
        String canonical = command.toCanonicalCommand();

        // THEN
        assertThat(canonical).isEqualTo("/plan Q3-2026 6 sprints 34 sp");
    }

    @Test
    @DisplayName("toCanonicalCommand formatea frentes cuando no hay objetivos")
    void givenCommandWithFrontsOnly_whenToCanonicalCommand_thenIncludesFronts() {
        // GIVEN
        ProgramPlanningCommand command = new ProgramPlanningCommand(
                QUARTER, SPRINTS, CAPACITY, List.of("Canales"), null, CONTEXT_ID);

        // WHEN
        String canonical = command.toCanonicalCommand();

        // THEN
        assertThat(canonical).isEqualTo("/plan Q3-2026 6 sprints 34 sp [Canales]");
    }

    @Test
    @DisplayName("fromRequest construye comando correctamente a partir de ProgramPlanRequest")
    void givenPlanRequest_whenFromRequest_thenBuildsMatchingCommand() {
        // GIVEN
        ProgramPlanRequest request = new ProgramPlanRequest(
                QUARTER, OBJECTIVES, FRONTS, SPRINTS, CAPACITY);

        // WHEN
        ProgramPlanningCommand commandWithCtx = ProgramPlanningCommand.fromRequest(request,
                "ctx-99");
        ProgramPlanningCommand commandWithoutCtx = ProgramPlanningCommand.fromRequest(request);

        // THEN
        assertThat(commandWithCtx.quarter()).isEqualTo("Q3-2026");
        assertThat(commandWithCtx.contextId()).isEqualTo("ctx-99");
        assertThat(commandWithoutCtx.contextId()).isEmpty();
    }

    @Test
    @DisplayName("fromRequest lanza IllegalArgumentException si el request es nulo")
    void givenNullRequest_whenFromRequest_thenThrowsException() {
        // WHEN / THEN
        assertThatThrownBy(() -> ProgramPlanningCommand.fromRequest(null, "ctx-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El request de planeación no puede ser nulo");
    }

    @Test
    @DisplayName("Comprueba igualdad estructural y hashCode en comandos idénticos")
    void givenSameValues_whenCheckEquality_thenAreEqual() {
        // GIVEN
        ProgramPlanningCommand c1 = new ProgramPlanningCommand(QUARTER, SPRINTS, CAPACITY, FRONTS,
                OBJECTIVES, CONTEXT_ID);
        ProgramPlanningCommand c2 = new ProgramPlanningCommand(QUARTER, SPRINTS, CAPACITY, FRONTS,
                OBJECTIVES, CONTEXT_ID);

        // THEN
        assertThat(c1).isEqualTo(c2).hasSameHashCodeAs(c2);
    }
}
