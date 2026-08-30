package co.com.bancolombia.model.workitem;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Elemento de trabajo de Azure DevOps.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> Antes era una clase con
 * {@code @Setter} en sus cinco campos y <b>cero invariantes</b>: cualquiera podía vaciar un
 * {@code WorkItem} a mitad de un flujo reactivo y nada lo impedía. Eso es el modelo anémico que
 * {@code spring-rules.md} §1 prohíbe expresamente.
 *
 * <p><b>Por qué ahora sí se puede ser un {@code record}.</b> Hasta esta fase esta clase <b>era</b> el
 * contrato de respuesta de cuatro tools: Jackson la serializaba tal cual hacia el cliente MCP
 * ({@code CONTRATO-MCP.md} §3.4). Convertirla en {@code record} habría renombrado los accesores
 * ({@code getId()} → {@code id()}) y, con ellos, los nombres de campo del JSON, <b>en silencio</b>.
 * DP-07 §0.2(a) cerró antes esa frontera con {@code WorkItemResponse} y {@code McpResponseMapper},
 * de modo que la forma del dominio y la del cable ya son independientes.
 *
 * <p><b>Invariantes — DP-07 §0.2(c).</b> Las colecciones se copian de forma defensiva y quedan
 * inmodificables, preservando su orden. <b>No se rechaza ningún valor</b>: ni un {@code id} nulo ni
 * un {@code fields} vacío, porque Azure DevOps puede devolverlos y convertir eso en un error sería
 * el mismo cambio de comportamiento observable que DP-04 §0.1 descartó — el precedente que dejó
 * escrito el javadoc de {@link TeamScope}.
 *
 * <p><b>{@code fields} sigue siendo un mapa abierto — DP-07 §0.2(d).</b> Es lo más anémico que hay
 * aquí, pero es también lo que permite que el cliente pida cualquier campo de Azure DevOps sin
 * tocar el servidor. Lo que sí gana es que ya <b>no se puede mutar</b>.
 */
@Builder(toBuilder = true)
public record WorkItem(
        Integer id,
        Integer rev,
        Map<String, Object> fields,
        List<WorkItemRelation> relations,
        String url) {

    public WorkItem {
        fields = DomainCollections.immutableCopy(fields);
        relations = DomainCollections.immutableCopy(relations);
    }
}
