package co.com.bancolombia.model.createworkitem.gateways;

import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import reactor.core.publisher.Mono;
import java.util.List;

public interface CreateWorkItemRepository {
    Mono<WorkItem> createWorkItem(String organization, String project, String type, List<JsonPatchOperation> patch, String apiVersion);
}
