package co.com.bancolombia.usecase.updateworkitem;

import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.List;

@RequiredArgsConstructor
public class UpdateWorkItemUseCase {
    private final UpdateWorkItemRepository repository;

    public Mono<WorkItem> updateWorkItem(String organization, String project, Integer id, List<JsonPatchOperation> patch, String apiVersion) {
        return repository.updateWorkItem(organization, project, id, patch, apiVersion);
    }
}
