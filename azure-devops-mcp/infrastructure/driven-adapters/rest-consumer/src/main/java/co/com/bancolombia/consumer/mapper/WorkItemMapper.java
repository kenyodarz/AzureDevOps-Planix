package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.WiqlResultDTO;
import co.com.bancolombia.consumer.dto.WorkItemDTO;
import co.com.bancolombia.consumer.dto.WorkItemReferenceDTO;
import co.com.bancolombia.consumer.dto.WorkItemRelationDTO;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemReference;
import co.com.bancolombia.model.workitem.WorkItemRelation;
import java.util.List;

/**
 * Mapper de respuesta: DTOs de Azure DevOps → modelos de dominio de Work Item.
 *
 * <p>Su contenido es <b>exactamente</b> el de los métodos privados {@code toDomain(...)} que vivían
 * dentro de {@code RestConsumer}. La Fase 03 los saca a un mapper propio, como manda
 * {@code spring-rules.md}, sin alterar una sola línea de su lógica: el comportamiento observable es
 * idéntico y {@code RestConsumerTest} lo verifica.
 */
public final class WorkItemMapper {

    private WorkItemMapper() {
        // Mapper estático: no se instancia.
    }

    public static WorkItem toDomain(WorkItemDTO dto) {
        if (dto == null) {
            return null;
        }
        return WorkItem.builder()
                .id(dto.getId())
                .rev(dto.getRev())
                .fields(dto.getFields())
                .relations(dto.getRelations() != null
                        ? dto.getRelations().stream().map(WorkItemMapper::toDomain).toList()
                        : List.of())
                .url(dto.getUrl())
                .build();
    }

    public static WorkItemRelation toDomain(WorkItemRelationDTO dto) {
        if (dto == null) {
            return null;
        }
        return WorkItemRelation.builder()
                .rel(dto.getRel())
                .url(dto.getUrl())
                .attributes(dto.getAttributes())
                .build();
    }

    public static WiqlResult toDomain(WiqlResultDTO dto) {
        if (dto == null) {
            return null;
        }
        return WiqlResult.builder()
                .queryType(dto.getQueryType())
                .queryResultType(dto.getQueryResultType())
                .asOf(dto.getAsOf())
                .workItems(dto.getWorkItems() != null
                        ? dto.getWorkItems().stream().map(WorkItemMapper::toDomain).toList()
                        : List.of())
                .build();
    }

    public static WorkItemReference toDomain(WorkItemReferenceDTO dto) {
        if (dto == null) {
            return null;
        }
        return WorkItemReference.builder()
                .id(dto.getId())
                .url(dto.getUrl())
                .build();
    }
}

