package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.config.AzureDevOpsAdapterProperties;
import co.com.bancolombia.consumer.dto.GitPullRequestChangesResponse;
import co.com.bancolombia.consumer.dto.GitPullRequestResponse;
import co.com.bancolombia.consumer.mapper.PullRequestMapper;
import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de infraestructura reactivo para consultar detalles y cambios de Pull Requests contra
 * la API REST de Azure DevOps Git.
 *
 * <p>Implementa el puerto {@link PullRequestPort} utilizando {@link WebClient} de forma no
 * bloqueante, protegido con cortacircuito Resilience4j y traducción centralizada de errores
 * técnicos mediante {@link AzureDevOpsErrorTranslator}.
 */
@Slf4j
@Service
public class GitPullRequestAdapter implements PullRequestPort {

    private static final String CIRCUIT_BREAKER = "gitPullRequest";
    private static final int DEFAULT_ITERATION_ID = 1;

    private final WebClient client;
    private final AzureDevOpsAdapterProperties properties;

    @Autowired
    public GitPullRequestAdapter(WebClient client, AzureDevOpsAdapterProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Constructor de conveniencia con valores por defecto para pruebas unitarias desacopladas del
     * contexto de Spring.
     */
    public GitPullRequestAdapter(WebClient client) {
        this(client, AzureDevOpsAdapterProperties.defaults());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<PullRequest> getPullRequestById(
            String organization,
            String project,
            String repositoryId,
            int pullRequestId,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().git());
        log.info("Fetching Pull Request {} | Org: {}, Project: {}, Repo: {}, API Version: {}",
                pullRequestId, organization, project, repositoryId, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}?api-version={version}",
                        organization, project, repositoryId, pullRequestId, version)
                .retrieve()
                .bodyToMono(GitPullRequestResponse.class)
                .map(PullRequestMapper::toDomain)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getPullRequestById"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Flux<GitChange> getPullRequestChanges(
            String organization,
            String project,
            String repositoryId,
            int pullRequestId,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().git());
        log.info(
                "Fetching Pull Request Changes for PR {} | Org: {}, Project: {}, Repo: {}, API Version: {}",
                pullRequestId, organization, project, repositoryId, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/iterations/{iterationId}/changes?api-version={version}",
                        organization, project, repositoryId, pullRequestId, DEFAULT_ITERATION_ID,
                        version)
                .retrieve()
                .bodyToMono(GitPullRequestChangesResponse.class)
                .flatMapIterable(PullRequestMapper::toDomainChanges)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getPullRequestChanges"));
    }
}
