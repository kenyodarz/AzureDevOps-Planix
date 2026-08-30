package co.com.bancolombia.usecase.createworkitem;

import co.com.bancolombia.model.workitem.gateways.WorkItemCommandPort;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.List;

@RequiredArgsConstructor
public class CreateWorkItemUseCase {
    private final WorkItemCommandPort repository;

    public Mono<WorkItem> createWorkItem(String organization, String project, String type, List<JsonPatchOperation> patch, String apiVersion) {
        return repository.createWorkItem(organization, project, type, patch, apiVersion);
    }
}
