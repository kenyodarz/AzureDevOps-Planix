package co.com.bancolombia.model.exception;

/**
 * La credencial con la que el MCP habla con Azure DevOps no es válida o no alcanza para la
 * operación pedida.
 *
 * <p>Corresponde a un {@code 401} o a un {@code 403}. <b>DP-06 §0.1(c)</b> pidió separarlo del resto
 * porque es el único fallo cuya causa <b>no está del lado del llamante ni del proveedor, sino de la
 * configuración de este servidor</b>: un PAT caducado, revocado o —el caso que la Fase 02 documentó
 * en {@code RestConsumerConfig}— entregado en crudo en lugar de {@code Base64(":" + PAT)}, que
 * produce un 401 mudo.
 *
 * <p>Que tenga código propio permite que quien opere el sistema distinga de un vistazo «hay que
 * rotar la credencial» de «Azure DevOps está caído», dos incidencias con respuestas opuestas.
 */
public class AzureDevOpsUnauthorizedException extends AzureDevOpsException {

    /** Código estable. <b>Contrato público</b> (DP-06 §0.1(a)). */
    public static final String CODE = "AZDO_UNAUTHORIZED";

    public AzureDevOpsUnauthorizedException(String message) {
        super(CODE, message);
    }

    public AzureDevOpsUnauthorizedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

