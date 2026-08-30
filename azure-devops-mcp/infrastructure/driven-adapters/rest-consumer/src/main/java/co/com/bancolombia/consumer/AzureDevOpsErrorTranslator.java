package co.com.bancolombia.consumer;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import co.com.bancolombia.model.exception.AzureDevOpsException;
import co.com.bancolombia.model.exception.AzureDevOpsUnauthorizedException;
import co.com.bancolombia.model.exception.AzureDevOpsUnavailableException;
import co.com.bancolombia.model.exception.WorkItemNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * <b>El único</b> traductor de fallos técnicos de Azure DevOps a excepciones de dominio
 * (D-13, <b>Fase 06</b>, decisión <b>B-08</b>).
 *
 * <h2>Uno, no tres</h2>
 *
 * <p>Desde la Fase 05 hay <b>tres</b> adaptadores —{@link WorkItemQueryAdapter},
 * {@link WorkItemCommandAdapter} y {@link TeamScopeAdapter}— con <b>siete</b> puntos por los que hoy
 * escapaba un error crudo. Esta clase se comparte entre los tres exactamente con el mismo criterio
 * con el que la Fase 05 repartió los cinco mappers de la Fase 03: <b>si acabara copiada tres veces,
 * la fase habría fallado</b>. Cada adaptador aporta únicamente el <i>nombre de la operación</i>; la
 * regla de traducción vive aquí y en ningún otro sitio.
 *
 * <h2>Reparto de responsabilidades con el entry-point (B-08)</h2>
 *
 * <p><b>B-08</b> decidió que se traduce en <b>ambas</b> capas, con papeles distintos y sin solaparse:
 *
 * <table border="1">
 *   <caption>Quién hace qué</caption>
 *   <tr><th>Capa</th><th>Traduce</th><th>A</th></tr>
 *   <tr><td>Adaptador — <b>esta clase</b></td><td>HTTP, red, timeout, cortacircuito</td>
 *       <td>excepción de <b>dominio</b></td></tr>
 *   <tr><td>Entry-point — {@code McpErrorTranslator}</td><td>excepción de dominio</td>
 *       <td>mensaje MCP con código estable</td></tr>
 * </table>
 *
 * <p>Es el reparto que exige {@code spring-rules.md}: «los driven-adapters capturan excepciones
 * técnicas y las traducen a excepciones tipadas del dominio». El entry-point no sabe qué es un 404,
 * y el dominio tampoco: solo esta clase lo sabe.
 *
 * <h2>El cuerpo original se registra, no se propaga (DP-06 §0.1(b))</h2>
 *
 * <p>Cada fallo HTTP deja en el log, en {@code ERROR}, el estado y el <b>cuerpo íntegro</b> que
 * devolvió Azure DevOps —incluidos sus códigos {@code TFxxxxxx}—, que es lo que necesita quien
 * opera. Al cliente MCP no le llega nada de eso: recibe un código estable y un mensaje neutro. Así
 * se cumplen a la vez §1 del plan maestro («los códigos de error de Azure DevOps no se filtran
 * aguas arriba») y la necesidad de diagnóstico.
 *
 * <p>Esto absorbe además el {@code doOnError} que hasta ahora vivía suelto en {@code queryByWiql} y
 * que era el <b>único</b> tratamiento de errores del repositorio: registraba el cuerpo y no
 * traducía nada, y solo en uno de los siete puntos.
 *
 * <h2>Lo que esta clase NO rompe</h2>
 *
 * <p>El repliegue de rutas de <b>DP-04</b> sigue intacto: {@code ResolveTeamScopeUseCase} captura
 * con {@code onErrorResume} <b>cualquier</b> error del puerto, así que un fallo al resolver el
 * {@code AreaPath} o el {@code IterationPath} sigue cayendo a la concatenación —con su {@code WARN}
 * y su métrica— y <b>no</b> se convierte en un error para el cliente. Es lo que
 * <b>DP-06 §0.1(d)</b> ratificó: la tool falla, pero el repliegue se respeta.
 */
@Slf4j
public final class AzureDevOpsErrorTranslator {

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;

    private AzureDevOpsErrorTranslator() {
    }

    /**
     * Función lista para encadenarse con {@code onErrorMap} al final de cada llamada.
     *
     * @param operation nombre de la operación, solo para el mensaje y la traza. Es el nombre de la
     *                  tool MCP, que ya es contrato público, de modo que no filtra nada nuevo.
     */
    public static Function<Throwable, Throwable> forOperation(String operation) {
        return error -> translate(operation, error);
    }

    /**
     * Traduce un fallo técnico a la excepción de dominio que le corresponde.
     *
     * <p>Si el error <b>ya</b> es de dominio se devuelve tal cual: la traducción es idempotente, de
     * modo que encadenarla dos veces por descuido no anida excepciones ni pierde el código.
     */
    static Throwable translate(String operation, Throwable error) {
        if (error instanceof AzureDevOpsException alreadyTranslated) {
            return alreadyTranslated;
        }
        if (error instanceof WebClientResponseException httpError) {
            return fromHttpStatus(operation, httpError);
        }
        if (error instanceof CallNotPermittedException) {
            log.error("Cortacircuito abierto para la operacion '{}': no se intento la llamada.",
                    operation, error);
            return new AzureDevOpsUnavailableException(
                    "Azure DevOps no esta disponible en este momento y la operacion no se intento ("
                            + operation + "). Reintente mas tarde.", error);
        }
        if (error instanceof TimeoutException) {
            log.error("Timeout por operacion agotado en '{}'.", operation, error);
            return new AzureDevOpsUnavailableException(
                    "Azure DevOps no respondio dentro del tiempo maximo de espera (" + operation
                            + "). Reintente mas tarde.", error);
        }
        log.error("Fallo tecnico no clasificado en la operacion '{}'.", operation, error);
        return new AzureDevOpsUnavailableException(
                "No fue posible completar la operacion contra Azure DevOps (" + operation
                        + "). Reintente mas tarde.", error);
    }

    /**
     * Correspondencia entre estado HTTP y excepción de dominio, según <b>DP-06 §0.1(c)</b>.
     *
     * <p>Aquí es donde el cuerpo original de Azure DevOps se registra y <b>muere</b>: ninguna de las
     * tres ramas lo incorpora al mensaje que verá el cliente.
     */
    private static AzureDevOpsException fromHttpStatus(String operation,
            WebClientResponseException httpError) {
        HttpStatusCode status = httpError.getStatusCode();
        log.error("Azure DevOps respondio {} en la operacion '{}'. Cuerpo original: {}",
                status.value(), operation, httpError.getResponseBodyAsString());

        if (status.value() == NOT_FOUND) {
            return new WorkItemNotFoundException(
                    "El recurso solicitado no existe o no es accesible en Azure DevOps ("
                            + operation + ").", httpError);
        }
        if (status.value() == UNAUTHORIZED || status.value() == FORBIDDEN) {
            return new AzureDevOpsUnauthorizedException(
                    "La credencial configurada no permite ejecutar la operacion solicitada ("
                            + operation + "). Verifique el token de servicio.", httpError);
        }
        return new AzureDevOpsUnavailableException(
                "Azure DevOps no pudo atender la operacion solicitada (" + operation
                        + "). Reintente mas tarde.", httpError);
    }
}

