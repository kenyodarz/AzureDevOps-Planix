package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.config.AzureDevOpsAdapterProperties;
import co.com.bancolombia.consumer.dto.WorkItemDTO;
import co.com.bancolombia.consumer.mapper.JsonPatchMapper;
import co.com.bancolombia.consumer.mapper.WorkItemMapper;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.gateways.WorkItemCommandPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 *
 * <p><b>Fase 06.</b> Las dos operaciones terminan ahora en {@link AzureDevOpsErrorTranslator} —
 * <b>el mismo</b> traductor que usan los otros dos adaptadores, no una copia— y llevan su propio
 * timeout reactivo (D-24). El cortacircuito {@code workItemCommand}, cuyo nombre fijó B-05, tiene
 * por fin umbrales declarados en {@code application.yaml} (D-06).
 *
 * <p>⚠️ <b>Por qué el timeout de escritura importa más que el de lectura.</b> Un timeout en una
 * consulta solo pierde tiempo; un timeout en un {@code createWorkItem} deja al llamante <b>sin
 * saber si el work item se creó o no</b>. Por eso su presupuesto es holgado y deliberado, no el
 * residuo de un valor compartido con las otras cinco llamadas.
 */
@Slf4j
@Service
public class WorkItemCommandAdapter implements WorkItemCommandPort {

    private static final String CIRCUIT_BREAKER = "workItemCommand";

    private final WebClient client;
    private final AzureDevOpsAdapterProperties properties;

    @Autowired
    public WorkItemCommandAdapter(WebClient client, AzureDevOpsAdapterProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Constructor de conveniencia con los valores por defecto, para que las pruebas heredadas de la
     * Fase 05 sigan compilando <b>sin modificarse</b>.
     */
    public WorkItemCommandAdapter(WebClient client) {
        this(client, AzureDevOpsAdapterProperties.defaults());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WorkItem> createWorkItem(String organization, String project, String type,
            List<JsonPatchOperation> patch, String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().workItem());
        String typeParam = type.startsWith("$") ? type : "$" + type;
        log.info("Creating Work Item type: {} | Org: {}, Project: {}, API Version: {}", typeParam, organization, project, version);

        return client.post()
                .uri("/{organization}/{project}/_apis/wit/workitems/{type}?api-version={version}",
                        organization, project, typeParam, version)
                .contentType(MediaType.valueOf("application/json-patch+json"))
                .bodyValue(JsonPatchMapper.toRequest(patch))
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(WorkItemMapper::toDomain)
                .timeout(properties.operationTimeout().command())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("createWorkItem"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WorkItem> updateWorkItem(String organization, String project, Integer id,
            List<JsonPatchOperation> patch, String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().workItem());
        log.info("Updating Work Item: {} | Org: {}, Project: {}, API Version: {}", id, organization, project, version);

        return client.patch()
                .uri("/{organization}/{project}/_apis/wit/workitems/{id}?api-version={version}",
                        organization, project, id, version)
                .contentType(MediaType.valueOf("application/json-patch+json"))
                .bodyValue(JsonPatchMapper.toRequest(patch))
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(WorkItemMapper::toDomain)
                .timeout(properties.operationTimeout().command())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("updateWorkItem"));
    }
}
