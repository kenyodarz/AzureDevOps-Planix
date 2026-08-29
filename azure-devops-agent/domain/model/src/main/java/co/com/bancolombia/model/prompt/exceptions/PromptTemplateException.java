package co.com.bancolombia.model.prompt.exceptions;

/**
 * Excepción de dominio para los fallos relacionados con las plantillas de prompt.
 *
 * <p>Los adaptadores traducen sus excepciones técnicas (por ejemplo {@code IOException}) a esta
 * excepción, de modo que el dominio nunca dependa de detalles de infraestructura.
 */
public class PromptTemplateException extends RuntimeException {

    public PromptTemplateException(String message) {
        super(message);
    }

    public PromptTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}

