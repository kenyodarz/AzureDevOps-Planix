package co.com.bancolombia.model.prompt;

/**
 * Identifica de forma unívoca cada plantilla de prompt que el agente puede solicitar.
 *
 * <p>Mantener el catálogo como enumeración —en lugar de cadenas sueltas— garantiza que una
 * plantilla inexistente sea un error de compilación y no un fallo en tiempo de ejecución.
 */
public enum PromptTemplateId {

    /**
     * Fase 1: propuesta inicial de título y descripción a partir de una idea, con contexto RAG.
     */
    PLANNING_DRAFT,

    /**
     * Fase 2: borrador estructurado de la Historia de Usuario o Habilitadora.
     */
    STRUCTURED_STORY,

    /**
     * División de una historia que supera el máximo de Story Points admitido.
     */
    STORY_DIVISION,

    /**
     * Refinamiento de una historia ya existente en Azure DevOps.
     */
    STORY_REFINEMENT,

    /**
     * Auditoría de calidad de una historia según los estándares corporativos.
     */
    QUALITY_AUDIT,

    /**
     * Planeación macro y roadmap trimestral (Program Planning) con distribución de HUs y HAs por
     * sprint.
     */
    PROGRAM_PLANNING
}

