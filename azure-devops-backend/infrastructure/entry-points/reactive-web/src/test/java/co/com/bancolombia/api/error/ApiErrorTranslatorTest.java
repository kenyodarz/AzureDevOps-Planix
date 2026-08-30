package co.com.bancolombia.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.agent.exceptions.AgentExecutionException;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.agent.exceptions.InvalidAgentRequestException;
import co.com.bancolombia.model.agent.exceptions.TaskNotFoundException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Fija el mapa acordado en DP-06 (D-16).
 *
 * <p>Es la prueba que impide que vuelvan a convivir dos criterios de traducción de errores en el
 * entry-point: si alguien añade un {@code onErrorResume} con su propio código, esta clase deja de
 * ser la única fuente de verdad y se nota aquí.
 */
class ApiErrorTranslatorTest {

    @Test
    @DisplayName("GIVEN una tarea inexistente WHEN se traduce THEN 404")
    void givenTaskNotFound_whenTranslated_then404() {
        assertThat(ApiErrorTranslator.toStatus(new TaskNotFoundException("task-1")))
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GIVEN una petición inválida WHEN se traduce THEN 400")
    void givenInvalidRequest_whenTranslated_then400() {
        assertThat(ApiErrorTranslator.toStatus(new InvalidAgentRequestException("sin texto")))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiErrorTranslator.toStatus(new IllegalArgumentException("maxResults inválido")))
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GIVEN el agente no disponible WHEN se traduce THEN 503")
    void givenAgentUnavailable_whenTranslated_then503() {
        assertThat(ApiErrorTranslator.toStatus(
                new AgentUnavailableException("sin conexión", new java.net.ConnectException())))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("GIVEN el agente responde mal WHEN se traduce THEN 502")
    void givenAgentExecutionFails_whenTranslated_then502() {
        assertThat(ApiErrorTranslator.toStatus(new AgentExecutionException("respuesta ilegible")))
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("GIVEN el agente no responde a tiempo WHEN se traduce THEN 504")
    void givenTimeout_whenTranslated_then504() {
        assertThat(ApiErrorTranslator.toStatus(new TimeoutException("120 s")))
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * El corazón de D-16: lo que no es culpa del cliente <b>ya no</b> sale como 400. Antes de la
     * Fase 07, esta misma excepción se traducía a 400 en las seis rutas del {@code Handler}.
     */
    @Test
    @DisplayName("GIVEN un fallo interno WHEN se traduce THEN 500, no 400")
    void givenInternalFailure_whenTranslated_then500NotBadRequest() {
        HttpStatus status = ApiErrorTranslator.toStatus(
                new IllegalStateException("pgvector caído"));

        assertThat(status)
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .isNotEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GIVEN un error sin mensaje WHEN se traduce THEN el cuerpo no revienta")
    void givenErrorWithoutMessage_whenTranslated_thenBodyIsBuilt() {
        assertThat(ApiErrorTranslator.toResponse(new IllegalStateException()).block())
                .isNotNull();
    }
}


