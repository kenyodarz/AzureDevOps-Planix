package co.com.bancolombia.usecase.dashboard;

import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.model.dashboard.BacklogMetrics;
import co.com.bancolombia.model.dashboard.StoryQuality;
import java.time.Instant;
import java.util.List;

/**
 * Compone el reporte Markdown del tablero.
 *
 * <p>Se separó de {@code DevOpsDashboardUseCase} en la Fase 04 por una razón de tamaño: con los
 * prompts fuera, la generación del reporte era lo único que impedía bajar el caso de uso del umbral
 * de 250 líneas. El texto no cambió ni una coma —lo verifican las pruebas de caracterización del
 * reporte— y sigue siendo dominio puro: ni escribe en disco ni conoce a Spring.
 *
 * <p>La Fase 06 sacará la <b>escritura</b> del caso de uso a un {@code ReportStoragePort} (DP-04);
 * esta clase es su futuro proveedor de contenido.
 */
final class DashboardMarkdownReport {

    private DashboardMarkdownReport() {
    }

    static String render(BacklogAudit data, String cell, String sprint) {
        return header(cell, sprint) + metricsTable(data.metrics()) + itemsTable(data.items());
    }

    private static String header(String cell, String sprint) {
        return "# Reporte de Análisis del Tablero de Calidad\n\n"
                + "- **Célula/Equipo:** " + cell + "\n"
                + "- **Sprint/Iteración:** " + sprint + "\n"
                + "- **Fecha de Generación:** " + Instant.now().toString() + "\n\n";
    }

    private static String metricsTable(BacklogMetrics metrics) {
        StringBuilder sb = new StringBuilder("## 📊 Métricas Consolidadas\n\n");
        if (metrics == null) {
            return sb.append("No se pudieron consolidar métricas.\n\n").toString();
        }
        sb.append("| Métrica | Valor |\n");
        sb.append("| --- | --- |\n");
        sb.append("| Total Story Points | ").append(metrics.totalPoints()).append(" |\n");
        sb.append("| completedPoints | ").append(metrics.completedPoints()).append(" |\n");
        sb.append("| completedPercentage | ").append(metrics.completedPercentage())
                .append("% |\n");
        sb.append("| avgQualityScore | ").append(metrics.avgQualityScore()).append("% |\n");
        sb.append("| undocumentedCount | ").append(metrics.undocumentedCount()).append(" |\n");
        return sb.append("\n").toString();
    }

    private static String itemsTable(List<StoryQuality> items) {
        StringBuilder sb = new StringBuilder(
                "## 📝 Detalle de Historias de Usuario / Historias Habilitadoras\n\n");
        if (items == null || items.isEmpty()) {
            return sb.append("No se encontraron elementos analizados en este sprint.\n").toString();
        }
        sb.append(
                "| ID | Título | Points | Estado | Miembro Asignado | CA | DoD | Score | Feedback |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        items.forEach(item -> sb.append(itemRow(item)));
        return sb.toString();
    }

    private static String itemRow(StoryQuality item) {
        if (item == null) {
            return "";
        }
        return "| " + safeStr(item.id()) + " "
                + "| " + safeMarkdownStr(item.title()) + " "
                + "| " + item.points() + " "
                + "| " + safeStr(item.state()) + " "
                + "| " + safeStr(item.assignedMember()) + " "
                + "| " + (item.hasAcceptanceCriteria() ? "✅" : "❌") + " "
                + "| " + (item.hasDoD() ? "✅" : "❌") + " "
                + "| " + item.qualityScore() + "% "
                + "| " + safeMarkdownStr(item.feedback()) + " |\n";
    }

    private static String safeStr(String str) {
        return str != null ? str : "";
    }

    /**
     * El pipe es el separador de columnas del Markdown: dentro de una celda hay que escaparlo.
     */
    private static String safeMarkdownStr(String str) {
        return str != null ? str.replace("|", "\\|") : "";
    }
}
