package co.com.bancolombia.model.prompt.exceptions;

/**
 * Fallo al obtener o renderizar una plantilla de prompt.
 *
 * <p>Es una excepción <b>de dominio</b>: el caso de uso no debe enterarse de que hoy las
 * plantillas son ficheros en el classpath. Por eso las implementaciones envuelven aquí cualquier
 * {@code IOException} en lugar de propagarla.
 */
public class PromptTemplateException extends RuntimeException {

    public PromptTemplateException(String message) {
        super(message);
    }

    public PromptTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * La plantilla no existe o no pudo leerse.
     */
    public static PromptTemplateException notFound(String name, Throwable cause) {
        return new PromptTemplateException(
                "No se pudo obtener la plantilla de prompt '" + name + "'", cause);
    }

    /**
     * El llamador no aportó valor para una variable declarada en la plantilla.
     */
    public static PromptTemplateException missingVariable(String name, String variable) {
        return new PromptTemplateException("La plantilla de prompt '" + name
                + "' declara la variable '" + variable
                + "' y no se recibió ningún valor para ella");
    }
}

