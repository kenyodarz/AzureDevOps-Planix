package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de tabla del {@link IntentResolver}.
 *
 * <p>No usan mocks: el resolutor es un servicio de dominio puro. Cada bloque corresponde a un
 * escalón de la tabla de precedencia de DP-01.
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
 */
@DisplayName("IntentResolver - Tabla de precedencia de intenciones (DP-01)")
class IntentResolverTest {

    private static final String LONG_IDEA =
            "Necesito construir un microservicio de notificaciones push para la app movil";

    /** Texto de exactamente {@code MIN_PLANNING_TEXT_LENGTH} caracteres. */
    private static final String TEXT_AT_THRESHOLD = "Idea nueva para el equipo";

    /** Texto de un carácter menos que el umbral. */
    private static final String TEXT_BELOW_THRESHOLD = "Idea nueva para el grupo";

    private IntentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new IntentResolver();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Prioridad 1 — Auditoría de calidad
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 1: verbo de auditoría + (ID: n)")
    class QualityAudit {

        @ParameterizedTest(name = "\"{0}\" resuelve QUALITY_AUDIT con id {1}")
        @CsvSource({
                "'Audita la historia (ID: 123)', 123",
                "'Evalúa la calidad (ID: 7)', 7",
                "'Revisa la historia (ID: 4321)', 4321"
        })
        @DisplayName("Verbos de auditoría con (ID: n) resuelven QUALITY_AUDIT")
        void givenAuditVerbsWithId_whenResolve_thenQualityAudit(String text, String expectedId) {
            // WHEN
            IntentResolution resolution = resolver.resolve(text, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.QUALITY_AUDIT);
            assertThat(resolution.workItemId()).contains(expectedId);
        }

        @Test
        @DisplayName("La auditoría gana incluso con un contextId 'general-*'")
        void givenAuditRequestAndGeneralContext_whenResolve_thenQualityAuditWins() {
            // WHEN
            IntentResolution resolution =
                    resolver.resolve("Audita la historia (ID: 55)", "general-1");

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.QUALITY_AUDIT);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Prioridad 2 — Refinamiento
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 2: marcador de refinamiento + (ID: n)")
    class Refinement {

        @Test
        @DisplayName("Caso 4: 'Analicemos y refinemos la historia (ID: 99)' resuelve REFINEMENT")
        void givenRefinementMarkerWithId_whenResolve_thenRefinement() {
            // WHEN
            IntentResolution resolution =
                    resolver.resolve("Analicemos y refinemos la historia (ID: 99)", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.REFINEMENT);
            assertThat(resolution.workItemId()).contains("99");
        }

        @Test
        @DisplayName("El marcador sin (ID: n) no resuelve REFINEMENT")
        void givenRefinementMarkerWithoutId_whenResolve_thenFallsThroughToPlanning() {
            // WHEN
            IntentResolution resolution =
                    resolver.resolve("Analicemos y refinemos la historia del sprint", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PLANNING_DRAFT);
            assertThat(resolution.workItemId()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Prioridad 3 — Comandos cortos
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 3: comandos cortos exactos")
    class ShortCommands {

        @ParameterizedTest(name = "\"{0}\" resuelve APPROVAL")
        @ValueSource(strings = {"Aprobado", "aprobado", "Sí", "si", "De acuerdo", "Aprobado."})
        @DisplayName("Caso 5: los comandos de aprobación resuelven APPROVAL")
        void givenApprovalCommand_whenResolve_thenApproval(String command) {
            // WHEN
            IntentResolution resolution = resolver.resolve(command, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.APPROVAL);
        }

        @ParameterizedTest(name = "\"{0}\" resuelve DIVISION")
        @ValueSource(strings = {"Dividir", "Divídela", "divide", "DIVIDIR"})
        @DisplayName("Caso 6: los comandos de división resuelven DIVISION")
        void givenDivisionCommand_whenResolve_thenDivision(String command) {
            // WHEN
            IntentResolution resolution = resolver.resolve(command, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.DIVISION);
        }

        @Test
        @DisplayName("Un comando corto gana sobre un contextId 'general-*'")
        void givenApprovalCommandAndGeneralContext_whenResolve_thenApproval() {
            // WHEN
            IntentResolution resolution = resolver.resolve("Aprobado", "general-1");

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.APPROVAL);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Prioridad 4 — Planeación de Programa (Program Planning)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 4: comandos o frases clave de planeación de programa")
    class ProgramPlanning {

        @ParameterizedTest(name = "\"{0}\" resuelve PROGRAM_PLANNING")
        @ValueSource(strings = {
                "/plan",
                "/plan Q3",
                "/roadmap",
                "/roadmap Q3-2026",
                "/program-planning",
                "/plan.",
                "/plan "
        })
        @DisplayName("Los comandos de planeación resuelven PROGRAM_PLANNING")
        void givenPlanningCommands_whenResolve_thenProgramPlanning(String command) {
            // WHEN
            IntentResolution resolution = resolver.resolve(command, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PROGRAM_PLANNING);
            assertThat(resolution.workItemId()).isEmpty();
        }

        @ParameterizedTest(name = "\"{0}\" resuelve PROGRAM_PLANNING")
        @ValueSource(strings = {
                "planear q",
                "planear q3",
                "planear Q3 2026",
                "planificar q4",
                "planeacion q2",
                "planeación q2",
                "planificacion trimestre",
                "roadmap q1",
                "generar roadmap",
                "generar roadmap del squad"
        })
        @DisplayName("Las frases y palabras clave de planeación resuelven PROGRAM_PLANNING")
        void givenPlanningPhrases_whenResolve_thenProgramPlanning(String phrase) {
            // WHEN
            IntentResolution resolution = resolver.resolve(phrase, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PROGRAM_PLANNING);
            assertThat(resolution.workItemId()).isEmpty();
        }

        @Test
        @DisplayName("Un comando de planeación gana sobre un contextId 'general-*'")
        void givenPlanningCommandAndGeneralContext_whenResolve_thenProgramPlanningWins() {
            // WHEN
            IntentResolution resolution = resolver.resolve("/plan Q3", "general-1");

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PROGRAM_PLANNING);
        }

        @Test
        @DisplayName("Una frase de planeación gana sobre una palabra clave genérica")
        void givenPlanningPhraseWithGeneralWord_whenResolve_thenProgramPlanningWins() {
            // GIVEN: contiene 'lista' que normalmente iría a general
            String phrase = "generar roadmap con la lista de iniciativas para Q3";

            // WHEN
            IntentResolution resolution = resolver.resolve(phrase, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PROGRAM_PLANNING);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Prioridad 5 — General
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 4: contexto explícito o palabra clave genérica")
    class General {

        @ParameterizedTest(name = "Caso: \"{0}\" resuelve GENERAL")
        @ValueSource(strings = {
                "Dame la lista de historias",
                "Hola",
                "audita esto"
        })
        @DisplayName("Textos cortos, sin comando o con palabras generales resuelven GENERAL")
        void givenGeneralOrShortTexts_whenResolve_thenGeneral(String text) {
            // WHEN
            IntentResolution resolution = resolver.resolve(text, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }

        @ParameterizedTest(name = "contextId={0} resuelve GENERAL")
        @ValueSource(strings = {"general-1", "dashboard-9"})
        @DisplayName("Contexto general o dashboard resuelve GENERAL sea cual sea el texto")
        void givenGeneralOrDashboardContextId_whenResolve_thenGeneral(String contextId) {
            // WHEN
            IntentResolution resolution = resolver.resolve(LONG_IDEA, contextId);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }

        @Test
        @DisplayName("Un texto nulo resuelve GENERAL sin lanzar excepción")
        void givenNullText_whenResolve_thenGeneral() {
            // WHEN
            IntentResolution resolution = resolver.resolve(null, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
            assertThat(resolution.workItemId()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Prioridad 6 — Borrador de Planeación (Planning Draft)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 6: texto libre de planeación de historias")
    class PlanningDraft {

        @Test
        @DisplayName("Caso 10 (DP-01): 'que consulta' dentro de una idea no secuestra la planeación")
        void givenLongIdeaWithRelativeClauseKeyword_whenResolve_thenPlanningDraft() {
            // GIVEN: «consulta» aparece gobernada por el relativo «que»: describe el sistema
            String userText = "Necesito un servicio que consulta el saldo del cliente desde el core";

            // WHEN
            IntentResolution resolution = resolver.resolve(userText, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PLANNING_DRAFT);
        }

        @Test
        @DisplayName("Caso 11: la forma derivada 'consultar' no dispara el flujo General")
        void givenLongIdeaWithDerivedKeyword_whenResolve_thenPlanningDraft() {
            // GIVEN
            String userText = "Necesito un microservicio para consultar el saldo del cliente";

            // WHEN
            IntentResolution resolution = resolver.resolve(userText, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PLANNING_DRAFT);
        }

        @Test
        @DisplayName("Caso 12: una idea libre larga resuelve PLANNING_DRAFT")
        void givenLongFreeText_whenResolve_thenPlanningDraft() {
            // WHEN
            IntentResolution resolution = resolver.resolve(LONG_IDEA, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PLANNING_DRAFT);
        }

        @Test
        @DisplayName("Caso 15: exactamente 25 caracteres ya se considera idea de planeación")
        void givenTextAtThreshold_whenResolve_thenPlanningDraft() {
            // GIVEN
            assertThat(TEXT_AT_THRESHOLD).hasSize(IntentResolver.MIN_PLANNING_TEXT_LENGTH);

            // WHEN
            IntentResolution resolution = resolver.resolve(TEXT_AT_THRESHOLD, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.PLANNING_DRAFT);
        }

        @Test
        @DisplayName("Caso 15 (frontera): 24 caracteres se atiende como GENERAL")
        void givenTextBelowThreshold_whenResolve_thenGeneral() {
            // GIVEN
            assertThat(TEXT_BELOW_THRESHOLD).hasSize(IntentResolver.MIN_PLANNING_TEXT_LENGTH - 1);

            // WHEN
            IntentResolution resolution = resolver.resolve(TEXT_BELOW_THRESHOLD, null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Contrato de IntentResolution
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Contrato de IntentResolution")
    class ResolutionContract {

        @Test
        @DisplayName("Una resolución sin parámetros expone un mapa vacío e inmutable")
        void givenResolutionWithoutParams_whenModifyParams_thenFails() {
            // GIVEN
            IntentResolution resolution = IntentResolution.of(AgentIntent.GENERAL);
            Map<String, String> params = resolution.params();

            // THEN
            assertThat(params).isEmpty();
            assertThatThrownBy(() -> params.put("k", "v"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Un mapa nulo se normaliza a un mapa vacío")
        void givenNullParams_whenBuild_thenEmptyMap() {
            // WHEN
            IntentResolution resolution = new IntentResolution(AgentIntent.GENERAL, null);

            // THEN
            assertThat(resolution.params()).isEmpty();
        }

        @Test
        @DisplayName("La intención es obligatoria")
        void givenNullIntent_whenBuild_thenThrows() {
            // WHEN / THEN
            assertThatThrownBy(() -> IntentResolution.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("El identificador de Work Item es obligatorio en withWorkItem")
        void givenBlankWorkItemId_whenBuild_thenThrows() {
            // WHEN / THEN
            assertThatThrownBy(() -> IntentResolution.withWorkItem(AgentIntent.REFINEMENT, " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

