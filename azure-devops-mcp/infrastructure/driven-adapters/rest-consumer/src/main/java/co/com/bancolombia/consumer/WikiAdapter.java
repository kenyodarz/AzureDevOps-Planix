package co.com.bancolombia.consumer;

import co.com.bancolombia.consumer.config.AzureDevOpsAdapterProperties;
import co.com.bancolombia.consumer.dto.WikiPageDTO;
import co.com.bancolombia.consumer.dto.WikiPagePutRequestDTO;
import co.com.bancolombia.consumer.dto.WikiSearchRequestDTO;
import co.com.bancolombia.consumer.dto.WikiSearchResponseDTO;
import co.com.bancolombia.consumer.mapper.WikiMapper;
import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.model.wiki.WikiSearchResult;
import co.com.bancolombia.model.wiki.gateways.WikiPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de infraestructura reactivo para interactuar con la Wiki de Azure DevOps.
 */
@Slf4j
@Service
public class WikiAdapter implements WikiPort {

    private static final String CIRCUIT_BREAKER = "wiki";

    private final WebClient client;
    private final AzureDevOpsAdapterProperties properties;

    @Autowired
    public WikiAdapter(WebClient client, AzureDevOpsAdapterProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public WikiAdapter(WebClient client) {
        this(client, AzureDevOpsAdapterProperties.defaults());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WikiPage> getWikiPage(
            String organization,
            String project,
            String wikiIdentifier,
            String path,
            boolean includeContent,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().wiki());
        log.info("Fetching wiki page {} | Org: {}, Project: {}, Wiki: {}, API Version: {}",
                path, organization, project, wikiIdentifier, version);

        return client.get()
                .uri("/{organization}/{project}/_apis/wiki/wikis/{wikiIdentifier}/pages?path={path}&includeContent={includeContent}&api-version={version}",
                        organization, project, wikiIdentifier, path, includeContent, version)
                .retrieve()
                .bodyToMono(WikiPageDTO.class)
                .map(WikiMapper::toDomain)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("getWikiPage"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Flux<WikiSearchResult> searchWiki(
            String organization,
            String project,
            String query,
            int top,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, "7.1-preview.1");
        log.info("Searching wiki for query '{}' | Org: {}, Project: {}, API Version: {}",
                query, organization, project, version);

        WikiSearchRequestDTO request = WikiMapper.toSearchRequest(query, top);

        return client.post()
                .uri("/{organization}/{project}/_apis/search/wikisearchresults?api-version={version}",
                        organization, project, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WikiSearchResponseDTO.class)
                .flatMapIterable(WikiMapper::toDomainSearchResults)
                .timeout(properties.operationTimeout().query())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("searchWiki"));
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER)
    public Mono<WikiPage> createOrUpdateWikiPage(
            String organization,
            String project,
            String wikiIdentifier,
            String path,
            String content,
            String comment,
            String apiVersion) {
        String version = ApiVersions.orDefault(apiVersion, properties.apiVersion().wiki());
        String effectiveComment = (comment != null && !comment.isBlank()) ? comment : "Updated via MCP";
        log.info("Creating or updating wiki page {} | Org: {}, Project: {}, Wiki: {}, API Version: {}",
                path, organization, project, wikiIdentifier, version);

        WikiPagePutRequestDTO request = WikiMapper.toPutRequest(content);

        return client.put()
                .uri("/{organization}/{project}/_apis/wiki/wikis/{wikiIdentifier}/pages?path={path}&comment={comment}&api-version={version}",
                        organization, project, wikiIdentifier, path, effectiveComment, version)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WikiPageDTO.class)
                .map(WikiMapper::toDomain)
                .timeout(properties.operationTimeout().command())
                .onErrorMap(AzureDevOpsErrorTranslator.forOperation("createOrUpdateWikiPage"));
    }
}
