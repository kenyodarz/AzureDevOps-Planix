package co.com.bancolombia.usecase.team;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.gateways.TeamScopePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetTeamFieldValuesUseCase {

    private final TeamScopePort repository;

    public Mono<TeamFieldValues> getTeamFieldValues(String organization, String project,
            String team) {
        return repository.getTeamFieldValues(organization, project, team);
    }
}
