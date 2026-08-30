package co.com.bancolombia.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.gateways.AgentGateway;
import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.model.dashboard.BacklogMetrics;
import co.com.bancolombia.model.dashboard.StoryQuality;
import co.com.bancolombia.model.dashboard.gateways.DashboardFallbackPort;
import co.com.bancolombia.model.dashboard.gateways.ReportStoragePort;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas del caso de uso del tablero.
 *
 * <p><b>Cambio de alcance en la Fase 04.</b> El texto de los prompts ya no vive aquí: es un
 * recurso servido por {@link PromptTemplatePort}. Por eso este test dejó de afirmar sobre
 * <i>frases</i> del prompt y pasó a afirmar sobre lo que sí es responsabilidad del dominio: <b>qué
 * plantilla</b> se pide y <b>con qué variables</b>. La literalidad del texto la fija
 * {@code PromptTemplateEquivalenceTest} en {@code app-service}.
 *
 * <p>Las aserciones sobre la resolución de {@code AreaPath} e {@code IterationPath} se conservan
 * intactas: siguen congelando el comportamiento hasta que la Fase 05 los convierta en Value Objects
 * (D-09, D-13).
 */
class DevOpsDashboardUseCaseTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String CELL = "EQU1096 - EXODIA";
    private static final String SESSION_KEY = "dashboard-test";

    /**
     * Mock del tablero de respaldo, recortado a los campos que las pruebas afirman. El contenido
     * real de {@code mock/dashboard.json} lo verifica {@code PromptTemplateEquivalenceTest}.
     */
    private static final String MOCK_DASHBOARD = """
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
                  "title": "BFF Aegis Backend | HA: endpoints de gestión de reglas",
                  "points": 8,
                  "qualityScore": 66
                }
              ]
            }
            """;
    private AgentGateway agentGateway;
    private RecordingPromptTemplatePort promptTemplatePort;
    private DashboardFallbackPort dashboardFallbackPort;
    private RecordingReportStoragePort reportStoragePort;
    private DevOpsDashboardUseCase devOpsDashboardUseCase;

    @BeforeEach
    void setUp() {
        agentGateway = mock(AgentGateway.class);
        promptTemplatePort = new RecordingPromptTemplatePort();
        dashboardFallbackPort = () -> MOCK_DASHBOARD;
        reportStoragePort = new RecordingReportStoragePort();
        devOpsDashboardUseCase = useCaseWithMockFallback(false);
    }

    /**
     * <b>Fase 06 (B-07).</b> El interruptor ya no es «hay MCP», sino «quiero datos de mentira».
     * Por defecto está apagado, que es lo que corre en producción.
     */
    private DevOpsDashboardUseCase useCaseWithMockFallback(boolean mockFallbackEnabled) {
        return new DevOpsDashboardUseCase(agentGateway, promptTemplatePort, dashboardFallbackPort,
                reportStoragePort, ORG, PROJECT, mockFallbackEnabled);
    }

    /**
     * Captura el prompt enviado al agente. Es la única forma de fijar la resolución de
     * {@code AreaPath}/{@code IterationPath} antes de extraerla a Value Objects en la Fase 05.
     */
    private String capturePrompt() {
        ArgumentCaptor<AgentCommand> commandCaptor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(agentGateway).sendMessage(commandCaptor.capture());
        return commandCaptor.getValue().prompt();
    }

    private void givenAgentAnswers(String response) {
        when(agentGateway.sendMessage(any(AgentCommand.class)))
                .thenReturn(Mono.just(AgentInteraction.replyOnly(response)));
    }

    private void givenAgentFails(RuntimeException error) {
        when(agentGateway.sendMessage(any(AgentCommand.class))).thenReturn(Mono.error(error));
    }

    @Test
    void shouldReturnDashboardDataFromAIWhenSuccessful() {
        // GIVEN
        String expectedResponse = """
                {
                  "metrics": {
                    "totalPoints": 10,
                    "completedPoints": 5,
                    "completedPercentage": 50,
                    "avgQualityScore": 90,
                    "undocumentedCount": 0
                  },
                  "items": []
                }
                """;
        givenAgentAnswers(expectedResponse);

        // WHEN
        Mono<String> resultMono = devOpsDashboardUseCase.getDashboardData("Aegis Backend",
                "Sprint 3");

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.contains("totalPoints"));
                    assertTrue(response.contains("completedPoints"));
                    assertTrue(response.contains("completedPercentage\": 50"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnMockDashboardDataWhenAIServiceFailsAndFallbackIsEnabled() {
        // GIVEN
        DevOpsDashboardUseCase fallbackUseCase = useCaseWithMockFallback(true);
        givenAgentFails(new RuntimeException("AI service unavailable"));

        // WHEN
        Mono<String> resultMono = fallbackUseCase.getDashboardData("Aegis Backend",
                "Sprint 3");

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    // Debe retornar los datos mock por defecto
                    assertTrue(response.contains("totalPoints\": 29"));
                    assertTrue(response.contains("completedPoints\": 13"));
                    assertTrue(response.contains("completedPercentage\": 44"));
                    assertTrue(response.contains("BFF Aegis Backend"));
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenAIServiceFailsAndFallbackIsDisabled() {
        // GIVEN
        givenAgentFails(new RuntimeException("AI service unavailable"));

        // WHEN
        Mono<String> resultMono = devOpsDashboardUseCase.getDashboardData("Aegis Backend",
                "Sprint 3");

        // THEN
        StepVerifier.create(resultMono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN célula sin prefijo WHEN getDashboardData THEN la envía tal cual, sin anteponer el proyecto")
    void givenCellWithoutPrefix_whenGetDashboardData_thenProjectIsNotPrepended() {
        // GIVEN
        givenAgentAnswers("{}");

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.getDashboardData(CELL, "Otro")).expectNext("{}")
                .verifyComplete();

        // THEN
        assertThat(promptTemplatePort.variable("cell")).isEqualTo(CELL);
        assertThat(capturePrompt()).doesNotContain(PROJECT + "\\" + CELL);
    }

    @Test
    @DisplayName("GIVEN espacios alrededor WHEN getDashboardData THEN el Value Object los recorta")
    void givenPaddedNames_whenGetDashboardData_thenValueObjectsTrimThem() {
        // GIVEN
        givenAgentAnswers("{}");

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.getDashboardData("  " + CELL + "  ",
                "  Sprint 247  ")).expectNext("{}").verifyComplete();

        // THEN
        assertThat(promptTemplatePort.variable("cell")).isEqualTo(CELL);
        assertThat(promptTemplatePort.variable("sprint")).isEqualTo("Sprint 247");
    }

    @Test
    void shouldFailWhenParametersAreEmpty() {
        // GIVEN/WHEN
        Mono<String> resultMono1 = devOpsDashboardUseCase.getDashboardData("", "Sprint 3");
        Mono<String> resultMono2 = devOpsDashboardUseCase.getDashboardData("Arkham", "");

        // THEN
        StepVerifier.create(resultMono1)
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(resultMono2)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ------------------------------------------------------------------------------------------
    // Fase 05 (B-01, D-09, D-19).
    // Estas cuatro pruebas congelaban la construcción de rutas de Azure DevOps por concatenación,
    // incluido el año sacado de LocalDate.now(). Esa resolución se ELIMINÓ del BFF: ahora envía el
    // nombre que escribió el usuario y quien traduce es el MCP, preguntándole a Azure DevOps. Lo
    // que se fija aquí es justamente lo contrario que antes: que el BFF NO fabrica rutas.
    // ------------------------------------------------------------------------------------------

    /**
     * <b>El fallo del año, cerrado.</b> Antes, {@code "Sprint 247"} se convertía en
     * {@code proyecto\<año en curso>\Sprint 247}. Un sprint de diciembre a enero pertenece al año
     * en que empezó, así que en enero la ruta apuntaba a una iteración inexistente y el tablero
     * salía vacío en silencio. Ahora no hay año que acertar: el BFF no lo calcula.
     */
    @Test
    @DisplayName("GIVEN sprint 'Sprint N' WHEN getDashboardData THEN NO intercala ningún año")
    void givenSprintNFormat_whenGetDashboardData_thenNoYearIsInserted() {
        // GIVEN
        givenAgentAnswers("{}");
        int currentYear = LocalDate.now(ZoneId.systemDefault()).getYear();

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.getDashboardData(CELL, "Sprint 247"))
                .expectNext("{}").verifyComplete();

        // THEN
        assertThat(promptTemplatePort.variable("sprint")).isEqualTo("Sprint 247");
        assertThat(capturePrompt())
                .as("el año dejó de calcularse: no puede aparecer en el prompt")
                .doesNotContain(String.valueOf(currentYear));
    }

    @Test
    @DisplayName("GIVEN sprint con otro formato WHEN getDashboardData THEN tampoco antepone el proyecto")
    void givenNonSprintNFormat_whenGetDashboardData_thenProjectIsNotPrepended() {
        // GIVEN
        givenAgentAnswers("{}");

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.getDashboardData(CELL, "Iteracion Beta"))
                .expectNext("{}").verifyComplete();

        // THEN
        assertThat(promptTemplatePort.variable("sprint")).isEqualTo("Iteracion Beta");
        assertThat(capturePrompt()).doesNotContain(PROJECT + "\\Iteracion Beta");
    }

    /**
     * Antes de la Fase 04 esta prueba buscaba la frase «NO analices la calidad» dentro del prompt.
     * Esa frase ya no es del dominio: lo que aquí se fija es que el caso de uso pide la plantilla
     * <b>inicial</b>, que es la que la contiene.
     */
    @Test
    @DisplayName("GIVEN parámetros válidos WHEN getDashboardInitialData THEN usa la plantilla inicial")
    void givenValidParams_whenGetDashboardInitialData_thenUsesInitialTemplate() {
        // GIVEN
        givenAgentAnswers("{}");

        // WHEN
        StepVerifier.create(
                        devOpsDashboardUseCase.getDashboardInitialData(CELL, "Sprint 247",
                                SESSION_KEY))
                .expectNext("{}").verifyComplete();

        // THEN
        assertThat(promptTemplatePort.name()).isEqualTo("dashboard-initial");
        assertThat(promptTemplatePort.variable("org")).isEqualTo(ORG);
        assertThat(promptTemplatePort.variable("project")).isEqualTo(PROJECT);
    }

    @Test
    @DisplayName("GIVEN parámetros vacíos WHEN getDashboardInitialData THEN falla igual que getDashboardData")
    void givenBlankParams_whenGetDashboardInitialData_thenFails() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(devOpsDashboardUseCase.getDashboardInitialData("", "S", SESSION_KEY))
                .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(devOpsDashboardUseCase.getDashboardInitialData("C", null, SESSION_KEY))
                .expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("GIVEN parámetros válidos WHEN getDashboardData THEN usa la plantilla completa")
    void givenValidParams_whenGetDashboardData_thenUsesFullTemplate() {
        // GIVEN
        givenAgentAnswers("{}");

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.getDashboardData(CELL, "Sprint 247"))
                .expectNext("{}").verifyComplete();

        // THEN
        assertThat(promptTemplatePort.name()).isEqualTo("dashboard");
    }

    /**
     * El prompt de auditoría por lotes recibe <b>proyecto y organización en orden invertido</b>
     * respecto a los otros dos. Se conserva tal cual y queda anotado en la propia plantilla.
     */
    @Test
    @DisplayName("GIVEN un lote WHEN getBatchAudit THEN entrega tamaño, proyecto, organización e IDs")
    void givenBatch_whenGetBatchAudit_thenPromptCarriesSizeProjectOrgAndIds() {
        // GIVEN
        givenAgentAnswers("{\"updates\":[]}");

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.getBatchAudit(3, "1,2,3", SESSION_KEY))
                .expectNext("{\"updates\":[]}").verifyComplete();

        // THEN
        assertThat(promptTemplatePort.name()).isEqualTo("batch-audit");
        assertThat(promptTemplatePort.variable("batchSize")).isEqualTo("3");
        assertThat(promptTemplatePort.variable("idsCsv")).isEqualTo("1,2,3");
        assertThat(promptTemplatePort.variable("project")).isEqualTo(PROJECT);
        assertThat(promptTemplatePort.variable("org")).isEqualTo(ORG);
    }

    /**
     * A diferencia de los otros dos métodos, {@code getBatchAudit} <b>no</b> tiene fallback a mock:
     * el error del agente se propaga siempre, incluso con MCP deshabilitado.
     */
    @Test
    @DisplayName("GIVEN el agente falla WHEN getBatchAudit THEN propaga el error aunque el repliegue esté habilitado")
    void givenAgentFails_whenGetBatchAudit_thenPropagatesEvenWithFallbackEnabled() {
        // GIVEN
        DevOpsDashboardUseCase fallbackUseCase = useCaseWithMockFallback(true);
        givenAgentFails(new IllegalStateException("agente caído"));

        // WHEN / THEN
        StepVerifier.create(fallbackUseCase.getBatchAudit(1, "1", SESSION_KEY))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN el repliegue habilitado WHEN getDashboardInitialData falla THEN devuelve el mock")
    void givenFallbackEnabled_whenInitialDataFails_thenReturnsMock() {
        // GIVEN
        DevOpsDashboardUseCase fallbackUseCase = useCaseWithMockFallback(true);
        givenAgentFails(new IllegalStateException("agente caído"));

        // WHEN / THEN
        StepVerifier.create(fallbackUseCase.getDashboardInitialData(CELL, "Sprint 1", SESSION_KEY))
                .assertNext(response -> assertThat(response).contains("\"totalPoints\": 29"))
                .verifyComplete();
    }

    /**
     * <b>B-07.</b> Con el repliegue apagado —lo que corre en producción— el fallo del agente llega
     * al llamador en lugar de disfrazarse de tablero vacío.
     */
    @Test
    @DisplayName("GIVEN el repliegue deshabilitado WHEN getDashboardInitialData falla THEN propaga el error")
    void givenFallbackDisabled_whenInitialDataFails_thenPropagates() {
        // GIVEN
        givenAgentFails(new IllegalStateException("agente caído"));

        // WHEN / THEN
        StepVerifier.create(
                        devOpsDashboardUseCase.getDashboardInitialData(CELL, "Sprint 1", SESSION_KEY))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN datos completos WHEN saveDashboardReport THEN entrega el Markdown esperado al puerto")
    void givenFullData_whenSaveDashboardReport_thenHandsExpectedMarkdownToThePort() {
        // GIVEN
        BacklogMetrics metrics = new BacklogMetrics(21, 8, 38, 75, 1);
        StoryQuality item = new StoryQuality("1001", "Historia | con pipe", 5, "Done",
                "Jorge Mina", true, false, 66, 0, "Falta DoD");
        BacklogAudit data = new BacklogAudit(metrics, List.of(item));

        // WHEN
        StepVerifier.create(
                        devOpsDashboardUseCase.saveDashboardReport(data, "Celula X", "Sprint 9"))
                .verifyComplete();

        // THEN
        assertThat(reportStoragePort.name()).isEqualTo("reporte_Celula_X_Sprint_9.md");
        assertThat(reportStoragePort.content())
                .contains("# Reporte de Análisis del Tablero de Calidad")
                .contains("- **Célula/Equipo:** Celula X")
                .contains("- **Sprint/Iteración:** Sprint 9")
                .contains("| Total Story Points | 21 |")
                .contains("| avgQualityScore | 75% |")
                .contains("Historia \\| con pipe")
                .contains("| ✅ | ❌ | 66% |");
    }

    @Test
    @DisplayName("GIVEN datos nulos WHEN saveDashboardReport THEN no guarda nada y completa")
    void givenNullData_whenSaveDashboardReport_thenDoesNothing() {
        // GIVEN / WHEN
        StepVerifier.create(devOpsDashboardUseCase.saveDashboardReport(null, "Celula", "Sprint"))
                .verifyComplete();

        // THEN
        assertThat(reportStoragePort.name()).isNull();
    }

    /**
     * <b>D-10.</b> Antes, un fallo al escribir se quedaba en un {@code catch} que solo registraba
     * y nadie se enteraba. Ahora se propaga; quién decide qué hacer con él es el llamador (B-05).
     */
    @Test
    @DisplayName("GIVEN el almacén falla WHEN saveDashboardReport THEN propaga el error")
    void givenStorageFails_whenSaveDashboardReport_thenPropagatesError() {
        // GIVEN
        reportStoragePort.failWith(new IllegalStateException("disco lleno"));
        BacklogAudit data = new BacklogAudit(null, List.of());

        // WHEN / THEN
        StepVerifier.create(devOpsDashboardUseCase.saveDashboardReport(data, "Celula", "Sprint 1"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN sin métricas ni ítems WHEN saveDashboardReport THEN entrega los textos de vacío")
    void givenNoMetricsNorItems_whenSaveDashboardReport_thenWritesEmptyTexts() {
        // GIVEN
        BacklogAudit data = new BacklogAudit(null, List.of());

        // WHEN
        StepVerifier.create(devOpsDashboardUseCase.saveDashboardReport(data, "Vacia", "Sprint 0"))
                .verifyComplete();

        // THEN
        assertThat(reportStoragePort.name()).isEqualTo("reporte_Vacia_Sprint_0.md");
        assertThat(reportStoragePort.content())
                .contains("No se pudieron consolidar métricas.")
                .contains("No se encontraron elementos analizados en este sprint.");
    }

    /**
     * Doble de {@link PromptTemplatePort} que recuerda la última petición y devuelve una
     * representación determinista de ella. Al incluir los valores de todas las variables, las
     * aserciones sobre rutas resueltas siguen funcionando sin depender del texto del prompt.
     */
    private static final class RecordingPromptTemplatePort implements PromptTemplatePort {

        private final AtomicReference<String> lastName = new AtomicReference<>("");
        private final AtomicReference<Map<String, String>> lastVariables =
                new AtomicReference<>(Map.of());

        @Override
        public String render(String name, Map<String, String> variables) {
            lastName.set(name);
            lastVariables.set(Map.copyOf(variables));
            StringBuilder sb = new StringBuilder("template=").append(name);
            new TreeMap<>(variables)
                    .forEach((key, value) -> sb.append('|').append(key).append('=').append(value));
            return sb.toString();
        }

        String name() {
            return lastName.get();
        }

        String variable(String key) {
            return lastVariables.get().get(key);
        }
    }

    /**
     * Doble de {@link ReportStoragePort} que recuerda lo último guardado. Sustituye a la escritura
     * real en disco que estas pruebas hacían hasta la Fase 05: el dominio ya no sabe qué es un
     * fichero, así que probarlo tampoco debería requerir uno (D-10).
     */
    private static final class RecordingReportStoragePort implements ReportStoragePort {

        private final AtomicReference<String> lastName = new AtomicReference<>();
        private final AtomicReference<String> lastContent = new AtomicReference<>();
        private final AtomicReference<RuntimeException> failure = new AtomicReference<>();

        @Override
        public Mono<Void> save(String reportName, String content) {
            RuntimeException error = failure.get();
            if (error != null) {
                return Mono.error(error);
            }
            lastName.set(reportName);
            lastContent.set(content);
            return Mono.empty();
        }

        void failWith(RuntimeException error) {
            failure.set(error);
        }

        String name() {
            return lastName.get();
        }

        String content() {
            return lastContent.get();
        }
    }
}

