package co.com.bancolombia.model.exception;

/**
 * Raíz de todos los fallos de Azure DevOps expresados en el lenguaje del dominio (D-13,
 * <b>Fase 06</b>).
 *
 * <h2>Por qué existe</h2>
 *
 * <p>Hasta esta fase <b>no había una sola excepción de dominio en el repositorio</b>. Un {@code 401},
 * un {@code 404} o un {@code TF401232} viajaban <b>crudos</b> hasta el cliente MCP como
 * {@code WebClientResponseException} —una excepción de <b>Spring</b>— a través de un contrato que
 * debería ser de dominio. Eso incumplía dos reglas a la vez: {@code spring-rules.md} exige que los
 * {@code driven-adapters} «capturen excepciones técnicas y las traduzcan a excepciones tipadas del
 * dominio», y §1 del plan maestro prohíbe que «versiones, formatos, <b>códigos de error</b> y
 * nomenclatura se filtren aguas arriba».
 *
 * <h2>El contrato: código estable + mensaje neutro</h2>
 *
 * <p><b>DP-06 §0.1(a)</b> decidió que el cliente MCP recibe <b>las dos cosas</b>:
 *
 * <ul>
 *   <li>un {@link #getErrorCode() código estable} —{@code AZDO_NOT_FOUND}, {@code AZDO_UNAUTHORIZED},
 *       {@code AZDO_UNAVAILABLE}— pensado para que un cliente pueda ramificar por él sin hacer
 *       {@code catch} por texto, que es lo frágil;</li>
 *   <li>un <b>mensaje neutro en español</b>, legible, que <b>no cita nomenclatura de Azure DevOps</b>.</li>
 * </ul>
 *
 * <p><b>DP-06 §0.1(b):</b> el cuerpo de respuesta original de Azure DevOps <b>no se propaga</b>. Se
 * registra íntegro en el log del MCP, que es donde le sirve a quien opera, y se oculta al cliente,
 * que es donde filtraría nomenclatura ajena.
 *
 * <h2>Dominio puro</h2>
 *
 * <p>Sin Spring, sin HTTP, sin Jackson: esta jerarquía no conoce el concepto de «código de estado».
 * La correspondencia entre un estado HTTP y una de estas excepciones vive en el <b>adaptador</b>
 * ({@code AzureDevOpsErrorTranslator}), que es la capa a la que le corresponde saber qué es un 404.
 *
 * @see WorkItemNotFoundException
 * @see AzureDevOpsUnauthorizedException
 * @see AzureDevOpsUnavailableException
 */
public abstract class AzureDevOpsException extends RuntimeException {

    private static final String CODE_SEPARATOR = ": ";

    private final String errorCode;

    protected AzureDevOpsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AzureDevOpsException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Código estable del error. <b>Es contrato público</b> desde esta fase: cambiarlo rompe en
     * silencio a cualquier cliente que ramifique por él.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Forma exacta que ve el cliente MCP: {@code CODIGO: mensaje neutro}.
     *
     * <p>Vive aquí, y no en el entry-point, para que la forma sea <b>una sola</b> y no dependa de
     * quién la componga.
     */
    public String toClientMessage() {
        return errorCode + CODE_SEPARATOR + getMessage();
    }
}

