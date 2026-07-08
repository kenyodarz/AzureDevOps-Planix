package co.com.bancolombia.model.updateworkitem.gateways;

import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import reactor.core.publisher.Mono;
import java.util.List;

public interface UpdateWorkItemRepository {
    Mono<WorkItem> updateWorkItem(String organization, String project, Integer id, List<JsonPatchOperation> patch, String apiVersion);
}
