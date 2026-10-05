package co.com.bancolombia.model.pipeline;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.List;
import lombok.Builder;

/**
 * Objeto de valor inmutable que encapsula el resumen de logs y errores de una ejecución de pipeline.
 *
 * @param runId        identificador de la corrida
 * @param hasErrors    indica si se detectaron errores críticos en los logs
 * @param errorCount   número de líneas o bloques de error detectados
 * @param warningCount número de advertencias detectadas
 * @param errorLines   líneas filtradas con el detalle de errores y advertencias
 */
@Builder(toBuilder = true)
public record PipelineLogSummary(
        Integer runId,
        boolean hasErrors,
        int errorCount,
        int warningCount,
        List<String> errorLines) {

    public PipelineLogSummary {
        errorLines = DomainCollections.immutableCopy(errorLines);
    }
}
