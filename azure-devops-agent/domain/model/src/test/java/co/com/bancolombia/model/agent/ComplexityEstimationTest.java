package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ComplexityEstimation")
class ComplexityEstimationTest {

    private static final int POINTS_LOW = 5;
    private static final int POINTS_AT_THRESHOLD = 8;
    private static final int POINTS_ABOVE_THRESHOLD = 9;
    private static final int POINTS_FIBONACCI_NEXT = 13;

    private static final String VALID_JSON = """
            ```json
            {
              "puntos": 5,
              "incertidumbre_nivel": "Baja",
              "incertidumbre_justificacion": "alcance acotado a un único endpoint"
            }
            ```""";

    // ═══════════════════════════════════════════════════════════════════════════
    // requiresSplit — DP-02
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * DP-02: la guía corporativa establece «&gt;8 Requiere Dividir». La frontera está entre 8 y 9,
     * no entre 12 y 13: el modelo no siempre respeta la escala Fibonacci.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-02
     */
    @ParameterizedTest(name = "{0} puntos -> requiere dividir: {1}")
    @CsvSource({"1,false", "3,false", "5,false", "8,false", "9,true", "10,true", "13,true", "21,true"})
    @DisplayName("DP-02: solo las estimaciones por encima de 8 puntos requieren división")
    void givenPoints_whenRequiresSplit_thenAppliesEightPointThreshold(int points,
            boolean expected) {
        // GIVEN
        ComplexityEstimation estimation =
                new ComplexityEstimation(points, UncertaintyLevel.MEDIA, "justificación");

        // WHEN / THEN
        assertThat(estimation.requiresSplit()).isEqualTo(expected);
    }

    @Test
    @DisplayName("El umbral expuesto es 8")
    void givenConstant_whenRead_thenIsEight() {
        assertThat(ComplexityEstimation.MAX_STORY_POINTS_PER_STORY)
                .isEqualTo(POINTS_AT_THRESHOLD);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Invariantes
    // ═══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "{0} puntos es inválido")
    @ValueSource(ints = {0, -1, -8})
    @DisplayName("Los Story Points deben ser mayores que cero")
    void givenNonPositivePoints_whenConstruct_thenThrows(int points) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new ComplexityEstimation(points, UncertaintyLevel.BAJA, "justificación"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");
    }

    @Test
    @DisplayName("El nivel de incertidumbre es obligatorio")
    void givenNullLevel_whenConstruct_thenThrows() {
        // WHEN / THEN
        assertThatThrownBy(() -> new ComplexityEstimation(POINTS_LOW, null, "justificación"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nivel de incertidumbre");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Una justificación ausente se sustituye por un texto explícito")
    void givenBlankRationale_whenConstruct_thenUsesFallback(String rationale) {
        // WHEN
        ComplexityEstimation estimation =
                new ComplexityEstimation(POINTS_LOW, UncertaintyLevel.BAJA, rationale);

        // THEN
        assertThat(estimation.rationale()).isEqualTo("El modelo no entregó una justificación.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // parseFrom — DP-03
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Extrae la estimación de un bloque JSON dentro de una cerca de código")
    void givenFencedJson_whenParseFrom_thenExtractsEstimation() {
        // WHEN
        Optional<ComplexityEstimation> result = ComplexityEstimation.parseFrom(VALID_JSON);

        // THEN
        assertThat(result).isPresent();
        ComplexityEstimation estimation = result.orElseThrow();
        assertThat(estimation.points()).isEqualTo(POINTS_LOW);
        assertThat(estimation.level()).isEqualTo(UncertaintyLevel.BAJA);
        assertThat(estimation.rationale()).isEqualTo("alcance acotado a un único endpoint");
    }

    @Test
    @DisplayName("Extrae la estimación de un bloque JSON suelto")
    void givenPlainJson_whenParseFrom_thenExtractsEstimation() {
        // GIVEN
        String response = "Texto previo {\"puntos\": 13, \"incertidumbre_nivel\": \"Alta\"} final";

        // WHEN
        Optional<ComplexityEstimation> result = ComplexityEstimation.parseFrom(response);

        // THEN
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().points()).isEqualTo(POINTS_FIBONACCI_NEXT);
        assertThat(result.orElseThrow().level()).isEqualTo(UncertaintyLevel.ALTA);
    }

    /**
     * DP-03: la ausencia de estimación se propaga como {@code Optional.empty()}. Está prohibido
     * inventar un valor por defecto, porque el usuario lo interpretaría como una estimación real.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-03
     */
    @Test
    @DisplayName("DP-03: sin bloque JSON no se inventa ninguna estimación")
    void givenResponseWithoutJson_whenParseFrom_thenReturnsEmpty() {
        // GIVEN
        String response = "```markdown\n# Historia sin bloque de estimación\n```";

        // WHEN / THEN
        assertThat(ComplexityEstimation.parseFrom(response)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("DP-03: una respuesta vacía no produce estimación")
    void givenBlankResponse_whenParseFrom_thenReturnsEmpty(String response) {
        // WHEN / THEN
        assertThat(ComplexityEstimation.parseFrom(response)).isEmpty();
    }

    @Test
    @DisplayName("DP-03: un valor de puntos igual a cero se descarta")
    void givenZeroPoints_whenParseFrom_thenReturnsEmpty() {
        // GIVEN
        String response = "{\"puntos\": 0, \"incertidumbre_nivel\": \"Baja\"}";

        // WHEN / THEN
        assertThat(ComplexityEstimation.parseFrom(response)).isEmpty();
    }

    @Test
    @DisplayName("Un nivel de incertidumbre no reconocible cae en MEDIA sin perder los puntos")
    void givenUnknownLevel_whenParseFrom_thenFallsBackToMedia() {
        // GIVEN
        String response = "{\"puntos\": 9, \"incertidumbre_nivel\": \"Desconocido\"}";

        // WHEN
        Optional<ComplexityEstimation> result = ComplexityEstimation.parseFrom(response);

        // THEN
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().points()).isEqualTo(POINTS_ABOVE_THRESHOLD);
        assertThat(result.orElseThrow().level()).isEqualTo(UncertaintyLevel.MEDIA);
    }

    @Test
    @DisplayName("Un JSON sin justificación conserva los puntos y usa el texto de reserva")
    void givenJsonWithoutRationale_whenParseFrom_thenUsesFallbackRationale() {
        // GIVEN
        String response = "{\"puntos\": 3, \"incertidumbre_nivel\": \"Baja\"}";

        // WHEN
        Optional<ComplexityEstimation> result = ComplexityEstimation.parseFrom(response);

        // THEN
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().rationale())
                .isEqualTo("El modelo no entregó una justificación.");
    }
}

