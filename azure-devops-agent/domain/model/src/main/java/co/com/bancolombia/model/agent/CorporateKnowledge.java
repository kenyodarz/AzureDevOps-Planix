package co.com.bancolombia.model.agent;

/**
 * Documentación corporativa que alimenta los prompts del agente.
 *
 * <p>Agrupa los tres cuerpos de conocimiento que hasta la Fase 04 viajaban sueltos como
 * {@code String} por los constructores de cuatro handlers, sin tipo ni validación.
 *
 * @param storyTemplate     plantilla HU/HA corporativa
 * @param agileGuide        guía de agilidad de la célula
 * @param qualityStandards  estándares de auditoría de calidad
 */
public record CorporateKnowledge(String storyTemplate, String agileGuide, String qualityStandards) {

    private static final String SECTION_SEPARATOR = "\n\n";

    public CorporateKnowledge {
        requireContent(storyTemplate, "La plantilla de historia corporativa");
        requireContent(agileGuide, "La guía de agilidad");
        requireContent(qualityStandards, "Los estándares de auditoría de calidad");
    }

    /**
     * Guía de agilidad y estándares de auditoría concatenados, tal como los espera la plantilla de
     * auditoría.
     *
     * <p>La composición es conocimiento de negocio —qué documentos forman el criterio de auditoría—
     * y no detalle de un flujo, por lo que vive aquí y no en el handler.
     *
     * @return el texto combinado
     */
    public String auditStandards() {
        return agileGuide + SECTION_SEPARATOR + qualityStandards;
    }

    private static void requireContent(String value, String fieldDescription) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldDescription + " es obligatoria y no puede estar vacía");
        }
    }
}

