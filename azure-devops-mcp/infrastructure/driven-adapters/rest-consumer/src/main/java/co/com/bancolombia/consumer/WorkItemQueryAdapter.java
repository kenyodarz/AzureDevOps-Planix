package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.dto.WiqlResultDTO;
import co.com.bancolombia.consumer.dto.WorkItemDTO;
import co.com.bancolombia.consumer.dto.WorkItemsBatchResponseDTO;
import co.com.bancolombia.consumer.mapper.WiqlQueryMapper;
import co.com.bancolombia.consumer.mapper.WorkItemBatchMapper;
import co.com.bancolombia.consumer.mapper.WorkItemMapper;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import co.com.bancolombia.model.workitem.gateways.WorkItemQueryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Adaptador de <b>consulta</b> de work items contra Azure DevOps (D-10, <b>Fase 05</b>).
 *
 * <p><b>De dónde sale.</b> Hasta esta fase estos tres métodos convivían con otros cuatro dentro de
 * {@code RestConsumer}, una clase de 191 líneas que implementaba <b>siete gateways</b>. La
 * consecuencia práctica no era estética: <b>no había forma de probar un flujo sin arrastrar los
 * otros seis</b>. {@code spring-rules.md} §2 pide lo contrario —separar lecturas de escrituras— y
 * §3 lo repite en SOLID-I y SOLID-S.
 *
 * <p><b>Qué NO cambió al mudarse.</b> Ni una URL, ni un parámetro de consulta, ni un
 * {@code contentType}, ni una versión de API por defecto, ni un byte del JSON emitido, ni una traza
 * del log. Los cuerpos siguen construyéndose con los mismos mappers de la Fase 03, que <b>se
 * reparten entre adaptadores, no se duplican</b>. {@code WorkItemQueryAdapterTest} y
 * {@code OutboundPayloadCharacterizationTest} son la red de seguridad.
 *
 * <p><b>Qué sí cambió.</b> El nombre de la instancia de cortacircuito: los tres métodos comparten
 * ahora {@code workItemQuery} en lugar de tener uno por operación (decisión <b>B-05</b>). El
 * comportamiento es idéntico porque <b>ninguna de las instancias, ni las viejas ni la nueva, está
 * declarada en {@code application.yaml}</b>: todas corren con la configuración por defecto de
 * Resilience4j. Darles umbrales elegidos es <b>D-06</b>, material de la <b>Fase 06</b>; este
 * renombrado le deja el terreno preparado.
 *
 * @see WorkItemCommandAdapter el lado de escritura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemQueryAdapter implements WorkItemQueryPort {

    private static final String CIRCUIT_BREAKER = "workItemQuery";

    private final WebClient client;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WorkItem> getWorkItem(String organization, String project, Integer id,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, ApiVersions.WORK_ITEM_DEFAULT);
        log.info("Fetching Work Item {} | Org: {}, Project: {}, API Version: {}", id, organization, project, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/wit/workItems/{id}?api-version={version}",
                        organization, project, id, version)
                .retrieve()
                .bodyToMono(WorkItemDTO.class)
                .map(WorkItemMapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<List<WorkItem>> getWorkItemsBatch(String organization, String project,
            WorkItemBatchCriteria criteria, String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, ApiVersions.WORK_ITEM_DEFAULT);
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
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WiqlResult> queryByWiql(String organization, String project, WiqlQuery query,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, ApiVersions.WIQL_DEFAULT);
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
                .doOnError(WebClientResponseException.class,
                        ex -> log.error("❌ Detalle del error de Azure DevOps: {}",
                                ex.getResponseBodyAsString()))
                .map(WorkItemMapper::toDomain);
    }
}

