package co.com.bancolombia.config.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.config.dashboard.ClasspathDashboardFallbackAdapter;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Seguro de la Fase 04: el texto que hoy se lee de {@code resources/} debe ser <b>idéntico</b>,
 * byte a byte, al que producían las constantes de {@code DevOpsDashboardUseCase}.
 *
 * <p>Un prompt es una interfaz con el proveedor de IA: reformatearlo —un espacio, un salto de
 * línea, una tilde— cambia el comportamiento del modelo sin romper ninguna compilación ni ningún
 * otro test. Por eso las constantes originales se conservan aquí como <i>golden copy</i>: si
 * alguien retoca un {@code .txt} creyendo que «solo es texto», esta prueba se pone roja.
 *
 * <p><b>No actualizar el golden para hacer pasar la prueba.</b> Si el prompt debe cambiar, es un
 * cambio de comportamiento y se documenta como tal.
 */
class PromptTemplateEquivalenceTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String CELL = PROJECT + "\\EQU1096 - EXODIA";
    private static final String SPRINT = PROJECT + "\\2026\\Sprint 247";

    private final ClasspathPromptTemplateAdapter adapter = new ClasspathPromptTemplateAdapter();

    private Map<String, String> backlogVariables() {
        return Map.of("org", ORG, "project", PROJECT, "cell", CELL, "sprint", SPRINT);
    }

    @Test
    @DisplayName("GIVEN prompts/dashboard.txt WHEN se renderiza THEN es idéntico a DASHBOARD_PROMPT")
    void givenDashboardTemplate_whenRendered_thenMatchesOriginalConstant() {
        // GIVEN / WHEN
        String rendered = adapter.render("dashboard", backlogVariables());

        // THEN
        assertThat(rendered).isEqualTo(originalDashboardPrompt());
    }

    @Test
    @DisplayName("GIVEN prompts/dashboard-initial.txt WHEN se renderiza THEN es idéntico a DASHBOARD_INITIAL_PROMPT")
    void givenInitialTemplate_whenRendered_thenMatchesOriginalConstant() {
        // GIVEN / WHEN
        String rendered = adapter.render("dashboard-initial", backlogVariables());

        // THEN
        assertThat(rendered).isEqualTo(originalInitialPrompt());
    }

    @Test
    @DisplayName("GIVEN prompts/batch-audit.txt WHEN se renderiza THEN es idéntico a BATCH_AUDIT_PROMPT")
    void givenBatchAuditTemplate_whenRendered_thenMatchesOriginalConstant() {
        // GIVEN / WHEN
        String rendered = adapter.render("batch-audit",
                Map.of("batchSize", "10", "project", PROJECT, "org", ORG, "idsCsv", "1,2,3"));

        // THEN
        assertThat(rendered).isEqualTo(originalBatchAuditPrompt());
    }

    @Test
    @DisplayName("GIVEN mock/dashboard.json WHEN se lee THEN es idéntico a MOCK_DASHBOARD_DATA")
    void givenMockResource_whenRead_thenMatchesOriginalConstant() {
        // GIVEN / WHEN
        String mock = new ClasspathDashboardFallbackAdapter().mockDashboard();

        // THEN
        assertThat(mock).isEqualTo(originalMockDashboardData());
    }

    // ------------------------------------------------------------------------------------------
    // Golden copy: constantes tal y como estaban en DevOpsDashboardUseCase antes de la Fase 04.
    // ------------------------------------------------------------------------------------------

    private String originalDashboardPrompt() {
        return """
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
                """.formatted(ORG, PROJECT, CELL, SPRINT);
    }

    private String originalInitialPrompt() {
        return """
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
                """.formatted(ORG, PROJECT, CELL, SPRINT);
    }

    private String originalBatchAuditPrompt() {
        return """
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
                """.formatted(10, PROJECT, ORG, "1,2,3");
    }

    private String originalMockDashboardData() {
        return """
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
    }
}

