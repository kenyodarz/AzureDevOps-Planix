package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.config.AzureDevOpsAdapterProperties;
import co.com.bancolombia.consumer.dto.ProjectOverviewDTO;
import co.com.bancolombia.consumer.mapper.OverviewMapper;
import co.com.bancolombia.model.overview.ProjectOverview;
import co.com.bancolombia.model.overview.gateways.OverviewPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptador de infraestructura reactivo para consultar detalles y visión general de proyectos en Azure DevOps.
 */
@Slf4j
@Service
public class OverviewAdapter implements OverviewPort {

    private static final String CIRCUIT_BREAKER = "overview";

    private final WebClient client;
    private final AzureDevOpsAdapterProperties properties;

    @Autowired
    public OverviewAdapter(WebClient client, AzureDevOpsAdapterProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public OverviewAdapter(WebClient client) {
        this(client, AzureDevOpsAdapterProperties.defaults());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<ProjectOverview> getProjectOverview(String organization, String project, String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().workItem());
        log.info("Fetching project overview | Org: {}, Project: {}, API Version: {}", organization, project, version);

        return client.get()
                .uri("/{organization}/_apis/projects/{project}?api-version={version}", organization, project, version)
                .retrieve()
                .bodyToMono(ProjectOverviewDTO.class)
                .map(OverviewMapper::toDomain)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getProjectOverview"));
    }
}
