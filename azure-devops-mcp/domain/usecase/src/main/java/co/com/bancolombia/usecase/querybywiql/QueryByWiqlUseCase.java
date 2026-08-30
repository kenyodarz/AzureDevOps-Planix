package co.com.bancolombia.usecase.querybywiql;

import co.com.bancolombia.model.workitem.gateways.WorkItemQueryPort;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class QueryByWiqlUseCase {
    private final WorkItemQueryPort repository;

    public Mono<WiqlResult> queryByWiql(String organization, String project, WiqlQuery query, String apiVersion) {
        return repository.queryByWiql(organization, project, query, apiVersion);
    }
}
