package co.com.bancolombia.model.spec;

/**
 * Excepción de dominio lanzada cuando un documento de especificación requerido no es encontrado.
 */
public class SpecNotFoundException extends RuntimeException {

    public SpecNotFoundException(String specName) {
        super("No se encontró el documento de especificación: " + specName);
    }
}
