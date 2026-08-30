package co.com.bancolombia.usecase.listworkitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * <b>Prueba de caracterización: congela la sentencia WIQL carácter a carácter.</b>
 *
 * <p><b>Esta prueba se ha movido, no reescrito.</b> Nació en la Fase 01 apuntando a
 * {@code AzureDevOpsTools}, donde la sentencia se construía con un {@code String.format} dentro de
 * un <i>entry-point</i> (deuda <b>D-07</b>). Su propio javadoc lo anticipaba: «esta prueba existe
 * para ser movida, no para ser borrada […] si alguna deja de compilar, se reubica; si alguna deja
 * de pasar, el refactor cambió la consulta y hay que detenerlo». La Fase 04 ejecutó exactamente
 * eso, <b>previa autorización expresa del propietario</b>: cambió el cableado —de qué se hacen los
 * mocks y a quién se invoca— y <b>no se tocó ni una aserción, ni la plantilla, ni ninguno de los
 * ocho escenarios</b>.
 *
 * <p><b>Por qué carácter a carácter y no {@code contains(...)}.</b> Comprobar fragmentos sueltos
 * deja pasar cambios en el orden de los predicados, en el {@code ORDER BY} o en los espacios. Una
 * consulta WIQL sintácticamente válida pero semánticamente distinta <b>no falla</b>: devuelve cero
 * elementos, sin error y sin log.
 */
@ExtendWith(MockitoExtension.class)
class WiqlCharacterizationTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";
    private static final String SPRINT = "Sprint 247";
    private static final String API_VERSION = "7.0";

    /** {@code Vicepresidencia Servicios de Tecnología\EQU1096 - EXODIA} */
    private static final String AREA_PATH = PROJECT + "\\" + TEAM;
    /** {@code Vicepresidencia Servicios de Tecnología\2025\Sprint 247} */
    private static final String ITERATION_PATH = PROJECT + "\\2025\\" + SPRINT;

    private static final String DEFAULT_TYPES = "'Historia de Usuario','Habilitador'";

    /**
     * La plantilla vigente, copiada literalmente de {@code WiqlStatement}. Duplicarla aquí es
     * deliberado: si alguien cambia la del código productivo, esta prueba lo detecta. Una constante
     * compartida no detectaría nada.
     */
    private static final String WIQL_TEMPLATE =
            "SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project"
                    + " AND [System.IterationPath] = '%s'"
                    + " AND [System.AreaPath] = '%s'"
                    + " AND [System.WorkItemType] IN (%s)"
                    + " ORDER BY [System.Id]";

    @Mock
    private QueryByWiqlUseCase queryByWiqlUseCase;
    @Mock
    private GetTeamFieldValuesUseCase getTeamFieldValuesUseCase;
    @Mock
    private GetTeamIterationsUseCase getTeamIterationsUseCase;

    private ListWorkItemsByTeamAndSprintUseCase useCase;

    @BeforeEach
    void setUp() {
        ResolveTeamScopeUseCase resolveTeamScopeUseCase = new ResolveTeamScopeUseCase(
                getTeamFieldValuesUseCase, getTeamIterationsUseCase,
                TeamScopeFallbackMetrics.noOp(), Clock.systemDefaultZone());
        useCase = new ListWorkItemsByTeamAndSprintUseCase(resolveTeamScopeUseCase,
                queryByWiqlUseCase);
    }

    private static String expectedWiql(String iterationPath, String areaPath, String types) {
        return String.format(WIQL_TEMPLATE, iterationPath, areaPath, types);
    }

    private void givenAreaPathResolvesTo(String team, String areaPath) {
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, team))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue(areaPath).build()));
    }

    private void givenAreaPathCannotBeResolved(String team) {
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, team))
                .thenReturn(Mono.error(new IllegalStateException("Azure DevOps no responde")));
    }

    private void givenIterationResolvesTo(String sprint, String iterationPath) {
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, sprint))
                .thenReturn(Mono.just(iterationPath));
    }

    private void givenIterationCannotBeResolved() {
        when(getTeamIterationsUseCase.resolveIterationPath(anyString(), anyString(), anyString(),
                anyString()))
                .thenReturn(Mono.error(new NoSuchElementException("sin iteraciones")));
    }

    private void givenQueryIsAccepted(WiqlResult result) {
        when(queryByWiqlUseCase.queryByWiql(eq(ORG), eq(PROJECT), any(WiqlQuery.class),
                eq(API_VERSION))).thenReturn(Mono.just(result));
    }

    /** Recupera la sentencia realmente enviada al caso de uso de consulta. */
    private String capturedWiql() {
        ArgumentCaptor<WiqlQuery> captor = ArgumentCaptor.forClass(WiqlQuery.class);
        verify(queryByWiqlUseCase).queryByWiql(eq(ORG), eq(PROJECT), captor.capture(),
                eq(API_VERSION));
        return captor.getValue().query();
    }

    private void whenListing(String team, String sprint, String types) {
        StepVerifier.create(useCase.execute(
                        ListWorkItemsCommand.of(ORG, PROJECT, team, sprint, types, API_VERSION)))
                .expectNextCount(1)
                .verifyComplete();
    }

    // ---------------------------------------------------------------------------------------
    // Caso 1 — Camino feliz: la plantilla completa y el ORDER BY
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN area e iteracion resueltas WHEN se listan los items THEN la sentencia WIQL es exactamente la esperada")
    void givenResolvedPaths_whenListing_thenWiqlIsExact() {
        // Arrange (GIVEN)
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationResolvesTo(SPRINT, ITERATION_PATH);
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, SPRINT, null);

        // Assert (THEN)
        assertEquals(expectedWiql(ITERATION_PATH, AREA_PATH, DEFAULT_TYPES), capturedWiql());
    }

    // ---------------------------------------------------------------------------------------
    // Casos 2 a 4 — Tipos de elemento de trabajo
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN workItemTypes en blanco WHEN se listan los items THEN se usan los tipos por defecto")
    void givenBlankTypes_whenListing_thenDefaultTypesAreUsed() {
        // Arrange (GIVEN)
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationResolvesTo(SPRINT, ITERATION_PATH);
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, SPRINT, "   ");

        // Assert (THEN)
        assertEquals(expectedWiql(ITERATION_PATH, AREA_PATH, DEFAULT_TYPES), capturedWiql());
    }

    /**
     * Congela la traducción {@code User Story → Historia de Usuario}. <b>DP-04 §0.2 decidió que es
     * regla de dominio</b>, así que ahora vive en el objeto de valor {@code WorkItemTypes} y no en
     * el entry-point (deuda <b>D-08</b>).
     */
    @Test
    @DisplayName("GIVEN tipos en ingles WHEN se listan los items THEN User Story se traduce y todo se entrecomilla")
    void givenEnglishTypes_whenListing_thenTypesAreTranslatedAndQuoted() {
        // Arrange (GIVEN)
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationResolvesTo(SPRINT, ITERATION_PATH);
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, SPRINT, "User Story, Bug");

        // Assert (THEN)
        assertEquals(expectedWiql(ITERATION_PATH, AREA_PATH, "'Historia de Usuario','Bug'"),
                capturedWiql());
    }

    @Test
    @DisplayName("GIVEN tipos ya entrecomillados WHEN se listan los items THEN no se duplican las comillas")
    void givenAlreadyQuotedTypes_whenListing_thenQuotesAreNotDuplicated() {
        // Arrange (GIVEN)
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationResolvesTo(SPRINT, ITERATION_PATH);
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, SPRINT, "'Task'");

        // Assert (THEN)
        assertEquals(expectedWiql(ITERATION_PATH, AREA_PATH, "'Task'"), capturedWiql());
    }

    // ---------------------------------------------------------------------------------------
    // Caso 5 — Limpieza de la ruta de la célula
    // ---------------------------------------------------------------------------------------

    /**
     * El nombre de la célula puede llegar como ruta y con las barras duplicadas por el cliente MCP.
     * El objeto de valor las colapsa y se queda con el último tramo antes de preguntarle a Azure
     * DevOps.
     */
    @Test
    @DisplayName("GIVEN una celda con ruta y barras dobles WHEN se listan los items THEN se consulta solo el ultimo tramo")
    void givenTeamAsPathWithDoubleBackslashes_whenListing_thenOnlyLastSegmentIsQueried() {
        // Arrange (GIVEN)
        String teamAsPath = PROJECT + "\\\\" + TEAM;
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationResolvesTo(SPRINT, ITERATION_PATH);
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(teamAsPath, SPRINT, null);

        // Assert (THEN)
        verify(getTeamFieldValuesUseCase).getTeamFieldValues(ORG, PROJECT, TEAM);
        assertEquals(expectedWiql(ITERATION_PATH, AREA_PATH, DEFAULT_TYPES), capturedWiql());
    }

    // ---------------------------------------------------------------------------------------
    // Casos 6 a 8 — Repliegues por concatenación
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN Azure DevOps no resuelve el AreaPath WHEN se listan los items THEN se fabrica por concatenacion")
    void givenAreaPathCannotBeResolved_whenListing_thenPathIsBuiltByConcatenation() {
        // Arrange (GIVEN)
        givenAreaPathCannotBeResolved(TEAM);
        givenIterationResolvesTo(SPRINT, ITERATION_PATH);
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, SPRINT, null);

        // Assert (THEN) — PROJECT + '\' + TEAM
        assertEquals(expectedWiql(ITERATION_PATH, AREA_PATH, DEFAULT_TYPES), capturedWiql());
    }

    /**
     * ⚠️ <b>Esta prueba fija un comportamiento que el plan considera defectuoso.</b> Cuando Azure
     * DevOps no resuelve la iteración y el sprint tiene la forma {@code "Sprint N"}, el repliegue
     * intercala <b>el año del calendario</b> (deuda <b>D-09</b>).
     *
     * <p><b>DP-04 §0.1 decidió conservarlo</b> —opción (a)—, porque eliminarlo cambiaría el
     * comportamiento observable ante un fallo de Azure DevOps, que {@code CONTRATO-MCP.md} §2.4
     * declara contrato de facto. Lo que sí cambió es que ahora <b>se cuenta y se avisa</b>: ver
     * {@code ResolveTeamScopeUseCaseTest}.
     */
    @Test
    @DisplayName("GIVEN la iteracion no se resuelve y el sprint es 'Sprint N' WHEN se listan los items THEN se intercala el anio en curso")
    void givenIterationCannotBeResolvedAndSprintIsNumbered_whenListing_thenCurrentYearIsInterleaved() {
        // Arrange (GIVEN)
        int currentYear = LocalDate.now(ZoneId.systemDefault()).getYear();
        String fallbackIteration = PROJECT + "\\" + currentYear + "\\" + SPRINT;
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationCannotBeResolved();
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, SPRINT, null);

        // Assert (THEN)
        assertEquals(expectedWiql(fallbackIteration, AREA_PATH, DEFAULT_TYPES), capturedWiql());
    }

    @Test
    @DisplayName("GIVEN la iteracion no se resuelve y el sprint no es 'Sprint N' WHEN se listan los items THEN NO se intercala anio alguno")
    void givenIterationCannotBeResolvedAndSprintIsNamed_whenListing_thenNoYearIsInterleaved() {
        // Arrange (GIVEN)
        String namedSprint = "Cierre de Trimestre";
        String fallbackIteration = PROJECT + "\\" + namedSprint;
        givenAreaPathResolvesTo(TEAM, AREA_PATH);
        givenIterationCannotBeResolved();
        givenQueryIsAccepted(WiqlResult.builder().build());

        // Act (WHEN)
        whenListing(TEAM, namedSprint, null);

        // Assert (THEN)
        assertEquals(expectedWiql(fallbackIteration, AREA_PATH, DEFAULT_TYPES), capturedWiql());
    }
}

