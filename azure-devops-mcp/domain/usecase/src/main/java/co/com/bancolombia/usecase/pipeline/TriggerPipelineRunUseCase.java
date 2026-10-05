package co.com.bancolombia.usecase.pipeline;

import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.gateways.PipelinePort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para disparar la ejecución de un pipeline en Azure DevOps.
 */
@RequiredArgsConstructor
public class TriggerPipelineRunUseCase {

    private static final String DEFAULT_BRANCH = "main";

    private final PipelinePort pipelinePort;

    /**
     * Dispara una corrida de pipeline.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param pipelineId   identificador del pipeline (> 0)
     * @param branch       rama sobre la cual ejecutar (opcional, default "main")
     * @param variables    mapa opcional de variables
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la corrida iniciada
     */
    public Mono<PipelineRun> triggerPipelineRun(
            String organization,
            String project,
            int pipelineId,
            String branch,
            Map<String, String> variables,
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
        String effectiveBranch = (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch;
        return pipelinePort.triggerPipelineRun(
                organization, project, pipelineId, effectiveBranch, variables, apiVersion);
    }
}
