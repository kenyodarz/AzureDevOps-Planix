package co.com.bancolombia.mcp.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterio de consulta en lote tal y como se arma en la <b>frontera de entrada</b> del servidor MCP.
 *
 * <p>Sustituye al antiguo {@code co.com.bancolombia.model.workitem.WorkItemsBatchRequest}, que vivía
 * en {@code domain/model} y era la única violación real de la regla de arquitectura
 * {@code Rule_2.2} (D-17): un <i>request</i> HTTP alojado en el dominio. Por <b>DP-03</b> la clase
 * de dominio pasó a llamarse {@code WorkItemBatchCriteria} y el cable se quedó con este DTO.
 *
 * <p>⚠️ <b>Los nombres de campo son contrato público</b> ({@code CONTRATO-MCP.md} §3): {@code ids},
 * {@code fields}, {@code expand} y {@code errorPolicy} se conservan <b>literalmente</b>. Los
 * parámetros de la tool {@code getWorkItemsBatch} tampoco cambian: se siguen recibiendo sueltos y
 * es el entry-point quien los agrupa aquí, igual que antes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkItemsBatchInput {
    private List<Integer> ids;
    private List<String> fields;
    private String expand;
    private String errorPolicy;
}

