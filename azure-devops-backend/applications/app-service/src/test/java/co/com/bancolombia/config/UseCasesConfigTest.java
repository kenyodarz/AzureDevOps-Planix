package co.com.bancolombia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.bancolombia.config.dashboard.ClasspathDashboardFallbackAdapter;
import co.com.bancolombia.config.prompt.ClasspathPromptTemplateAdapter;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.gateways.AgentGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.dashboard.gateways.DashboardFallbackPort;
import co.com.bancolombia.model.dashboard.gateways.ReportStoragePort;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.planning.TriggerProgramPlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import co.com.bancolombia.usecase.spec.GetSpecDocumentUseCase;
import co.com.bancolombia.usecase.spec.ListAvailableSpecsUseCase;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Verifica que el cableado de casos de uso de {@link UseCasesConfig} funciona de verdad.
 *
 * <p><b>Historia de este test.</b> Su versión anterior envolvía la carga del contexto en un
 * {@code try/catch} que capturaba {@code UnsatisfiedDependencyException} y respondía
 * {@code assertTrue(true)}. Daba verde <b>mientras el contexto no arrancaba</b>, de modo que un
 * cableado roto habría pasado inadvertido. Se reescribió en la Fase 01 con
 * {@link ApplicationContextRunner}, que permite afirmar sobre el arranque sin capturar nada.
 *
 * <p>Regla para quien lo modifique: <b>prohibido capturar excepciones para dar el test por
 * bueno</b>. Si el contexto no arranca, este test debe ponerse rojo.
 */
class UseCasesConfigTest {

    private static final String DEFAULT_ORG = "grupobancolombia";
    private static final String DEFAULT_PROJECT = "Vicepresidencia Servicios de Tecnología";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GatewayMocksConfig.class, UseCasesConfig.class);

    /**
     * Mismo cableado, pero con un agente que siempre falla: es lo que activa el repliegue.
     */
    private final ApplicationContextRunner failingAgentRunner = new ApplicationContextRunner()
            .withUserConfiguration(FailingAgentConfig.class, UseCasesConfig.class);

    @Test
    @DisplayName("GIVEN los gateways disponibles WHEN arranca el contexto THEN se registran los ocho casos de uso")
    void givenGatewaysAvailable_whenContextStarts_thenAllUseCasesAreRegistered() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IngestPlanningSpecUseCase.class);
            assertThat(context).hasSingleBean(SearchPlanningSpecUseCase.class);
            assertThat(context).hasSingleBean(ManagePlanningUseCase.class);
            assertThat(context).hasSingleBean(DevOpsDashboardUseCase.class);
            assertThat(context).hasSingleBean(TrackAgentTaskUseCase.class);
            assertThat(context).hasSingleBean(GetSpecDocumentUseCase.class);
            assertThat(context).hasSingleBean(ListAvailableSpecsUseCase.class);
            assertThat(context).hasSingleBean(TriggerProgramPlanningUseCase.class);
        });
    }

    @Test
    @DisplayName("GIVEN falta SpecStoragePort WHEN arranca el contexto THEN el arranque falla")
    void givenMissingSpecStoragePort_whenContextStarts_thenStartupFails() {
        new ApplicationContextRunner()
                .withUserConfiguration(MissingSpecStoragePortConfig.class, UseCasesConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("GIVEN falta PlanningVectorStorePort WHEN arranca el contexto THEN el arranque falla")
    void givenMissingVectorStorePort_whenContextStarts_thenStartupFails() {
        new ApplicationContextRunner()
                .withUserConfiguration(OnlyAgentGatewayConfig.class, UseCasesConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("GIVEN falta AgentGateway WHEN arranca el contexto THEN el arranque falla")
    void givenMissingAgentGateway_whenContextStarts_thenStartupFails() {
        new ApplicationContextRunner()
                .withUserConfiguration(OnlyVectorStoreConfig.class, UseCasesConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("GIVEN sin propiedades WHEN arranca el contexto THEN el dashboard usa los valores por defecto")
    void givenNoProperties_whenContextStarts_thenDashboardUsesDefaults() {
        contextRunner.run(context -> {
            DevOpsDashboardUseCase useCase = context.getBean(DevOpsDashboardUseCase.class);
            CapturingAgentGateway gateway = context.getBean(CapturingAgentGateway.class);

            assertThat(promptOf(useCase, gateway)).contains(DEFAULT_ORG, DEFAULT_PROJECT);
        });
    }

    @Test
    @DisplayName("GIVEN propiedades explícitas WHEN arranca el contexto THEN llegan al caso de uso")
    void givenExplicitProperties_whenContextStarts_thenTheyReachTheUseCase() {
        contextRunner
                .withPropertyValues(
                        "azure-devops.default-org=otra-org",
                        "azure-devops.default-project=Otro Proyecto")
                .run(context -> {
                    DevOpsDashboardUseCase useCase = context.getBean(DevOpsDashboardUseCase.class);
                    CapturingAgentGateway gateway = context.getBean(CapturingAgentGateway.class);

                    assertThat(promptOf(useCase, gateway)).contains("otra-org", "Otro Proyecto");
                });
    }

    /**
     * <b>B-07.</b> El repliegue al tablero simulado está apagado si nadie lo pide, que es lo que
     * corre en producción: el fallo del agente llega al usuario en lugar de disfrazarse de datos.
     */
    @Test
    @DisplayName("GIVEN sin propiedades WHEN el agente falla THEN el error se propaga")
    void givenNoProperties_whenAgentFails_thenErrorPropagates() {
        failingAgentRunner.run(context -> StepVerifier
                .create(context.getBean(DevOpsDashboardUseCase.class)
                        .getDashboardInitialData("Celula", "Sprint 1", "wiring-test"))
                .expectError(IllegalStateException.class)
                .verify());
    }

    @Test
    @DisplayName("GIVEN dashboard.mock-fallback.enabled=true WHEN el agente falla THEN devuelve el mock")
    void givenMockFallbackEnabled_whenAgentFails_thenReturnsMock() {
        failingAgentRunner
                .withPropertyValues("dashboard.mock-fallback.enabled=true")
                .run(context -> StepVerifier
                        .create(context.getBean(DevOpsDashboardUseCase.class)
                                .getDashboardInitialData("Celula", "Sprint 1", "wiring-test"))
                        .assertNext(response -> assertThat(response).contains("totalPoints"))
                        .verifyComplete());
    }

    /**
     * Fase 08 (D-32, B-11). Hasta la Fase 07, sin propiedad explícita el repliegue se deducía de
     * {@code spring.ai.mcp.client.enabled}. Esa bandera describía un cliente MCP que el BFF ya no
     * tiene: el módulo {@code mcp-client} se eliminó, y con él la deducción. La prueba que fijaba
     * aquel comportamiento se sustituye por la que fija el nuevo, que es el explícito: apagar la
     * propiedad deja pasar el error en lugar de servir un tablero simulado.
     */
    @Test
    @DisplayName("GIVEN dashboard.mock-fallback.enabled=false WHEN el agente falla THEN el error se propaga")
    void givenMockFallbackDisabled_whenAgentFails_thenErrorPropagates() {
        failingAgentRunner
                .withPropertyValues("dashboard.mock-fallback.enabled=false")
                .run(context -> StepVerifier
                        .create(context.getBean(DevOpsDashboardUseCase.class)
                                .getDashboardInitialData("Celula", "Sprint 1", "wiring-test"))
                        .expectError(IllegalStateException.class)
                        .verify());
    }

    @Test
    @DisplayName("GIVEN el contexto WHEN se invoca GetSpecDocumentUseCase THEN delega a SpecStoragePort")
    void givenContext_whenGetSpecDocumentUseCaseInvoked_thenDelegatesToPort() {
        contextRunner.run(context -> {
            GetSpecDocumentUseCase useCase = context.getBean(GetSpecDocumentUseCase.class);
            SpecStoragePort port = context.getBean(SpecStoragePort.class);
            SpecDocument doc = new SpecDocument("ideas.md", "# Ideas", "/path/ideas.md");
            when(port.getSpec("ideas.md")).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.execute("ideas.md"))
                    .expectNext(doc)
                    .verifyComplete();
        });
    }

    @Test
    @DisplayName("GIVEN el contexto WHEN se invoca ListAvailableSpecsUseCase THEN delega a SpecStoragePort")
    void givenContext_whenListAvailableSpecsUseCaseInvoked_thenDelegatesToPort() {
        contextRunner.run(context -> {
            ListAvailableSpecsUseCase useCase = context.getBean(ListAvailableSpecsUseCase.class);
            SpecStoragePort port = context.getBean(SpecStoragePort.class);
            when(port.listAvailableSpecs()).thenReturn(Flux.just("frente_1.md", "frente_2.md"));

            StepVerifier.create(useCase.execute())
                    .expectNext("frente_1.md", "frente_2.md")
                    .verifyComplete();
        });
    }

    @Test
    @DisplayName("GIVEN el contexto WHEN se invoca TriggerProgramPlanningUseCase THEN envía comando canónico")
    void givenContext_whenTriggerProgramPlanningUseCaseInvoked_thenDispatchesCommand() {
        contextRunner.run(context -> {
            TriggerProgramPlanningUseCase useCase = context.getBean(
                    TriggerProgramPlanningUseCase.class);
            ProgramPlanRequest request = new ProgramPlanRequest(
                    "Q3-2026",
                    "Objetivos estratégicos",
                    List.of("FrenteA"),
                    4,
                    80);

            StepVerifier.create(useCase.execute(request, "ctx-123"))
                    .assertNext(interaction -> assertThat(interaction).isNotNull())
                    .verifyComplete();
        });
    }

    /**
     * La organización y el proyecto son campos privados sin getter: la única forma de comprobar que
     * el valor inyectado llegó al bean es observarlo en el prompt que el caso de uso construye.
     * Esta indirección desaparece cuando la Fase 08 introduzca configuración tipada.
     */
    private String promptOf(DevOpsDashboardUseCase useCase, CapturingAgentGateway gateway) {
        StepVerifier.create(useCase.getBatchAudit(1, "1", "wiring-test"))
                .expectNext("{}")
                .verifyComplete();
        return gateway.lastPrompt();
    }

    @Configuration(proxyBeanMethods = false)
    static class GatewayMocksConfig {

        @Bean
        PlanningVectorStorePort planningVectorStorePort() {
            return mock(PlanningVectorStorePort.class);
        }

        @Bean
        TaskStoreGateway taskStoreGateway() {
            return mock(TaskStoreGateway.class);
        }

        @Bean
        CapturingAgentGateway agentGateway() {
            return new CapturingAgentGateway();
        }

        /**
         * Adaptadores <b>reales</b>, no dobles: desde la Fase 04 el prompt se compone leyendo
         * {@code resources/prompts/}, y este test comprueba precisamente que la organización y el
         * proyecto inyectados llegan hasta ese texto.
         */
        @Bean
        PromptTemplatePort promptTemplatePort() {
            return new ClasspathPromptTemplateAdapter();
        }

        @Bean
        DashboardFallbackPort dashboardFallbackPort() {
            return new ClasspathDashboardFallbackAdapter();
        }

        /**
         * El dominio ya no escribe en disco (D-10): aquí basta con un puerto que no hace nada.
         */
        @Bean
        ReportStoragePort reportStoragePort() {
            return (reportName, content) -> Mono.empty();
        }

        @Bean
        SpecStoragePort specStoragePort() {
            return mock(SpecStoragePort.class);
        }
    }

    /**
     * Cableado con un agente que siempre falla. Es la única forma de observar desde fuera si el
     * repliegue al tablero simulado está encendido, ahora que el caso de uso no expone la bandera.
     */
    @Configuration(proxyBeanMethods = false)
    static class FailingAgentConfig {

        @Bean
        PlanningVectorStorePort planningVectorStorePort() {
            return mock(PlanningVectorStorePort.class);
        }

        @Bean
        TaskStoreGateway taskStoreGateway() {
            return mock(TaskStoreGateway.class);
        }

        @Bean
        AgentGateway agentGateway() {
            AgentGateway gateway = mock(AgentGateway.class);
            when(gateway.sendMessage(any(AgentCommand.class)))
                    .thenReturn(Mono.error(new IllegalStateException("agente caído")));
            return gateway;
        }

        @Bean
        PromptTemplatePort promptTemplatePort() {
            return new ClasspathPromptTemplateAdapter();
        }

        @Bean
        DashboardFallbackPort dashboardFallbackPort() {
            return new ClasspathDashboardFallbackAdapter();
        }

        @Bean
        ReportStoragePort reportStoragePort() {
            return (reportName, content) -> Mono.empty();
        }

        @Bean
        SpecStoragePort specStoragePort() {
            return mock(SpecStoragePort.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OnlyAgentGatewayConfig {

        @Bean
        CapturingAgentGateway agentGateway() {
            return new CapturingAgentGateway();
        }

        @Bean
        TaskStoreGateway taskStoreGateway() {
            return mock(TaskStoreGateway.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OnlyVectorStoreConfig {

        @Bean
        PlanningVectorStorePort planningVectorStorePort() {
            return mock(PlanningVectorStorePort.class);
        }

        @Bean
        TaskStoreGateway taskStoreGateway() {
            return mock(TaskStoreGateway.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MissingSpecStoragePortConfig {

        @Bean
        PlanningVectorStorePort planningVectorStorePort() {
            return mock(PlanningVectorStorePort.class);
        }

        @Bean
        TaskStoreGateway taskStoreGateway() {
            return mock(TaskStoreGateway.class);
        }

        @Bean
        CapturingAgentGateway agentGateway() {
            return new CapturingAgentGateway();
        }

        @Bean
        PromptTemplatePort promptTemplatePort() {
            return new ClasspathPromptTemplateAdapter();
        }

        @Bean
        DashboardFallbackPort dashboardFallbackPort() {
            return new ClasspathDashboardFallbackAdapter();
        }

        @Bean
        ReportStoragePort reportStoragePort() {
            return (reportName, content) -> Mono.empty();
        }
    }

    /**
     * Doble de prueba que recuerda el último prompt recibido. Se prefiere a un mock de Mockito
     * porque el prompt se lee después de que el contexto lo haya inyectado.
     */
    static class CapturingAgentGateway implements AgentGateway {

        private final AtomicReference<String> lastPrompt = new AtomicReference<>("");

        String lastPrompt() {
            return lastPrompt.get();
        }

        @Override
        public Mono<AgentInteraction> sendMessage(AgentCommand command) {
            lastPrompt.set(command.prompt());
            return Mono.just(AgentInteraction.replyOnly("{}"));
        }

        @Override
        public Mono<Task> getTask(String taskId) {
            return Mono.empty();
        }

        @Override
        public Mono<Task> cancelTask(String taskId) {
            return Mono.empty();
        }

        @Override
        public Flux<Task> listTasks() {
            return Flux.empty();
        }

        @Override
        public Mono<AgentCard> getAgentCard() {
            return Mono.empty();
        }
    }
}

