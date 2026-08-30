package co.com.bancolombia.mcp.dto;

import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemReference;
import co.com.bancolombia.model.workitem.WorkItemRelation;
import java.util.List;

/**
 * Mapper de la <b>frontera de salida hacia el cliente MCP</b>: traduce los modelos de dominio a los
 * DTOs de respuesta que Jackson serializa en el protocolo.
 *
 * <p><b>Es la mitad que faltaba.</b> La Fase 03 interpuso {@link McpToolDtoMapper} en la entrada y
 * cinco mappers en la salida hacia Azure DevOps, pero dejó el <b>retorno</b> sin frontera: cuatro
 * modelos de {@code domain/model} se serializaban tal cual hacia el cliente
 * ({@code CONTRATO-MCP.md} §3.4, «queda anotada para la Fase 07»). Ese momento es éste, y lo
 * autorizó <b>DP-07 §0.2(a)</b>.
 *
 * <p><b>Qué gana el dominio con esto.</b> Mientras el modelo <i>era</i> el contrato de respuesta, no
 * podía enriquecerse sin arriesgar el JSON: convertirlo en {@code record} habría renombrado los
 * accesores y, con ellos, los nombres de campo emitidos. Con esta frontera en medio, la forma del
 * dominio y la forma del cable son <b>independientes</b>, que es exactamente lo que pide DP-03.
 *
 * <p>⚠️ <b>Traducción y nada más.</b> Sin Spring, sin lógica de negocio y —deliberadamente— <b>sin
 * normalizar los nulos</b>: si {@code relations} llega nulo, sale nulo. Convertirlo en lista vacía
 * sería más limpio, pero cambiaría el JSON que ve el cliente, y eso no lo decide un refactor
 * (mismo criterio que el javadoc de {@code TeamScope} aplicó a las cadenas en blanco).
 */
public final class McpResponseMapper {

    private McpResponseMapper() {
        // Mapper estático: no se instancia.
    }

    public static WorkItemResponse toResponse(WorkItem workItem) {
        if (workItem == null) {
            return null;
        }
        return new WorkItemResponse(
                workItem.id(),
                workItem.rev(),
                workItem.fields(),
                toRelationResponses(workItem.relations()),
                workItem.url());
    }

    public static List<WorkItemResponse> toResponses(List<WorkItem> workItems) {
        if (workItems == null) {
            return null;
        }
        return workItems.stream().map(McpResponseMapper::toResponse).toList();
    }

    public static WorkItemRelationResponse toResponse(WorkItemRelation relation) {
        if (relation == null) {
            return null;
        }
        return new WorkItemRelationResponse(relation.rel(), relation.url(), relation.attributes());
    }

    public static WiqlResultResponse toResponse(WiqlResult result) {
        if (result == null) {
            return null;
        }
        return new WiqlResultResponse(
                result.queryType(),
                result.queryResultType(),
                result.asOf(),
                toReferenceResponses(result.workItems()));
    }

    public static WorkItemReferenceResponse toResponse(WorkItemReference reference) {
        if (reference == null) {
            return null;
        }
        return new WorkItemReferenceResponse(reference.id(), reference.url());
    }

    private static List<WorkItemRelationResponse> toRelationResponses(List<WorkItemRelation> relations) {
        if (relations == null) {
            return null;
        }
        return relations.stream().map(McpResponseMapper::toResponse).toList();
    }

    private static List<WorkItemReferenceResponse> toReferenceResponses(List<WorkItemReference> references) {
        if (references == null) {
            return null;
        }
        return references.stream().map(McpResponseMapper::toResponse).toList();
    }
}

