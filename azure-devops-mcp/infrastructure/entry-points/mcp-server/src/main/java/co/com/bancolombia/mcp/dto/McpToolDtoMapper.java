package co.com.bancolombia.mcp.dto;

import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import java.util.List;

/**
 * Mapper de la <b>frontera de entrada</b>: traduce los DTOs que Jackson deserializa desde el payload
 * MCP a los modelos de dominio.
 *
 * <p>Es la mitad que faltaba de la simetría que exige {@code spring-rules.md}: hasta la Fase 03 no
 * había ninguna traducción en ninguno de los dos extremos, porque el dominio <i>era</i> el contrato
 * de cable (D-11).
 *
 * <p>Sin Spring y sin lógica de negocio: <b>solo traducción</b>. Si algún día hay que validar o
 * normalizar, eso pertenece al dominio o al caso de uso, no aquí.
 */
public final class McpToolDtoMapper {

    private McpToolDtoMapper() {
        // Mapper estático: no se instancia.
    }

    /**
     * Traduce la lista de operaciones JSON Patch del cable MCP al dominio.
     *
     * <p>Una lista nula se traduce a lista vacía y no a {@code null}, para que el adaptador nunca
     * reciba una referencia sin desreferenciar (S2259).
     */
    public static List<JsonPatchOperation> toDomain(List<JsonPatchOperationInput> patch) {
        if (patch == null) {
            return List.of();
        }
        return patch.stream()
                .filter(java.util.Objects::nonNull)
                .map(McpToolDtoMapper::toDomain)
                .toList();
    }

    public static JsonPatchOperation toDomain(JsonPatchOperationInput input) {
        if (input == null) {
            return null;
        }
        return JsonPatchOperation.builder()
                .op(input.getOp())
                .path(input.getPath())
                .value(input.getValue())
                .from(input.getFrom())
                .build();
    }

    public static WorkItemBatchCriteria toDomain(WorkItemsBatchInput input) {
        if (input == null) {
            return null;
        }
        return WorkItemBatchCriteria.builder()
                .ids(input.getIds())
                .fields(input.getFields())
                .expand(input.getExpand())
                .errorPolicy(input.getErrorPolicy())
                .build();
    }
}

