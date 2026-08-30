package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.TeamIteration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Invariantes del modelo de dominio — Fase 07, D-16, DP-07 §0.2(c).
 *
 * <p><b>Qué demuestra esta clase.</b> Que el objetivo declarado de la fase se cumple de verdad:
 * <i>un {@code WorkItem} válido no puede dejar de serlo a mitad de un flujo reactivo</i>. Antes de
 * esta fase, las nueve clases del modelo tenían {@code @Setter} —o {@code @Data}, que es peor— y
 * <b>cero invariantes</b>. Cualquiera podía vaciar un objeto ya construido, o mutar su
 * {@code Map} de campos, y nada lo impedía.
 *
 * <p><b>Y qué demuestra igual de importante: lo que NO se endureció.</b> Los casos de «no rechaza»
 * son tan deliberados como los de «rechaza». El javadoc de {@link TeamScope} dejó escrito el
 * precedente: rechazar una cadena en blanco convertiría un tablero vacío en un error, que es
 * «<i>exactamente</i> el cambio de comportamiento observable que DP-04 §0.1 descartó». Estas pruebas
 * fijan esa tolerancia para que un refactor futuro no la elimine por parecer más limpio.
 */
class DomainInvariantsTest {

    @Nested
    @DisplayName("Inmutabilidad: las colecciones del dominio no se pueden mutar")
    class Immutability {

        @Test
        @DisplayName("GIVEN un WorkItem WHEN se muta su mapa de campos THEN se rechaza")
        void givenWorkItem_whenMutatingFields_thenRejected() {
            // Arrange (GIVEN)
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("System.Title", "Historia");
            WorkItem workItem = WorkItem.builder().fields(fields).build();

            // Act + Assert (WHEN/THEN)
            assertThrows(UnsupportedOperationException.class,
                    () -> workItem.fields().put("System.State", "Closed"));
        }

        @Test
        @DisplayName("GIVEN un WorkItem WHEN se muta la coleccion original THEN el WorkItem no cambia")
        void givenWorkItem_whenMutatingSource_thenItIsUnaffected() {
            // Arrange (GIVEN) — la copia es defensiva, no una simple referencia
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("System.Title", "Historia");
            WorkItem workItem = WorkItem.builder().fields(fields).build();

            // Act (WHEN)
            fields.put("System.State", "Closed");

            // Assert (THEN)
            assertEquals(1, workItem.fields().size());
        }

        @Test
        @DisplayName("GIVEN un WiqlResult WHEN se muta su lista de referencias THEN se rechaza")
        void givenWiqlResult_whenMutatingWorkItems_thenRejected() {
            // Arrange (GIVEN)
            WiqlResult result = WiqlResult.builder()
                    .workItems(new ArrayList<>(List.of(WorkItemReference.builder().id(1).build())))
                    .build();

            // Act + Assert (WHEN/THEN)
            assertThrows(UnsupportedOperationException.class, () -> result.workItems().clear());
        }

        @Test
        @DisplayName("GIVEN un WorkItemRelation WHEN se mutan sus atributos THEN se rechaza")
        void givenRelation_whenMutatingAttributes_thenRejected() {
            // Arrange (GIVEN)
            WorkItemRelation relation = WorkItemRelation.builder()
                    .rel("System.LinkTypes.Hierarchy-Reverse")
                    .attributes(new LinkedHashMap<>(Map.of("isLocked", false)))
                    .build();

            // Act + Assert (WHEN/THEN)
            assertThrows(UnsupportedOperationException.class,
                    () -> relation.attributes().put("isLocked", true));
        }

        @Test
        @DisplayName("GIVEN un WorkItemBatchCriteria WHEN se mutan sus ids THEN se rechaza")
        void givenCriteria_whenMutatingIds_thenRejected() {
            // Arrange (GIVEN)
            WorkItemBatchCriteria criteria = WorkItemBatchCriteria.builder()
                    .ids(new ArrayList<>(List.of(1, 2)))
                    .fields(new ArrayList<>(List.of("System.Id")))
                    .build();

            // Act + Assert (WHEN/THEN)
            assertThrows(UnsupportedOperationException.class, () -> criteria.ids().add(3));
            assertThrows(UnsupportedOperationException.class, () -> criteria.fields().clear());
        }

        @Test
        @DisplayName("GIVEN un TeamFieldValues WHEN se mutan sus valores THEN se rechaza")
        void givenTeamFieldValues_whenMutatingValues_thenRejected() {
            // Arrange (GIVEN)
            TeamFieldValues values = TeamFieldValues.builder()
                    .defaultValue("Proj\\Team")
                    .values(new ArrayList<>(List.of("Proj\\Team")))
                    .build();

            // Act + Assert (WHEN/THEN)
            assertThrows(UnsupportedOperationException.class, () -> values.values().clear());
        }
    }

    @Nested
    @DisplayName("Invariantes de rechazo: las unicas dos que declara DP-07 §0.2(c)")
    class Rejections {

        @Test
        @DisplayName("GIVEN una sentencia WIQL nula o en blanco WHEN se construye THEN se rechaza")
        void givenBlankStatement_whenBuildingWiqlQuery_thenRejected() {
            // Act + Assert (WHEN/THEN)
            assertThrows(IllegalArgumentException.class,
                    () -> WiqlQuery.builder().query(null).build());
            assertThrows(IllegalArgumentException.class,
                    () -> WiqlQuery.builder().query("   ").build());
        }

        @Test
        @DisplayName("GIVEN una operacion JSON Patch sin op o sin path WHEN se construye THEN se rechaza")
        void givenIncompletePatch_whenBuilding_thenRejected() {
            // Act + Assert (WHEN/THEN) — sin verbo o sin destino no es una operacion
            assertThrows(IllegalArgumentException.class,
                    () -> JsonPatchOperation.builder().path("/fields/System.Title").build());
            assertThrows(IllegalArgumentException.class,
                    () -> JsonPatchOperation.builder().op("add").build());
            assertThrows(IllegalArgumentException.class,
                    () -> JsonPatchOperation.builder().op(" ").path(" ").build());
        }

        @Test
        @DisplayName("GIVEN una operacion de borrado sin value ni from WHEN se construye THEN se acepta")
        void givenRemoveWithoutValue_whenBuilding_thenAccepted() {
            // Arrange + Act (GIVEN/WHEN) — `remove` no lleva valor y solo `move`/`copy` llevan origen
            JsonPatchOperation operation = JsonPatchOperation.builder()
                    .op("remove").path("/relations/0").build();

            // Assert (THEN)
            assertNull(operation.value());
            assertNull(operation.from());
        }
    }

    @Nested
    @DisplayName("Tolerancias deliberadas: lo que NO se endurecio, y por que")
    class DeliberateTolerances {

        @Test
        @DisplayName("GIVEN un defaultValue en blanco WHEN se construye TeamFieldValues THEN se acepta")
        void givenBlankDefaultValue_whenBuilding_thenAccepted() {
            // Arrange + Act (GIVEN/WHEN) — Azure DevOps lo devuelve para una celula mal configurada.
            // Rechazarlo convertiria ese tablero vacio en un error: el cambio que DP-04 descarto.
            TeamFieldValues values = TeamFieldValues.builder().defaultValue("").build();

            // Assert (THEN)
            assertEquals("", values.defaultValue());
        }

        @Test
        @DisplayName("GIVEN colecciones nulas WHEN se construye el modelo THEN se conservan nulas y no se vacian")
        void givenNullCollections_whenBuilding_thenTheyStayNull() {
            // Arrange + Act (GIVEN/WHEN) — convertirlas en vacias cambiaria el JSON publico
            WorkItem workItem = WorkItem.builder().build();
            WiqlResult result = WiqlResult.builder().build();

            // Assert (THEN)
            assertNull(workItem.fields());
            assertNull(workItem.relations());
            assertNull(result.workItems());
        }

        @Test
        @DisplayName("GIVEN un WorkItem sin id WHEN se construye THEN se acepta")
        void givenWorkItemWithoutId_whenBuilding_thenAccepted() {
            // Arrange + Act (GIVEN/WHEN) — una respuesta degradada no es un error de dominio
            WorkItem workItem = WorkItem.builder().url("https://dev.azure.com/wi/1").build();

            // Assert (THEN)
            assertNull(workItem.id());
        }

        @Test
        @DisplayName("GIVEN un lote sin ids WHEN se construye el criterio THEN se acepta")
        void givenEmptyBatch_whenBuilding_thenAccepted() {
            // Arrange + Act (GIVEN/WHEN) — hoy llega a Azure DevOps y vuelve una lista vacia
            WorkItemBatchCriteria criteria = WorkItemBatchCriteria.builder()
                    .ids(List.of()).expand("None").errorPolicy("Omit").build();

            // Assert (THEN)
            assertEquals(0, criteria.ids().size());
        }

        @Test
        @DisplayName("GIVEN una iteracion sin nombre WHEN se construye THEN se acepta")
        void givenIterationWithoutName_whenBuilding_thenAccepted() {
            // Arrange + Act (GIVEN/WHEN) — GetTeamIterationsUseCase ya tolera el nombre nulo
            TeamIteration iteration = TeamIteration.builder().id("1").path("Proj\\Sprint 1").build();

            // Assert (THEN)
            assertNull(iteration.name());
        }
    }

    @Nested
    @DisplayName("Igualdad por valor: consecuencia de dejar de ser mutable")
    class ValueEquality {

        @Test
        @DisplayName("GIVEN dos WorkItem con los mismos datos WHEN se comparan THEN son iguales")
        void givenSameData_whenComparing_thenEqual() {
            // Arrange (GIVEN)
            WorkItem one = WorkItem.builder().id(1).url("u").build();
            WorkItem other = WorkItem.builder().id(1).url("u").build();

            // Assert (THEN)
            assertEquals(one, other);
            assertEquals(one.hashCode(), other.hashCode());
        }

        @Test
        @DisplayName("GIVEN un WorkItem WHEN se deriva con toBuilder THEN el original no cambia")
        void givenWorkItem_whenDerived_thenOriginalIsUntouched() {
            // Arrange (GIVEN)
            WorkItem original = WorkItem.builder().id(1).rev(1).build();

            // Act (WHEN)
            WorkItem derived = original.toBuilder().rev(2).build();

            // Assert (THEN)
            assertEquals(1, original.rev());
            assertEquals(2, derived.rev());
        }
    }
}

