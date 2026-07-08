package co.com.bancolombia.consumer;

import co.com.bancolombia.model.createworkitem.gateways.CreateWorkItemRepository;
import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.querybywiql.gateways.QueryByWiqlRepository;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.gateways.GetTeamFieldValuesRepository;
import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemReference;
import co.com.bancolombia.model.workitem.WorkItemRelation;
import co.com.bancolombia.model.workitem.WorkItemsBatchRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestConsumer implements
        GetWorkItemRepository,
        CreateWorkItemRepository,
        UpdateWorkItemRepository,
        QueryByWiqlRepository,
        GetWorkItemsBatchRepository,
        GetTeamFieldValuesRepository {

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
                .map(this::toDomain);
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
                .bodyValue(patch)
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(this::toDomain);
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
                .bodyValue(patch)
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(this::toDomain);
    }

    @Override
    @CircuitBreaker(name = "queryByWiql")
    public Mono<WiqlResult> queryByWiql(String organization, String project, WiqlQuery query, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.0";
        log.info("Executing WIQL query | Org: {}, Project: {}, API Version: {}", organization, project, version);

        return client.post()
                .uri("/{organization}/{project}/_apis/wit/wiql?api-version={version}",
                        organization, project, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(query)
                .retrieve()
                .bodyToMono(WiqlResultDTO.class)
                .doOnError(
                        org.springframework.web.reactive.function.client.WebClientResponseException.class,
                        ex ->
                                log.error("❌ Detalle del error de Azure DevOps: {}",
                                        ex.getResponseBodyAsString())
                )
                .map(this::toDomain);
    }

    @Override
    @CircuitBreaker(name = "getWorkItemsBatch")
    public Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project, WorkItemsBatchRequest request, String apiVersion) {
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.1";
        log.info("Fetching Work Items Batch for ids size: {} | Org: {}, Project: {}, API Version: {}", 
                request.getIds() != null ? request.getIds().size() : 0, organization, project, version);

        return client.post()
                .uri("/{organization}/{project}/_apis/wit/workitemsbatch?api-version={version}",
                        organization, project, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WorkItemsBatchResponseDTO.class)
                .map(response -> response.getValue() != null 
                        ? response.getValue().stream().map(this::toDomain).toList()
                        : List.of());
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
                .map(this::toDomain);
    }

    // --- MAPPING METHODS ---

    private TeamFieldValues toDomain(TeamFieldValuesDTO dto) {
        if (dto == null) {
            return null;
        }
        return TeamFieldValues.builder()
                .defaultValue(dto.getDefaultValue())
                .values(dto.getValues() != null
                        ? dto.getValues().stream().map(TeamFieldValueDTO::getValue).toList()
                        : List.of())
                .build();
    }

    private WorkItem toDomain(WorkItemDTO dto) {
        if (dto == null) return null;
        return WorkItem.builder()
                .id(dto.getId())
                .rev(dto.getRev())
                .fields(dto.getFields())
                .relations(dto.getRelations() != null 
                        ? dto.getRelations().stream().map(this::toDomain).toList()
                        : List.of())
                .url(dto.getUrl())
                .build();
    }

    private WorkItemRelation toDomain(WorkItemRelationDTO dto) {
        if (dto == null) return null;
        return WorkItemRelation.builder()
                .rel(dto.getRel())
                .url(dto.getUrl())
                .attributes(dto.getAttributes())
                .build();
    }

    private WiqlResult toDomain(WiqlResultDTO dto) {
        if (dto == null) return null;
        return WiqlResult.builder()
                .queryType(dto.getQueryType())
                .queryResultType(dto.getQueryResultType())
                .asOf(dto.getAsOf())
                .workItems(dto.getWorkItems() != null 
                        ? dto.getWorkItems().stream().map(this::toDomain).toList()
                        : List.of())
                .build();
    }

    private WorkItemReference toDomain(WorkItemReferenceDTO dto) {
        if (dto == null) return null;
        return WorkItemReference.builder()
                .id(dto.getId())
                .url(dto.getUrl())
                .build();
    }
}
