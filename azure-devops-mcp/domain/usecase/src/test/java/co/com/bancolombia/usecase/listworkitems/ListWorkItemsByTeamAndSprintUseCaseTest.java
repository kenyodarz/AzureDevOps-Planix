package co.com.bancolombia.usecase.listworkitems;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.workitem.ListWorkItemsCommand;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import java.time.Clock;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Los siete escenarios del flujo compuesto, <b>reubicados desde {@code AzureDevOpsToolsTest}</b>.
 *
 * <p>Nacieron apuntando al entry-point porque allí vivía el flujo. Al bajarlo a esta capa en la
 * Fase 04, las pruebas bajan con él: <b>ni una aserción, ni un escenario, ni un valor esperado ha
 * cambiado</b>; solo de quién se hacen los mocks y a quién se invoca. Es la reubicación que el
 * propietario autorizó al detectarse que «entry-point sin lógica» y «cero pruebas heredadas
 * modificadas» eran incompatibles en esta fase.
 *
 * <p>Se conservan además de {@link WiqlCharacterizationTest} a propósito: aquélla compara la
 * sentencia completa carácter a carácter, éstas comprueban fragmentos concretos y la forma del
 * resultado devuelto. Son redes de seguridad complementarias, no duplicadas.
 */
@ExtendWith(MockitoExtension.class)
class ListWorkItemsByTeamAndSprintUseCaseTest {

    @Mock
    private QueryByWiqlUseCase queryByWiqlUseCase;
    @Mock
    private GetTeamFieldValuesUseCase getTeamFieldValuesUseCase;
    @Mock
    private GetTeamIterationsUseCase getTeamIterationsUseCase;

    private ListWorkItemsByTeamAndSprintUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListWorkItemsByTeamAndSprintUseCase(
                new ResolveTeamScopeUseCase(getTeamFieldValuesUseCase, getTeamIterationsUseCase,
                        TeamScopeFallbackMetrics.noOp(), Clock.systemDefaultZone()),
                queryByWiqlUseCase);
    }

    /**
     * Simula que Azure DevOps no puede resolver la iteración, de modo que el flujo cae al repliegue
     * por concatenación. Es lo que hacían <b>siempre</b> estas pruebas antes de que la resolución
     * dinámica existiera, así que sus aserciones se conservan intactas.
     */
    private void givenIterationCannotBeResolved() {
        when(getTeamIterationsUseCase.resolveIterationPath(anyString(), anyString(), anyString(),
                anyString()))
                .thenReturn(Mono.error(new NoSuchElementException("sin iteraciones")));
    }

    private Mono<WiqlResult> listing(String org, String proj, String team, String sprint,
            String types, String apiVersion) {
        return useCase.execute(
                ListWorkItemsCommand.of(org, proj, team, sprint, types, apiVersion));
    }

    @Test
    void givenParams_whenListWorkItemsByTeamAndSprint_thenReturnsQueryResult() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        givenIterationCannotBeResolved();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA")
                        .build()));
        when(queryByWiqlUseCase.queryByWiql(eq(org), eq(proj), any(WiqlQuery.class),
                eq(apiVersion)))
                .thenReturn(Mono.just(mockResult));

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, team, sprint, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenParamsWithDoubleBackslashes_whenListWorkItemsByTeamAndSprint_thenReturnsCorrectQuery() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String teamWithDoubleBackslashes = "Vicepresidencia Servicios de Tecnología\\\\EQU1096 - EXODIA";
        String sprintWithDoubleBackslashes = "Vicepresidencia Servicios de Tecnología\\\\2026\\\\Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        givenIterationCannotBeResolved();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, "EQU1096 - EXODIA"))
                .thenReturn(Mono.error(new RuntimeException("API error")));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that double backslashes were successfully replaced with a single backslash
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA"));
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\2026\\Sprint 247"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, teamWithDoubleBackslashes,
                sprintWithDoubleBackslashes, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenParamsWithoutProjectPrefix_whenListWorkItemsByTeamAndSprint_thenPrependsProjectName() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String teamWithoutProject = "EQU1096 - EXODIA";
        String sprintWithoutProject = "2026\\Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        givenIterationCannotBeResolved();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, teamWithoutProject))
                .thenReturn(Mono.error(new RuntimeException("API error")));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that project prefix was correctly prepended
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA"));
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\2026\\Sprint 247"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, teamWithoutProject, sprintWithoutProject, null,
                apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenCustomWorkItemTypes_whenListWorkItemsByTeamAndSprint_thenFormatsTypesCorrectly() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String customTypes = "User Story, Task, Bug";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        givenIterationCannotBeResolved();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA")
                        .build()));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that the work item types are formatted properly as SQL literals
            assertTrue(argQuery.getQuery()
                    .contains("[System.WorkItemType] IN ('Historia de Usuario','Task','Bug')"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, team, sprint, customTypes, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenDynamicAreaPath_whenListWorkItemsByTeamAndSprint_thenUsesResolvedAreaPath() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        givenIterationCannotBeResolved();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("DynamicAreaPath\\SpecialBranch\\Exodia").build()));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that the dynamically resolved AreaPath is used
            assertTrue(argQuery.getQuery()
                    .contains("[System.AreaPath] = 'DynamicAreaPath\\SpecialBranch\\Exodia'"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, team, sprint, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    // ---------------------------------------------------------------------------------------
    // Resolución dinámica del IterationPath.
    // Es el gemelo de `givenDynamicAreaPath_...`: la simetría que faltaba y que causaba que el
    // tablero saliera vacío cada enero.
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN Azure DevOps resuelve la iteración WHEN se listan los ítems THEN usa el IterationPath real")
    void givenDynamicIterationPath_whenListWorkItemsByTeamAndSprint_thenUsesResolvedIterationPath() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamIterationsUseCase.resolveIterationPath(org, proj, team, sprint))
                .thenReturn(Mono.just(
                        "Vicepresidencia Servicios de Tecnología\\2025\\Sprint 247"));
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA")
                        .build()));
        when(queryByWiqlUseCase.queryByWiql(eq(org), eq(proj), any(WiqlQuery.class),
                eq(apiVersion))).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            assertTrue(argQuery.getQuery().contains(
                    "[System.IterationPath] = 'Vicepresidencia Servicios de Tecnología\\2025\\Sprint 247'"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, team, sprint, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    /**
     * <b>El fallo del año, cerrado.</b> Un sprint que empezó en 2025 y termina en 2026 vive en
     * {@code ...\2025\Sprint 247}. El cálculo anterior lo buscaba en el año en curso, no lo
     * encontraba y devolvía cero ítems sin decir nada. Ahora el año lo pone Azure DevOps.
     */
    @Test
    @DisplayName("GIVEN un sprint que cruza el año WHEN se listan los ítems THEN NO se usa el año en curso")
    void givenSprintCrossingTheYear_whenListWorkItemsByTeamAndSprint_thenCurrentYearIsNotUsed() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String apiVersion = "7.0";
        int currentYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).getYear();
        String realPath = proj + "\\" + (currentYear - 1) + "\\" + sprint;

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamIterationsUseCase.resolveIterationPath(org, proj, team, sprint))
                .thenReturn(Mono.just(realPath));
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue(proj + "\\" + team)
                        .build()));
        when(queryByWiqlUseCase.queryByWiql(eq(org), eq(proj), any(WiqlQuery.class),
                eq(apiVersion))).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            assertTrue(argQuery.getQuery().contains(realPath));
            assertFalse(argQuery.getQuery()
                    .contains("\\" + currentYear + "\\" + sprint));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = listing(org, proj, team, sprint, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }
}

