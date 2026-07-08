package co.com.bancolombia.usecase.dashboard;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import java.util.logging.Level;
import java.util.logging.Logger;
import reactor.core.publisher.Mono;

/**
 * Caso de uso encargado de recopilar la información del Dashboard de DevOps (Calidad y Velocity) a
 * través del agente y sus herramientas MCP.
 */
public class DevOpsDashboardUseCase {

    private static final Logger log = Logger.getLogger(DevOpsDashboardUseCase.class.getName());
    private static final String DASHBOARD_PROMPT = """
            Usa las herramientas de Azure DevOps para listar los Work Items (User Stories y Habilitadores) del proyecto.
            La organización de Azure DevOps a usar en las herramientas es "%s" y el proyecto es "%s".
            Los ítems deben pertenecer específicamente a la célula/equipo "%s" y al sprint/iteración "%s".
            
            REGLAS CRÍTICAS PARA CONSULTAS WIQL (Azure DevOps):
            1. Al construir consultas WIQL, debes filtrar por el proyecto usando 'System.TeamProject' comparándolo únicamente con el macro '@project' o el nombre del proyecto, de la siguiente forma: [System.TeamProject] = @project. NUNCA uses el UUID del proyecto.
            2. NUNCA utilices los operadores 'CONTAINS' o 'LIKE' sobre campos de ruta como 'System.AreaPath' o 'System.IterationPath', ya que no son válidos para estos tipos de campo y provocarán un error '400 Bad Request'. En su lugar, debes usar el operador '=' (igual) para la ruta completa exacta, o 'UNDER' para rutas jerárquicas inferiores.
            
            Por cada ítem recuperado, analiza detalladamente la calidad de su documentación:
            1. Criterios de Aceptación (si cuenta con ellos en su descripción).
            2. Definition of Done (DoD).
            3. Si tiene tareas hijas asignadas.
            Calcula el puntaje de calidad (qualityScore de 0 a 100) en base a esto.
            4. En 'feedback', proporciona un mensaje constructivo muy breve (máximo 150 caracteres) justificando el puntaje de calidad obtenido (ej: qué le hace falta, si cumple con DoD o criterios de aceptación).
            
            Calcula métricas agregadas del equipo:
            - totalPoints: Suma de todos los Story Points de las historias.
            - completedPoints: Suma de Story Points en estado 'Done' o 'Closed'.
            - completedPercentage: Porcentaje de avance de puntos.
            - avgQualityScore: Promedio del puntaje de calidad de las historias.
            - undocumentedCount: Cantidad de historias sin criterios de aceptación o sin DoD.
            
            Retorna obligatoriamente un JSON estructurado con el siguiente formato exacto, sin comentarios, sin envoltorios markdown, solo el JSON puro:
            {
              "metrics": {
                "totalPoints": X,
                "completedPoints": Y,
                "completedPercentage": Z,
                "avgQualityScore": W,
                "undocumentedCount": K
              },
              "items": [
                {
                  "id": "123",
                  "title": "...",
                  "points": Y,
                  "state": "...",
                  "assignedMember": "...",
                  "hasAcceptanceCriteria": true/false,
                  "hasDoD": true/false,
                  "qualityScore": Q,
                  "linkedTasksCount": T,
                  "feedback": "..."
                }
              ]
            }
            """;

    private static final String DASHBOARD_INITIAL_PROMPT = """
            Usa las herramientas de Azure DevOps para listar los Work Items (User Stories y Habilitadores) del proyecto.
            La organización de Azure DevOps a usar en las herramientas es "%s" y el proyecto es "%s".
            Los ítems deben pertenecer específicamente a la célula/equipo "%s" y al sprint/iteración "%s".
            
            REGLAS CRÍTICAS PARA CONSULTAS WIQL (Azure DevOps):
            1. Al construir consultas WIQL, debes filtrar por el proyecto usando 'System.TeamProject' comparándolo únicamente con el macro '@project' o el nombre del proyecto, de la siguiente forma: [System.TeamProject] = @project. NUNCA uses el UUID del proyecto.
            2. NUNCA utilices los operadores 'CONTAINS' o 'LIKE' sobre campos de ruta como 'System.AreaPath' o 'System.IterationPath', ya que no son válidos para estos tipos de campo y provocarán un error '400 Bad Request'. En su lugar, debes usar el operador '=' (igual) para la ruta completa exacta, o 'UNDER' para rutas jerárquicas inferiores.
            
            Para esta fase inicial, solo necesitamos listar los ítems rápidamente. NO analices la calidad de la documentación ni DoD en este paso. El campo "assignedMember" debe corresponder a la persona asignada al Work Item (System.AssignedTo).
            Defina todos los campos de chequeo (hasAcceptanceCriteria, hasDoD) como false y qualityScore como 0 de momento.
            
            Calcula métricas agregadas básicas (totalPoints, completedPoints, completedPercentage) usando los Story Points.
            avgQualityScore debe ser 0 y undocumentedCount debe ser 0 en esta fase.
            
            Retorna obligatoriamente un JSON estructurado con el siguiente formato exacto, sin comentarios, sin envoltorios markdown, solo el JSON puro:
            {
              "metrics": {
                "totalPoints": X,
                "completedPoints": Y,
                "completedPercentage": Z,
                "avgQualityScore": 0,
                "undocumentedCount": 0
              },
              "items": [
                {
                  "id": "123",
                  "title": "...",
                  "points": Y,
                  "state": "...",
                  "assignedMember": "...",
                  "hasAcceptanceCriteria": false,
                  "hasDoD": false,
                  "qualityScore": 0,
                  "linkedTasksCount": 0,
                  "feedback": ""
                }
              ]
            }
            """;

    private static final String BATCH_AUDIT_PROMPT = """
            Analiza detalladamente la calidad de la documentación de los siguientes %d Work Items en el proyecto "%s" de la organización "%s".
            Usa la herramienta `getWorkItem` para cada uno de los IDs dados para obtener su descripción, criterios de aceptación, DoD y tareas hijas.
            
            IDs a analizar: %s
            
            Sigue estas reglas para calcular:
            1. contains 'Criterios de Aceptación' o escenario estructurado -> hasAcceptanceCriteria = true.
            2. hasDoD = true si menciona criterios para el DoD.
            3. linkedTasksCount es el número de dependencias o tareas hijas vinculadas que tenga.
            4. Calcula qualityScore (0-100) según la solidez de la documentación.
            5. En 'feedback', genera una frase corta y descriptiva en español explicando qué le hace falta al ítem (ej: "Falta definición de criterios de aceptación", "DoD incompleto" o "Excelente documentación" si el score es >= 80).
            
            Retorna obligatoriamente un JSON estructurado en el formato exacto de mapa de actualización, sin comentarios, sin envoltorios markdown, solo el JSON puro:
            {
              "updates": [
                {
                  "id": "...",
                  "hasAcceptanceCriteria": true/false,
                  "hasDoD": true/false,
                  "qualityScore": Q,
                  "linkedTasksCount": T,
                  "feedback": "..."
                }
              ]
            }
            """;

    private static final String MOCK_DASHBOARD_DATA = """
            {
              "metrics": {
                "totalPoints": 29,
                "completedPoints": 13,
                "completedPercentage": 44,
                "avgQualityScore": 78,
                "undocumentedCount": 2
              },
              "items": [
                {
                  "id": "1001",
                  "title": "BFF Aegis Backend | HA: Desarrollar endpoints BFF para gestión de reglas",
                  "points": 8,
                  "state": "Approved",
                  "assignedMember": "Jorge Mario Mina Diaz",
                  "hasAcceptanceCriteria": true,
                  "hasDoD": false,
                  "qualityScore": 66,
                  "linkedTasksCount": 6,
                  "feedback": "El ítem cuenta con criterios de aceptación pero le falta definir el DoD (Definition of Done)."
                },
                {
                  "id": "1002",
                  "title": "Core Engine | feat: Procesador de reglas de negocio en memoria",
                  "points": 5,
                  "state": "Done",
                  "assignedMember": "Giancarlo Vasquez Sepulveda",
                  "hasAcceptanceCriteria": true,
                  "hasDoD": true,
                  "qualityScore": 100,
                  "linkedTasksCount": 8,
                  "feedback": "Excelente documentación. Cumple con DoD y Criterios de Aceptación estructurados."
                },
                {
                  "id": "1003",
                  "title": "Portal Angular | UI: Implementar panel de simulación de reglas",
                  "points": 8,
                  "state": "Committed",
                  "assignedMember": "Jorge Mario Mina Diaz",
                  "hasAcceptanceCriteria": false,
                  "hasDoD": false,
                  "qualityScore": 33,
                  "linkedTasksCount": 2,
                  "feedback": "Faltan criterios de aceptación y definir el DoD en el backlog."
                },
                {
                  "id": "1004",
                  "title": "Configurador DB | chore: Script de inicialización PostgreSQL",
                  "points": 8,
                  "state": "Done",
                  "assignedMember": "Giancarlo Vasquez Sepulveda",
                  "hasAcceptanceCriteria": true,
                  "hasDoD": true,
                  "qualityScore": 100,
                  "linkedTasksCount": 3,
                  "feedback": "Excelente documentación. Cumple con DoD y Criterios de Aceptación estructurados."
                }
              ]
            }
            """;
    private final ChatGateway chatGateway;
    private final String defaultOrg;
    private final String defaultProject;
    private final boolean isMcpEnabled;

    public DevOpsDashboardUseCase(ChatGateway chatGateway, String defaultOrg,
            String defaultProject, boolean isMcpEnabled) {
        this.chatGateway = chatGateway;
        this.defaultOrg = defaultOrg;
        this.defaultProject = defaultProject;
        this.isMcpEnabled = isMcpEnabled;
    }

    public boolean isMcpEnabled() {
        return isMcpEnabled;
    }

    private static String safeStr(String str) {
        return str != null ? str : "";
    }

    private static String safeMarkdownStr(String str) {
        return str != null ? str.replace("|", "\\|") : "";
    }

    /**
     * Obtiene el mock estático directamente como string.
     */
    public String getMockDashboardData() {
        return MOCK_DASHBOARD_DATA;
    }

    /**
     * Consulta la información analítica del Dashboard del PO Asistente para una célula y sprint
     * específicos. Si el canal de IA falla o las credenciales no están parametrizadas, devuelve
     * datos simulados (mock).
     *
     * @param cell   nombre de la célula (Area Path) a filtrar
     * @param sprint nombre de la iteración (Iteration Path) a filtrar
     * @return Mono con la respuesta JSON estructurada
     */
    public Mono<String> getDashboardData(String cell, String sprint) {
        if (cell == null || cell.trim().isEmpty() || sprint == null || sprint.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "La célula y el sprint son requeridos para auditar el backlog"));
        }

        String resolvedCell = cell.trim();
        if (!resolvedCell.startsWith(defaultProject)) {
            resolvedCell = defaultProject + "\\" + resolvedCell;
        }

        String resolvedSprint = sprint.trim();
        if (resolvedSprint.matches("Sprint \\d+")) {
            int currentYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).getYear();
            resolvedSprint = defaultProject + "\\" + currentYear + "\\" + resolvedSprint;
        } else if (!resolvedSprint.startsWith(defaultProject)) {
            resolvedSprint = defaultProject + "\\" + resolvedSprint;
        }

        log.log(Level.INFO,
                "Fetching DevOps dashboard data from AI Agent for Cell: {0} (resolved: {1}), Sprint: {2} (resolved: {3})",
                new Object[]{cell, resolvedCell, sprint, resolvedSprint});

        String formattedPrompt = String.format(DASHBOARD_PROMPT, defaultOrg, defaultProject,
                resolvedCell, resolvedSprint);

        String sessionKey = "dashboard-" + java.util.UUID.randomUUID().toString();
        return chatGateway.sendMessage(formattedPrompt, sessionKey)
                .onErrorResume(error -> {
                    log.log(Level.WARNING, "Failed to fetch dashboard data from AI.", error);
                    if (!isMcpEnabled) {
                        log.log(Level.INFO, "MCP is disabled. Returning mock dashboard data.");
                        return Mono.just(MOCK_DASHBOARD_DATA);
                    }
                    return Mono.error(error); // Propaga el error real
                });
    }

    /**
     * Obtiene el prompt analítico inicial que lista los items rápidamente sin auditar calidad.
     */
    public Mono<String> getDashboardInitialData(String cell, String sprint, String sessionKey) {
        if (cell == null || cell.trim().isEmpty() || sprint == null || sprint.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "La célula y el sprint son requeridos para auditar el backlog"));
        }

        String resolvedCell = cell.trim();
        if (!resolvedCell.startsWith(defaultProject)) {
            resolvedCell = defaultProject + "\\" + resolvedCell;
        }

        String resolvedSprint = sprint.trim();
        if (resolvedSprint.matches("Sprint \\d+")) {
            int currentYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).getYear();
            resolvedSprint = defaultProject + "\\" + currentYear + "\\" + resolvedSprint;
        } else if (!resolvedSprint.startsWith(defaultProject)) {
            resolvedSprint = defaultProject + "\\" + resolvedSprint;
        }

        String initialPrompt = String.format(DASHBOARD_INITIAL_PROMPT, defaultOrg, defaultProject,
                resolvedCell, resolvedSprint);

        return chatGateway.sendMessage(initialPrompt, sessionKey)
                .onErrorResume(error -> {
                    log.log(Level.WARNING, "Failed to fetch initial dashboard data.", error);
                    if (!isMcpEnabled) {
                        log.log(Level.INFO,
                                "MCP is disabled. Returning mock initial dashboard data.");
                        return Mono.just(MOCK_DASHBOARD_DATA);
                    }
                    return Mono.error(error); // Propaga el error real
                });
    }

    /**
     * Escribe un reporte estructurado final de análisis del backlog en formato Markdown en disco.
     */
    public void saveDashboardReport(co.com.bancolombia.model.dashboard.DashboardResponse data,
            String cell, String sprint) {
        if (data == null) {
            return;
        }
        try {
            java.io.File reportsDir = new java.io.File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            String cleanCell = cell.replaceAll("[^a-zA-Z0-9-]", "_");
            String cleanSprint = sprint.replaceAll("[^a-zA-Z0-9-]", "_");
            String fileName = "reports/reporte_" + cleanCell + "_" + cleanSprint + ".md";
            java.io.File reportFile = new java.io.File(fileName);

            StringBuilder sb = new StringBuilder();
            sb.append(buildReportHeader(cell, sprint));
            sb.append(buildMetricsTable(data.getMetrics()));
            sb.append(buildItemsTable(data.getItems()));

            java.nio.file.Files.writeString(reportFile.toPath(), sb.toString());
            log.log(Level.INFO, "Reporte Markdown generado con éxito en {0}", fileName);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Fallo al generar el reporte Markdown del dashboard", e);
        }
    }

    private String buildReportHeader(String cell, String sprint) {
        return "# Reporte de Análisis del Tablero de Calidad\n\n" +
                "- **Célula/Equipo:** " + cell + "\n" +
                "- **Sprint/Iteración:** " + sprint + "\n" +
                "- **Fecha de Generación:** " + java.time.Instant.now().toString() + "\n\n";
    }

    private String buildMetricsTable(
            co.com.bancolombia.model.dashboard.DashboardMetricsResponse metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 📊 Métricas Consolidadas\n\n");
        if (metrics != null) {
            sb.append("| Métrica | Valor |\n");
            sb.append("| --- | --- |\n");
            sb.append("| Total Story Points | ").append(metrics.getTotalPoints()).append(" |\n");
            sb.append("| completedPoints | ").append(metrics.getCompletedPoints()).append(" |\n");
            sb.append("| completedPercentage | ").append(metrics.getCompletedPercentage())
                    .append("% |\n");
            sb.append("| avgQualityScore | ").append(metrics.getAvgQualityScore()).append("% |\n");
            sb.append("| undocumentedCount | ").append(metrics.getUndocumentedCount())
                    .append(" |\n");
        } else {
            sb.append("No se pudieron consolidar métricas.\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildItemsTable(
            java.util.List<co.com.bancolombia.model.dashboard.DashboardStoryItemResponse> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 📝 Detalle de Historias de Usuario / Historias Habilitadoras\n\n");
        if (items == null || items.isEmpty()) {
            sb.append("No se encontraron elementos analizados en este sprint.\n");
            return sb.toString();
        }

        sb.append(
                "| ID | Título | Points | Estado | Miembro Asignado | CA | DoD | Score | Feedback |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (co.com.bancolombia.model.dashboard.DashboardStoryItemResponse item : items) {
            sb.append(buildItemRow(item));
        }
        return sb.toString();
    }

    private String buildItemRow(
            co.com.bancolombia.model.dashboard.DashboardStoryItemResponse item) {
        if (item == null) {
            return "";
        }
        return "| " + safeStr(item.getId()) + " " +
                "| " + safeMarkdownStr(item.getTitle()) + " " +
                "| " + item.getPoints() + " " +
                "| " + safeStr(item.getState()) + " " +
                "| " + safeStr(item.getAssignedMember()) + " " +
                "| " + (item.isHasAcceptanceCriteria() ? "✅" : "❌") + " " +
                "| " + (item.isHasDoD() ? "✅" : "❌") + " " +
                "| " + item.getQualityScore() + "% " +
                "| " + safeMarkdownStr(item.getFeedback()) + " |\n";
    }

    /**
     * Obtiene el análisis de calidad para un subconjunto de Work Items específicos.
     */
    public Mono<String> getBatchAudit(int batchSize, String idsCsv, String sessionKey) {
        String auditPrompt = String.format(BATCH_AUDIT_PROMPT, batchSize, defaultProject,
                defaultOrg, idsCsv);
        return chatGateway.sendMessage(auditPrompt, sessionKey);
    }
}
