package co.com.bancolombia.mcp.error;

import co.com.bancolombia.model.exception.AzureDevOpsUnauthorizedException;
import co.com.bancolombia.model.exception.AzureDevOpsUnavailableException;
import co.com.bancolombia.model.exception.WorkItemNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La <b>forma exacta</b> del error que ve el cliente MCP — contrato público desde la Fase 06.
 *
 * <p>Estas aserciones son tan contrato como los nombres de campo del cable que congela
 * {@code McpToolDtoMapperTest}: el agente y el BFF son clientes reales, y desde el momento en que
 * pueden ramificar por el código, cambiarlo los rompe en silencio.
 */
class McpErrorTranslatorTest {

    @Test
    @DisplayName("GIVEN una excepcion de dominio WHEN se traduce THEN el mensaje tiene la forma CODIGO: mensaje (DP-06 §0.1(a))")
    void givenDomainException_whenTranslate_thenStableCodePrefix() {
        // Arrange (GIVEN)
        var domainError = new WorkItemNotFoundException("El recurso solicitado no existe.");

        // Act (WHEN)
        Throwable translated = McpErrorTranslator.translate("getWorkItem", domainError);

        // Assert (THEN)
        assertInstanceOf(McpToolExecutionException.class, translated);
        assertEquals("AZDO_NOT_FOUND: El recurso solicitado no existe.", translated.getMessage());
        assertSame(domainError, translated.getCause());
    }

    @Test
    @DisplayName("GIVEN las tres excepciones de dominio WHEN se traducen THEN cada una conserva SU codigo, sin fundirse")
    void givenEachDomainException_whenTranslate_thenEachKeepsItsOwnCode() {
        // Act (WHEN) + Assert (THEN)
        assertTrue(McpErrorTranslator.translate("t", new WorkItemNotFoundException("x"))
                .getMessage().startsWith("AZDO_NOT_FOUND: "));
        assertTrue(McpErrorTranslator.translate("t", new AzureDevOpsUnauthorizedException("x"))
                .getMessage().startsWith("AZDO_UNAUTHORIZED: "));
        assertTrue(McpErrorTranslator.translate("t", new AzureDevOpsUnavailableException("x"))
                .getMessage().startsWith("AZDO_UNAVAILABLE: "));
    }

    @Test
    @DisplayName("GIVEN un fallo NO previsto WHEN se traduce THEN cae en MCP_INTERNAL_ERROR y no escapa crudo")
    void givenUnexpectedFailure_whenTranslate_thenInternalError() {
        // Arrange (GIVEN) — un defecto del propio entry-point, no un fallo de Azure DevOps.
        var bug = new NullPointerException("mapeo roto");

        // Act (WHEN)
        Throwable translated = McpErrorTranslator.translate("getWorkItemsBatch", bug);

        // Assert (THEN) — la diferencia entre «0 errores crudos» y «0 errores crudos de los que se
        // me ocurrieron».
        assertInstanceOf(McpToolExecutionException.class, translated);
        assertTrue(translated.getMessage().startsWith(McpErrorTranslator.INTERNAL_CODE + ": "));
        assertTrue(translated.getMessage().contains("getWorkItemsBatch"));
        assertSame(bug, translated.getCause());
    }

    @Test
    @DisplayName("GIVEN un fallo NO previsto WHEN se traduce THEN su mensaje tecnico NO viaja al cliente")
    void givenUnexpectedFailure_whenTranslate_thenTechnicalDetailIsHidden() {
        // Act (WHEN)
        Throwable translated = McpErrorTranslator.translate("getWorkItem",
                new IllegalStateException("jdbc://usuario:clave@host"));

        // Assert (THEN)
        assertFalse(translated.getMessage().contains("jdbc"));
    }

    @Test
    @DisplayName("GIVEN un error YA traducido WHEN se traduce otra vez THEN se devuelve el mismo, sin anidar codigos")
    void givenAlreadyTranslated_whenTranslateAgain_thenIdempotent() {
        // Arrange (GIVEN)
        var original = new McpToolExecutionException("AZDO_NOT_FOUND: x", null);

        // Act (WHEN)
        Throwable translated = McpErrorTranslator.translate("getWorkItem", original);

        // Assert (THEN)
        assertSame(original, translated);
    }

    @Test
    @DisplayName("GIVEN una tool WHEN se pide la funcion THEN traduce igual que el metodo directo")
    void givenTool_whenForTool_thenTranslatesTheSameWay() {
        // Act (WHEN)
        Throwable translated = McpErrorTranslator.forTool("updateWorkItem")
                .apply(new AzureDevOpsUnavailableException("Reintente mas tarde."));

        // Assert (THEN)
        assertEquals("AZDO_UNAVAILABLE: Reintente mas tarde.", translated.getMessage());
    }
}

