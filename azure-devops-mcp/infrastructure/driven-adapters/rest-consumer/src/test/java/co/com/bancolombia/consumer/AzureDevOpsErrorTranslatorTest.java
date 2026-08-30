package co.com.bancolombia.consumer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import co.com.bancolombia.model.exception.AzureDevOpsUnauthorizedException;
import co.com.bancolombia.model.exception.AzureDevOpsUnavailableException;
import co.com.bancolombia.model.exception.WorkItemNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El contrato de errores de la <b>Fase 06</b>, verificado en el único sitio donde se decide.
 *
 * <p>Estas pruebas son la contrapartida de las de caracterización: aquéllas comprobaban que el error
 * salía crudo, éstas comprueban <b>en qué</b> se traduce y, sobre todo, <b>qué NO viaja</b> con él.
 */
class AzureDevOpsErrorTranslatorTest {

    private static final String OPERATION = "getWorkItem";
    private static final String AZURE_BODY = "{\"message\":\"TF401232: no existe\"}";

    private static WebClientResponseException httpError(HttpStatus status) {
        return WebClientResponseException.create(status.value(), status.getReasonPhrase(),
                HttpHeaders.EMPTY, AZURE_BODY.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("GIVEN un 404 WHEN se traduce THEN se obtiene WorkItemNotFoundException con codigo AZDO_NOT_FOUND")
    void givenNotFound_whenTranslate_thenWorkItemNotFound() {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION,
                httpError(HttpStatus.NOT_FOUND));

        // Assert (THEN)
        var domainError = assertInstanceOf(WorkItemNotFoundException.class, translated);
        assertEquals(WorkItemNotFoundException.CODE, domainError.getErrorCode());
    }

    @ParameterizedTest(name = "un {0} se traduce a AZDO_UNAUTHORIZED")
    @ValueSource(ints = {401, 403})
    @DisplayName("GIVEN un fallo de credencial WHEN se traduce THEN se obtiene AzureDevOpsUnauthorizedException")
    void givenCredentialFailure_whenTranslate_thenUnauthorized(int status) {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION,
                httpError(HttpStatus.valueOf(status)));

        // Assert (THEN)
        var domainError = assertInstanceOf(AzureDevOpsUnauthorizedException.class, translated);
        assertEquals(AzureDevOpsUnauthorizedException.CODE, domainError.getErrorCode());
    }

    @ParameterizedTest(name = "un {0} se traduce a AZDO_UNAVAILABLE")
    @ValueSource(ints = {400, 409, 429, 500, 502, 503})
    @DisplayName("GIVEN cualquier otro estado WHEN se traduce THEN se obtiene AzureDevOpsUnavailableException")
    void givenAnyOtherStatus_whenTranslate_thenUnavailable(int status) {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION,
                httpError(HttpStatus.valueOf(status)));

        // Assert (THEN)
        var domainError = assertInstanceOf(AzureDevOpsUnavailableException.class, translated);
        assertEquals(AzureDevOpsUnavailableException.CODE, domainError.getErrorCode());
    }

    @Test
    @DisplayName("GIVEN un fallo HTTP WHEN se traduce THEN el cuerpo original de Azure DevOps NO viaja en el mensaje (DP-06 §0.1(b))")
    void givenHttpFailure_whenTranslate_thenAzureBodyIsNotPropagated() {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION,
                httpError(HttpStatus.NOT_FOUND));

        // Assert (THEN) — el codigo TFxxxxxx queda en el log, no en el contrato. Es §1 del plan
        // maestro: «los codigos de error de Azure DevOps no se filtran aguas arriba».
        var domainError = assertInstanceOf(WorkItemNotFoundException.class, translated);
        assertFalse(domainError.toClientMessage().contains("TF401232"));
        assertFalse(domainError.toClientMessage().contains(AZURE_BODY));
        assertTrue(domainError.toClientMessage().startsWith(WorkItemNotFoundException.CODE + ": "));
    }

    @Test
    @DisplayName("GIVEN el cortacircuito abierto WHEN se traduce THEN se obtiene AZDO_UNAVAILABLE y no un fallo de Resilience4j")
    void givenOpenCircuit_whenTranslate_thenUnavailable() {
        // Arrange (GIVEN)
        CircuitBreaker breaker = CircuitBreaker.ofDefaults("workItemQuery");
        breaker.transitionToOpenState();

        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION,
                CallNotPermittedException.createCallNotPermittedException(breaker));

        // Assert (THEN)
        assertInstanceOf(AzureDevOpsUnavailableException.class, translated);
    }

    @Test
    @DisplayName("GIVEN un timeout por operacion WHEN se traduce THEN se obtiene AZDO_UNAVAILABLE (D-24)")
    void givenTimeout_whenTranslate_thenUnavailable() {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate("getWorkItemsBatch",
                new TimeoutException("Did not observe any item or terminal signal"));

        // Assert (THEN)
        assertInstanceOf(AzureDevOpsUnavailableException.class, translated);
    }

    @Test
    @DisplayName("GIVEN un fallo de red WHEN se traduce THEN tampoco escapa crudo")
    void givenNetworkFailure_whenTranslate_thenUnavailable() {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION,
                new IllegalStateException("Connection prematurely closed"));

        // Assert (THEN) — la red de seguridad: lo que nadie previo tampoco viaja crudo.
        assertInstanceOf(AzureDevOpsUnavailableException.class, translated);
    }

    @Test
    @DisplayName("GIVEN un error YA traducido WHEN se traduce otra vez THEN se devuelve el mismo, sin anidar")
    void givenDomainError_whenTranslateAgain_thenIdempotent() {
        // Arrange (GIVEN)
        var original = new WorkItemNotFoundException("no existe");

        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION, original);

        // Assert (THEN)
        assertSame(original, translated);
    }

    @Test
    @DisplayName("GIVEN una operacion WHEN se pide la funcion THEN traduce igual que el metodo directo")
    void givenOperation_whenForOperation_thenTranslatesTheSameWay() {
        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.forOperation("queryByWiql")
                .apply(httpError(HttpStatus.UNAUTHORIZED));

        // Assert (THEN)
        assertInstanceOf(AzureDevOpsUnauthorizedException.class, translated);
        assertTrue(translated.getMessage().contains("queryByWiql"));
    }

    @Test
    @DisplayName("GIVEN un fallo de conexion de WebClient WHEN se traduce THEN se obtiene AZDO_UNAVAILABLE")
    void givenRequestException_whenTranslate_thenUnavailable() {
        // Arrange (GIVEN)
        var requestFailure = new WebClientRequestException(new java.io.IOException("host caido"),
                org.springframework.http.HttpMethod.GET, java.net.URI.create("https://dev.azure.com"),
                HttpHeaders.EMPTY);

        // Act (WHEN)
        Throwable translated = AzureDevOpsErrorTranslator.translate(OPERATION, requestFailure);

        // Assert (THEN)
        assertInstanceOf(AzureDevOpsUnavailableException.class, translated);
    }
}

