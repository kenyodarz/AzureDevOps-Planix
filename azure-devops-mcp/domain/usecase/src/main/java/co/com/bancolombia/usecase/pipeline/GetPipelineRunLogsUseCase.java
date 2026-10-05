package co.com.bancolombia.usecase.pipeline;

import co.com.bancolombia.model.pipeline.PipelineLogSummary;
import co.com.bancolombia.model.pipeline.gateways.PipelinePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro para extraer y resumir los logs y errores de una corrida de pipeline.
 */
@RequiredArgsConstructor
public class GetPipelineRunLogsUseCase {

    private static final int DEFAULT_MAX_LINES = 100;
    private static final int UPPER_LIMIT_LINES = 500;

    private final PipelinePort pipelinePort;

    /**
     * Consulta el resumen de logs y errores de una corrida.
     *
     * @param organization nombre de la organización
     * @param project      nombre o UUID del proyecto
     * @param pipelineId   identificador del pipeline (> 0)
     * @param runId        identificador de la corrida (> 0)
     * @param onlyErrors   si es true, solo extrae líneas de error (por defecto true)
     * @param maxLines     límite máximo de líneas a recopilar (por defecto 100, max 500)
     * @param apiVersion   versión opcional de la API
     * @return {@link Mono} con el resumen {@link PipelineLogSummary}
     */
    public Mono<PipelineLogSummary> getPipelineRunLogs(
            String organization,
            String project,
            int pipelineId,
            int runId,
            Boolean onlyErrors,
            Integer maxLines,
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
        boolean filterErrors = onlyErrors == null || onlyErrors;
        int limit = (maxLines == null || maxLines <= 0) ? DEFAULT_MAX_LINES : Math.min(maxLines, UPPER_LIMIT_LINES);
        return pipelinePort.getPipelineRunLogs(
                organization, project, pipelineId, runId, filterErrors, limit, apiVersion);
    }
}
