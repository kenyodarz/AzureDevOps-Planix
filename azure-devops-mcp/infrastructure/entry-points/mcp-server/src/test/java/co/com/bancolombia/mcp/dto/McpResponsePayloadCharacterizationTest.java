package co.com.bancolombia.mcp.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemReference;
import co.com.bancolombia.model.workitem.WorkItemRelation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Caracterización del <b>JSON que sale hacia el cliente MCP</b> — Fase 07, D-16, DP-07 §0.2(a).
 *
 * <p><b>Por qué existe y por qué se escribió ANTES de tocar una sola clase.</b> Hasta esta fase,
 * {@code WorkItem}, {@code WorkItemRelation}, {@code WorkItemReference} y {@code WiqlResult} —cuatro
 * modelos de {@code domain/model}— <b>eran</b> el contrato de respuesta de las seis tools:
 * {@code CONTRATO-MCP.md} §3.4 lo dejó anotado al cerrar la Fase 03. La Fase 07 hace dos cosas que,
 * por separado, pueden alterar ese JSON <b>sin que nada se ponga rojo</b>:
 *
 * <ol>
 *   <li>convertir esos modelos en {@code record}, lo que renombra los accesores
 *       ({@code getId()} → {@code id()}) y con ellos los nombres de campo que Jackson emite;</li>
 *   <li>interponer DTOs de respuesta y un mapper (DP-07 §0.2(a)), que es donde un campo puede
 *       perderse, renombrarse o reordenarse en silencio.</li>
 * </ol>
 *
 * <p><b>Cómo demuestra la equivalencia — y en qué se diferencia de
 * {@code OutboundPayloadCharacterizationTest}.</b> Aquella clase compara el cuerpo emitido contra
 * {@code MAPPER.writeValueAsString(modeloDeDominio)}, es decir, usa el propio dominio como oráculo.
 * Eso vale para demostrar que un mapper <i>reproduce</i> al dominio, pero <b>es autorreferencial</b>:
 * si el dominio y el DTO cambian a la vez y de la misma forma, la igualdad se mantiene y el cambio
 * pasa inadvertido. Que es exactamente el riesgo de esta fase.
 *
 * <p>Por eso aquí se compara contra <b>cadenas literales</b>, escritas mientras el dominio todavía
 * era el contrato. Un literal no cambia solo. Si tras el refactor un campo se llama distinto, se
 * pierde, cambia de orden o un nulo deja de emitirse, esta clase se pone roja — que es justo lo que
 * MCP no haría, porque no valida esquemas al vuelo.
 *
 * <p>⚠️ <b>Estos cuatro literales son el contrato público de respuesta.</b> Si alguna vez hay que
 * cambiarlos, no es un detalle de implementación: es un cambio observable para el agente y el BFF, y
 * lo decide el propietario.
 *
 * <p><b>Estado tras el refactor.</b> Los cuatro literales <b>no se han tocado</b>: son exactamente
 * los que se escribieron contra el modelo de dominio antes de convertirlo en {@code record}. Lo que
 * cambió es únicamente <i>qué objeto</i> se serializa —ahora el DTO de respuesta que produce
 * {@link McpResponseMapper}— y esa es justamente la demostración que la fase necesitaba: el JSON que
 * ve el cliente MCP es idéntico byte a byte.
 */
class McpResponsePayloadCharacterizationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, Object> fields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("System.Title", "Historia de prueba");
        fields.put("System.State", "Active");
        return fields;
    }

    private static Map<String, Object> attributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("isLocked", false);
        return attributes;
    }

    @Test
    @DisplayName("GIVEN un WorkItem completo WHEN se serializa hacia MCP THEN el JSON conserva id, rev, fields, relations y url")
    void givenWorkItem_whenSerialized_thenFieldNamesAreFrozen() {
        // Arrange (GIVEN)
        WorkItem workItem = WorkItem.builder()
                .id(7539457)
                .rev(3)
                .fields(fields())
                .relations(List.of(WorkItemRelation.builder()
                        .rel("System.LinkTypes.Hierarchy-Reverse")
                        .url("https://dev.azure.com/rel/1")
                        .attributes(attributes())
                        .build()))
                .url("https://dev.azure.com/wi/7539457")
                .build();

        // Act (WHEN) — se serializa el DTO de respuesta, no el dominio (DP-07 §0.2(a))
        String json = MAPPER.writeValueAsString(McpResponseMapper.toResponse(workItem));

        // Assert (THEN)
        assertEquals("{\"id\":7539457,\"rev\":3,"
                + "\"fields\":{\"System.Title\":\"Historia de prueba\",\"System.State\":\"Active\"},"
                + "\"relations\":[{\"rel\":\"System.LinkTypes.Hierarchy-Reverse\","
                + "\"url\":\"https://dev.azure.com/rel/1\",\"attributes\":{\"isLocked\":false}}],"
                + "\"url\":\"https://dev.azure.com/wi/7539457\"}", json);
    }

    @Test
    @DisplayName("GIVEN un WorkItem con todo a nulo WHEN se serializa THEN los nulos se siguen emitiendo y no se sustituyen por vacios")
    void givenEmptyWorkItem_whenSerialized_thenNullsAreStillEmitted() {
        // Arrange (GIVEN) — fija el precedente de TeamScope: no se endurece nada que cambie el
        // resultado observable. Un `fields` nulo NO se convierte en `{}` ni un `relations` nulo
        // en `[]`, porque eso sería un cambio de contrato que DP-07 §0.2(c) no autorizó.
        WorkItem workItem = WorkItem.builder().build();

        // Act (WHEN)
        String json = MAPPER.writeValueAsString(McpResponseMapper.toResponse(workItem));

        // Assert (THEN)
        assertEquals("{\"id\":null,\"rev\":null,\"fields\":null,\"relations\":null,\"url\":null}",
                json);
    }

    @Test
    @DisplayName("GIVEN un WiqlResult WHEN se serializa hacia MCP THEN conserva queryType, queryResultType, asOf y workItems")
    void givenWiqlResult_whenSerialized_thenFieldNamesAreFrozen() {
        // Arrange (GIVEN) — es el retorno de listWorkItemsByTeamAndSprint, la tool mas usada
        WiqlResult result = WiqlResult.builder()
                .queryType("flat")
                .queryResultType("workItem")
                .asOf("2026-08-30T12:00:00Z")
                .workItems(List.of(WorkItemReference.builder()
                        .id(7539457)
                        .url("https://dev.azure.com/wi/7539457")
                        .build()))
                .build();

        // Act (WHEN)
        String json = MAPPER.writeValueAsString(McpResponseMapper.toResponse(result));

        // Assert (THEN)
        assertEquals("{\"queryType\":\"flat\",\"queryResultType\":\"workItem\","
                + "\"asOf\":\"2026-08-30T12:00:00Z\","
                + "\"workItems\":[{\"id\":7539457,\"url\":\"https://dev.azure.com/wi/7539457\"}]}",
                json);
    }

    @Test
    @DisplayName("GIVEN una lista de WorkItem WHEN se serializa THEN getWorkItemsBatch devuelve un array con la misma forma")
    void givenWorkItemList_whenSerialized_thenBatchShapeIsFrozen() {
        // Arrange (GIVEN)
        List<WorkItem> batch = List.of(
                WorkItem.builder().id(1).rev(1).url("https://dev.azure.com/wi/1").build(),
                WorkItem.builder().id(2).rev(1).url("https://dev.azure.com/wi/2").build());

        // Act (WHEN)
        String json = MAPPER.writeValueAsString(McpResponseMapper.toResponses(batch));

        // Assert (THEN)
        assertEquals("[{\"id\":1,\"rev\":1,\"fields\":null,\"relations\":null,"
                + "\"url\":\"https://dev.azure.com/wi/1\"},"
                + "{\"id\":2,\"rev\":1,\"fields\":null,\"relations\":null,"
                + "\"url\":\"https://dev.azure.com/wi/2\"}]", json);
    }

    @Test
    @DisplayName("GIVEN un modelo de dominio inmutable WHEN se intenta mutar su coleccion THEN falla: el WorkItem valido no puede dejar de serlo")
    void givenDomainWorkItem_whenMutatingCollections_thenItIsRejected() {
        // Arrange (GIVEN) — el objetivo declarado de la Fase 07, verificado
        WorkItem workItem = WorkItem.builder().fields(fields()).build();

        // Act + Assert (WHEN/THEN)
        assertThrows(UnsupportedOperationException.class,
                () -> workItem.fields().put("System.State", "Closed"));
    }
}
