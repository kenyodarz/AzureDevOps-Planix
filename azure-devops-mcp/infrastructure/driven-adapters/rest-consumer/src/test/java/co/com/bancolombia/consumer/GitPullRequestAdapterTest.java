package co.com.bancolombia.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.exception.AzureDevOpsUnauthorizedException;
import co.com.bancolombia.model.exception.AzureDevOpsUnavailableException;
import co.com.bancolombia.model.exception.WorkItemNotFoundException;
import co.com.bancolombia.model.pullrequest.PullRequestComment;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class GitPullRequestAdapterTest {

    private GitPullRequestAdapter adapter;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        adapter = new GitPullRequestAdapter(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("getPullRequestById: 200 OK debe retornar PullRequest mapeado")
    void shouldReturnPullRequestWhenResponseIs200() throws InterruptedException {
        String json = """
                {
                    "pullRequestId": 1234,
                    "title": "Add MCP Server",
                    "description": "Integration with ADO",
                    "status": "active",
                    "sourceRefName": "refs/heads/feature/mcp",
                    "targetRefName": "refs/heads/main",
                    "repository": {
                        "id": "repo-uuid",
                        "name": "azure-devops-mcp"
                    },
                    "createdBy": {
                        "displayName": "Dev User",
                        "uniqueName": "dev@test.com"
                    },
                    "creationDate": "2026-09-08T12:00:00Z",
                    "workItemRefs": [
                        { "id": "999", "url": "https://dev.azure.com/wi/999" }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(json));

        var response = adapter.getPullRequestById("OrgTest", "ProjTest", "repo-uuid", 1234, "7.1");

        StepVerifier.create(response)
                .assertNext(pr -> {
                    assertThat(pr.pullRequestId()).isEqualTo(1234);
                    assertThat(pr.title()).isEqualTo("Add MCP Server");
                    assertThat(pr.description()).isEqualTo("Integration with ADO");
                    assertThat(pr.status()).isEqualTo("active");
                    assertThat(pr.sourceRefName()).isEqualTo("refs/heads/feature/mcp");
                    assertThat(pr.targetRefName()).isEqualTo("refs/heads/main");
                    assertThat(pr.repositoryId()).isEqualTo("repo-uuid");
                    assertThat(pr.createdBy()).isEqualTo("Dev User");
                    assertThat(pr.creationDate()).isEqualTo("2026-09-08T12:00:00Z");
                    assertThat(pr.workItemIds()).containsExactly(999);
                })
                .verifyComplete();

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo(
                "/OrgTest/ProjTest/_apis/git/repositories/repo-uuid/pullRequests/1234?api-version=7.1");
    }

    @Test
    @DisplayName("getPullRequestById: 401 Unauthorized debe traducirse a AzureDevOpsUnauthorizedException")
    void shouldTranslateUnauthorizedWhenGetPullRequestById() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .setBody("{\"message\": \"User not authenticated\"}"));

        var response = adapter.getPullRequestById("OrgTest", "ProjTest", "repo-uuid", 1234, null);

        StepVerifier.create(response)
                .expectError(AzureDevOpsUnauthorizedException.class)
                .verify();
    }

    @Test
    @DisplayName("getPullRequestById: 404 Not Found debe traducirse a WorkItemNotFoundException")
    void shouldTranslateNotFoundWhenGetPullRequestById() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .setBody("{\"message\": \"PR not found\"}"));

        var response = adapter.getPullRequestById("OrgTest", "ProjTest", "repo-uuid", 9999, null);

        StepVerifier.create(response)
                .expectError(WorkItemNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("getPullRequestById: 500 Server Error debe traducirse a AzureDevOpsUnavailableException")
    void shouldTranslateServerErrorWhenGetPullRequestById() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody("{\"message\": \"Internal Error\"}"));

        var response = adapter.getPullRequestById("OrgTest", "ProjTest", "repo-uuid", 1234, null);

        StepVerifier.create(response)
                .expectError(AzureDevOpsUnavailableException.class)
                .verify();
    }

    @Test
    @DisplayName("getPullRequestChanges: 200 OK debe emitir secuencia de GitChange")
    void shouldReturnChangesWhenResponseIs200() throws InterruptedException {
        String json = """
                {
                    "changeEntries": [
                        {
                            "changeTrackingId": 1,
                            "changeId": 1,
                            "changeType": "edit",
                            "item": {
                                "objectId": "hash-new-1",
                                "originalObjectId": "hash-orig-1",
                                "path": "/src/main/java/Main.java"
                            }
                        },
                        {
                            "changeTrackingId": 2,
                            "changeId": 2,
                            "changeType": "add",
                            "item": {
                                "objectId": "hash-new-2",
                                "originalObjectId": null,
                                "path": "/src/test/java/MainTest.java"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(json));

        var response = adapter.getPullRequestChanges("OrgTest", "ProjTest", "repo-uuid", 1234,
                "7.1");

        StepVerifier.create(response)
                .assertNext(change -> {
                    assertThat(change.changeType()).isEqualTo("edit");
                    assertThat(change.itemPath()).isEqualTo("/src/main/java/Main.java");
                    assertThat(change.originalObjectId()).isEqualTo("hash-orig-1");
                    assertThat(change.newObjectId()).isEqualTo("hash-new-1");
                })
                .assertNext(change -> {
                    assertThat(change.changeType()).isEqualTo("add");
                    assertThat(change.itemPath()).isEqualTo("/src/test/java/MainTest.java");
                    assertThat(change.originalObjectId()).isNull();
                    assertThat(change.newObjectId()).isEqualTo("hash-new-2");
                })
                .verifyComplete();

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo(
                "/OrgTest/ProjTest/_apis/git/repositories/repo-uuid/pullRequests/1234/iterations/1/changes?api-version=7.1");
    }

    @Test
    @DisplayName("getPullRequestChanges: 404 Not Found debe traducirse a WorkItemNotFoundException")
    void shouldTranslateNotFoundWhenGetPullRequestChanges() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .setBody("{\"message\": \"Changes not found\"}"));

        var response = adapter.getPullRequestChanges("OrgTest", "ProjTest", "repo-uuid", 1234,
                null);

        StepVerifier.create(response)
                .expectError(WorkItemNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("getPullRequestChanges: 401 Unauthorized debe traducirse a AzureDevOpsUnauthorizedException")
    void shouldTranslateUnauthorizedWhenGetPullRequestChanges() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .setBody("{\"message\": \"Unauthorized\"}"));

        var response = adapter.getPullRequestChanges("OrgTest", "ProjTest", "repo-uuid", 1234,
                null);

        StepVerifier.create(response)
                .expectError(AzureDevOpsUnauthorizedException.class)
                .verify();
    }

    @Test
    @DisplayName("createComment: 200 OK debe enviar POST /threads y retornar PullRequestComment")
    void shouldCreateCommentWhenResponseIs200() throws InterruptedException {
        String json = """
                {
                    "id": 5001,
                    "status": "active",
                    "comments": [
                        {
                            "id": 1,
                            "content": "Excelente arquitectura",
                            "author": {
                                "displayName": "Agente Evaluador",
                                "uniqueName": "agent@devops.corp"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(json));

        PullRequestComment inputComment = PullRequestComment.builder()
                .content("Excelente arquitectura")
                .status("active")
                .build();

        var response = adapter.createComment("OrgTest", "ProjTest", "repo-uuid", 1234, inputComment,
                "7.1");

        StepVerifier.create(response)
                .assertNext(comment -> {
                    assertThat(comment.id()).isEqualTo(5001);
                    assertThat(comment.status()).isEqualTo("active");
                    assertThat(comment.content()).isEqualTo("Excelente arquitectura");
                    assertThat(comment.author()).isEqualTo("Agente Evaluador");
                })
                .verifyComplete();

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo(
                "/OrgTest/ProjTest/_apis/git/repositories/repo-uuid/pullRequests/1234/threads?api-version=7.1");
        assertThat(recorded.getBody().readUtf8()).contains("Excelente arquitectura");
    }

    @Test
    @DisplayName("createComment: 401 Unauthorized debe traducirse a AzureDevOpsUnauthorizedException")
    void shouldTranslateUnauthorizedWhenCreateComment() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .setBody("{\"message\": \"Unauthorized token\"}"));

        PullRequestComment inputComment = PullRequestComment.builder()
                .content("Comentario")
                .build();

        var response = adapter.createComment("OrgTest", "ProjTest", "repo-uuid", 1234, inputComment,
                null);

        StepVerifier.create(response)
                .expectError(AzureDevOpsUnauthorizedException.class)
                .verify();
    }

    @Test
    @DisplayName("createComment: 404 Not Found debe traducirse a WorkItemNotFoundException")
    void shouldTranslateNotFoundWhenCreateComment() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .setBody("{\"message\": \"Repository or PR not found\"}"));

        PullRequestComment inputComment = PullRequestComment.builder()
                .content("Comentario")
                .build();

        var response = adapter.createComment("OrgTest", "ProjTest", "repo-uuid", 1234, inputComment,
                null);

        StepVerifier.create(response)
                .expectError(WorkItemNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("createComment: 500 Server Error debe traducirse a AzureDevOpsUnavailableException")
    void shouldTranslateServerErrorWhenCreateComment() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody("{\"message\": \"ADO service error\"}"));

        PullRequestComment inputComment = PullRequestComment.builder()
                .content("Comentario")
                .build();

        var response = adapter.createComment("OrgTest", "ProjTest", "repo-uuid", 1234, inputComment,
                null);

        StepVerifier.create(response)
                .expectError(AzureDevOpsUnavailableException.class)
                .verify();
    }
}
