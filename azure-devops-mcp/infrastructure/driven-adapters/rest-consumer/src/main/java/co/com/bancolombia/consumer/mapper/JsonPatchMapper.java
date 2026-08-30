package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.JsonPatchOperationRequestDTO;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import java.util.List;
import java.util.Objects;

/**
 * Mapper de la <b>frontera de salida</b>: dominio → cuerpo JSON Patch de Azure DevOps.
 *
 * <p>Hasta la Fase 03 no existía: el {@code WebClient} serializaba el modelo de dominio tal cual
 * ({@code .bodyValue(patch)}), de modo que cualquier cambio en el JSON de Azure DevOps repercutía
 * directamente en {@code domain/model} y no había ninguna capa donde absorberlo (D-11).
 * {@code spring-rules.md} declara estos mappers <b>obligatorios</b> en {@code driven-adapters}.
 */
public final class JsonPatchMapper {

    private JsonPatchMapper() {
        // Mapper estático: no se instancia.
    }

    public static List<JsonPatchOperationRequestDTO> toRequest(List<JsonPatchOperation> patch) {
        if (patch == null) {
            return List.of();
        }
        return patch.stream()
                .filter(Objects::nonNull)
                .map(JsonPatchMapper::toRequest)
                .toList();
    }

    public static JsonPatchOperationRequestDTO toRequest(JsonPatchOperation operation) {
        if (operation == null) {
            return null;
        }
        return JsonPatchOperationRequestDTO.builder()
                .op(operation.op())
                .path(operation.path())
                .value(operation.value())
                .from(operation.from())
                .build();
    }
}

