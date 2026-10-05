package co.com.bancolombia.usecase.pipeline;

import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.gateways.PipelinePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para consultar el estado y detalle de una corrida de pipeline en Azure DevOps.
 */
@RequiredArgsConstructor
public class GetPipelineRunUseCase {

    private final PipelinePort pipelinePort;

    /**
     * Consulta la corrida de un pipeline.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param pipelineId   identificador del pipeline (> 0)
     * @param runId        identificador de la corrida (> 0)
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la corrida consultada
     */
    public Mono<PipelineRun> getPipelineRun(
            String organization,
            String project,
            int pipelineId,
            int runId,
            String apiVersion) {
        if (organization == null || organization.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("organization no puede ser nula ni vacía"));
        }
        if (project == null || project.isBlank()) {
            return Mono.error(new IllegalArgumentException("project no puede ser nulo ni vacío"));
        }
        if (pipelineId <= 0) {
            return Mono.error(new IllegalArgumentException("pipelineId debe ser mayor a 0"));
        }
        if (runId <= 0) {
            return Mono.error(new IllegalArgumentException("runId debe ser mayor a 0"));
        }
        return pipelinePort.getPipelineRun(organization, project, pipelineId, runId, apiVersion);
    }
}
