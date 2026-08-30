package co.com.bancolombia.model.getworkitemsbatch.gateways;

import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import reactor.core.publisher.Mono;
import java.util.List;

public interface GetWorkItemsBatchRepository {
    Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project, WorkItemBatchCriteria criteria, String apiVersion);
}
