package co.com.bancolombia.model.dashboard;

import java.util.List;

/**
 * Métricas consolidadas del backlog auditado.
 *
 * <p><b>Fase 05 (D-07, D-13).</b> Sustituye a {@code DashboardMetricsResponse}. Lo importante no
 * es el cambio de nombre sino {@link #recalculatedFrom(List)}: ese cálculo vivía en
 * {@code Handler.recalculateMetrics}, es decir, una <b>regla de negocio dentro de un adaptador
 * HTTP</b>. Sólo se podía probar levantando un servidor web, y por eso las únicas pruebas que lo
 * cubrían estaban en el test de caracterización de las rutas.
 */
public record BacklogMetrics(int totalPoints,
                             int completedPoints,
                             int completedPercentage,
                             int avgQualityScore,
                             int undocumentedCount) {

    /**
     * Recalcula la calidad media y el número de historias sin documentar a partir de los ítems.
     *
     * <p>Los totales de puntos no se tocan: los calcula el agente al construir el tablero inicial
     * y la auditoría no los altera.
     *
     * <p>La media es <b>entera</b> y divide por {@code max(1, tamaño)}. Ambas cosas se conservan
     * tal cual estaban: son el comportamiento que fija la prueba de caracterización, y cambiarlas
     * sería modificar el resultado que ve el usuario, no refactorizar.
     */
    public BacklogMetrics recalculatedFrom(List<StoryQuality> items) {
        if (items == null) {
            return this;
        }
        int totalQuality = 0;
        int undocumented = 0;
        for (StoryQuality item : items) {
            totalQuality += item.qualityScore();
            if (item.isUndocumented()) {
                undocumented++;
            }
        }
        return new BacklogMetrics(totalPoints, completedPoints, completedPercentage,
                totalQuality / Math.max(1, items.size()), undocumented);
    }
}

