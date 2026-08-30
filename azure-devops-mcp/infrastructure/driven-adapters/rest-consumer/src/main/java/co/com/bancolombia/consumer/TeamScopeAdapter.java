package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.config.AzureDevOpsAdapterProperties;
import co.com.bancolombia.consumer.dto.TeamFieldValuesDTO;
import co.com.bancolombia.consumer.dto.TeamIterationsDTO;
import co.com.bancolombia.consumer.mapper.TeamMapper;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.TeamIteration;
import co.com.bancolombia.model.team.gateways.TeamScopePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * parámetro que lo sobreescriba, exactamente igual que antes. Por eso no usan {@link ApiVersions} ni
 * las versiones configurables de {@link AzureDevOpsAdapterProperties}: <b>B-10 decidió NO
 * parametrizarlas</b>, porque hacerlo habría cambiado dos llamadas HTTP y la regla innegociable de
 * esta fase es que no cambia ninguna.
 *
 * <h2>Fase 06: estas dos consultas son el caso especial de la traducción de errores</h2>
 *
 * <p>Los dos métodos traducen sus fallos con {@link AzureDevOpsErrorTranslator}, el <b>mismo</b>
 * traductor que los otros dos adaptadores. Pero aquí la excepción de dominio <b>casi nunca llega al
 * cliente</b>: {@code ResolveTeamScopeUseCase} la captura con {@code onErrorResume} y cae al
 * repliegue por concatenación que <b>DP-04</b> conservó, con su {@code WARN} y su contador
 * {@code azuredevops.teamscope.fallback}. <b>DP-06 §0.1(d)</b> ratificó ese reparto: la tool falla
 * ante un error, <b>pero el repliegue de rutas se respeta intacto</b>, porque convertir un tablero
 * vacío en un error es contrato de facto y lo decide el propietario, no un refactor.
 *
 * <p>Traducir aquí sigue mereciendo la pena por dos motivos: cuando estas consultas se invocan
 * fuera del flujo compuesto —a través de sus casos de uso— el error sí viaja, y en todos los casos
 * el cuerpo original de Azure DevOps queda registrado en {@code ERROR}, que es lo que permite saber
 * <b>por qué</b> se disparó el repliegue y no solo <b>cuántas veces</b>.
 *
 * <p>Su timeout por operación es <b>el más corto de los cuatro</b> (D-24) precisamente porque su
 * fallo no rompe nada: esperar más solo retrasaría el repliegue.
 */
@Slf4j
@Service
public class TeamScopeAdapter implements TeamScopePort {

    private static final String CIRCUIT_BREAKER = "teamScope";

    private final WebClient client;
    private final AzureDevOpsAdapterProperties properties;

    @Autowired
    public TeamScopeAdapter(WebClient client, AzureDevOpsAdapterProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Constructor de conveniencia con los valores por defecto, para que las pruebas heredadas de la
     * Fase 05 sigan compilando sin arrastrar el contexto de Spring.
     */
    public TeamScopeAdapter(WebClient client) {
        this(client, AzureDevOpsAdapterProperties.defaults());
    }

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
                .map(TeamMapper::toDomain)
                .timeout(properties.operationTimeout().teamScope())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getTeamFieldValues"));
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
                .map(TeamMapper::toDomain)
                .timeout(properties.operationTimeout().teamScope())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getTeamIterations"));
    }
}
