package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.config.AzureDevOpsAdapterProperties;
import co.com.bancolombia.consumer.dto.PipelineListResponseDTO;
import co.com.bancolombia.consumer.dto.PipelineRunDTO;
import co.com.bancolombia.consumer.dto.PipelineRunRequestDTO;
import co.com.bancolombia.consumer.dto.PipelineTimelineResponseDTO;
import co.com.bancolombia.consumer.mapper.PipelineMapper;
import co.com.bancolombia.model.pipeline.PipelineLogSummary;
import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.PipelineSummary;
import co.com.bancolombia.model.pipeline.gateways.PipelinePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de infraestructura reactivo para interactuar con Pipelines de Azure DevOps.
 */
@Slf4j
@Service
public class PipelineAdapter implements PipelinePort {

    private static final String CIRCUIT_BREAKER = "pipeline";

    private final WebClient client;
    private final AzureDevOpsAdapterProperties properties;

    @Autowired
    public PipelineAdapter(WebClient client, AzureDevOpsAdapterProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public PipelineAdapter(WebClient client) {
        this(client, AzureDevOpsAdapterProperties.defaults());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Flux<PipelineSummary> listPipelines(
            String organization,
            String project,
            int top,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().pipeline());
        log.info("Listing pipelines | Org: {}, Project: {}, Top: {}, API Version: {}",
                organization, project, top, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/pipelines?$top={top}&api-version={version}",
                        organization, project, top, version)
                .retrieve()
                .bodyToMono(PipelineListResponseDTO.class)
                .flatMapIterable(PipelineMapper::toDomainList)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("listPipelines"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<PipelineRun> getPipelineRun(
            String organization,
            String project,
            int pipelineId,
            int runId,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().pipeline());
        log.info("Fetching pipeline run {} for pipeline {} | Org: {}, Project: {}, API Version: {}",
                runId, pipelineId, organization, project, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/pipelines/{pipelineId}/runs/{runId}?api-version={version}",
                        organization, project, pipelineId, runId, version)
                .retrieve()
                .bodyToMono(PipelineRunDTO.class)
                .map(PipelineMapper::toDomain)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getPipelineRun"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<PipelineLogSummary> getPipelineRunLogs(
            String organization,
            String project,
            int pipelineId,
            int runId,
            boolean onlyErrors,
            int maxLines,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().pipeline());
        log.info("Fetching timeline/logs for run {} | Org: {}, Project: {}, OnlyErrors: {}, MaxLines: {}, API Version: {}",
                runId, organization, project, onlyErrors, maxLines, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/build/builds/{runId}/timeline?api-version={version}",
                        organization, project, runId, version)
                .retrieve()
                .bodyToMono(PipelineTimelineResponseDTO.class)
                .map(timeline -> PipelineMapper.toDomain(timeline, runId, onlyErrors, maxLines))
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getPipelineRunLogs"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<PipelineRun> triggerPipelineRun(
            String organization,
            String project,
            int pipelineId,
            String branch,
            Map<String, String> variables,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().pipeline());
        log.info("Triggering pipeline {} on branch {} | Org: {}, Project: {}, API Version: {}",
                pipelineId, branch, organization, project, version);

        PipelineRunRequestDTO request = PipelineMapper.toRunRequest(branch, variables);

        return client.post()
                .uri("/{organization}/{project}/_apis/pipelines/{pipelineId}/runs?api-version={version}",
                        organization, project, pipelineId, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PipelineRunDTO.class)
                .map(PipelineMapper::toDomain)
                .timeout(properties.operationTimeout().command())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("triggerPipelineRun"));
    }
}
