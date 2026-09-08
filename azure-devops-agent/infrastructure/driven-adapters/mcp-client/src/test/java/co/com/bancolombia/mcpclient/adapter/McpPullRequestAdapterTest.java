package co.com.bancolombia.mcpclient.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

@DisplayName("McpPullRequestAdapter - Adaptador MCP de Pull Requests")
class McpPullRequestAdapterTest {

    private McpSyncClient mcpClient;
    private ObjectMapper objectMapper;
    private McpPullRequestAdapter adapter;

    @BeforeEach
    void setUp() {
        mcpClient = mock(McpSyncClient.class);
        objectMapper = new ObjectMapper();
        adapter = new McpPullRequestAdapter(List.of(mcpClient), objectMapper);
    }

    @Test
    @DisplayName("getPullRequest ejecuta la tool y mapea el resultado a PullRequestInfo")
    void givenValidPrResponse_whenGetPullRequest_thenReturnsPullRequestInfo() {
        String json = """
                {
                  "pullRequestId": 42,
                  "repositoryId": "azure-devops-core",
                  "title": "feat: soporte git",
                  "description": "desc",
                  "sourceRefName": "refs/heads/feature/x",
                  "targetRefName": "refs/heads/main",
                  "status": "active",
                  "createdBy": "autor"
                }
                """;

        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(json)), false, null, null);
        when(mcpClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(result);

        StepVerifier.create(adapter.getPullRequest("org", "proj", "azure-devops-core", 42))
                .assertNext(info -> {
                    assertThat(info.getPullRequestId()).isEqualTo(42);
                    assertThat(info.getRepositoryId()).isEqualTo("azure-devops-core");
                    assertThat(info.getTitle()).isEqualTo("feat: soporte git");
                    assertThat(info.getSourceRefName()).isEqualTo("refs/heads/feature/x");
                    assertThat(info.getStatus()).isEqualTo("active");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getPullRequestChanges ejecuta la tool y retorna Flux con cambios")
    void givenValidChangesResponse_whenGetPullRequestChanges_thenReturnsChangesFlux() {
        String json = """
                [
                  {"path": "/src/A.java", "changeType": "add"},
                  {"path": "/src/B.java", "changeType": "edit"}
                ]
                """;

        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(json)), false, null, null);
        when(mcpClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(result);

        StepVerifier.create(adapter.getPullRequestChanges("org", "proj", "azure-devops-core", 42))
                .assertNext(change -> {
                    assertThat(change.getPath()).isEqualTo("/src/A.java");
                    assertThat(change.getChangeType()).isEqualTo("add");
                })
                .assertNext(change -> {
                    assertThat(change.getPath()).isEqualTo("/src/B.java");
                    assertThat(change.getChangeType()).isEqualTo("edit");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createPullRequestComment ejecuta la tool y retorna el identificador del comentario")
    void givenValidCommentResponse_whenCreatePullRequestComment_thenReturnsCommentId() {
        String json = """
                {
                  "id": 999,
                  "commentId": "comment-999",
                  "status": "active"
                }
                """;

        McpSchema.CallToolResult result = new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(json)), false, null, null);
        when(mcpClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(result);

        StepVerifier.create(adapter.createPullRequestComment("org", "proj", "repo", 42,
                        "Comentario de revisión"))
                .assertNext(commentId -> assertThat(commentId).isEqualTo("999"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Falla con error reactivo si no hay clientes MCP disponibles")
    void givenNoMcpClients_whenCall_thenEmitsError() {
        McpPullRequestAdapter noClientsAdapter = new McpPullRequestAdapter(Collections.emptyList(),
                objectMapper);

        StepVerifier.create(noClientsAdapter.getPullRequest("org", "proj", "repo", 1))
                .expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage()
                        .contains("No hay clientes MCP disponibles"))
                .verify();
    }

    @Test
    @DisplayName("Maneja resultado con contenido nulo o vacío retornando DTO por defecto")
    void givenEmptyResultContent_whenGetPullRequest_thenReturnsDefaultDto() {
        McpSchema.CallToolResult emptyResult = new McpSchema.CallToolResult(List.of(), false, null,
                null);
        when(mcpClient.callTool(any())).thenReturn(emptyResult);

        StepVerifier.create(adapter.getPullRequest("org", "proj", "repo", 42))
                .assertNext(info -> {
                    assertThat(info.getPullRequestId()).isEqualTo(42);
                    assertThat(info.getTitle()).isEmpty();
                })
                .verifyComplete();
    }
}
