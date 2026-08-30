package co.com.bancolombia.model.workitem;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.List;
import lombok.Builder;

/**
 * Criterio de consulta en lote de Work Items, expresado en términos del dominio.
 *
 * <p><b>Antes se llamaba {@code WorkItemsBatchRequest}</b> y era la única violación real de la regla
 * de arquitectura {@code Rule_2.2} (D-17): un sufijo {@code Request} dentro de {@code domain/model}
 * delataba que el dominio estaba haciendo de contrato de cable HTTP. Desde la Fase 03 el cable tiene
 * sus propios tipos —{@code WorkItemsBatchInput} en el entry-point y {@code WorkItemsBatchRequestDTO}
 * en el adaptador— y esta clase expresa únicamente <b>qué</b> se quiere consultar, no <b>cómo</b> se
 * transporta.
 *
 * <p>Los nombres de campo se conservan literalmente porque son los que viajan por el cable en ambos
 * extremos ({@code CONTRATO-MCP.md} §3): renombrar la clase es seguro, renombrar un campo no lo es.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> {@code ids} y {@code fields} se
 * copian de forma defensiva. <b>Deliberadamente no se exige que {@code ids} traiga elementos:</b> un
 * lote vacío hoy llega a Azure DevOps y vuelve como una lista vacía, y convertir eso en un error
 * sería un cambio de comportamiento observable que DP-07 §0.2(c) no autorizó.
 */
@Builder(toBuilder = true)
public record WorkItemBatchCriteria(
        List<Integer> ids,
        List<String> fields,
        String expand,
        String errorPolicy) {

    public WorkItemBatchCriteria {
        ids = DomainCollections.immutableCopy(ids);
        fields = DomainCollections.immutableCopy(fields);
    }
}
