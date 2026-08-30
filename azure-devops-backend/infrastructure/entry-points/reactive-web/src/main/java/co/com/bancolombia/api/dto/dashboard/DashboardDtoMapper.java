package co.com.bancolombia.api.dto.dashboard;

import co.com.bancolombia.api.dto.event.StoryUpdateEvent;
import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.model.dashboard.BacklogMetrics;
import co.com.bancolombia.model.dashboard.StoryQuality;
import co.com.bancolombia.model.dashboard.StoryUpdate;
import java.util.List;

/**
 * Traduce entre el contrato JSON de la capa web y el modelo de negocio inmutable.
 *
 * <p>Es la frontera que hacía falta para que {@code domain/model} dejara de contener clases
 * mutables con sufijo técnico (D-13) y para que nadie pudiera modificarlas in situ dentro de una
 * cadena reactiva (D-06). La traducción es aburrida a propósito: campo a campo, sin lógica. La Fase
 * 06 la reducirá a un único punto al rediseñar el flujo SSE.
 */
public final class DashboardDtoMapper {

    private DashboardDtoMapper() {
    }

    public static BacklogAudit toDomain(DashboardResponse dto) {
        if (dto == null) {
            return new BacklogAudit(null, List.of());
        }
        return new BacklogAudit(toDomain(dto.getMetrics()), toDomainItems(dto.getItems()));
    }

    private static List<StoryQuality> toDomainItems(List<DashboardStoryItemResponse> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().filter(java.util.Objects::nonNull)
                .map(DashboardDtoMapper::toDomain).toList();
    }

    private static BacklogMetrics toDomain(DashboardMetricsResponse dto) {
        if (dto == null) {
            return null;
        }
        return new BacklogMetrics(dto.getTotalPoints(), dto.getCompletedPoints(),
                dto.getCompletedPercentage(), dto.getAvgQualityScore(), dto.getUndocumentedCount());
    }

    private static StoryQuality toDomain(DashboardStoryItemResponse dto) {
        return new StoryQuality(dto.getId(), dto.getTitle(), dto.getPoints(), dto.getState(),
                dto.getAssignedMember(), dto.isHasAcceptanceCriteria(), dto.isHasDoD(),
                dto.getQualityScore(), dto.getLinkedTasksCount(), dto.getFeedback());
    }

    /**
     * Convierte los eventos de auditoría que envía el agente en actualizaciones de dominio.
     */
    public static List<StoryUpdate> toDomainUpdates(List<StoryUpdateEvent> updates) {
        if (updates == null) {
            return List.of();
        }
        return updates.stream().filter(java.util.Objects::nonNull)
                .map(update -> new StoryUpdate(update.getId(), update.isHasAcceptanceCriteria(),
                        update.isHasDoD(), update.getQualityScore(), update.getLinkedTasksCount(),
                        update.getFeedback()))
                .toList();
    }

    public static DashboardResponse toDto(BacklogAudit audit) {
        if (audit == null) {
            return null;
        }
        return new DashboardResponse(toDto(audit.metrics()),
                audit.items().stream().map(DashboardDtoMapper::toDto).toList());
    }

    private static DashboardMetricsResponse toDto(BacklogMetrics metrics) {
        if (metrics == null) {
            return null;
        }
        DashboardMetricsResponse dto = new DashboardMetricsResponse();
        dto.setTotalPoints(metrics.totalPoints());
        dto.setCompletedPoints(metrics.completedPoints());
        dto.setCompletedPercentage(metrics.completedPercentage());
        dto.setAvgQualityScore(metrics.avgQualityScore());
        dto.setUndocumentedCount(metrics.undocumentedCount());
        return dto;
    }

    private static DashboardStoryItemResponse toDto(StoryQuality item) {
        DashboardStoryItemResponse dto = new DashboardStoryItemResponse();
        dto.setId(item.id());
        dto.setTitle(item.title());
        dto.setPoints(item.points());
        dto.setState(item.state());
        dto.setAssignedMember(item.assignedMember());
        dto.setHasAcceptanceCriteria(item.hasAcceptanceCriteria());
        dto.setHasDoD(item.hasDoD());
        dto.setQualityScore(item.qualityScore());
        dto.setLinkedTasksCount(item.linkedTasksCount());
        dto.setFeedback(item.feedback());
        return dto;
    }
}

