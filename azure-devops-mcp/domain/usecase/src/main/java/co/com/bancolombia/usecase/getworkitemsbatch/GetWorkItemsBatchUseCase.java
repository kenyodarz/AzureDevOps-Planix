package co.com.bancolombia.usecase.getworkitemsbatch;

import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemsBatchRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.List;

@RequiredArgsConstructor
public class GetWorkItemsBatchUseCase {
    private final GetWorkItemsBatchRepository repository;

    public Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project, WorkItemsBatchRequest request, String apiVersion) {
        return repository.getWorkItemsBatch(organization, project, request, apiVersion);
    }
}
