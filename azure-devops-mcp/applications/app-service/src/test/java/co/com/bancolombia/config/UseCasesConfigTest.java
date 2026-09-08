package co.com.bancolombia.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import co.com.bancolombia.model.team.gateways.TeamScopePort;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.model.workitem.gateways.WorkItemCommandPort;
import co.com.bancolombia.model.workitem.gateways.WorkItemQueryPort;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.listworkitems.ListWorkItemsByTeamAndSprintUseCase;
import co.com.bancolombia.usecase.listworkitems.ResolveTeamScopeUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Wiring de los casos de uso — Fase 08, D-27 y D-05 (B-13).
 *
 * <p><b>Por qué esta clase se reescribió entera.</b> La versión anterior <b>no podía fallar</b>, por
 * dos motivos que se sumaban:
 *
 * <ol>
 *   <li>Registraba un bean propio llamado {@code myUseCase} y después comprobaba «existe algún bean
 *       cuyo nombre acabe en {@code UseCase}». <b>Ese bean satisfacía la aserción por sí solo</b>,
 *       aunque no existiera ni un solo caso de uso real.</li>
 *   <li>Envolvía to-do en un {@code catch (UnsatisfiedDependencyException) { assertTrue(true); }}
 *       sobre un contexto que <b>no podía arrancar</b>, porque no registraba ningún gateway. El
 *       camino que tomaba siempre era el {@code catch}, así que <b>la aserción probablemente no se
 *       ejecutó nunca</b>.</li>
 * </ol>
 *
 * <p>Una prueba que no puede ponerse roja no es una red de seguridad: es un adorno que da
 * confianza falsa. Es el mismo antipatrón que el plan del BFF registró como su D-17.
 *
 * <p><b>Qué hace ahora.</b> Registra los <b>cuatro puertos</b> como dobles y arranca el contexto de
 * verdad. Si el wiring se rompe —si falta un bean, si una dependencia no resuelve, si alguien
 * retira el {@code Clock}— el contexto <b>no arranca y la prueba falla</b>, que es exactamente lo
 * que debe ocurrir.
 *
 * <p><b>Y verifica además lo que B-13 decidió:</b> que los nueve casos de uso los registra el
 * {@code @ComponentScan} y que <b>ya no hay {@code @Bean} manuales del mismo tipo</b>. Si alguien
 * vuelve a introducir el doble mecanismo, {@code thereIsExactlyOnePerUseCase} lo detecta.
 */
class UseCasesConfigTest {

    private static final List<Class<?>> USE_CASES = Arrays.asList(
            GetTeamFieldValuesUseCase.class,
            GetTeamIterationsUseCase.class,
            GetWorkItemUseCase.class,
            CreateWorkItemUseCase.class,
            UpdateWorkItemUseCase.class,
            QueryByWiqlUseCase.class,
            GetWorkItemsBatchUseCase.class,
            ResolveTeamScopeUseCase.class,
            ListWorkItemsByTeamAndSprintUseCase.class,
            co.com.bancolombia.usecase.pullrequest.GetPullRequestUseCase.class,
            co.com.bancolombia.usecase.pullrequest.GetPullRequestChangesUseCase.class,
            co.com.bancolombia.usecase.pullrequest.CreatePullRequestCommentUseCase.class);

    @Test
    @DisplayName("GIVEN los puertos disponibles WHEN arranca el contexto THEN los nueve casos de uso se registran")
    void givenPorts_whenContextStarts_thenEveryUseCaseIsRegistered() {
        // Arrange (GIVEN) + Act (WHEN) — sin catch: si el contexto no arranca, la prueba falla
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            // Assert (THEN)
            for (Class<?> useCase : USE_CASES) {
                assertNotNull(context.getBean(useCase),
                        "No se registro el caso de uso " + useCase.getSimpleName());
            }
        }
    }

    /**
     * Fija la decisión de B-13: <b>un solo mecanismo de wiring</b>.
     *
     * <p>Antes de la Fase 08 convivían el {@code @ComponentScan} y nueve {@code @Bean} manuales. Si
     * alguien reintrodujera los {@code @Bean}, cada caso de uso resolvería a <b>dos</b> definiciones
     * y esta prueba se pondría roja.
     */
    @Test
    @DisplayName("GIVEN el wiring WHEN se cuentan las definiciones THEN hay exactamente una por caso de uso")
    void givenWiring_whenCountingDefinitions_thenThereIsExactlyOnePerUseCase() {
        // Arrange (GIVEN) + Act (WHEN)
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            // Assert (THEN)
            for (Class<?> useCase : USE_CASES) {
                assertEquals(1, context.getBeanNamesForType(useCase).length,
                        "Se esperaba exactamente un bean de " + useCase.getSimpleName()
                                + ": el doble mecanismo de wiring (D-05) ha vuelto");
            }
        }
    }

    @Test
    @DisplayName("GIVEN el contexto WHEN se pide el Clock THEN existe, porque el escaneo no lo alcanza")
    void givenContext_whenAskingForClock_thenItIsPresent() {
        // Arrange (GIVEN) + Act (WHEN) — el Clock es el unico @Bean que sobrevive a B-13
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            // Assert (THEN)
            assertNotNull(context.getBean(Clock.class));
            assertTrue(context.getBeanNamesForType(Clock.class).length >= 1);
        }
    }

    /**
     * Contexto de prueba: aporta los <b>cuatro puertos</b> que los casos de uso necesitan.
     *
     * <p>Es lo que faltaba en la versión anterior y la razón por la que el contexto nunca podía
     * arrancar. Son dobles de Mockito porque aquí no se prueba el comportamiento de los adaptadores,
     * sino que <b>el wiring resuelve</b>.
     */
    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {

        @Bean
        public WorkItemQueryPort workItemQueryPort() {
            return mock(WorkItemQueryPort.class);
        }

        @Bean
        public WorkItemCommandPort workItemCommandPort() {
            return mock(WorkItemCommandPort.class);
        }

        @Bean
        public TeamScopePort teamScopePort() {
            return mock(TeamScopePort.class);
        }

        @Bean
        public TeamScopeFallbackMetrics teamScopeFallbackMetrics() {
            return mock(TeamScopeFallbackMetrics.class);
        }

        @Bean
        public co.com.bancolombia.model.pullrequest.gateways.PullRequestPort pullRequestPort() {
            return mock(co.com.bancolombia.model.pullrequest.gateways.PullRequestPort.class);
        }
    }
}