package co.com.bancolombia.model.dashboard;

/**
 * Una historia del backlog con su valoración de calidad.
 *
 * <p><b>Fase 05 (D-06, D-13).</b> Sustituye a {@code DashboardStoryItemResponse}, que vivía en el
 * dominio con sufijo técnico y con {@code setQualityScore}, {@code setFeedback} y compañía. El
 * {@code Handler} usaba esos setters para modificar las historias <b>in situ</b>, en mitad de una
 * cadena reactiva cuyos lotes se ejecutan en paralelo: quien leyera la lista a medio camino veía un
 * estado a medias, y nada garantizaba la visibilidad entre hilos. Aquí no hay nada que mutar;
 * aplicar una auditoría produce una historia nueva.
 */
public record StoryQuality(String id,
                           String title,
                           int points,
                           String state,
                           String assignedMember,
                           boolean hasAcceptanceCriteria,
                           boolean hasDoD,
                           int qualityScore,
                           int linkedTasksCount,
                           String feedback) {

    /**
     * Devuelve una copia con la auditoría aplicada; los datos descriptivos no se tocan.
     */
    public StoryQuality applied(StoryUpdate update) {
        if (update == null) {
            return this;
        }
        return new StoryQuality(id, title, points, state, assignedMember,
                update.hasAcceptanceCriteria(), update.hasDoD(), update.qualityScore(),
                update.linkedTasksCount(), update.feedback());
    }

    /**
     * Una historia está sin documentar si le faltan los criterios de aceptación <b>o</b> el DoD. La
     * disyunción es la regla real del negocio y estaba escrita dentro del entry-point (D-07).
     */
    public boolean isUndocumented() {
        return !hasAcceptanceCriteria || !hasDoD;
    }

    /**
     * La misma historia antes de auditarla: sin valoración, sin chequeos y sin tareas contadas.
     */
    public StoryQuality unaudited() {
        return new StoryQuality(id, title, points, state, assignedMember, false, false, 0, 0,
                feedback);
    }
}

