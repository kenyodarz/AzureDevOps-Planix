package co.com.bancolombia.model.workitem;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

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
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkItemBatchCriteria {
    private List<Integer> ids;
    private List<String> fields;
    private String expand;
    private String errorPolicy;
}

