package co.com.bancolombia.usecase.dashboard;

import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.gateways.AgentGateway;
import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.model.dashboard.SprintName;
import co.com.bancolombia.model.dashboard.TeamName;
import co.com.bancolombia.model.dashboard.gateways.DashboardFallbackPort;
import co.com.bancolombia.model.dashboard.gateways.ReportStoragePort;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Caso de uso encargado de recopilar la información del Dashboard de DevOps (Calidad y Velocity) a
 * través del agente y sus herramientas MCP.
 *
 * <p><b>Fase 04.</b> Los tres prompts y el JSON simulado que vivían aquí como constantes —184 de
 * las 430 líneas de la clase— salieron a {@link PromptTemplatePort} y {@link DashboardFallbackPort}.
 * Un prompt es la interfaz con un proveedor de IA concreto y un mock es un artefacto de pruebas:
 * ninguno de los dos es lógica de negocio.
 *
 * <p><b>Fase 06.</b> La <b>escritura</b> del reporte sale de aquí detrás de
 * {@link ReportStoragePort} (D-10, DP-04): este caso de uso compone el contenido y decide el
 * nombre, pero ya no sabe qué es un fichero. Y el <b>repliegue al tablero simulado</b> tiene por fin
 * un único punto —{@link #askWithFallback}— gobernado por configuración (D-11, B-07): antes estaba
 * repartido en cuatro sitios, tres de ellos en el entry-point HTTP.
 *
 * <p><b>Fase 08 (D-12, B-12).</b> El log de esta clase es el <b>único del dominio</b> y usa
 * {@code java.util.logging} <b>a propósito</b>: es la fachada del JDK, no una dependencia externa.
 * Meter SLF4J aquí sería introducir una dependencia técnica en {@code domain/usecase}, justo lo que
 * prohíbe {@code rules/spring-rules.md}. Lo único que cambia en la Fase 08 es <b>cómo se declara</b>:
 * la anotación {@link Log} de Lombok sustituye al {@code private static final Logger} escrito a mano,
 * de forma que en el proyecto entero el log se declara siempre con una anotación y nunca a mano.
 * Lombok es procesamiento en tiempo de compilación y no deja rastro en el classpath de ejecución.
 */
@Log
public class DevOpsDashboardUseCase {


    /**
     * Nombres lógicos de plantilla. El dominio no sabe dónde viven; eso habilita D-35.
     */
    private static final String TEMPLATE_DASHBOARD = "dashboard";
    private static final String TEMPLATE_DASHBOARD_INITIAL = "dashboard-initial";
    private static final String TEMPLATE_BATCH_AUDIT = "batch-audit";

    private static final String MISSING_PARAMS_MESSAGE =
            "La célula y el sprint son requeridos para auditar el backlog";
    private static final String REPORT_NAME_PREFIX = "reporte_";
    private static final String REPORT_NAME_SUFFIX = ".md";

    private final AgentGateway agentGateway;
    private final PromptTemplatePort promptTemplatePort;
    private final DashboardFallbackPort dashboardFallbackPort;
    private final ReportStoragePort reportStoragePort;
    private final String defaultOrg;
    private final String defaultProject;

    /**
     * Si el tablero simulado debe sustituir al agente cuando éste falla (B-07).
     *
     * <p>Antes esto se deducía de {@code spring.ai.mcp.client.enabled}, una bandera que describe
     * <b>otra cosa</b>: si hay cliente MCP, no si queremos datos de mentira. Ahora es una decisión
     * explícita y propia, con valor por defecto {@code false}: <b>en producción el repliegue no
     * existe</b>. Quien quiera la ruta local de la POC tiene que pedirla.
     */
    private final boolean mockFallbackEnabled;

    public DevOpsDashboardUseCase(AgentGateway agentGateway,
            PromptTemplatePort promptTemplatePort,
            DashboardFallbackPort dashboardFallbackPort,
            ReportStoragePort reportStoragePort,
            String defaultOrg,
            String defaultProject,
            boolean mockFallbackEnabled) {
        this.agentGateway = agentGateway;
        this.promptTemplatePort = promptTemplatePort;
        this.dashboardFallbackPort = dashboardFallbackPort;
        this.reportStoragePort = reportStoragePort;
        this.defaultOrg = defaultOrg;
        this.defaultProject = defaultProject;
        this.mockFallbackEnabled = mockFallbackEnabled;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9-]", "_");
    }

    /**
     * Pide al agente que ejecute un prompt y devuelve solo su texto.
     */
    private Mono<String> ask(String prompt, String sessionKey) {
        return agentGateway.sendMessage(AgentCommand.blocking(prompt, sessionKey))
                .map(AgentInteraction::reply);
    }

    /**
     * <b>El único punto de repliegue al tablero simulado que queda en el BFF</b> (D-11, B-07).
     *
     * <p>Con el repliegue deshabilitado —lo normal— el error del agente se propaga tal cual y el
     * stream se encarga de contárselo al usuario. Con el repliegue habilitado se devuelve el mock,
     * dejando rastro en el log de que lo que se está viendo no son datos reales.
     */
    private Mono<String> askWithFallback(String prompt, String sessionKey, String reason) {
        return ask(prompt, sessionKey)
                .onErrorResume(error -> {
                    log.log(Level.WARNING, reason, error);
                    if (!mockFallbackEnabled) {
                        return Mono.error(error);
                    }
                    log.log(Level.INFO,
                            "Repliegue al tablero simulado habilitado: los datos no son reales. {0}",
                            reason);
                    return Mono.fromSupplier(dashboardFallbackPort::mockDashboard);
                });
    }

    /**
     * Variables que cualquier prompt del backlog necesita.
     *
     * <p><b>Fase 05 (B-01).</b> {@code cell} y {@code sprint} viajan con el <b>nombre</b> que
     * escribió el usuario, no con una ruta de Azure DevOps. Traducir un nombre a un
     * {@code AreaPath} o a un {@code IterationPath} es competencia del MCP, que además lo resuelve
     * preguntando en lugar de concatenando. Las plantillas no cambiaron ni una coma: siempre
     * pidieron «la célula/equipo» y «el sprint/iteración», nunca sus rutas.
     */
    private Map<String, String> backlogVariables(TeamName team, SprintName sprint) {
        return Map.of("org", defaultOrg, "project", defaultProject,
                "cell", team.value(), "sprint", sprint.value());
    }

    /**
     * Consulta la información analítica del Dashboard del PO Asistente para una célula y sprint
     * específicos. Si el canal de IA falla y el repliegue está habilitado, devuelve datos simulados.
     *
     * @param cell   nombre de la célula (Area Path) a filtrar
     * @param sprint nombre de la iteración (Iteration Path) a filtrar
     * @return Mono con la respuesta JSON estructurada
     */
    public Mono<String> getDashboardData(String cell, String sprint) {
        if (isBlank(cell) || isBlank(sprint)) {
            return Mono.error(new IllegalArgumentException(MISSING_PARAMS_MESSAGE));
        }

        TeamName team = TeamName.of(cell);
        SprintName sprintName = SprintName.of(sprint);

        log.log(Level.INFO,
                "Fetching DevOps dashboard data from AI Agent for Cell: {0}, Sprint: {1}",
                new Object[]{team.value(), sprintName.value()});

        String formattedPrompt = promptTemplatePort.render(TEMPLATE_DASHBOARD,
                backlogVariables(team, sprintName));

        return askWithFallback(formattedPrompt, "dashboard-" + UUID.randomUUID(),
                "Failed to fetch dashboard data from AI.");
    }

    /**
     * Obtiene el prompt analítico inicial, que lista los ítems rápidamente sin auditar calidad.
     */
    public Mono<String> getDashboardInitialData(String cell, String sprint, String sessionKey) {
        if (isBlank(cell) || isBlank(sprint)) {
            return Mono.error(new IllegalArgumentException(MISSING_PARAMS_MESSAGE));
        }

        String initialPrompt = promptTemplatePort.render(TEMPLATE_DASHBOARD_INITIAL,
                backlogVariables(TeamName.of(cell), SprintName.of(sprint)));

        return askWithFallback(initialPrompt, sessionKey,
                "Failed to fetch initial dashboard data.");
    }

    /**
     * Compone el reporte Markdown del análisis del backlog y lo entrega a {@link ReportStoragePort}.
     *
     * <p><b>D-10 cerrada.</b> Antes esto era un método {@code void} que escribía en disco de forma
     * síncrona dentro de una cadena reactiva y se tragaba las excepciones. Ahora el resultado es un
     * {@code Mono<Void>} que <b>propaga el fallo</b>: quién decide si eso debe tumbar algo es el
     * llamador, no este método. Por B-05, el stream lo registra y continúa.
     *
     * @return señal de finalización; vacío si no hay auditoría que reportar
     */
    public Mono<Void> saveDashboardReport(BacklogAudit data, String cell, String sprint) {
        if (data == null) {
            log.log(Level.INFO, "No hay auditoría que reportar; no se genera Markdown.");
            return Mono.empty();
        }
        String reportName = REPORT_NAME_PREFIX + sanitize(cell) + "_" + sanitize(sprint)
                + REPORT_NAME_SUFFIX;
        return Mono.fromSupplier(() -> DashboardMarkdownReport.render(data, cell, sprint))
                .flatMap(content -> reportStoragePort.save(reportName, content))
                .doOnSuccess(ignored -> log.log(Level.INFO, "Reporte Markdown generado: {0}",
                        reportName));
    }

    /**
     * Obtiene el análisis de calidad para un subconjunto de Work Items específicos.
     */
    public Mono<String> getBatchAudit(int batchSize, String idsCsv, String sessionKey) {
        String auditPrompt = promptTemplatePort.render(TEMPLATE_BATCH_AUDIT,
                Map.of("batchSize", String.valueOf(batchSize),
                        "project", defaultProject,
                        "org", defaultOrg,
                        "idsCsv", idsCsv));
        return ask(auditPrompt, sessionKey);
    }
}

