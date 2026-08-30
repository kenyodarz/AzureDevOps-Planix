package co.com.bancolombia.model.exception;

/**
 * Azure DevOps no pudo atender la operación: está caído, tardó más de lo tolerado, rechazó la
 * petición por un motivo que no es ni «no existe» ni «no autorizado», o el cortacircuito está
 * abierto y ni siquiera se intentó.
 *
 * <p>Es la excepción <b>por defecto</b> de la traducción: cubre {@code 5xx}, los {@code 4xx} que no
 * son 401/403/404, los timeouts por operación introducidos por <b>D-24</b> en esta misma fase, los
 * fallos de red y el {@code CallNotPermittedException} de Resilience4j.
 *
 * <p><b>Por qué el circuito abierto cae aquí y no en una excepción propia.</b> Porque desde el punto
 * de vista del cliente MCP significa exactamente lo mismo que un 503: «ahora no puedo, vuelve a
 * intentarlo». Darle un código distinto obligaría al cliente a tratar dos casos donde solo hay una
 * decisión. <b>DP-06 §0.1(c)</b> fijó cuatro excepciones, no seis, justamente por eso.
 */
public class AzureDevOpsUnavailableException extends AzureDevOpsException {

    /** Código estable. <b>Contrato público</b> (DP-06 §0.1(a)). */
    public static final String CODE = "AZDO_UNAVAILABLE";

    public AzureDevOpsUnavailableException(String message) {
        super(CODE, message);
    }

    public AzureDevOpsUnavailableException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

