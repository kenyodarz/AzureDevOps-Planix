package co.com.bancolombia.model.querybywiql.gateways;

import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import reactor.core.publisher.Mono;

public interface QueryByWiqlRepository {
    Mono<WiqlResult> queryByWiql(String organization, String project, WiqlQuery query, String apiVersion);
}
