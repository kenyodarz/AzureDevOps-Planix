package co.com.bancolombia.model.getworkitemsbatch.gateways;

import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemsBatchRequest;
import reactor.core.publisher.Mono;
import java.util.List;

public interface GetWorkItemsBatchRepository {
    Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project, WorkItemsBatchRequest request, String apiVersion);
}
