package co.com.bancolombia.model.pullrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias para la entidad inmutable de dominio {@link PullRequestComment}.
 */
class PullRequestCommentTest {

    @Test
    @DisplayName("GIVEN datos validos WHEN se construye PullRequestComment THEN se preservan los campos")
    void givenValidData_whenBuilt_thenFieldsArePreserved() {
        PullRequestComment comment = PullRequestComment.builder()
                .id(1001)
                .content("Revisión de arquitectura: cumple con Clean Architecture")
                .status("active")
                .author("Arquitecto MCP")
                .build();

        assertEquals(1001, comment.id());
        assertEquals("Revisión de arquitectura: cumple con Clean Architecture", comment.content());
        assertEquals("active", comment.status());
        assertEquals("Arquitecto MCP", comment.author());
    }

    @Test
    @DisplayName("GIVEN campos nulos WHEN se construye PullRequestComment THEN se aceptan sin excepcion")
    void givenNullFields_whenBuilt_thenAllowsNulls() {
        PullRequestComment comment = PullRequestComment.builder()
                .content("Solo contenido")
                .build();

        assertNull(comment.id());
        assertEquals("Solo contenido", comment.content());
        assertNull(comment.status());
        assertNull(comment.author());
    }

    @Test
    @DisplayName("GIVEN un PullRequestComment WHEN se usa toBuilder THEN se genera una copia con cambios")
    void givenExistingComment_whenToBuilder_thenNewInstanceCreatedWithModifications() {
        PullRequestComment original = PullRequestComment.builder()
                .id(1)
                .content("Comentario inicial")
                .status("active")
                .author("Reviewer 1")
                .build();

        PullRequestComment modified = original.toBuilder()
                .content("Comentario corregido")
                .status("fixed")
                .build();

        assertEquals(1, modified.id());
        assertEquals("Comentario corregido", modified.content());
        assertEquals("fixed", modified.status());
        assertEquals("Reviewer 1", modified.author());

        assertEquals("Comentario inicial", original.content());
        assertEquals("active", original.status());
    }
}
