package co.com.bancolombia.mcp.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Congela la <b>frontera de entrada</b> introducida por la Fase 03 (D-11).
 *
 * <p>Verifica dos cosas distintas y ambas críticas:
 *
 * <ol>
 *   <li>Que el mapper traduce sin perder ni un dato entre el DTO del cable y el dominio.</li>
 *   <li>Que los <b>nombres de campo</b> de los DTOs son <b>exactamente</b> los que documenta
 *       {@code CONTRATO-MCP.md} §3. Ésta es la aserción que protege a los clientes reales: MCP no
 *       valida esquemas al vuelo, así que renombrar {@code op} o {@code errorPolicy} no rompería
 *       ninguna compilación — solo dejaría de llegar el valor, en silencio y en producción.</li>
 * </ol>
 */
class McpToolDtoMapperTest {

    private static Set<String> fieldNamesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("GIVEN el DTO de entrada de JSON Patch THEN sus nombres de campo son los del contrato: op, path, value, from")
    void givenJsonPatchInput_whenInspectingFields_thenWireNamesAreUnchanged() {
        assertEquals(Set.of("op", "path", "value", "from"),
                fieldNamesOf(JsonPatchOperationInput.class),
                "Los nombres de campo de JsonPatchOperation son contrato público (CONTRATO-MCP.md §3)");
    }

    @Test
    @DisplayName("GIVEN el DTO de entrada de lote THEN sus nombres de campo son los del contrato: ids, fields, expand, errorPolicy")
    void givenBatchInput_whenInspectingFields_thenWireNamesAreUnchanged() {
        assertEquals(Set.of("ids", "fields", "expand", "errorPolicy"),
                fieldNamesOf(WorkItemsBatchInput.class),
                "Los nombres de campo de WorkItemsBatchRequest son contrato público (CONTRATO-MCP.md §3)");
    }

    @Test
    @DisplayName("GIVEN operaciones JSON Patch del cable WHEN toDomain THEN se traducen los cuatro campos")
    void givenPatchInput_whenToDomain_thenAllFieldsAreTranslated() {
        // Arrange (GIVEN)
        List<JsonPatchOperationInput> input = List.of(
                JsonPatchOperationInput.builder()
                        .op("add").path("/fields/System.Title").value("Título").from("origen")
                        .build());

        // Act (WHEN)
        List<JsonPatchOperation> domain = McpToolDtoMapper.toDomain(input);

        // Assert (THEN)
        assertEquals(1, domain.size());
        assertEquals("add", domain.get(0).getOp());
        assertEquals("/fields/System.Title", domain.get(0).getPath());
        assertEquals("Título", domain.get(0).getValue());
        assertEquals("origen", domain.get(0).getFrom());
    }

    @Test
    @DisplayName("GIVEN una lista nula de patch WHEN toDomain THEN se devuelve lista vacia y no null (S2259)")
    void givenNullPatch_whenToDomain_thenReturnsEmptyList() {
        List<JsonPatchOperation> domain = McpToolDtoMapper.toDomain((List<JsonPatchOperationInput>) null);

        assertNotNull(domain);
        assertTrue(domain.isEmpty());
    }

    @Test
    @DisplayName("GIVEN el criterio de lote del cable WHEN toDomain THEN se traducen los cuatro campos")
    void givenBatchInput_whenToDomain_thenAllFieldsAreTranslated() {
        // Arrange (GIVEN)
        WorkItemsBatchInput input = WorkItemsBatchInput.builder()
                .ids(List.of(1, 2))
                .fields(List.of("System.Id"))
                .expand("None")
                .errorPolicy("Omit")
                .build();

        // Act (WHEN)
        WorkItemBatchCriteria criteria = McpToolDtoMapper.toDomain(input);

        // Assert (THEN)
        assertEquals(List.of(1, 2), criteria.getIds());
        assertEquals(List.of("System.Id"), criteria.getFields());
        assertEquals("None", criteria.getExpand());
        assertEquals("Omit", criteria.getErrorPolicy());
    }

    @Test
    @DisplayName("GIVEN entradas nulas WHEN toDomain THEN no se lanza NullPointerException")
    void givenNullInputs_whenToDomain_thenReturnsNull() {
        org.junit.jupiter.api.Assertions.assertNull(
                McpToolDtoMapper.toDomain((WorkItemsBatchInput) null));
        org.junit.jupiter.api.Assertions.assertNull(
                McpToolDtoMapper.toDomain((JsonPatchOperationInput) null));
    }
}

