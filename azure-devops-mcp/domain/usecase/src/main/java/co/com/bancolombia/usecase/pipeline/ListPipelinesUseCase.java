package co.com.bancolombia.usecase.pipeline;

import co.com.bancolombia.model.pipeline.PipelineSummary;
import co.com.bancolombia.model.pipeline.gateways.PipelinePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para listar los pipelines disponibles en un proyecto de Azure DevOps.
 */
@RequiredArgsConstructor
public class ListPipelinesUseCase {

    private static final int DEFAULT_TOP = 10;
    private static final int MAX_TOP = 50;

    private final PipelinePort pipelinePort;

    /**
     * Lista los pipelines existentes.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param top          número máximo de pipelines a retornar
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la lista de pipelines
     */
    public Mono<List<PipelineSummary>> listPipelines(
            String organization,
            String project,
            Integer top,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Mono.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        int effectiveTop = (top == null || top <= 0) ? DEFAULT_TOP : Math.min(top, MAX_TOP);
        return pipelinePort.listPipelines(organization, project, effectiveTop, apiVersion)
                .collectList();
    }
}
