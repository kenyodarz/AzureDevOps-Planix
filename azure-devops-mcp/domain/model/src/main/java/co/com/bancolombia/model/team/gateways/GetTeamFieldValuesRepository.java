package co.com.bancolombia.model.team.gateways;

import co.com.bancolombia.model.team.TeamFieldValues;
import reactor.core.publisher.Mono;

public interface GetTeamFieldValuesRepository {

    Mono<TeamFieldValues> getTeamFieldValues(String organization, String project, String team);
}
