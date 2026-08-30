package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.WorkItemsBatchRequestDTO;
import co.com.bancolombia.consumer.dto.WorkItemsBatchResponseDTO;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import java.util.List;

/**
 * Mapper de la consulta en lote, en <b>los dos sentidos</b> de la frontera de salida:
 * criterio de dominio → cuerpo de petición, y cuerpo de respuesta → dominio.
 *
 * <p>La ida es la que no existía antes de la Fase 03: el {@code WebClient} serializaba el modelo de
 * dominio tal cual con {@code .bodyValue(request)} (D-11).
 */
public final class WorkItemBatchMapper {

    private WorkItemBatchMapper() {
        // Mapper estático: no se instancia.
    }

    public static WorkItemsBatchRequestDTO toRequest(WorkItemBatchCriteria criteria) {
        if (criteria == null) {
            return null;
        }
        return WorkItemsBatchRequestDTO.builder()
                .ids(criteria.ids())
                .fields(criteria.fields())
                .expand(criteria.expand())
                .errorPolicy(criteria.errorPolicy())
                .build();
    }

    public static List<WorkItem> toDomain(WorkItemsBatchResponseDTO response) {
        if (response == null || response.getValue() == null) {
            return List.of();
        }
        return response.getValue().stream()
                .map(WorkItemMapper::toDomain)
                .toList();
    }
}

