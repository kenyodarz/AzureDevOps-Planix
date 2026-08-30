package co.com.bancolombia.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import co.com.bancolombia.model.createworkitem.gateways.CreateWorkItemRepository;
import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.iteration.gateways.GetTeamIterationsRepository;
import co.com.bancolombia.model.querybywiql.gateways.QueryByWiqlRepository;
import co.com.bancolombia.model.team.gateways.GetTeamFieldValuesRepository;
import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.listworkitems.ListWorkItemsByTeamAndSprintUseCase;
import co.com.bancolombia.usecase.listworkitems.ResolveTeamScopeUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * <b>Verificación honesta del wiring de {@link UseCasesConfig} — deuda D-05.</b>
 *
 * <p><b>Por qué existe esta clase teniendo ya un {@code UseCasesConfigTest}.</b> Porque aquél
 * <b>no verifica nada</b>, y da verde por dos motivos independientes:
 *
 * <ol>
 *   <li>Registra su propio bean llamado {@code myUseCase}. Como la única aserción es «existe algún
 *       bean cuyo nombre acabe en {@code UseCase}», ese bean la satisface <b>él solo</b>: el test
 *       pasaría igual aunque {@link UseCasesConfig} estuviera vacío.</li>
 *   <li>Envuelve todo en un {@code catch (UnsatisfiedDependencyException)} cuyo cuerpo es
 *       {@code assertTrue(true)}. Es decir, <b>si el contexto no arranca, el test aprueba</b>.
 *       Y como no registra ningún gateway, ése es precisamente el camino que toma.</li>
 * </ol>
 *
 * <p>Es el mismo antipatrón que el plan de {@code azure-devops-backend} registró como su D-17
 * («{@code UseCasesConfigTest} miente»). Se deja intacto en la Fase 01 —cuya regla es no tocar
 * nada— y su sustitución queda para la <b>Fase 08</b>.
 *
 * <p><b>Qué verifica esta clase.</b> Registra los siete gateways como dobles y comprueba que el
 * contexto arranca de verdad y que <b>cada caso de uso resuelve a exactamente un bean</b>. Eso
 * responde D-05: si el {@code @ComponentScan} con filtro por expresión regular registrase las
 * clases <i>además</i> de los siete {@code @Bean} manuales, aquí se vería.
 */
class UseCasesConfigWiringTest {

    /** Los tipos de caso de uso que {@link UseCasesConfig} declara. */
    private static final List<Class<?>> USE_CASE_TYPES = List.of(
            GetTeamFieldValuesUseCase.class,
            GetTeamIterationsUseCase.class,
            GetWorkItemUseCase.class,
            CreateWorkItemUseCase.class,
            UpdateWorkItemUseCase.class,
            QueryByWiqlUseCase.class,
            GetWorkItemsBatchUseCase.class,
            // Añadidos por la Fase 04: el flujo compuesto y la resolución de rutas. La aserción
            // que se les aplica es exactamente la misma que a los otros siete.
            ResolveTeamScopeUseCase.class,
            ListWorkItemsByTeamAndSprintUseCase.class);

    @Test
    @DisplayName("GIVEN los gateways disponibles WHEN se carga UseCasesConfig THEN el contexto arranca y cada caso de uso resuelve a UN solo bean")
    void givenGateways_whenLoadingConfig_thenEachUseCaseResolvesToExactlyOneBean() {
        // Arrange (GIVEN) + Act (WHEN) — sin try/catch: si el contexto no arranca, el test FALLA,
        // que es justo lo que el test original impedía.
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(GatewaysTestConfig.class)) {

            // Assert (THEN)
            for (Class<?> useCaseType : USE_CASE_TYPES) {
                String[] names = context.getBeanNamesForType(useCaseType);
                assertEquals(1, names.length,
                        () -> "Se esperaba exactamente una definición de bean para "
                                + useCaseType.getSimpleName() + " pero se encontraron "
                                + names.length + ": " + String.join(", ", names)
                                + ". Más de una confirmaría D-05: el @ComponentScan de "
                                + "UseCasesConfig registra los casos de uso además de los @Bean.");
                assertNotNull(context.getBean(useCaseType),
                        () -> useCaseType.getSimpleName() + " no se pudo instanciar");
            }
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class GatewaysTestConfig {

        @Bean
        GetTeamFieldValuesRepository getTeamFieldValuesRepository() {
            return mock(GetTeamFieldValuesRepository.class);
        }

        @Bean
        GetTeamIterationsRepository getTeamIterationsRepository() {
            return mock(GetTeamIterationsRepository.class);
        }

        @Bean
        GetWorkItemRepository getWorkItemRepository() {
            return mock(GetWorkItemRepository.class);
        }

        @Bean
        CreateWorkItemRepository createWorkItemRepository() {
            return mock(CreateWorkItemRepository.class);
        }

        @Bean
        UpdateWorkItemRepository updateWorkItemRepository() {
            return mock(UpdateWorkItemRepository.class);
        }

        @Bean
        QueryByWiqlRepository queryByWiqlRepository() {
            return mock(QueryByWiqlRepository.class);
        }

        @Bean
        GetWorkItemsBatchRepository getWorkItemsBatchRepository() {
            return mock(GetWorkItemsBatchRepository.class);
        }

        /**
         * Puerto de observabilidad del repliegue, introducido por la Fase 04 (D-09). El doble
         * inerte basta: aquí solo se comprueba el wiring, no la métrica.
         */
        @Bean
        TeamScopeFallbackMetrics teamScopeFallbackMetrics() {
            return TeamScopeFallbackMetrics.noOp();
        }
    }
}

