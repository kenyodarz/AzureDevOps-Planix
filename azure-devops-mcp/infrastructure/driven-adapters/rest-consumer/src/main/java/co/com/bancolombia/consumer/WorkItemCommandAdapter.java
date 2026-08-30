package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.dto.WorkItemDTO;
import co.com.bancolombia.consumer.mapper.JsonPatchMapper;
import co.com.bancolombia.consumer.mapper.WorkItemMapper;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.gateways.WorkItemCommandPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptador de <b>mutación</b> de work items contra Azure DevOps (D-10, <b>Fase 05</b>).
 *
 * <p>Las dos únicas operaciones que <b>escriben</b> en Azure DevOps quedan aisladas del resto. Es
 * la separación lectura/escritura que pide {@code spring-rules.md} §2 y, en la práctica, la que
 * permite que las cuatro consultas dejen de compartir destino con ellas: hasta la Fase 05 las siete
 * vivían en la misma clase de 191 líneas.
 *
 * <p>Nótese que ambas usan {@code application/json-patch+json}, un {@code contentType} distinto del
 * de todas las demás llamadas del repositorio. Tenerlas juntas y solas hace visible ese detalle,
 * que antes se perdía entre otros cinco métodos.
 *
 * <p><b>Nada de lo que sale por el cable ha cambiado:</b> mismas URLs, mismo {@code contentType},
 * misma versión por defecto y mismo cuerpo JSON, construido con el mismo {@link JsonPatchMapper} de
 * la Fase 03. {@code OutboundPayloadCharacterizationTest} lo compara carácter a carácter.
 * El cortacircuito pasa a llamarse {@code workItemCommand} (decisión <b>B-05</b>); como ninguna
 * instancia estaba declarada en {@code application.yaml} (D-06), el comportamiento es el mismo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemCommandAdapter implements WorkItemCommandPort {

    private static final String CIRCUIT_BREAKER = "workItemCommand";

    private final WebClient client;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WorkItem> createWorkItem(String organization, String project, String type,
            List<JsonPatchOperation> patch, String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, ApiVersions.WORK_ITEM_DEFAULT);
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
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WorkItem> updateWorkItem(String organization, String project, Integer id,
            List<JsonPatchOperation> patch, String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, ApiVersions.WORK_ITEM_DEFAULT);
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
}

