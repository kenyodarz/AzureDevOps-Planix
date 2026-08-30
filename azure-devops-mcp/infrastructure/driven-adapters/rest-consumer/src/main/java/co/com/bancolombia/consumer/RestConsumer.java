package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.dto.TeamFieldValuesDTO;
import co.com.bancolombia.consumer.dto.TeamIterationsDTO;
import co.com.bancolombia.consumer.dto.WiqlResultDTO;
import co.com.bancolombia.consumer.dto.WorkItemDTO;
import co.com.bancolombia.consumer.dto.WorkItemsBatchResponseDTO;
import co.com.bancolombia.consumer.mapper.JsonPatchMapper;
import co.com.bancolombia.consumer.mapper.TeamMapper;
import co.com.bancolombia.consumer.mapper.WiqlQueryMapper;
import co.com.bancolombia.consumer.mapper.WorkItemBatchMapper;
import co.com.bancolombia.consumer.mapper.WorkItemMapper;
import co.com.bancolombia.model.createworkitem.gateways.CreateWorkItemRepository;
import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.iteration.TeamIteration;
import co.com.bancolombia.model.iteration.gateways.GetTeamIterationsRepository;
import co.com.bancolombia.model.querybywiql.gateways.QueryByWiqlRepository;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.gateways.GetTeamFieldValuesRepository;
import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptador REST hacia Azure DevOps.
 *
 * <p><b>Cambio de la Fase 03 (D-11).</b> Hasta ahora el {@code WebClient} serializaba <b>modelos de
 * dominio</b> directamente hacia el cable: {@code .bodyValue(patch)}, {@code .bodyValue(query)} y
 * {@code .bodyValue(request)}. No existía ni un solo mapper en la frontera de salida, pese a que
 * {@code spring-rules.md} los declara obligatorios en {@code driven-adapters}. Ahora cada cuerpo
 * enviado tiene su DTO en {@code consumer.dto} y su mapper en {@code consumer.mapper}, y los
 * {@code toDomain} privados que antes vivían aquí se movieron a {@code WorkItemMapper} y
 * {@code TeamMapper} <b>sin cambiar una sola línea de su lógica</b>.
 *
 * <p><b>Lo que NO cambió:</b> ninguna URL, ningún parámetro de consulta, ningún {@code contentType},
 * ninguna versión de API por defecto y <b>ni un byte del JSON emitido</b>. {@code RestConsumerTest}
 * es la red de seguridad: compara las peticiones reales contra {@code MockWebServer}.
 *
 * <p>Partir esta clase por agregado es D-10, material de la <b>Fase 05</b>; traducir los errores
 * técnicos a excepciones de dominio es D-13, material de la <b>Fase 06</b>.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestConsumer implements
        GetWorkItemRepository,
        CreateWorkItemRepository,
        UpdateWorkItemRepository,
        QueryByWiqlRepository,
        GetWorkItemsBatchRepository,
        GetTeamFieldValuesRepository,
        GetTeamIterationsRepository {

    private final WebClient client;

    @Override
    @CircuitBreaker(name = "getWorkItem")
    public Mono<WorkItem> getWorkItem(String organization, String project, Integer id, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.1";
        log.info("Fetching Work Item {} | Org: {}, Project: {}, API Version: {}", id, organization, project, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/wit/workItems/{id}?api-version={version}",
                        organization, project, id, version)
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(WorkItemMapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "createWorkItem")
    public Mono<WorkItem> createWorkItem(String organization, String project, String type, List<JsonPatchOperation> patch, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.1";
        String typeParam = type.startsWith("$") ? type : "$" + type;
        log.info("Creating Work Item type: {} | Org: {}, Project: {}, API Version: {}", typeParam, organization, project, version);

        return client.post()
                .uri("/{organization}/{project}/_apis/wit/workitems/{type}?api-version={version}",
                        organization, project, typeParam, version)
                .contentType(MediaType.valueOf("application/json-patch+json"))
                .bodyValue(JsonPatchMapper.toRequest(patch))
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(WorkItemMapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "updateWorkItem")
    public Mono<WorkItem> updateWorkItem(String organization, String project, Integer id, List<JsonPatchOperation> patch, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.1";
        log.info("Updating Work Item: {} | Org: {}, Project: {}, API Version: {}", id, organization, project, version);

        return client.patch()
                .uri("/{organization}/{project}/_apis/wit/workitems/{id}?api-version={version}",
                        organization, project, id, version)
                .contentType(MediaType.valueOf("application/json-patch+json"))
                .bodyValue(JsonPatchMapper.toRequest(patch))
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(WorkItemMapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "queryByWiql")
    public Mono<WiqlResult> queryByWiql(String organization, String project, WiqlQuery query, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.0";
        log.info("Executing WIQL query | Org: {}, Project: {}, API Version: {}", organization, project, version);
        // La sentencia completa se registra aquí porque la capa de dominio no puede escribir en el
        // log: `validateStructure` no admite ninguna dependencia extra en `domain/usecase`, SLF4J
        // incluido. Este es el último punto donde la consulta sigue siendo visible antes de salir
        // por el cable, y es la traza que permitía diagnosticar el tablero vacío.
        log.info("WIQL Query construida: {}", query.getQuery());

        return client.post()
                .uri("/{organization}/{project}/_apis/wit/wiql?api-version={version}",
                        organization, project, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(WiqlQueryMapper.toRequest(query))
                .retrieve()
                .bodyToMono(WiqlResultDTO.class)
                .doOnError(
                        org.springframework.web.reactive.function.client.WebClientResponseException.class,
                        ex ->
                                log.error("❌ Detalle del error de Azure DevOps: {}",
                                        ex.getResponseBodyAsString())
                )
                .map(WorkItemMapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "getWorkItemsBatch")
    public Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project, WorkItemBatchCriteria criteria, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.1";
        log.info("Fetching Work Items Batch for ids size: {} | Org: {}, Project: {}, API Version: {}",
                criteria.getIds() != null ? criteria.getIds().size() : 0, organization, project, version);

        return client.post()
                .uri("/{organization}/{project}/_apis/wit/workitemsbatch?api-version={version}",
                        organization, project, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(WorkItemBatchMapper.toRequest(criteria))
                .retrieve()
                .bodyToMono(WorkItemsBatchResponseDTO.class)
                .map(WorkItemBatchMapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "getTeamFieldValues")
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
    @CircuitBreaker(name = "getTeamIterations")
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
