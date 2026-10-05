package co.com.bancolombia.usecase.overview;

import co.com.bancolombia.model.overview.ProjectOverview;
import co.com.bancolombia.model.overview.gateways.OverviewPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para consultar la visión general y metadatos de un proyecto en Azure DevOps.
 */
@RequiredArgsConstructor
public class GetProjectOverviewUseCase {

    private final OverviewPort overviewPort;

    /**
     * Consulta los metadatos y estado del proyecto.
     *
     * @param organization nombre de la organización en Azure DevOps
     * @param project      nombre o UUID del proyecto
     * @param apiVersion    versión opcional de la API
     * @return {@link Mono} con la entidad {@link ProjectOverview}
     */
    public Mono<ProjectOverview> getProjectOverview(
            String organization,
            String project,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Mono.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        return overviewPort.getProjectOverview(organization, project, apiVersion);
    }
}
