package co.com.bancolombia.usecase.getworkitemsbatch;

import co.com.bancolombia.model.workitem.gateways.WorkItemQueryPort;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.List;

@RequiredArgsConstructor
public class GetWorkItemsBatchUseCase {
    private final WorkItemQueryPort repository;

    public Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project, WorkItemBatchCriteria criteria, String apiVersion) {
        return repository.getWorkItemsBatch(organization, project, criteria, apiVersion);
    }
}
