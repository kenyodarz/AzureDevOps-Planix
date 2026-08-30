package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.dto.TeamFieldValuesDTO;
import co.com.bancolombia.consumer.dto.TeamIterationsDTO;
import co.com.bancolombia.consumer.mapper.TeamMapper;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.TeamIteration;
import co.com.bancolombia.model.team.gateways.TeamScopePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptador del <b>ámbito de equipo</b> en Azure DevOps: {@code AreaPath} e iteraciones
 * (D-10, <b>Fase 05</b>).
 *
 * <p>Son las <b>dos mitades de la misma pregunta</b> —dónde vive el trabajo de esta célula y en qué
 * carpeta vive este sprint— y hasta la Fase 05 estaban repartidas entre dos gateways alojados en
 * dos paquetes distintos del dominio, implementados por una clase que además hacía otras cinco
 * cosas. Son también las dos consultas de las que depende {@code ResolveTeamScopeUseCase} y, por
 * tanto, las que sostienen el arreglo del tablero vacío (D-09): tenerlas juntas y aisladas es lo
 * que permite probarlas sin arrastrar ningún flujo de work items.
 *
 * <p><b>Las dos URLs llevan {@code api-version=7.0} escrito literalmente en la ruta</b>, sin
 * parámetro que lo sobreescriba, exactamente igual que antes: esta fase no cambia ni una llamada
 * HTTP. Por eso no usan {@link ApiVersions}. Tipar y configurar las versiones es <b>D-18</b>,
 * material de la <b>Fase 06</b>.
 *
 * <p>El cortacircuito pasa a llamarse {@code teamScope} (decisión <b>B-05</b>); como ninguna
 * instancia estaba declarada en {@code application.yaml} (D-06), el comportamiento no cambia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamScopeAdapter implements TeamScopePort {

    private static final String CIRCUIT_BREAKER = "teamScope";

    private final WebClient client;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<TeamFieldValues> getTeamFieldValues(String organization, String project,
            String team) {
        log.info("Fetching Team Field Values | Org: {}, Project: {}, Team: {}", organization,
                project, team);
        return client.get()
                .uri("/{organization}/{project}/{team}/_apis/work/teamsettings/teamfieldvalues?api-version=7.0",
                        organization, project, team)
                .retrieve()
                .bodyToMono(TeamFieldValuesDTO.class)
                .map(TeamMapper::toDomain);
    }

    /**
     * Iteraciones configuradas del equipo. Es la consulta gemela de
     * {@link #getTeamFieldValues(String, String, String)}: la que devuelve el {@code IterationPath}
     * real en lugar de obligar a fabricarlo con el año del calendario.
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<List<TeamIteration>> getTeamIterations(String organization, String project,
            String team) {
        log.info("Fetching Team Iterations | Org: {}, Project: {}, Team: {}", organization,
                project, team);
        return client.get()
                .uri("/{organization}/{project}/{team}/_apis/work/teamsettings/iterations?api-version=7.0",
                        organization, project, team)
                .retrieve()
                .bodyToMono(TeamIterationsDTO.class)
                .map(TeamMapper::toDomain);
    }
}

