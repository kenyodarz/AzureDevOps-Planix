package co.com.bancolombia.api.dto.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.api.dto.event.StoryUpdateEvent;
import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.model.dashboard.BacklogMetrics;
import co.com.bancolombia.model.dashboard.StoryQuality;
import co.com.bancolombia.model.dashboard.StoryUpdate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la frontera entre el contrato JSON y el modelo de negocio (D-06, D-13).
 */
class DashboardDtoMapperTest {

    private DashboardStoryItemResponse itemDto() {
        DashboardStoryItemResponse dto = new DashboardStoryItemResponse();
        dto.setId("1001");
        dto.setTitle("Historia mock");
        dto.setPoints(8);
        dto.setState("Approved");
        dto.setAssignedMember("Alguien");
        dto.setHasAcceptanceCriteria(true);
        dto.setHasDoD(false);
        dto.setQualityScore(66);
        dto.setLinkedTasksCount(6);
        dto.setFeedback("falta DoD");
        return dto;
    }

    private DashboardMetricsResponse metricsDto() {
        DashboardMetricsResponse dto = new DashboardMetricsResponse();
        dto.setTotalPoints(29);
        dto.setCompletedPoints(13);
        dto.setCompletedPercentage(44);
        dto.setAvgQualityScore(78);
        dto.setUndocumentedCount(2);
        return dto;
    }

    @Test
    @DisplayName("GIVEN un tablero completo WHEN ida y vuelta THEN no se pierde ningún campo")
    void givenFullBoard_whenRoundTrip_thenNothingIsLost() {
        // GIVEN
        DashboardResponse original = new DashboardResponse(metricsDto(), List.of(itemDto()));

        // WHEN
        DashboardResponse result = DashboardDtoMapper.toDto(DashboardDtoMapper.toDomain(original));

        // THEN
        assertThat(result.getMetrics().getTotalPoints()).isEqualTo(29);
        assertThat(result.getMetrics().getCompletedPoints()).isEqualTo(13);
        assertThat(result.getMetrics().getCompletedPercentage()).isEqualTo(44);
        assertThat(result.getMetrics().getAvgQualityScore()).isEqualTo(78);
        assertThat(result.getMetrics().getUndocumentedCount()).isEqualTo(2);

        DashboardStoryItemResponse item = result.getItems().getFirst();
        assertThat(item.getId()).isEqualTo("1001");
        assertThat(item.getTitle()).isEqualTo("Historia mock");
        assertThat(item.getPoints()).isEqualTo(8);
        assertThat(item.getState()).isEqualTo("Approved");
        assertThat(item.getAssignedMember()).isEqualTo("Alguien");
        assertThat(item.isHasAcceptanceCriteria()).isTrue();
        assertThat(item.isHasDoD()).isFalse();
        assertThat(item.getQualityScore()).isEqualTo(66);
        assertThat(item.getLinkedTasksCount()).isEqualTo(6);
        assertThat(item.getFeedback()).isEqualTo("falta DoD");
    }

    @Test
    @DisplayName("GIVEN un JSON incompleto WHEN toDomain THEN no revienta con nulos")
    void givenIncompletePayload_whenToDomain_thenNoNullPointer() {
        // GIVEN / WHEN
        BacklogAudit fromNull = DashboardDtoMapper.toDomain(null);
        BacklogAudit fromEmpty = DashboardDtoMapper.toDomain(new DashboardResponse(null, null));

        // THEN
        assertThat(fromNull.items()).isEmpty();
        assertThat(fromNull.metrics()).isNull();
        assertThat(fromEmpty.items()).isEmpty();
        assertThat(fromEmpty.metrics()).isNull();
        assertThat(DashboardDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("GIVEN una lista con huecos WHEN toDomain THEN los descarta")
    void givenListWithNulls_whenToDomain_thenTheyAreDiscarded() {
        // GIVEN
        List<DashboardStoryItemResponse> items = new ArrayList<>();
        items.add(null);
        items.add(itemDto());

        // WHEN / THEN
        assertThat(DashboardDtoMapper.toDomain(new DashboardResponse(null, items)).items())
                .hasSize(1);
    }

    @Test
    @DisplayName("GIVEN eventos de auditoría WHEN toDomainUpdates THEN los traduce y descarta huecos")
    void givenAuditEvents_whenToDomainUpdates_thenTranslated() {
        // GIVEN
        StoryUpdateEvent event = new StoryUpdateEvent();
        event.setId("1001");
        event.setHasAcceptanceCriteria(true);
        event.setHasDoD(true);
        event.setQualityScore(90);
        event.setLinkedTasksCount(2);
        event.setFeedback("revisado");

        List<StoryUpdateEvent> events = new ArrayList<>();
        events.add(event);
        events.add(null);

        // WHEN
        List<StoryUpdate> updates = DashboardDtoMapper.toDomainUpdates(events);

        // THEN
        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst())
                .isEqualTo(new StoryUpdate("1001", true, true, 90, 2, "revisado"));
        assertThat(DashboardDtoMapper.toDomainUpdates(null)).isEmpty();
    }

    @Test
    @DisplayName("GIVEN un dominio sin métricas WHEN toDto THEN el JSON las lleva en nulo")
    void givenDomainWithoutMetrics_whenToDto_thenMetricsAreNull() {
        // GIVEN
        BacklogAudit audit = new BacklogAudit(null,
                List.of(new StoryQuality("1", "T", 1, "S", "A", false, false, 0, 0, "")));

        // WHEN
        DashboardResponse dto = DashboardDtoMapper.toDto(audit);

        // THEN
        assertThat(dto.getMetrics()).isNull();
        assertThat(dto.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("GIVEN métricas de dominio WHEN toDto THEN viajan íntegras")
    void givenDomainMetrics_whenToDto_thenAllFieldsTravel() {
        // GIVEN
        BacklogAudit audit = new BacklogAudit(new BacklogMetrics(1, 2, 3, 4, 5), List.of());

        // WHEN
        DashboardMetricsResponse metrics = DashboardDtoMapper.toDto(audit).getMetrics();

        // THEN
        assertThat(metrics.getTotalPoints()).isEqualTo(1);
        assertThat(metrics.getCompletedPoints()).isEqualTo(2);
        assertThat(metrics.getCompletedPercentage()).isEqualTo(3);
        assertThat(metrics.getAvgQualityScore()).isEqualTo(4);
        assertThat(metrics.getUndocumentedCount()).isEqualTo(5);
    }
}

