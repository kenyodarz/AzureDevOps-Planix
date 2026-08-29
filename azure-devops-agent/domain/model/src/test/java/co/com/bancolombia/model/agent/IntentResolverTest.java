package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

        @Test
        @DisplayName("Caso 1: 'Audita la historia (ID: 123)' resuelve QUALITY_AUDIT")
        void givenAuditVerbWithId_whenResolve_thenQualityAudit() {
            // WHEN
            IntentResolution resolution = resolver.resolve("Audita la historia (ID: 123)", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.QUALITY_AUDIT);
            assertThat(resolution.workItemId()).contains("123");
        }

        @Test
        @DisplayName("Caso 2 (DP-01): la palabra 'reporte' ya no secuestra una auditoría con (ID: n)")
        void givenAuditRequestContainingGeneralKeyword_whenResolve_thenQualityAuditWins() {
            // GIVEN: el texto contiene la palabra clave genérica «reporte», que antes ganaba
            String userText = "Genera un reporte de calidad de la historia (ID: 12345)";

            // WHEN
            IntentResolution resolution = resolver.resolve(userText, null);

            // THEN: la señal fuerte (ID: n) tiene precedencia sobre la palabra clave genérica
            assertThat(resolution.intent()).isEqualTo(AgentIntent.QUALITY_AUDIT);
            assertThat(resolution.workItemId()).contains("12345");
        }

        @Test
        @DisplayName("Caso 3: 'Evalúa la calidad (ID: 7)' resuelve QUALITY_AUDIT pese a la tilde")
        void givenAccentedAuditVerbWithId_whenResolve_thenQualityAudit() {
            // WHEN
            IntentResolution resolution = resolver.resolve("Evalúa la calidad (ID: 7)", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.QUALITY_AUDIT);
            assertThat(resolution.workItemId()).contains("7");
        }

        @Test
        @DisplayName("'Revisa' también dispara la auditoría cuando hay (ID: n)")
        void givenReviewVerbWithId_whenResolve_thenQualityAudit() {
            // WHEN
            IntentResolution resolution = resolver.resolve("Revisa la historia (ID: 4321)", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.QUALITY_AUDIT);
            assertThat(resolution.workItemId()).contains("4321");
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
    // Prioridad 4 — General
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 4: contexto explícito o palabra clave genérica")
    class General {

        @Test
        @DisplayName("Caso 7: 'Dame la lista de historias' resuelve GENERAL")
        void givenGeneralKeyword_whenResolve_thenGeneral() {
            // WHEN
            IntentResolution resolution = resolver.resolve("Dame la lista de historias", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }

        @Test
        @DisplayName("Caso 8: el contextId 'general-1' resuelve GENERAL sea cual sea el texto")
        void givenGeneralContextId_whenResolve_thenGeneral() {
            // WHEN
            IntentResolution resolution = resolver.resolve(LONG_IDEA, "general-1");

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }

        @Test
        @DisplayName("Caso 9: el contextId 'dashboard-9' resuelve GENERAL")
        void givenDashboardContextId_whenResolve_thenGeneral() {
            // WHEN
            IntentResolution resolution = resolver.resolve(LONG_IDEA, "dashboard-9");

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }

        @Test
        @DisplayName("Caso 13: un texto corto que no es comando resuelve GENERAL")
        void givenShortNonCommandText_whenResolve_thenGeneral() {
            // WHEN
            IntentResolution resolution = resolver.resolve("Hola", null);

            // THEN
            assertThat(resolution.intent()).isEqualTo(AgentIntent.GENERAL);
        }

        /**
         * Caso 14 de la tabla. «audita esto» tiene 11 caracteres y no incluye {@code (ID: n)}, por
         * lo que no alcanza el umbral de planeación. <b>Resultado documentado: GENERAL.</b>
         */
        @Test
        @DisplayName("Caso 14: 'audita esto' sin (ID: n) resuelve GENERAL por longitud")
        void givenAuditVerbWithoutId_whenResolve_thenGeneral() {
            // WHEN
            IntentResolution resolution = resolver.resolve("audita esto", null);

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
    // Prioridad 5 — Planeación
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad 5: texto libre de planeación")
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
            assertThat(TEXT_BELOW_THRESHOLD)
                    .hasSize(IntentResolver.MIN_PLANNING_TEXT_LENGTH - 1);

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

            // THEN
            assertThat(resolution.params()).isEmpty();
            assertThatThrownBy(() -> resolution.params().put("k", "v"))
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

