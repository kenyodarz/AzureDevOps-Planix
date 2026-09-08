package co.com.bancolombia.consumer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.consumer.dto.GitPullRequestChangesResponse;
import co.com.bancolombia.consumer.dto.GitPullRequestCommentRequest;
import co.com.bancolombia.consumer.dto.GitPullRequestCommentResponse;
import co.com.bancolombia.consumer.dto.GitPullRequestResponse;
import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;
import co.com.bancolombia.model.pullrequest.PullRequestComment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PullRequestMapperTest {

    @Test
    @DisplayName("toDomain(null) debe retornar null")
    void shouldReturnNullWhenDtoIsNull() {
        assertThat(PullRequestMapper.toDomain((GitPullRequestResponse) null)).isNull();
    }

    @Test
    @DisplayName("toDomain debe mapear todos los campos correctamente")
    void shouldMapCompletePullRequestResponse() {
        var dto = GitPullRequestResponse.builder()
                .pullRequestId(101)
                .title("Feature: PR Assessment")
                .description("PR assessment implementation")
                .status("active")
                .sourceRefName("refs/heads/feature/mcp")
                .targetRefName("refs/heads/main")
                .repository(GitPullRequestResponse.GitRepositoryDTO.builder()
                        .id("repo-id-1")
                        .name("repo-name-1")
                        .build())
                .createdBy(GitPullRequestResponse.IdentityRefDTO.builder()
                        .id("user-id-1")
                        .displayName("Juan Mina")
                        .uniqueName("jmina@test.com")
                        .build())
                .creationDate("2026-09-08T10:00:00Z")
                .workItemRefs(List.of(
                        GitPullRequestResponse.ResourceRefDTO.builder().id("123")
                                .url("http://wi/123").build(),
                        GitPullRequestResponse.ResourceRefDTO.builder().id("456")
                                .url("http://wi/456").build()
                ))
                .build();

        PullRequest result = PullRequestMapper.toDomain(dto);

        assertThat(result).isNotNull();
        assertThat(result.pullRequestId()).isEqualTo(101);
        assertThat(result.title()).isEqualTo("Feature: PR Assessment");
        assertThat(result.description()).isEqualTo("PR assessment implementation");
        assertThat(result.status()).isEqualTo("active");
        assertThat(result.sourceRefName()).isEqualTo("refs/heads/feature/mcp");
        assertThat(result.targetRefName()).isEqualTo("refs/heads/main");
        assertThat(result.repositoryId()).isEqualTo("repo-id-1");
        assertThat(result.createdBy()).isEqualTo("Juan Mina");
        assertThat(result.creationDate()).isEqualTo("2026-09-08T10:00:00Z");
        assertThat(result.workItemIds()).containsExactly(123, 456);
    }

    @Test
    @DisplayName("toDomain debe tolerar repository sin id usando name y createdBy sin displayName usando uniqueName")
    void shouldHandleFallbackRepositoryAndAuthor() {
        var dto = GitPullRequestResponse.builder()
                .pullRequestId(102)
                .repository(GitPullRequestResponse.GitRepositoryDTO.builder().name("repo-name-only")
                        .build())
                .createdBy(
                        GitPullRequestResponse.IdentityRefDTO.builder().uniqueName("dev@test.com")
                                .build())
                .workItemRefs(List.of(
                        GitPullRequestResponse.ResourceRefDTO.builder().id("invalid").build(),
                        GitPullRequestResponse.ResourceRefDTO.builder().id(null).build(),
                        GitPullRequestResponse.ResourceRefDTO.builder().id(" 789 ").build()
                ))
                .build();

        PullRequest result = PullRequestMapper.toDomain(dto);

        assertThat(result).isNotNull();
        assertThat(result.repositoryId()).isEqualTo("repo-name-only");
        assertThat(result.createdBy()).isEqualTo("dev@test.com");
        assertThat(result.workItemIds()).containsExactly(789);
    }

    @Test
    @DisplayName("toDomain(ChangeEntryDTO) debe mapear cambios y tolerar nulos")
    void shouldMapChangeEntryDto() {
        assertThat(PullRequestMapper.toDomain(
                (GitPullRequestChangesResponse.ChangeEntryDTO) null)).isNull();

        var entryWithoutItem = GitPullRequestChangesResponse.ChangeEntryDTO.builder()
                .changeType("delete")
                .item(null)
                .build();
        GitChange changeWithoutItem = PullRequestMapper.toDomain(entryWithoutItem);
        assertThat(changeWithoutItem).isNotNull();
        assertThat(changeWithoutItem.changeType()).isEqualTo("delete");
        assertThat(changeWithoutItem.itemPath()).isNull();

        var entryWithItem = GitPullRequestChangesResponse.ChangeEntryDTO.builder()
                .changeTrackingId(1)
                .changeType("edit")
                .item(GitPullRequestChangesResponse.GitItemDTO.builder()
                        .path("/src/Main.java")
                        .originalObjectId("abc")
                        .objectId("def")
                        .build())
                .build();
        GitChange changeWithItem = PullRequestMapper.toDomain(entryWithItem);
        assertThat(changeWithItem).isNotNull();
        assertThat(changeWithItem.changeType()).isEqualTo("edit");
        assertThat(changeWithItem.itemPath()).isEqualTo("/src/Main.java");
        assertThat(changeWithItem.originalObjectId()).isEqualTo("abc");
        assertThat(changeWithItem.newObjectId()).isEqualTo("def");
    }

    @Test
    @DisplayName("toDomainChanges debe convertir lista de cambios y tolerar nulos")
    void shouldMapChangesList() {
        assertThat(PullRequestMapper.toDomainChanges(null)).isEmpty();

        var emptyResponse = new GitPullRequestChangesResponse();
        assertThat(PullRequestMapper.toDomainChanges(emptyResponse)).isEmpty();

        var responseWithEntries = GitPullRequestChangesResponse.builder()
                .changeEntries(List.of(
                        GitPullRequestChangesResponse.ChangeEntryDTO.builder()
                                .changeType("add")
                                .item(GitPullRequestChangesResponse.GitItemDTO.builder()
                                        .path("/file1.txt").build())
                                .build()
                ))
                .build();

        List<GitChange> result = PullRequestMapper.toDomainChanges(responseWithEntries);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemPath()).isEqualTo("/file1.txt");
        assertThat(result.get(0).changeType()).isEqualTo("add");

        var responseWithChangesFallback = GitPullRequestChangesResponse.builder()
                .changes(List.of(
                        GitPullRequestChangesResponse.ChangeEntryDTO.builder()
                                .changeType("edit")
                                .item(GitPullRequestChangesResponse.GitItemDTO.builder()
                                        .path("/file2.txt").build())
                                .build()
                ))
                .build();

        List<GitChange> resultFallback = PullRequestMapper.toDomainChanges(
                responseWithChangesFallback);
        assertThat(resultFallback).hasSize(1);
        assertThat(resultFallback.get(0).itemPath()).isEqualTo("/file2.txt");
    }

    @Test
    @DisplayName("toRequest debe convertir PullRequestComment a GitPullRequestCommentRequest y aplicar default de status")
    void shouldMapCommentToRequest() {
        assertThat(PullRequestMapper.toRequest(null)).isNull();

        PullRequestComment commentWithStatus = PullRequestComment.builder()
                .content("Buen PR")
                .status("fixed")
                .build();
        GitPullRequestCommentRequest request1 = PullRequestMapper.toRequest(commentWithStatus);
        assertThat(request1).isNotNull();
        assertThat(request1.getStatus()).isEqualTo("fixed");
        assertThat(request1.getComments()).hasSize(1);
        assertThat(request1.getComments().getFirst().getContent()).isEqualTo("Buen PR");
        assertThat(request1.getComments().getFirst().getParentCommentId()).isZero();
        assertThat(request1.getComments().getFirst().getCommentType()).isEqualTo(1);

        PullRequestComment commentWithoutStatus = PullRequestComment.builder()
                .content("Comentario simple")
                .build();
        GitPullRequestCommentRequest request2 = PullRequestMapper.toRequest(commentWithoutStatus);
        assertThat(request2).isNotNull();
        assertThat(request2.getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("toDomainComment debe convertir GitPullRequestCommentResponse a PullRequestComment y tolerar nulos")
    void shouldMapResponseToDomainComment() {
        assertThat(PullRequestMapper.toDomainComment(null)).isNull();

        var emptyResponse = GitPullRequestCommentResponse.builder()
                .id(50)
                .status("active")
                .comments(List.of())
                .build();
        PullRequestComment commentFromEmpty = PullRequestMapper.toDomainComment(emptyResponse);
        assertThat(commentFromEmpty).isNotNull();
        assertThat(commentFromEmpty.id()).isEqualTo(50);
        assertThat(commentFromEmpty.status()).isEqualTo("active");
        assertThat(commentFromEmpty.content()).isNull();
        assertThat(commentFromEmpty.author()).isNull();

        var responseWithDisplayName = GitPullRequestCommentResponse.builder()
                .id(51)
                .status("closed")
                .comments(List.of(
                        GitPullRequestCommentResponse.CommentItemResponse.builder()
                                .id(1)
                                .content("Observación técnica")
                                .author(GitPullRequestCommentResponse.IdentityRefDTO.builder()
                                        .displayName("Arquitecto")
                                        .uniqueName("arq@test.com")
                                        .build())
                                .build()
                ))
                .build();
        PullRequestComment commentWithDisplayName = PullRequestMapper.toDomainComment(
                responseWithDisplayName);
        assertThat(commentWithDisplayName).isNotNull();
        assertThat(commentWithDisplayName.id()).isEqualTo(51);
        assertThat(commentWithDisplayName.content()).isEqualTo("Observación técnica");
        assertThat(commentWithDisplayName.author()).isEqualTo("Arquitecto");

        var responseWithUniqueName = GitPullRequestCommentResponse.builder()
                .id(52)
                .status("active")
                .comments(List.of(
                        GitPullRequestCommentResponse.CommentItemResponse.builder()
                                .id(2)
                                .content("Review 2")
                                .author(GitPullRequestCommentResponse.IdentityRefDTO.builder()
                                        .uniqueName("bot@test.com")
                                        .build())
                                .build()
                ))
                .build();
        PullRequestComment commentWithUniqueName = PullRequestMapper.toDomainComment(
                responseWithUniqueName);
        assertThat(commentWithUniqueName.author()).isEqualTo("bot@test.com");
    }
}

