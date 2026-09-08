package co.com.bancolombia.mcpclient.adapter;

import co.com.bancolombia.model.pullrequest.PullRequestChangeInfo;
import co.com.bancolombia.model.pullrequest.PullRequestInfo;
import co.com.bancolombia.model.pullrequest.gateways.PullRequestMcpPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adaptador reactivo que implementa {@link PullRequestMcpPort} delegando en {@link McpSyncClient}.
 *
 * <p>Consume las herramientas MCP de Git del servidor (ej. {@code getPullRequest},
 * {@code getPullRequestChanges}, {@code createPullRequestComment}) de forma no bloqueante usando el
 * planificador {@link Schedulers#boundedElastic()}.
 */
@Slf4j
@Component
public class McpPullRequestAdapter implements PullRequestMcpPort {

    private static final String PARAM_ORGANIZATION = "organization";
    private static final String PARAM_PROJECT = "project";
    private static final String PARAM_REPOSITORY_ID = "repositoryId";
    private static final String PARAM_PULL_REQUEST_ID = "pullRequestId";

    private final List<McpSyncClient> mcpClients;
    private final ObjectMapper objectMapper;

    public McpPullRequestAdapter(List<McpSyncClient> mcpClients, ObjectMapper objectMapper) {
        this.mcpClients = mcpClients != null ? mcpClients : List.of();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public Mono<PullRequestInfo> getPullRequest(
            String organization, String project, String repositoryId, int pullRequestId) {
        return Mono.fromCallable(() -> {
            Map<String, Object> args = Map.of(
                    PARAM_ORGANIZATION, organization,
                    PARAM_PROJECT, project,
                    PARAM_REPOSITORY_ID, repositoryId,
                    PARAM_PULL_REQUEST_ID, pullRequestId
            );
            String rawJson = executeCallTool("getPullRequest", args);
            JsonNode root = objectMapper.readTree(rawJson);
            return PullRequestInfo.builder()
                    .pullRequestId(root.path(PARAM_PULL_REQUEST_ID).asInt(pullRequestId))
                    .repositoryId(root.path(PARAM_REPOSITORY_ID).asText(repositoryId))
                    .title(root.path("title").asText(""))
                    .description(root.path("description").asText(""))
                    .sourceRefName(root.path("sourceRefName").asText(""))
                    .targetRefName(root.path("targetRefName").asText(""))
                    .status(root.path("status").asText(""))
                    .createdBy(root.path("createdBy").asText(""))
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<PullRequestChangeInfo> getPullRequestChanges(
            String organization, String project, String repositoryId, int pullRequestId) {
        return Mono.fromCallable(() -> {
                    Map<String, Object> args = Map.of(
                            PARAM_ORGANIZATION, organization,
                            PARAM_PROJECT, project,
                            PARAM_REPOSITORY_ID, repositoryId,
                            PARAM_PULL_REQUEST_ID, pullRequestId
                    );
                    String rawJson = executeCallTool("getPullRequestChanges", args);
                    JsonNode root = objectMapper.readTree(rawJson);
                    List<PullRequestChangeInfo> changes = new ArrayList<>();
                    if (root.isArray()) {
                        for (JsonNode node : root) {
                            changes.add(PullRequestChangeInfo.builder()
                                    .path(node.path("path").asText(""))
                                    .changeType(node.path("changeType").asText(""))
                                    .build());
                        }
                    }
                    return changes;
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<String> createPullRequestComment(
            String organization, String project, String repositoryId, int pullRequestId,
            String comment) {
        return Mono.fromCallable(() -> {
            Map<String, Object> args = Map.of(
                    PARAM_ORGANIZATION, organization,
                    PARAM_PROJECT, project,
                    PARAM_REPOSITORY_ID, repositoryId,
                    PARAM_PULL_REQUEST_ID, pullRequestId,
                    "content", comment
            );
            String rawJson = executeCallTool("createPullRequestComment", args);
            JsonNode root = objectMapper.readTree(rawJson);
            String commentId = root.path("id").asText();
            if (commentId.isBlank()) {
                commentId = root.path("commentId").asText("created");
            }
            return commentId;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String executeCallTool(String toolName, Map<String, Object> args) {
        if (mcpClients.isEmpty()) {
            throw new IllegalStateException(
                    "No hay clientes MCP disponibles para ejecutar [" + toolName + "]");
        }
        McpSyncClient client = mcpClients.get(0);
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
        McpSchema.CallToolResult result = client.callTool(request);
        if (result == null || result.content() == null || result.content().isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        }
        return sb.toString();
    }
}
