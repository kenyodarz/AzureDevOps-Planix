package co.com.bancolombia.model.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias exhaustivas de los modelos de dominio para Program Planning.
 *
 * <p>Verifica invariantes de constructores compactos, serialización de conveniencia,
 * inmutabilidad defensiva y reglas de negocio según DP-PL-02 y DP-PL-04.
 */
@DisplayName("Modelos de Dominio para Program Planning (DP-PL-02, DP-PL-04)")
class ProgramPlanningModelsTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // ActivityType (DP-PL-02)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ActivityType - Tipificación de actividades HU vs HA")
    class ActivityTypeTests {

        @Test
        @DisplayName("Mapeo correcto por tag corto")
        void givenShortTags_whenFromTag_thenResolvesExpectedActivityType() {
            // WHEN / THEN
            assertThat(ActivityType.fromTag("HU")).isEqualTo(ActivityType.USER_STORY);
            assertThat(ActivityType.fromTag("hu")).isEqualTo(ActivityType.USER_STORY);
            assertThat(ActivityType.fromTag("HA")).isEqualTo(ActivityType.ENABLER);
            assertThat(ActivityType.fromTag("ha")).isEqualTo(ActivityType.ENABLER);
        }

        @Test
        @DisplayName("Mapeo correcto por nombre de enum")
        void givenEnumNames_whenFromTag_thenResolvesExpectedActivityType() {
            // WHEN / THEN
            assertThat(ActivityType.fromTag("USER_STORY")).isEqualTo(ActivityType.USER_STORY);
            assertThat(ActivityType.fromTag("ENABLER")).isEqualTo(ActivityType.ENABLER);
            assertThat(ActivityType.fromTag("user_story")).isEqualTo(ActivityType.USER_STORY);
        }

        @Test
        @DisplayName("Getters tag() y getTag() devuelven los valores esperados")
        void givenEnumValues_whenGetTag_thenReturnsCorrectTags() {
            // WHEN / THEN
            assertThat(ActivityType.USER_STORY.tag()).isEqualTo("HU");
            assertThat(ActivityType.USER_STORY.getTag()).isEqualTo("HU");
            assertThat(ActivityType.ENABLER.tag()).isEqualTo("HA");
            assertThat(ActivityType.ENABLER.getTag()).isEqualTo("HA");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "UNKNOWN", "BUG", "EPIC"})
        @DisplayName("Lanza IllegalArgumentException ante tags no reconocidos o vacíos")
        void givenInvalidTags_whenFromTag_thenThrowsException(String invalidTag) {
            // WHEN / THEN
            assertThatThrownBy(() -> ActivityType.fromTag(invalidTag))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException ante tag nulo")
        void givenNullTag_whenFromTag_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> ActivityType.fromTag(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El tag de actividad es obligatorio");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SprintAllocation
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SprintAllocation - Asignación de actividades a sprints")
    class SprintAllocationTests {

        @Test
        @DisplayName("Construcción válida y acceso a campos")
        void givenValidFields_whenCreated_thenPopulatesCorrectly() {
            // GIVEN
            List<String> deps = List.of("HU-01", "HA-00");

            // WHEN
            SprintAllocation allocation = new SprintAllocation(
                    1,
                    ActivityType.USER_STORY,
                    "Crear API de catálogo",
                    "Construcción de endpoints REST hasta QA",
                    5,
                    "Marketplace - Tools",
                    deps
            );

            // THEN
            assertThat(allocation.sprintNumber()).isEqualTo(1);
            assertThat(allocation.type()).isEqualTo(ActivityType.USER_STORY);
            assertThat(allocation.title()).isEqualTo("Crear API de catálogo");
            assertThat(allocation.description()).isEqualTo(
                    "Construcción de endpoints REST hasta QA");
            assertThat(allocation.storyPoints()).isEqualTo(5);
            assertThat(allocation.front()).isEqualTo("Marketplace - Tools");
            assertThat(allocation.dependencies()).containsExactly("HU-01", "HA-00");
        }

        @Test
        @DisplayName("Construcción con Builder de Lombok")
        void givenBuilder_whenBuilt_thenCreatesAllocation() {
            // WHEN
            SprintAllocation allocation = SprintAllocation.builder()
                    .sprintNumber(2)
                    .type(ActivityType.ENABLER)
                    .title("Paso a Producción HyMS")
                    .description("Runbook y pruebas de seguridad")
                    .storyPoints(3)
                    .front("Core - Security")
                    .dependencies(List.of("HU-05"))
                    .build();

            // THEN
            assertThat(allocation.sprintNumber()).isEqualTo(2);
            assertThat(allocation.type()).isEqualTo(ActivityType.ENABLER);
            assertThat(allocation.title()).isEqualTo("Paso a Producción HyMS");
            assertThat(allocation.storyPoints()).isEqualTo(3);
        }

        @Test
        @DisplayName("Campos opcionales nulos se normalizan a cadenas o listas vacías")
        void givenNullOptionalFields_whenCreated_thenNormalizesToDefaults() {
            // WHEN
            SprintAllocation allocation = new SprintAllocation(
                    1,
                    ActivityType.USER_STORY,
                    "HU Sin Descripción",
                    null,
                    8,
                    null,
                    null
            );

            // THEN
            assertThat(allocation.description()).isEmpty();
            assertThat(allocation.front()).isEmpty();
            assertThat(allocation.dependencies()).isEmpty();
        }

        @Test
        @DisplayName("Inmutabilidad defensiva en dependencias")
        void givenMutableDependencies_whenCreated_thenListIsImmutable() {
            // GIVEN
            List<String> mutableDeps = new ArrayList<>();
            mutableDeps.add("DEP-1");

            // WHEN
            SprintAllocation allocation = new SprintAllocation(
                    1,
                    ActivityType.USER_STORY,
                    "Título",
                    "Desc",
                    3,
                    "Frente",
                    mutableDeps
            );
            mutableDeps.add("DEP-2");

            // THEN
            List<String> dependencies = allocation.dependencies();
            assertThat(dependencies).containsExactly("DEP-1");
            assertThatThrownBy(() -> dependencies.add("DEP-3"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Invariante: sprintNumber debe ser mayor a cero")
        void givenZeroOrNegativeSprint_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new SprintAllocation(
                    0, ActivityType.USER_STORY, "Título", "Desc", 3, "Frente", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El número de sprint debe ser mayor a cero");

            assertThatThrownBy(() -> new SprintAllocation(
                    -1, ActivityType.USER_STORY, "Título", "Desc", 3, "Frente", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Invariante: type no puede ser nulo")
        void givenNullType_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new SprintAllocation(
                    1, null, "Título", "Desc", 3, "Frente", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El tipo de actividad es obligatorio");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Invariante: title no puede ser nulo ni vacío")
        void givenBlankTitle_whenCreated_thenThrowsException(String blankTitle) {
            // WHEN / THEN
            assertThatThrownBy(() -> new SprintAllocation(
                    1, ActivityType.USER_STORY, blankTitle, "Desc", 3, "Frente", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El título de la actividad es obligatorio");
        }

        @Test
        @DisplayName("Invariante: title nulo lanza excepción")
        void givenNullTitle_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new SprintAllocation(
                    1, ActivityType.USER_STORY, null, "Desc", 3, "Frente", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El título de la actividad es obligatorio");
        }

        @Test
        @DisplayName("Invariante: storyPoints no puede ser negativo")
        void givenNegativeStoryPoints_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new SprintAllocation(
                    1, ActivityType.USER_STORY, "Título", "Desc", -1, "Frente", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Los story points no pueden ser negativos");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ProgramPlanRequest
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ProgramPlanRequest - Solicitud de planeación trimestral")
    class ProgramPlanRequestTests {

        @Test
        @DisplayName("Construcción válida y acceso a campos")
        void givenValidFields_whenCreated_thenPopulatesCorrectly() {
            // GIVEN
            List<String> fronts = List.of("Marketplace - Tools", "Aegis Engine");

            // WHEN
            ProgramPlanRequest request = new ProgramPlanRequest(
                    "Q3-2026",
                    "Habilitar arquitectura de plugins y migración de base de datos",
                    fronts,
                    6,
                    30
            );

            // THEN
            assertThat(request.quarter()).isEqualTo("Q3-2026");
            assertThat(request.objectives()).contains("Habilitar arquitectura de plugins");
            assertThat(request.targetFronts()).containsExactly("Marketplace - Tools",
                    "Aegis Engine");
            assertThat(request.sprintCount()).isEqualTo(6);
            assertThat(request.maxCapacityPerSprint()).isEqualTo(30);
        }

        @Test
        @DisplayName("Construcción con Builder de Lombok")
        void givenBuilder_whenBuilt_thenCreatesRequest() {
            // WHEN
            ProgramPlanRequest request = ProgramPlanRequest.builder()
                    .quarter("Q4-2026")
                    .objectives("Despliegue multiregión")
                    .targetFronts(List.of("Core"))
                    .sprintCount(5)
                    .maxCapacityPerSprint(25)
                    .build();

            // THEN
            assertThat(request.quarter()).isEqualTo("Q4-2026");
            assertThat(request.sprintCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("targetFronts nulo se normaliza a lista vacía e inmutable")
        void givenNullTargetFronts_whenCreated_thenNormalizesToEmptyImmutableList() {
            // WHEN
            ProgramPlanRequest request = new ProgramPlanRequest(
                    "Q1-2027", "Objetivo anual", null, 4, 20);

            // THEN
            List<String> targetFronts = request.targetFronts();
            assertThat(targetFronts).isEmpty();
            assertThatThrownBy(() -> targetFronts.add("Nuevo Frente"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Invariante: quarter obligatorio y no vacío")
        void givenBlankQuarter_whenCreated_thenThrowsException(String blankQuarter) {
            // WHEN / THEN
            assertThatThrownBy(() -> new ProgramPlanRequest(blankQuarter, "Obj", List.of(), 6, 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El trimestre (quarter) es obligatorio");
        }

        @Test
        @DisplayName("Invariante: quarter nulo lanza excepción")
        void givenNullQuarter_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new ProgramPlanRequest(null, "Obj", List.of(), 6, 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El trimestre (quarter) es obligatorio");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Invariante: objectives obligatorio y no vacío")
        void givenBlankObjectives_whenCreated_thenThrowsException(String blankObjectives) {
            // WHEN / THEN
            assertThatThrownBy(
                    () -> new ProgramPlanRequest("Q3-2026", blankObjectives, List.of(), 6, 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Los objetivos de planeación son obligatorios");
        }

        @Test
        @DisplayName("Invariante: sprintCount debe ser mayor a cero")
        void givenZeroOrNegativeSprintCount_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new ProgramPlanRequest("Q3-2026", "Obj", List.of(), 0, 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("La cantidad de sprints debe ser mayor a cero");
        }

        @Test
        @DisplayName("Invariante: maxCapacityPerSprint debe ser mayor a cero")
        void givenZeroOrNegativeCapacity_whenCreated_thenThrowsException() {
            // WHEN / THEN
            assertThatThrownBy(() -> new ProgramPlanRequest("Q3-2026", "Obj", List.of(), 6, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("La capacidad máxima por sprint debe ser mayor a cero");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ProgramPlanResult (DP-PL-04)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ProgramPlanResult - Resultado estructurado de planeación")
    class ProgramPlanResultTests {

        private SprintAllocation alloc1;
        private SprintAllocation alloc2;
        private SprintAllocation alloc3;

        void setupAllocations() {
            alloc1 = new SprintAllocation(1, ActivityType.USER_STORY, "HU 1", "Desc", 5, "Front A",
                    List.of());
            alloc2 = new SprintAllocation(1, ActivityType.ENABLER, "HA 1", "Desc", 3, "Front A",
                    List.of("HU 1"));
            alloc3 = new SprintAllocation(2, ActivityType.USER_STORY, "HU 2", "Desc", 8, "Front B",
                    List.of());
        }

        @Test
        @DisplayName("Construcción válida y cálculo de métricas de conveniencia")
        void givenAllocations_whenTotalPointsAndFilters_thenComputesAccurately() {
            // GIVEN
            setupAllocations();
            List<SprintAllocation> allocations = List.of(alloc1, alloc2, alloc3);
            List<String> specs = List.of("frente_tools.md", "frente_aegis.md");

            // WHEN
            ProgramPlanResult result = new ProgramPlanResult(
                    "Q3-2026",
                    "Resumen ejecutivo del trimestre Q3 con foco en seguridad",
                    allocations,
                    specs
            );

            // THEN
            assertThat(result.quarter()).isEqualTo("Q3-2026");
            assertThat(result.executiveSummary()).contains("foco en seguridad");
            assertThat(result.allocations()).hasSize(3);
            assertThat(result.generatedSpecNames()).containsExactly("frente_tools.md",
                    "frente_aegis.md");

            // Métodos de conveniencia
            assertThat(result.totalStoryPoints()).isEqualTo(5 + 3 + 8);
            assertThat(result.allocationsForSprint(1)).containsExactly(alloc1, alloc2);
            assertThat(result.allocationsForSprint(2)).containsExactly(alloc3);
            assertThat(result.allocationsForSprint(3)).isEmpty();

            assertThat(result.allocationsForType(ActivityType.USER_STORY)).containsExactly(alloc1,
                    alloc3);
            assertThat(result.allocationsForType(ActivityType.ENABLER)).containsExactly(alloc2);
            assertThat(result.allocationsForType(null)).isEmpty();
        }

        @Test
        @DisplayName("Listas nulas se normalizan a colecciones vacías e inmutables")
        void givenNullLists_whenCreated_thenNormalizesToEmptyImmutableLists() {
            // WHEN
            ProgramPlanResult result = new ProgramPlanResult(
                    "Q2-2026", "Resumen", null, null);

            // THEN
            List<SprintAllocation> allocations = result.allocations();
            List<String> specNames = result.generatedSpecNames();
            assertThat(allocations).isEmpty();
            assertThat(specNames).isEmpty();
            assertThat(result.totalStoryPoints()).isZero();

            assertThatThrownBy(() -> allocations.add(alloc1))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> specNames.add("spec.md"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Invariante: quarter no puede estar en blanco")
        void givenBlankQuarter_whenCreated_thenThrowsException(String blankQuarter) {
            // WHEN / THEN
            assertThatThrownBy(
                    () -> new ProgramPlanResult(blankQuarter, "Resumen", List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El trimestre (quarter) es obligatorio");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Invariante: executiveSummary no puede estar en blanco")
        void givenBlankSummary_whenCreated_thenThrowsException(String blankSummary) {
            // WHEN / THEN
            assertThatThrownBy(
                    () -> new ProgramPlanResult("Q3-2026", blankSummary, List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El resumen ejecutivo es obligatorio");
        }
    }
}
