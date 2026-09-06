package co.com.bancolombia.model.agent;

/**
 * Intención de negocio detrás de un mensaje del usuario.
 *
 * <p>Cada valor identifica un flujo de conversación distinto. El orden de declaración refleja la
 * tabla de precedencia acordada en DP-01, de la señal más específica a la más genérica: un
 * identificador explícito de Work Item pesa más que una palabra clave suelta.
 *
 * @see IntentResolver
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
 */
public enum AgentIntent {

    /** Auditoría de calidad de un Work Item existente. */
    QUALITY_AUDIT,

    /** Refinamiento de una historia existente. */
    REFINEMENT,

    /** Confirmación para generar la historia estructurada. */
    APPROVAL,

    /** Petición de dividir una historia demasiado grande. */
    DIVISION,

    /** Consulta general: el texto del usuario viaja al modelo sin plantilla. */
    GENERAL,

    /** Idea de desarrollo en texto libre que alimenta el borrador de planeación con RAG. */
    PLANNING_DRAFT,

    /**
     * Planeación macro de programa / roadmap trimestral y distribución en sprints.
     */
    PROGRAM_PLANNING
}

