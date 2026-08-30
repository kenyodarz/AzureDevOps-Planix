package co.com.bancolombia.model.iteration.gateways;

import co.com.bancolombia.model.iteration.TeamIteration;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Puerto para consultar las iteraciones configuradas de un equipo en Azure DevOps.
 *
 * <p>Simétrico a {@code GetTeamFieldValuesRepository}, que es el que resuelve el {@code AreaPath}.
 */
public interface GetTeamIterationsRepository {

    Mono<List<TeamIteration>> getTeamIterations(String organization, String project, String team);
}

