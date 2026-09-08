package co.com.bancolombia.model.pullrequest;

/**
 * Veredicto resultante de la evaluación arquitectural y técnica de un Pull Request.
 */
public enum EvaluationVerdict {

    /**
     * El Pull Request cumple con todos los estándares y directrices de calidad y arquitectura.
     */
    APPROVED,

    /**
     * El Pull Request requiere ajustes o correcciones antes de ser fusionado.
     */
    CHANGES_REQUESTED,

    /**
     * El Pull Request infringe reglas críticas de arquitectura, seguridad o integridad y debe
     * rechazarse.
     */
    REJECTED
}
