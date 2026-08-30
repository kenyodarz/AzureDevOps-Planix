package co.com.bancolombia.model.workitem.gateways;

import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WorkItem;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Lado de <b>mutación</b> del agregado {@code WorkItem} (D-14, decisión <b>DP-05 opción (a)</b>).
 *
 * <p>Funde {@code CreateWorkItemRepository} y {@code UpdateWorkItemRepository}, que vivían en dos
 * paquetes nombrados por operación CRUD. La separación respecto de {@link WorkItemQueryPort} <b>no
 * es cosmética</b>: es la separación lectura/escritura que pide {@code spring-rules.md} §2 (CQS) y
 * la que permite que el adaptador de escritura se pruebe, se despliegue y —cuando llegue la Fase
 * 06— se proteja con un cortacircuito propio, sin arrastrar los flujos de consulta.
 *
 * <p><b>Ninguna firma cambió al fundirlas.</b>
 */
public interface WorkItemCommandPort {

    Mono<WorkItem> createWorkItem(String organization, String project, String type,
            List<JsonPatchOperation> patch, String apiVersion);

    Mono<WorkItem> updateWorkItem(String organization, String project, Integer id,
            List<JsonPatchOperation> patch, String apiVersion);
}

