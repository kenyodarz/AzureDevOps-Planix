package co.com.bancolombia.model.getworkitem.gateways;

import co.com.bancolombia.model.workitem.WorkItem;
import reactor.core.publisher.Mono;

public interface GetWorkItemRepository {
    Mono<WorkItem> getWorkItem(String organization, String project, Integer id, String apiVersion);
}
