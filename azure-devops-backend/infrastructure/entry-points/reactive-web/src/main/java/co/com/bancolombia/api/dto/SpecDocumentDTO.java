package co.com.bancolombia.api.dto;

import co.com.bancolombia.model.spec.SpecDocument;

/**
 * Representación HTTP de un documento de especificación funcional o técnica.
 *
 * <p>Desacopla el contrato externo de la entidad de dominio {@link SpecDocument}.
 *
 * @param name    Nombre identificador del documento (ej. "ideas_planning_q3.md")
 * @param content Contenido completo en Markdown
 * @param path    Ruta física o relativa del documento
 */
public record SpecDocumentDTO(String name, String content, String path) {

    public SpecDocumentDTO {
        if (content == null) {
            content = "";
        }
    }

    /**
     * Mapea desde la entidad pura de dominio {@link SpecDocument}.
     *
     * @param document entidad de dominio
     * @return DTO listo para serializar a JSON
     */
    public static SpecDocumentDTO fromDomain(SpecDocument document) {
        if (document == null) {
            return null;
        }
        return new SpecDocumentDTO(document.name(), document.content(), document.path());
    }
}
