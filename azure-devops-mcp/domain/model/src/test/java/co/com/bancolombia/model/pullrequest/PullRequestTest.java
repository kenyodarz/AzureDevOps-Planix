package co.com.bancolombia.model.pullrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias para las entidades de dominio {@link PullRequest} y {@link GitChange}.
 */
class PullRequestTest {

    @Test
    @DisplayName("GIVEN datos validos WHEN se construye un PullRequest THEN se preservan los valores")
    void givenValidData_whenBuilt_thenFieldsArePreserved() {
        List<Integer> workItemIds = List.of(101, 102);

        PullRequest pr = PullRequest.builder()
                .pullRequestId(42)
                .title("feat: implement PR evaluation")
                .description("Detailed description")
                .status("active")
                .sourceRefName("refs/heads/feature/pr-eval")
                .targetRefName("refs/heads/main")
                .repositoryId("repo-uuid-123")
                .createdBy("Jane Doe")
                .creationDate("2026-09-08T10:00:00Z")
                .workItemIds(workItemIds)
                .build();

        assertEquals(42, pr.pullRequestId());
        assertEquals("feat: implement PR evaluation", pr.title());
        assertEquals("Detailed description", pr.description());
        assertEquals("active", pr.status());
        assertEquals("refs/heads/feature/pr-eval", pr.sourceRefName());
        assertEquals("refs/heads/main", pr.targetRefName());
        assertEquals("repo-uuid-123", pr.repositoryId());
        assertEquals("Jane Doe", pr.createdBy());
        assertEquals("2026-09-08T10:00:00Z", pr.creationDate());
        assertEquals(List.of(101, 102), pr.workItemIds());
    }

    @Test
    @DisplayName("GIVEN una lista mutable WHEN se crea el PullRequest THEN la coleccion queda inmutable y protegida")
    void givenMutableList_whenBuilt_thenCollectionIsDefensivelyCopiedAndUnmodifiable() {
        List<Integer> mutableIds = new ArrayList<>();
        mutableIds.add(201);

        PullRequest pr = PullRequest.builder()
                .pullRequestId(1)
                .workItemIds(mutableIds)
                .build();

        // Mutar la lista original externa no debe alterar la entidad
        mutableIds.add(202);
        assertEquals(1, pr.workItemIds().size());
        assertEquals(201, pr.workItemIds().getFirst());

        // Modificar la lista interna debe fallar con UnsupportedOperationException
        List<Integer> internalIds = pr.workItemIds();
        assertThrows(UnsupportedOperationException.class, () -> internalIds.add(203));
    }

    @Test
    @DisplayName("GIVEN workItemIds nulos WHEN se construye un PullRequest THEN se conserva el nulo sin error")
    void givenNullWorkItemIds_whenBuilt_thenNullIsPreserved() {
        PullRequest pr = PullRequest.builder()
                .pullRequestId(10)
                .workItemIds(null)
                .build();

        assertNull(pr.workItemIds());
    }

    @Test
    @DisplayName("GIVEN un PullRequest existente WHEN se usa toBuilder THEN se genera una copia modificada")
    void givenExistingPullRequest_whenToBuilder_thenNewInstanceIsCreated() {
        PullRequest original = PullRequest.builder()
                .pullRequestId(50)
                .title("Initial title")
                .status("active")
                .build();

        PullRequest updated = original.toBuilder()
                .title("Updated title")
                .status("completed")
                .build();

        assertEquals(50, updated.pullRequestId());
        assertEquals("Updated title", updated.title());
        assertEquals("completed", updated.status());
        assertEquals("Initial title", original.title());
    }

    @Test
    @DisplayName("GIVEN datos validos WHEN se construye un GitChange THEN se preservan los valores y toBuilder")
    void givenValidData_whenGitChangeBuilt_thenFieldsArePreserved() {
        GitChange change = GitChange.builder()
                .itemPath("/src/Main.java")
                .changeType("edit")
                .originalObjectId("abc1234")
                .newObjectId("def5678")
                .build();

        assertEquals("/src/Main.java", change.itemPath());
        assertEquals("edit", change.changeType());
        assertEquals("abc1234", change.originalObjectId());
        assertEquals("def5678", change.newObjectId());

        GitChange copy = change.toBuilder()
                .changeType("delete")
                .build();

        assertEquals("delete", copy.changeType());
        assertEquals("/src/Main.java", copy.itemPath());
    }
}
