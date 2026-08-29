package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EstimationSummary")
class EstimationSummaryTest {

    private static final String CONFIRMATION = "\n\n¿Deseas continuar? Responde 'Crear'.";
    private static final String ALERT_FRAGMENT = "Alerta de Complejidad";
    private static final String MISSING_FRAGMENT = "No se pudo obtener la estimación";

    private static final int POINTS_LOW = 5;
    private static final int POINTS_AT_THRESHOLD = 8;
    private static final int POINTS_ABOVE_THRESHOLD = 9;

    @Test
    @DisplayName("Con estimación por debajo del umbral muestra el resumen y la confirmación")
    void givenEstimationBelowThreshold_whenCompose_thenShowsSummaryAndConfirmation() {
        // GIVEN
        Optional<ComplexityEstimation> estimation = Optional.of(
                new ComplexityEstimation(POINTS_LOW, UncertaintyLevel.BAJA, "alcance acotado"));

        // WHEN
        String result = EstimationSummary.compose("Contenido de la historia", estimation,
                CONFIRMATION);

        // THEN
        assertThat(result).contains("Contenido de la historia")
                .contains("**Estimación de Complejidad (Story Points):** 5")
                .contains("**Nivel de Incertidumbre:** Baja")
                .contains("alcance acotado")
                .contains(CONFIRMATION)
                .doesNotContain(ALERT_FRAGMENT);
    }

    @Test
    @DisplayName("Con estimación de 8 puntos no se sugiere dividir")
    void givenEstimationAtThreshold_whenCompose_thenDoesNotSuggestSplit() {
        // GIVEN
        Optional<ComplexityEstimation> estimation = Optional.of(
                new ComplexityEstimation(POINTS_AT_THRESHOLD, UncertaintyLevel.ALTA, "extensa"));

        // WHEN
        String result = EstimationSummary.compose("Historia", estimation, CONFIRMATION);

        // THEN
        assertThat(result).doesNotContain(ALERT_FRAGMENT).contains(CONFIRMATION);
    }

    /**
     * DP-02: por encima de 8 puntos la alerta sustituye a la confirmación normal.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-02
     */
    @Test
    @DisplayName("DP-02: por encima de 8 puntos la alerta reemplaza a la confirmación")
    void givenEstimationAboveThreshold_whenCompose_thenReplacesConfirmationWithAlert() {
        // GIVEN
        Optional<ComplexityEstimation> estimation = Optional.of(
                new ComplexityEstimation(POINTS_ABOVE_THRESHOLD, UncertaintyLevel.ALTA,
                        "varios flujos"));

        // WHEN
        String result = EstimationSummary.compose("Historia", estimation, CONFIRMATION);

        // THEN
        assertThat(result).contains(ALERT_FRAGMENT).doesNotContain(CONFIRMATION);
    }

    /**
     * DP-03: sin estimación se informa del fallo en lugar de mostrar un número inventado, para que
     * el usuario sepa que debe estimar manualmente.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-03
     */
    @Test
    @DisplayName("DP-03: sin estimación se avisa del fallo y no se muestra ningún número")
    void givenNoEstimation_whenCompose_thenShowsExplicitNotice() {
        // WHEN
        String result = EstimationSummary.compose("Historia", Optional.empty(), CONFIRMATION);

        // THEN
        assertThat(result).contains(MISSING_FRAGMENT)
                .contains("estima manualmente esta historia")
                .contains(CONFIRMATION)
                .doesNotContain("Estimación de Complejidad (Story Points):")
                .doesNotContain(ALERT_FRAGMENT);
    }

    @Test
    @DisplayName("Retira el bloque JSON encerrado en una cerca de código")
    void givenFencedJson_whenCompose_thenStripsIt() {
        // GIVEN
        String llmResponse = """
                Historia propuesta
                ```json
                {
                  "puntos": 5,
                  "incertidumbre_nivel": "Baja",
                  "incertidumbre_justificacion": "acotado"
                }
                ```""";

        // WHEN
        String result = EstimationSummary.compose(llmResponse,
                ComplexityEstimation.parseFrom(llmResponse), CONFIRMATION);

        // THEN
        assertThat(result).contains("Historia propuesta")
                .doesNotContain("\"puntos\"")
                .doesNotContain("incertidumbre_justificacion");
    }

    @Test
    @DisplayName("Retira el bloque JSON suelto, sin cerca de código")
    void givenPlainJson_whenStripEstimationJson_thenRemovesIt() {
        // GIVEN
        String llmResponse = "Historia {\"puntos\": 3, \"incertidumbre_nivel\": \"Baja\"}";

        // WHEN
        String result = EstimationSummary.stripEstimationJson(llmResponse);

        // THEN
        assertThat(result).isEqualTo("Historia");
    }

    @Test
    @DisplayName("Una respuesta nula se trata como texto vacío")
    void givenNullResponse_whenStripEstimationJson_thenReturnsEmpty() {
        // WHEN / THEN
        assertThat(EstimationSummary.stripEstimationJson(null)).isEmpty();
    }

    @Test
    @DisplayName("Un texto sin bloque JSON se conserva intacto")
    void givenResponseWithoutJson_whenStripEstimationJson_thenKeepsText() {
        // WHEN / THEN
        assertThat(EstimationSummary.stripEstimationJson("Historia sin JSON"))
                .isEqualTo("Historia sin JSON");
    }
}

