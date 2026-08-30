package co.com.bancolombia.model.workitem;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.List;
import lombok.Builder;

/**
 * Resultado de una consulta WIQL: los metadatos de la consulta y las referencias encontradas.
 *
 * <p><b>Es el retorno de {@code listWorkItemsByTeamAndSprint}, la tool más usada del sistema</b>, y
 * por eso la clase más delicada de la Fase 07: hasta ahora se serializaba tal cual hacia el cliente
 * MCP ({@code CONTRATO-MCP.md} §3.4). La frontera de salida que introdujo DP-07 §0.2(a)
 * —{@code WiqlResultResponse} y {@code McpResponseMapper}— es lo que permite que aquí ya no haya
 * ningún compromiso con el formato del cable.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> {@code workItems} se copia de forma
 * defensiva; una lista nula se conserva nula, para no alterar el JSON de un resultado vacío.
 */
@Builder(toBuilder = true)
public record WiqlResult(
        String queryType,
        String queryResultType,
        String asOf,
        List<WorkItemReference> workItems) {

    public WiqlResult {
        workItems = DomainCollections.immutableCopy(workItems);
    }
}
