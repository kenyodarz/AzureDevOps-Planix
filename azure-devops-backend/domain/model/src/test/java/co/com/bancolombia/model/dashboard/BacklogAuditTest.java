package co.com.bancolombia.model.dashboard;

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
 * Pruebas de las reglas de negocio del tablero.
 *
 * <p>Hasta la Fase 05 estas reglas vivían en {@code Handler}, un adaptador HTTP, y sólo se podían
 * probar levantando un servidor web. Que ahora se puedan ejercitar con objetos planos es
 * precisamente lo que se buscaba con D-07.
 */
class BacklogAuditTest {

    private static StoryQuality story(String id) {
        return new StoryQuality(id, "Historia " + id, 3, "Active", "Alguien", false, false, 0, 0,
                "");
    }

    private static BacklogAudit auditWith(int itemCount) {
        List<StoryQuality> items = new ArrayList<>();
        for (int i = 1; i <= itemCount; i++) {
            items.add(story(String.valueOf(i)));
        }
        return new BacklogAudit(new BacklogMetrics(itemCount * 3, 0, 0, 0, 0), items);
    }

    @Nested
    @DisplayName("Partición en lotes (D-07)")
    class Partition {

        /**
         * Es el mismo reparto que fija la prueba de caracterización del entry-point.
         */
        @Test
        @DisplayName("GIVEN 25 historias y lotes de 10 WHEN partition THEN salen 10, 10 y 5")
        void given25Items_whenPartition_thenTenTenAndFive() {
            // GIVEN / WHEN
            List<List<StoryQuality>> batches = auditWith(25).partition(10);

            // THEN
            assertThat(batches).hasSize(3);
            assertThat(batches.get(0)).hasSize(10);
            assertThat(batches.get(1)).hasSize(10);
            assertThat(batches.get(2)).hasSize(5);
            assertThat(BacklogAudit.idsCsv(batches.get(2))).isEqualTo("21,22,23,24,25");
        }

        @Test
        @DisplayName("GIVEN menos historias que el lote WHEN partition THEN sale un único lote")
        void givenFewerItemsThanBatch_whenPartition_thenSingleBatch() {
            // GIVEN / WHEN / THEN
            assertThat(auditWith(3).partition(10)).hasSize(1);
        }

        @Test
        @DisplayName("GIVEN un múltiplo exacto WHEN partition THEN no sobra ningún lote vacío")
        void givenExactMultiple_whenPartition_thenNoEmptyTrailingBatch() {
            // GIVEN / WHEN / THEN
            assertThat(auditWith(20).partition(10)).hasSize(2);
        }

        @Test
        @DisplayName("GIVEN un tablero sin historias WHEN partition THEN no hay lotes")
        void givenNoItems_whenPartition_thenNoBatches() {
            // GIVEN / WHEN / THEN
            assertThat(new BacklogAudit(null, null).partition(10)).isEmpty();
            assertThat(BacklogAudit.idsCsv(List.of())).isEmpty();
        }

        /**
         * Un lote de cero produciría lotes infinitos: mejor fallar al configurarlo.
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("GIVEN un tamaño de lote no positivo WHEN partition THEN falla en el acto")
        void givenNonPositiveBatchSize_whenPartition_thenFails(int batchSize) {
            // GIVEN
            BacklogAudit audit = auditWith(5);

            // WHEN / THEN
            assertThatThrownBy(() -> audit.partition(batchSize))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mayor que cero");
        }
    }

    @Nested
    @DisplayName("Aplicación de auditorías y recálculo (D-06, D-07)")
    class Updates {

        /**
         * Mismos números que la prueba de caracterización: (90+90+30+30)/4 = 60, entera.
         */
        @Test
        @DisplayName("GIVEN cuatro auditorías WHEN withUpdates THEN recalcula media entera y no documentados")
        void givenFourUpdates_whenWithUpdates_thenRecalculates() {
            // GIVEN
            BacklogAudit audit = auditWith(4);

            // WHEN
            BacklogAudit result = audit.withUpdates(List.of(
                    new StoryUpdate("1", true, true, 90, 1, "ok"),
                    new StoryUpdate("2", true, true, 90, 1, "ok"),
                    new StoryUpdate("3", true, false, 30, 0, "falta DoD"),
                    new StoryUpdate("4", false, false, 30, 0, "falta todo")));

            // THEN
            assertThat(result.metrics().avgQualityScore()).isEqualTo(60);
            assertThat(result.metrics().undocumentedCount()).isEqualTo(2);
            assertThat(result.items().getFirst().feedback()).isEqualTo("ok");
        }

        @Test
        @DisplayName("GIVEN una auditoría aplicada WHEN se compara THEN la original no cambió")
        void givenAppliedUpdates_whenCompared_thenOriginalIsUntouched() {
            // GIVEN
            BacklogAudit original = auditWith(2);

            // WHEN
            BacklogAudit result = original.withUpdates(
                    List.of(new StoryUpdate("1", true, true, 100, 3, "perfecta")));

            // THEN
            assertThat(original.items().getFirst().qualityScore()).isZero();
            assertThat(result.items().getFirst().qualityScore()).isEqualTo(100);
            assertThat(original.metrics().avgQualityScore()).isZero();
        }

        @Test
        @DisplayName("GIVEN los totales de puntos WHEN withUpdates THEN no se tocan")
        void givenPointTotals_whenWithUpdates_thenTheyAreKept() {
            // GIVEN / WHEN
            BacklogAudit result = auditWith(2)
                    .withUpdates(List.of(new StoryUpdate("1", true, true, 80, 1, "ok")));

            // THEN
            assertThat(result.metrics().totalPoints()).isEqualTo(6);
        }

        @Test
        @DisplayName("GIVEN una auditoría de una historia que no existe WHEN withUpdates THEN se ignora")
        void givenUpdateForUnknownStory_whenWithUpdates_thenItIsIgnored() {
            // GIVEN / WHEN
            BacklogAudit result = auditWith(2)
                    .withUpdates(List.of(new StoryUpdate("999", true, true, 100, 1, "ok")));

            // THEN
            assertThat(result.items()).allMatch(item -> item.qualityScore() == 0);
            assertThat(result.metrics().undocumentedCount()).isEqualTo(2);
        }

        /**
         * Antes ganaba la última coincidencia, por un doble bucle. Ahora gana la primera.
         */
        @Test
        @DisplayName("GIVEN dos auditorías del mismo ítem WHEN withUpdates THEN gana la primera")
        void givenDuplicatedUpdates_whenWithUpdates_thenFirstWins() {
            // GIVEN / WHEN
            BacklogAudit result = auditWith(1).withUpdates(List.of(
                    new StoryUpdate("1", true, true, 90, 1, "primera"),
                    new StoryUpdate("1", false, false, 10, 0, "segunda")));

            // THEN
            assertThat(result.items().getFirst().feedback()).isEqualTo("primera");
        }

        @Test
        @DisplayName("GIVEN una tanda vacía o nula WHEN withUpdates THEN devuelve el mismo estado")
        void givenNoUpdates_whenWithUpdates_thenStateIsUnchanged() {
            // GIVEN
            BacklogAudit audit = auditWith(2);

            // WHEN / THEN
            assertThat(audit.withUpdates(List.of())).isSameAs(audit);
            assertThat(audit.withUpdates(null)).isSameAs(audit);
        }

        @Test
        @DisplayName("GIVEN un tablero sin métricas WHEN withUpdates THEN no intenta recalcularlas")
        void givenNoMetrics_whenWithUpdates_thenNothingIsRecalculated() {
            // GIVEN
            BacklogAudit audit = new BacklogAudit(null, List.of(story("1")));

            // WHEN
            BacklogAudit result = audit.withUpdates(
                    List.of(new StoryUpdate("1", true, true, 70, 1, "ok")));

            // THEN
            assertThat(result.metrics()).isNull();
            assertThat(result.items().getFirst().qualityScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("GIVEN auditorías nulas o sin id WHEN withUpdates THEN se descartan sin fallar")
        void givenMalformedUpdates_whenWithUpdates_thenTheyAreDiscarded() {
            // GIVEN
            List<StoryUpdate> updates = new ArrayList<>();
            updates.add(null);
            updates.add(new StoryUpdate(null, true, true, 50, 1, "sin id"));

            // WHEN / THEN
            assertThat(auditWith(1).withUpdates(updates).items().getFirst().qualityScore())
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Estado sin auditar")
    class Unaudited {

        @Test
        @DisplayName("GIVEN un tablero auditado WHEN unaudited THEN pone la calidad a cero y conserva los puntos")
        void givenAuditedBoard_whenUnaudited_thenQualityIsReset() {
            // GIVEN
            BacklogAudit audited = new BacklogAudit(new BacklogMetrics(29, 13, 44, 78, 2),
                    List.of(new StoryQuality("1001", "Historia mock", 8, "Approved", "Alguien",
                            true, true, 66, 6, "falta DoD")));

            // WHEN
            BacklogAudit result = audited.unaudited();

            // THEN
            assertThat(result.metrics().avgQualityScore()).isZero();
            assertThat(result.metrics().undocumentedCount()).isZero();
            assertThat(result.metrics().totalPoints()).isEqualTo(29);
            assertThat(result.metrics().completedPercentage()).isEqualTo(44);
            StoryQuality item = result.items().getFirst();
            assertThat(item.qualityScore()).isZero();
            assertThat(item.linkedTasksCount()).isZero();
            assertThat(item.hasAcceptanceCriteria()).isFalse();
            assertThat(item.hasDoD()).isFalse();
            assertThat(item.points()).isEqualTo(8);
            assertThat(item.title()).isEqualTo("Historia mock");
        }

        @Test
        @DisplayName("GIVEN un tablero sin métricas WHEN unaudited THEN no falla")
        void givenNoMetrics_whenUnaudited_thenDoesNotFail() {
            // GIVEN / WHEN / THEN
            assertThat(new BacklogAudit(null, List.of()).unaudited().metrics()).isNull();
        }
    }

    @Nested
    @DisplayName("Detalles del modelo")
    class ModelDetails {

        @Test
        @DisplayName("GIVEN una historia WHEN isUndocumented THEN basta con que le falte CA o DoD")
        void givenStory_whenIsUndocumented_thenEitherMissingCounts() {
            // GIVEN / WHEN / THEN
            assertThat(story("1").applied(new StoryUpdate("1", true, true, 90, 1, "ok"))
                    .isUndocumented()).isFalse();
            assertThat(story("1").applied(new StoryUpdate("1", true, false, 90, 1, "ok"))
                    .isUndocumented()).isTrue();
            assertThat(story("1").applied(new StoryUpdate("1", false, true, 90, 1, "ok"))
                    .isUndocumented()).isTrue();
        }

        @Test
        @DisplayName("GIVEN una auditoría nula WHEN applied THEN la historia no cambia")
        void givenNullUpdate_whenApplied_thenStoryIsUnchanged() {
            // GIVEN
            StoryQuality original = story("1");

            // WHEN / THEN
            assertThat(original.applied(null)).isSameAs(original);
        }

        @Test
        @DisplayName("GIVEN una lista externa WHEN se construye THEN la auditoría no la comparte")
        void givenExternalList_whenBuilt_thenItIsCopied() {
            // GIVEN
            List<StoryQuality> mutable = new ArrayList<>(List.of(story("1")));
            BacklogAudit audit = new BacklogAudit(null, mutable);

            // WHEN
            mutable.add(story("2"));

            // THEN
            assertThat(audit.items()).hasSize(1);
        }

        @Test
        @DisplayName("GIVEN unas métricas sin ítems WHEN recalculatedFrom THEN devuelve las mismas")
        void givenNullItems_whenRecalculated_thenSameMetrics() {
            // GIVEN
            BacklogMetrics metrics = new BacklogMetrics(10, 5, 50, 80, 1);

            // WHEN / THEN
            assertThat(metrics.recalculatedFrom(null)).isEqualTo(metrics);
        }

        @Test
        @DisplayName("GIVEN una lista vacía WHEN recalculatedFrom THEN no divide por cero")
        void givenEmptyItems_whenRecalculated_thenNoDivisionByZero() {
            // GIVEN / WHEN
            BacklogMetrics result = new BacklogMetrics(0, 0, 0, 50, 3).recalculatedFrom(List.of());

            // THEN
            assertThat(result.avgQualityScore()).isZero();
            assertThat(result.undocumentedCount()).isZero();
        }
    }
}

