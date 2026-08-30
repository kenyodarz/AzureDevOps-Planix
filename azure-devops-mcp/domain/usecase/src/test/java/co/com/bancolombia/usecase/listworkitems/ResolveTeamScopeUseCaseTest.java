package co.com.bancolombia.usecase.listworkitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.exception.AzureDevOpsUnavailableException;
import co.com.bancolombia.model.exception.WorkItemNotFoundException;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.workitem.SprintName;
import co.com.bancolombia.model.workitem.TeamName;
import co.com.bancolombia.model.workitem.TeamScope;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
 * <b>Cierra la mitad que faltaba de D-09: el repliegue por concatenación ahora se mide.</b>
 *
 * <p>Hasta la Fase 04 el repliegue existía, se sabía defectuoso y <b>nadie sabía cuántas veces se
 * disparaba</b>, de modo que no había ningún dato con el que decidir si podía retirarse. Estas
 * pruebas verifican que cada disparo queda contado y, además, que el caso realmente peligroso —el
 * que intercala el año del calendario— se distingue del inocuo.
 *
 * <p>El reloj es fijo, de modo que el año del repliegue deja de depender de cuándo se ejecute la
 * prueba. Ése es el motivo de que {@code ResolveTeamScopeUseCase} reciba un {@link Clock} en lugar
 * de llamar a {@code LocalDate.now()}.
 */
@ExtendWith(MockitoExtension.class)
class ResolveTeamScopeUseCaseTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";
    private static final String SPRINT = "Sprint 247";

    /** 2025-06-15T00:00:00Z: un año fijo y distinto del actual, para que el fallo sea evidente. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-06-15T00:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private GetTeamFieldValuesUseCase getTeamFieldValuesUseCase;
    @Mock
    private GetTeamIterationsUseCase getTeamIterationsUseCase;
    @Mock
    private TeamScopeFallbackMetrics fallbackMetrics;

    private ResolveTeamScopeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveTeamScopeUseCase(getTeamFieldValuesUseCase, getTeamIterationsUseCase,
                fallbackMetrics, FIXED_CLOCK);
    }

    private Mono<TeamScope> resolve(String team, String sprint) {
        return useCase.resolve(ORG, PROJECT, TeamName.of(team), SprintName.of(sprint));
    }

    @Test
    @DisplayName("GIVEN Azure DevOps resuelve ambas rutas WHEN se resuelve el ambito THEN NO se cuenta ningun repliegue")
    void givenBothPathsResolved_whenResolving_thenNoFallbackIsCounted() {
        // Arrange (GIVEN)
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue("area").build()));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.just("iteracion"));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(resolve(TEAM, SPRINT))
                .expectNext(new TeamScope("area", "iteracion"))
                .verifyComplete();
        verifyNoInteractions(fallbackMetrics);
    }

    @Test
    @DisplayName("GIVEN el AreaPath no se resuelve WHEN se resuelve el ambito THEN se cuenta el repliegue del area")
    void givenAreaPathUnresolved_whenResolving_thenAreaFallbackIsCounted() {
        // Arrange (GIVEN)
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.error(new IllegalStateException("Azure DevOps no responde")));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.just("iteracion"));

        // Act (WHEN)
        StepVerifier.create(resolve(TEAM, SPRINT))
                .expectNext(new TeamScope(PROJECT + "\\" + TEAM, "iteracion"))
                .verifyComplete();

        // Assert (THEN)
        verify(fallbackMetrics).areaPathFallbackUsed(eq(TEAM), any(Throwable.class));
        verify(fallbackMetrics, never()).iterationPathFallbackUsed(anyString(), anyBoolean(), any(Throwable.class));
    }

    /**
     * ⚠️ El caso caro: el año lo pone el reloj, no Azure DevOps. Con el reloj fijado en 2025 la
     * ruta fabricada es {@code ...\2025\Sprint 247}, y la métrica lo marca como
     * {@code calendarYearInterleaved = true} para que sea distinguible en el panel.
     */
    @Test
    @DisplayName("GIVEN el IterationPath no se resuelve y el sprint es numerado WHEN se resuelve THEN se cuenta el repliegue con anio")
    void givenIterationUnresolvedAndNumberedSprint_whenResolving_thenFallbackWithYearIsCounted() {
        // Arrange (GIVEN)
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue("area").build()));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.error(new NoSuchElementException("sin iteraciones")));

        // Act (WHEN)
        StepVerifier.create(resolve(TEAM, SPRINT))
                .expectNext(new TeamScope("area", PROJECT + "\\2025\\" + SPRINT))
                .verifyComplete();

        // Assert (THEN)
        verify(fallbackMetrics).iterationPathFallbackUsed(eq(SPRINT), eq(true), any(Throwable.class));
    }

    @Test
    @DisplayName("GIVEN el IterationPath no se resuelve y el sprint tiene nombre propio WHEN se resuelve THEN se cuenta el repliegue sin anio")
    void givenIterationUnresolvedAndNamedSprint_whenResolving_thenFallbackWithoutYearIsCounted() {
        // Arrange (GIVEN)
        String namedSprint = "Cierre de Trimestre";
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue("area").build()));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, namedSprint))
                .thenReturn(Mono.error(new NoSuchElementException("sin iteraciones")));

        // Act (WHEN)
        StepVerifier.create(resolve(TEAM, namedSprint))
                .expectNext(new TeamScope("area", PROJECT + "\\" + namedSprint))
                .verifyComplete();

        // Assert (THEN)
        verify(fallbackMetrics).iterationPathFallbackUsed(eq(namedSprint), eq(false), any(Throwable.class));
    }

    @Test
    @DisplayName("GIVEN ninguna ruta se resuelve WHEN se resuelve el ambito THEN se cuentan los dos repliegues")
    void givenNeitherPathResolved_whenResolving_thenBothFallbacksAreCounted() {
        // Arrange (GIVEN)
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.error(new IllegalStateException("Azure DevOps no responde")));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.error(new NoSuchElementException("sin iteraciones")));

        // Act (WHEN)
        StepVerifier.create(resolve(TEAM, SPRINT))
                .expectNextCount(1)
                .verifyComplete();

        // Assert (THEN)
        verify(fallbackMetrics).areaPathFallbackUsed(eq(TEAM), any(Throwable.class));
        verify(fallbackMetrics).iterationPathFallbackUsed(eq(SPRINT), eq(true), any(Throwable.class));
    }

    /**
     * La célula llega como ruta: a Azure DevOps se le pregunta por el último tramo, pero la ruta
     * completa es la que alimenta el repliegue.
     */
    @Test
    @DisplayName("GIVEN una celula como ruta WHEN se resuelve el ambito THEN se consulta el ultimo tramo y se repliega con la ruta completa")
    void givenTeamAsPath_whenResolving_thenShortNameIsQueriedAndFullPathIsUsedForFallback() {
        // Arrange (GIVEN)
        String teamAsPath = PROJECT + "\\" + TEAM;
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.error(new IllegalStateException("Azure DevOps no responde")));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.just("iteracion"));

        // Act (WHEN)
        StepVerifier.create(resolve(teamAsPath, SPRINT))
                .expectNext(new TeamScope(teamAsPath, "iteracion"))
                .verifyComplete();

        // Assert (THEN)
        verify(getTeamFieldValuesUseCase).getTeamFieldValues(ORG, PROJECT, TEAM);
        verify(fallbackMetrics).areaPathFallbackUsed(eq(teamAsPath), any(Throwable.class));
    }

    @Test
    @DisplayName("GIVEN el ambito resuelto WHEN se leen sus rutas THEN conserva ambas")
    void givenResolvedScope_whenReadingPaths_thenBothAreKept() {
        // Arrange (GIVEN)
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue("area").build()));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.just("iteracion"));

        // Act (WHEN)
        TeamScope scope = resolve(TEAM, SPRINT).block();

        // Assert (THEN)
        assertEquals("area", scope.areaPath());
        assertEquals("iteracion", scope.iterationPath());
    }

    /**
     * <b>La garantía de DP-06 §0.1(d), escrita como prueba.</b>
     *
     * <p>La Fase 06 traduce los fallos técnicos a excepciones de dominio, y la duda legítima era si
     * eso rompería el repliegue que <b>DP-04 §0.1 decidió conservar</b>: si la excepción hubiera
     * cambiado de naturaleza y dejado de ser capturada, donde hoy sale un tablero vacío saldría un
     * error, que es <b>exactamente</b> el cambio de comportamiento observable que DP-04 descartó y
     * que DP-06 §0.1(d) ratificó no tocar.
     *
     * <p>No ocurre, y esta prueba lo fija: el {@code onErrorResume} captura <b>cualquier</b> error
     * del puerto, así que un {@code AZDO_UNAVAILABLE} recién traducido cae al repliegue igual que
     * caía un {@code WebClientResponseException} crudo. Se cuenta, se avisa, y <b>no llega al
     * cliente</b>.
     */
    @Test
    @DisplayName("GIVEN una excepcion de DOMINIO al resolver rutas WHEN se resuelve el ambito THEN sigue cayendo al repliegue y NO se propaga (DP-06 §0.1(d))")
    void givenDomainException_whenResolving_thenFallbackStillAbsorbsIt() {
        // Arrange (GIVEN) — esto es lo que devuelve el adaptador DESDE la Fase 06, no un
        // WebClientResponseException.
        var translated = new AzureDevOpsUnavailableException(
                "Azure DevOps no pudo atender la operacion solicitada (getTeamFieldValues).");
        when(getTeamFieldValuesUseCase.getTeamFieldValues(ORG, PROJECT, TEAM))
                .thenReturn(Mono.error(translated));
        when(getTeamIterationsUseCase.resolveIterationPath(ORG, PROJECT, TEAM, SPRINT))
                .thenReturn(Mono.error(new WorkItemNotFoundException("no existe la iteracion")));

        // Act (WHEN) + Assert (THEN) — completa con valores replegados; NO emite error.
        StepVerifier.create(resolve(TEAM, SPRINT))
                .expectNext(new TeamScope(PROJECT + "\\" + TEAM, PROJECT + "\\2025\\" + SPRINT))
                .verifyComplete();

        // Y la causa que se registra es la excepcion de dominio, no una envoltura opaca.
        verify(fallbackMetrics).areaPathFallbackUsed(TEAM, translated);
        verify(fallbackMetrics).iterationPathFallbackUsed(eq(SPRINT), eq(true),
                any(WorkItemNotFoundException.class));
    }
}




