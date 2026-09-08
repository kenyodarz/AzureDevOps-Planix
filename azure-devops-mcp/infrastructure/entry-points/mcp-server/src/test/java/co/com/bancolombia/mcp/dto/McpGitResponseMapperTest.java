package co.com.bancolombia.mcp.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;
import co.com.bancolombia.model.pullrequest.PullRequestComment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class McpGitResponseMapperTest {

    @Test
    @DisplayName("GIVEN un PullRequest de dominio WHEN toResponse THEN mapea todos los campos fielmente")
    void givenPullRequest_whenToResponse_thenMapsCorrectly() {
        // Arrange
        PullRequest domain = PullRequest.builder()
                .pullRequestId(101)
                .title("Feature: Soporte de Git")
                .description("Detalles del PR")
                .status("active")
                .sourceRefName("refs/heads/feature/git")
                .targetRefName("refs/heads/main")
                .repositoryId("repo-uuid-123")
                .createdBy("Dev Lead")
                .creationDate("2026-09-08T12:00:00Z")
                .workItemIds(List.of(501, 502))
                .build();

        // Act
        PullRequestResponse response = McpResponseMapper.toResponse(domain);

        // Assert
        assertNotNull(response);
        assertEquals(101, response.pullRequestId());
        assertEquals("Feature: Soporte de Git", response.title());
        assertEquals("Detalles del PR", response.description());
        assertEquals("active", response.status());
        assertEquals("refs/heads/feature/git", response.sourceRefName());
        assertEquals("refs/heads/main", response.targetRefName());
        assertEquals("repo-uuid-123", response.repositoryId());
        assertEquals("Dev Lead", response.createdBy());
        assertEquals("2026-09-08T12:00:00Z", response.creationDate());
        assertEquals(List.of(501, 502), response.workItemIds());
    }

    @Test
    @DisplayName("GIVEN PullRequest nulo WHEN toResponse THEN retorna null")
    void givenNullPullRequest_whenToResponse_thenReturnsNull() {
        assertNull(McpResponseMapper.toResponse((PullRequest) null));
    }

    @Test
    @DisplayName("GIVEN un GitChange de dominio WHEN toResponse THEN mapea los campos fielmente")
    void givenGitChange_whenToResponse_thenMapsCorrectly() {
        // Arrange
        GitChange domain = GitChange.builder()
                .itemPath("/src/Main.java")
                .changeType("edit")
                .originalObjectId("hash-orig-1")
                .newObjectId("hash-new-2")
                .build();

        // Act
        GitChangeResponse response = McpResponseMapper.toResponse(domain);

        // Assert
        assertNotNull(response);
        assertEquals("/src/Main.java", response.itemPath());
        assertEquals("edit", response.changeType());
        assertEquals("hash-orig-1", response.originalObjectId());
        assertEquals("hash-new-2", response.newObjectId());
    }

    @Test
    @DisplayName("GIVEN GitChange nulo WHEN toResponse THEN retorna null")
    void givenNullGitChange_whenToResponse_thenReturnsNull() {
        assertNull(McpResponseMapper.toResponse((GitChange) null));
    }

    @Test
    @DisplayName("GIVEN lista de GitChange WHEN toGitChangeResponses THEN mapea cada elemento")
    void givenGitChangesList_whenToGitChangeResponses_thenMapsList() {
        // Arrange
        GitChange change1 = GitChange.builder().itemPath("/file1.txt").changeType("add").build();
        GitChange change2 = GitChange.builder().itemPath("/file2.txt").changeType("delete").build();

        // Act
        List<GitChangeResponse> responses = McpResponseMapper.toGitChangeResponses(
                List.of(change1, change2));

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("/file1.txt", responses.get(0).itemPath());
        assertEquals("/file2.txt", responses.get(1).itemPath());
    }

    @Test
    @DisplayName("GIVEN lista de GitChange nula WHEN toGitChangeResponses THEN retorna null")
    void givenNullGitChangesList_whenToGitChangeResponses_thenReturnsNull() {
        assertNull(McpResponseMapper.toGitChangeResponses(null));
    }

    @Test
    @DisplayName("GIVEN un PullRequestComment de dominio WHEN toResponse THEN mapea los campos fielmente")
    void givenPullRequestComment_whenToResponse_thenMapsCorrectly() {
        // Arrange
        PullRequestComment domain = PullRequestComment.builder()
                .id(999)
                .content("Excelente implementación")
                .status("active")
                .author("Reviewer Bot")
                .build();

        // Act
        PullRequestCommentResponse response = McpResponseMapper.toResponse(domain);

        // Assert
        assertNotNull(response);
        assertEquals(999, response.id());
        assertEquals("Excelente implementación", response.content());
        assertEquals("active", response.status());
        assertEquals("Reviewer Bot", response.author());
    }

    @Test
    @DisplayName("GIVEN PullRequestComment nulo WHEN toResponse THEN retorna null")
    void givenNullPullRequestComment_whenToResponse_thenReturnsNull() {
        assertNull(McpResponseMapper.toResponse((PullRequestComment) null));
    }
}
