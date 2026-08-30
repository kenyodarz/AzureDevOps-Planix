package co.com.bancolombia.model.workitem.gateways;

import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Lado de <b>consulta</b> del agregado {@code WorkItem} (D-14, decisión <b>DP-05 opción (a)</b>).
 *
 * <p>Hasta la Fase 05 este contrato estaba repartido en <b>tres interfaces de una sola operación</b>
 * —{@code GetWorkItemRepository}, {@code GetWorkItemsBatchRepository} y
 * {@code QueryByWiqlRepository}— alojadas en <b>tres paquetes distintos</b> nombrados por operación
 * CRUD ({@code getworkitem}, {@code getworkitemsbatch}, {@code querybywiql}). Eran siete paquetes y
 * siete puertos para <b>un solo agregado</b>: lo contrario de lo que pide {@code spring-rules.md}
 * §2, que agrupa por agregado y separa por responsabilidad.
 *
 * <p><b>Ninguna firma cambió al fundirlas.</b> Los nombres de método, el orden de los parámetros y
 * los tipos son literalmente los que había; lo único que cambió es en qué interfaz viven. Esa
 * disciplina es la que permite que los casos de uso solo cambien su import.
 *
 * @see WorkItemCommandPort el lado de escritura, separado a propósito (CQS)
 */
public interface WorkItemQueryPort {

    Mono<WorkItem> getWorkItem(String organization, String project, Integer id, String apiVersion);

    Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project,
            WorkItemBatchCriteria criteria, String apiVersion);

    Mono<WiqlResult> queryByWiql(String organization, String project, WiqlQuery query,
            String apiVersion);
}

