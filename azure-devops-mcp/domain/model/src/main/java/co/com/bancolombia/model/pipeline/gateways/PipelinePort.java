package co.com.bancolombia.model.pipeline.gateways;

import co.com.bancolombia.model.pipeline.PipelineLogSummary;
import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.PipelineSummary;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto gateway para interactuar con Pipelines (Azure Pipelines / Builds) en Azure DevOps.
 */
public interface PipelinePort {

    /**
     * Lista los pipelines disponibles en el proyecto.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param top          número máximo de pipelines a retornar
     * @param apiVersion   versión opcional de la API
     * @return {@link Flux} con los pipelines encontrados
     */
    Flux<PipelineSummary> listPipelines(
            String organization,
            String project,
            int top,
            String apiVersion);

    /**
     * Obtiene el estado y detalle de una corrida específica de un pipeline.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param pipelineId   identificador del pipeline
     * @param runId        identificador numérico de la corrida
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la corrida del pipeline
     */
    Mono<PipelineRun> getPipelineRun(
            String organization,
            String project,
            int pipelineId,
            int runId,
            String apiVersion);

    /**
     * Obtiene y resume los logs de una corrida, filtrando errores y advertencias.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param pipelineId   identificador del pipeline
     * @param runId        identificador numérico de la corrida
     * @param onlyErrors   si es true, extrae exclusivamente líneas con errores
     * @param maxLines     límite máximo de líneas a recopilar para evitar desbordar el contexto
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con el resumen de logs
     */
    Mono<PipelineLogSummary> getPipelineRunLogs(
            String organization,
            String project,
            int pipelineId,
            int runId,
            boolean onlyErrors,
            int maxLines,
            String apiVersion);

    /**
     * Dispara una nueva ejecución de un pipeline.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param pipelineId   identificador del pipeline a disparar
     * @param branch       rama sobre la cual ejecutar (ej. "main" o "refs/heads/main")
     * @param variables    mapa opcional de variables de ejecución
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con la corrida creada
     */
    Mono<PipelineRun> triggerPipelineRun(
            String organization,
            String project,
            int pipelineId,
            String branch,
            Map<String, String> variables,
            String apiVersion);
}
