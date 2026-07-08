package co.com.bancolombia.usecase.getworkitem;

import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.workitem.WorkItem;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetWorkItemUseCase {
    private final GetWorkItemRepository repository;

    public Mono<WorkItem> getWorkItem(String organization, String project, Integer id, String apiVersion) {
        return repository.getWorkItem(organization, project, id, apiVersion);
    }
}
