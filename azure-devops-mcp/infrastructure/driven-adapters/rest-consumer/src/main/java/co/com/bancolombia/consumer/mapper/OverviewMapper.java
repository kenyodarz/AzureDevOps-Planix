package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.ProjectOverviewDTO;
import co.com.bancolombia.model.overview.ProjectOverview;

/**
 * Mapper para transformar DTOs del proyecto de Azure DevOps a entidades de dominio {@link ProjectOverview}.
 */
public final class OverviewMapper {

    private OverviewMapper() {
    }

    public static ProjectOverview toDomain(ProjectOverviewDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProjectOverview.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .state(dto.getState())
                .visibility(dto.getVisibility())
                .url(dto.getUrl())
                .build();
    }
}
