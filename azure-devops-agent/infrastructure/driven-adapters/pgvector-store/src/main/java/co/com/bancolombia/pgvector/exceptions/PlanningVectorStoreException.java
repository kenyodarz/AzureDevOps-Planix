package co.com.bancolombia.pgvector.exceptions;

/**
 * Excepción personalizada para errores del adaptador de pgvector.
 */
public class PlanningVectorStoreException extends RuntimeException {

    public PlanningVectorStoreException(String message) {
        super(message);
    }

    public PlanningVectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
