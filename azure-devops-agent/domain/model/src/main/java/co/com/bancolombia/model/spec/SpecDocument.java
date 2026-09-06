package co.com.bancolombia.model.spec;

/**
 * Entidad de dominio inmutable que representa un documento de especificación funcional o técnica.
 *
 * @param name    Nombre identificador del documento (ej. "ideas_planning_q3.md",
 *                "aegis_engine.md").
 * @param content Contenido completo del documento en formato Markdown.
 * @param path    Ruta física o lógica donde reside o se almacena el documento.
 */
public record SpecDocument(String name, String content, String path) {

    public SpecDocument {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del spec no puede ser nulo ni vacío");
        }
        if (content == null) {
            content = "";
        }
    }
}
