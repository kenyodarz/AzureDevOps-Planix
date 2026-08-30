package co.com.bancolombia.model.dashboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auditoría del backlog de una célula en un sprint: sus métricas y sus historias.
 *
 * <p><b>Fase 05 (D-06, D-07, D-13).</b> Sustituye a {@code DashboardResponse} y concentra las tres
 * reglas de negocio que vivían dentro del {@code Handler}, un adaptador HTTP:
 *
 * <ul>
 *   <li>{@link #partition(int)} — antes {@code Handler.partitionItems}, con el 10 escrito a mano.</li>
 *   <li>{@link #withUpdates(List)} — antes {@code Handler.updateOriginalItems} seguido de
 *       {@code recalculateMetrics}, que además mutaban los modelos en mitad de la cadena
 *       reactiva.</li>
 * </ul>
 *
 * <p>El tipo es inmutable de arriba abajo. Aplicar una tanda de auditorías no modifica nada:
 * devuelve una auditoría nueva. Eso es lo que permite eliminar el
 * {@code final DashboardResponse[] sharedData = new DashboardResponse[1]} del entry-point, que no
 * era más que una variable global con disfraz de array.
 */
public record BacklogAudit(BacklogMetrics metrics, List<StoryQuality> items) {

    public BacklogAudit {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Identificadores de las historias del lote, en el formato que espera el prompt.
     */
    public static String idsCsv(List<StoryQuality> batch) {
        return batch.stream().map(StoryQuality::id).reduce((a, b) -> a + "," + b).orElse("");
    }

    /**
     * Parte las historias en lotes del tamaño indicado.
     *
     * <p>El tamaño es un parámetro y no una constante porque <b>no responde a ningún límite
     * técnico</b>: no hay tope de ítems, ni de página, ni de tokens que lo justifique. Era un
     * número escrito a mano. Ahora se configura (B-02) y su valor por defecto sigue siendo 10 para
     * no alterar el comportamiento observable.
     *
     * @throws IllegalArgumentException si el tamaño no es positivo, porque un lote de cero
     *                                  historias produciría lotes infinitos
     */
    public List<List<StoryQuality>> partition(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "El tamaño del lote de auditoría debe ser mayor que cero, y llegó "
                            + batchSize);
        }
        List<List<StoryQuality>> batches = new ArrayList<>();
        for (int index = 0; index < items.size(); index += batchSize) {
            batches.add(List.copyOf(items.subList(index,
                    Math.min(index + batchSize, items.size()))));
        }
        return List.copyOf(batches);
    }

    /**
     * Aplica una tanda de auditorías y devuelve una auditoría nueva con las métricas recalculadas.
     *
     * <p>Las actualizaciones se indexan por identificador antes de recorrer las historias. El
     * código anterior anidaba dos bucles —cada actualización contra cada historia—, lo que además
     * de ser cuadrático aplicaba la última coincidencia si venían repetidas. Aquí gana la primera,
     * que es lo que el usuario espera cuando el agente responde dos veces por el mismo ítem.
     */
    public BacklogAudit withUpdates(List<StoryUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return this;
        }
        Map<String, StoryUpdate> byId = new LinkedHashMap<>();
        for (StoryUpdate update : updates) {
            if (update != null && update.id() != null) {
                byId.putIfAbsent(update.id(), update);
            }
        }
        List<StoryQuality> updated = items.stream()
                .map(item -> item.applied(byId.get(item.id())))
                .toList();
        return new BacklogAudit(
                metrics == null ? null : metrics.recalculatedFrom(updated),
                updated);
    }

    /**
     * El mismo tablero tal y como se ve <b>antes</b> de auditar: historias sin valoración y
     * métricas de calidad a cero, conservando los puntos, que sí son conocidos desde el principio.
     *
     * <p>Existe porque el flujo simulado necesitaba ese estado inicial y lo fabricaba a base de
     * {@code setQualityScore(0)} y {@code setHasDoD(false)} dentro del entry-point.
     */
    public BacklogAudit unaudited() {
        return new BacklogAudit(
                metrics == null ? null
                        : new BacklogMetrics(metrics.totalPoints(), metrics.completedPoints(),
                                metrics.completedPercentage(), 0, 0),
                items.stream().map(StoryQuality::unaudited).toList());
    }
}

