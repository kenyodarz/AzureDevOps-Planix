package co.com.bancolombia.model.overview.gateways;

import co.com.bancolombia.model.overview.ProjectOverview;
import reactor.core.publisher.Mono;

/**
 * Puerto gateway para consultar información y visión general de proyectos en Azure DevOps.
 */
public interface OverviewPort {

    /**
     * Consulta los detalles y metadatos de un proyecto en Azure DevOps.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la entidad {@link ProjectOverview}
     */
    Mono<ProjectOverview> getProjectOverview(String organization, String project, String apiVersion);
}
