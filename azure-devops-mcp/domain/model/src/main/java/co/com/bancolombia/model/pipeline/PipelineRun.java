package co.com.bancolombia.model.pipeline;

import lombok.Builder;

/**
 * Entidad de dominio inmutable que representa la ejecución (run/build) de un pipeline.
 *
 * @param id           identificador numérico único de la corrida
 * @param name         nombre o número identificador de la corrida
 * @param status       estado de la ejecución (ej. "inProgress", "completed", "cancelling")
 * @param result       resultado final (ej. "succeeded", "failed", "canceled")
 * @param createdDate  fecha y hora de creación en formato ISO-8601
 * @param finishedDate fecha y hora de finalización en formato ISO-8601
 * @param pipelineId   identificador del pipeline al que pertenece
 * @param pipelineName nombre del pipeline
 * @param sourceBranch rama sobre la que se ejecutó (ej. "refs/heads/main")
 * @param sourceCommit hash SHA del commit evaluado
 * @param webUrl       enlace web para visualizar la ejecución en la interfaz de Azure DevOps
 */
@Builder(toBuilder = true)
public record PipelineRun(
        Integer id,
        String name,
        String status,
        String result,
        String createdDate,
        String finishedDate,
        Integer pipelineId,
        String pipelineName,
        String sourceBranch,
        String sourceCommit,
        String webUrl) {
}
