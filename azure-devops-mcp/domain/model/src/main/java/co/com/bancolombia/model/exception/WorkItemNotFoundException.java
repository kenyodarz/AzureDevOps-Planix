package co.com.bancolombia.model.exception;

/**
 * El recurso pedido no existe en Azure DevOps, o existe pero no es visible con la credencial actual.
 *
 * <p>Corresponde a un {@code 404}. <b>DP-06 §0.1(c)</b> pidió distinguirlo explícitamente de un
 * problema de credenciales y de una caída del proveedor: no es lo mismo «ese work item no existe»
 * —que casi siempre es un error del llamante— que «no tengo permiso» o «Azure DevOps está caído».
 *
 * <p>⚠️ <b>Ojo con el 404 de Azure DevOps.</b> Un identificador inexistente y un identificador que
 * existe pero está fuera del alcance del PAT producen <b>el mismo estado</b>. Por eso el mensaje no
 * afirma que el recurso no exista: dice que no existe <b>o</b> no es accesible.
 */
public class WorkItemNotFoundException extends AzureDevOpsException {

    /** Código estable. <b>Contrato público</b> (DP-06 §0.1(a)). */
    public static final String CODE = "AZDO_NOT_FOUND";

    public WorkItemNotFoundException(String message) {
        super(CODE, message);
    }

    public WorkItemNotFoundException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

