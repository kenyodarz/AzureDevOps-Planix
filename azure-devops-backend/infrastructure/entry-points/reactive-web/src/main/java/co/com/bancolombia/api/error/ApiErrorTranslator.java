package co.com.bancolombia.api.error;

import co.com.bancolombia.model.agent.exceptions.AgentExecutionException;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.agent.exceptions.InvalidAgentRequestException;
import co.com.bancolombia.model.agent.exceptions.TaskNotFoundException;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Traducción de errores del entry-point: <b>un solo criterio</b> para todas las rutas (D-16,
 * DP-06).
 *
 * <p>Hasta la Fase 07 convivían dos: {@code TaskHandler} traducía cinco excepciones a cinco
 * códigos y {@code Handler} devolvía <b>400 para cualquier error</b> en seis bloques
 * {@code onErrorResume} casi idénticos. Un 400 significa «te equivocaste tú»; devolverlo cuando el
 * que se cayó fue el agente o la base de datos es la misma familia de fallo que cerraron las fases
 * 05 y 06: <b>información que se pierde por el camino</b>. Un BFF cuya misión es validar y traducir
 * no puede tener dos criterios de traducción en el mismo fichero.
 *
 * <p><b>Mapa acordado en DP-06.</b> Parte del que ya aplicaba {@code TaskHandler} y lo extiende al
 * resto del entry-point:
 *
 * <table border="1">
 *   <caption>Excepción de dominio → código HTTP</caption>
 *   <tr><th>Excepción</th><th>Código</th><th>Por qué</th></tr>
 *   <tr><td>{@link TaskNotFoundException}</td><td>404</td><td>el recurso pedido no existe</td></tr>
 *   <tr><td>{@link InvalidAgentRequestException}, {@link IllegalArgumentException}</td>
 *       <td>400</td><td>la petición del cliente es inválida</td></tr>
 *   <tr><td>{@link AgentUnavailableException}</td><td>503</td><td>el agente no está disponible</td></tr>
 *   <tr><td>{@link AgentExecutionException}</td><td>502</td><td>el agente respondió mal</td></tr>
 *   <tr><td>{@link TimeoutException}</td><td>504</td><td>el agente no respondió a tiempo</td></tr>
 *   <tr><td>cualquier otra</td><td>500</td><td>fallo del BFF; <b>no</b> es culpa del cliente</td></tr>
 * </table>
 *
 * <p><b>El cuerpo no cambia:</b> sigue siendo {@code {"error": "<mensaje>"}}, exactamente el mismo
 * que devolvían los dos criterios anteriores. Lo único que cambia es el código, que es justo lo que
 * D-16 pedía cambiar.
 *
 * <p>Es una utilidad <b>sin estado</b> y sin inyección deliberadamente: así no altera el
 * {@code @ContextConfiguration} de las pruebas de caracterización (misma razón que D-42).
 */
@Slf4j
public final class ApiErrorTranslator {

    private static final String ERROR_KEY = "error";

    private ApiErrorTranslator() {
        throw new IllegalStateException("Clase de utilidad, no instanciable");
    }

    /**
     * Traduce cualquier error a la respuesta HTTP que le corresponde según el mapa de DP-06.
     *
     * @param error error propagado por la cadena reactiva
     * @return respuesta con el código del mapa y el cuerpo {@code {"error": "..."}}
     */
    public static Mono<ServerResponse> toResponse(Throwable error) {
        HttpStatus status = toStatus(error);
        logAccordingTo(status, error);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(ERROR_KEY, String.valueOf(error.getMessage())));
    }

    /**
     * Código HTTP que corresponde a una excepción. Expuesto aparte de {@link #toResponse} porque el
     * stream SSE no puede responder con un código —ya escribió la cabecera— pero sí necesita
     * clasificar el fallo.
     *
     * @param error error a clasificar
     * @return código HTTP según el mapa de DP-06
     */
    public static HttpStatus toStatus(Throwable error) {
        if (error instanceof TaskNotFoundException || error instanceof SpecNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (error instanceof InvalidAgentRequestException
                || error instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (error instanceof AgentUnavailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (error instanceof AgentExecutionException) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (error instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static void logAccordingTo(HttpStatus status, Throwable error) {
        if (status.is5xxServerError()) {
            log.error("Fallo al atender una petición del frontend", error);
        } else {
            log.warn("Petición rechazada: {}", error.getMessage());
        }
    }
}

