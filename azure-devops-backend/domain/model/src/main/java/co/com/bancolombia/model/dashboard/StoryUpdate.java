package co.com.bancolombia.model.dashboard;

/**
 * Resultado de auditar la calidad de <b>una</b> historia.
 *
 * <p>Es lo que el agente devuelve por cada ítem de un lote. Se separa de {@link StoryQuality}
 * porque no son lo mismo: la historia tiene título, puntos y estado; la auditoría solo dice qué
 * encontró. Antes ambas cosas viajaban mezcladas y la actualización se aplicaba mutando la historia
 * original dentro de la cadena reactiva (D-06).
 */
public record StoryUpdate(String id,
                          boolean hasAcceptanceCriteria,
                          boolean hasDoD,
                          int qualityScore,
                          int linkedTasksCount,
                          String feedback) {

}

