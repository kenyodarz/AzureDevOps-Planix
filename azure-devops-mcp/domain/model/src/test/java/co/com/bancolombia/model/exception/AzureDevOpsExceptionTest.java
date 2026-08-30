package co.com.bancolombia.model.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las tres excepciones de dominio de la <b>Fase 06</b> y su código estable.
 *
 * <p>Antes de esta fase el repositorio tenía <b>cero</b>. Estas pruebas fijan lo único que un
 * cliente puede dar por seguro: que cada situación tiene su código y que ese código no cambia.
 */
class AzureDevOpsExceptionTest {

    @Test
    @DisplayName("GIVEN las tres excepciones WHEN se consultan THEN cada una expone su codigo estable")
    void givenTheThreeExceptions_whenAskedForCode_thenEachHasItsOwn() {
        // Assert (THEN) — estos tres literales son CONTRATO PÚBLICO (DP-06 §0.1(a)).
        assertEquals("AZDO_NOT_FOUND", WorkItemNotFoundException.CODE);
        assertEquals("AZDO_UNAUTHORIZED", AzureDevOpsUnauthorizedException.CODE);
        assertEquals("AZDO_UNAVAILABLE", AzureDevOpsUnavailableException.CODE);
    }

    @Test
    @DisplayName("GIVEN una excepcion de dominio WHEN toClientMessage THEN devuelve 'CODIGO: mensaje'")
    void givenDomainException_whenToClientMessage_thenCodeAndMessage() {
        // Arrange (GIVEN)
        var error = new AzureDevOpsUnavailableException("Reintente mas tarde.");

        // Act (WHEN) + Assert (THEN)
        assertEquals("AZDO_UNAVAILABLE: Reintente mas tarde.", error.toClientMessage());
        assertEquals("AZDO_UNAVAILABLE", error.getErrorCode());
    }

    @Test
    @DisplayName("GIVEN una causa tecnica WHEN se construye la excepcion THEN la conserva sin exponerla en el mensaje")
    void givenTechnicalCause_whenBuilt_thenCauseIsKeptButNotExposed() {
        // Arrange (GIVEN)
        var cause = new IllegalStateException("TF400813 detalle interno");

        // Act (WHEN)
        var error = new AzureDevOpsUnauthorizedException("Verifique el token de servicio.", cause);

        // Assert (THEN) — la causa viaja para el log, no para el cliente.
        assertSame(cause, error.getCause());
        assertTrue(error.toClientMessage().contains("Verifique el token de servicio."));
        assertEquals("AZDO_UNAUTHORIZED: Verifique el token de servicio.", error.toClientMessage());
    }

    @Test
    @DisplayName("GIVEN el constructor sin causa WHEN se construye THEN no hay causa y el codigo se mantiene")
    void givenNoCause_whenBuilt_thenCauseIsNull() {
        // Act (WHEN)
        var error = new WorkItemNotFoundException("No existe.");

        // Assert (THEN)
        assertNull(error.getCause());
        assertEquals(WorkItemNotFoundException.CODE, error.getErrorCode());
    }

    @Test
    @DisplayName("GIVEN un 404 con su causa tecnica WHEN se construye THEN la causa se conserva y el mensaje sigue siendo neutro")
    void givenNotFoundWithCause_whenBuilt_thenCauseIsKept() {
        // Arrange (GIVEN)
        var cause = new IllegalStateException("404 Not Found from GET /_apis/wit/workItems/1");

        // Act (WHEN)
        var error = new WorkItemNotFoundException(
                "El recurso solicitado no existe o no es accesible en Azure DevOps.", cause);

        // Assert (THEN)
        assertSame(cause, error.getCause());
        assertEquals(WorkItemNotFoundException.CODE, error.getErrorCode());
        assertTrue(error.toClientMessage().startsWith("AZDO_NOT_FOUND: "));
    }

    @Test
    @DisplayName("GIVEN las tres excepciones WHEN se comprueba su jerarquia THEN todas son AzureDevOpsException")
    void givenTheThreeExceptions_whenCheckingHierarchy_thenAllShareTheSameRoot() {
        // Assert (THEN) — un cliente que no quiera distinguir puede capturar una sola cosa.
        assertTrue(new WorkItemNotFoundException("x") instanceof AzureDevOpsException);
        assertTrue(new AzureDevOpsUnauthorizedException("x") instanceof AzureDevOpsException);
        assertTrue(new AzureDevOpsUnavailableException("x", null) instanceof AzureDevOpsException);
    }

    @Test
    @DisplayName("GIVEN una excepcion de dominio WHEN se inspecciona THEN NO conoce ningun concepto HTTP")
    void givenDomainException_whenInspected_thenNoHttpLeak() {
        // Assert (THEN) — dominio puro: la correspondencia con un codigo de estado vive en el
        // adaptador, que es la capa a la que le corresponde saber que es un 404.
        assertTrue(java.util.Arrays.stream(AzureDevOpsException.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("status")));
        assertEquals("java.lang.RuntimeException",
                AzureDevOpsException.class.getSuperclass().getName());
    }
}

