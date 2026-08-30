package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fija el repliegue por concatenación de {@link TeamPathFallback}.
 *
 * <p>Estas reglas son <b>contrato de facto</b> ({@code CONTRATO-MCP.md} §2.4, puntos 5 y 6) y
 * <b>DP-04 §0.1 decidió conservarlas</b>. La única diferencia respecto al código original es que
 * el año se recibe como parámetro, lo que permite probarlas de forma determinista en lugar de
 * depender del reloj de la máquina.
 */
class TeamPathFallbackTest {

    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";
    private static final String SPRINT = "Sprint 247";
    private static final int YEAR = 2025;

    @Test
    @DisplayName("GIVEN una celula sin el proyecto delante WHEN se fabrica el AreaPath THEN se antepone el proyecto")
    void givenTeamWithoutProject_whenAreaPath_thenProjectIsPrepended() {
        assertEquals(PROJECT + "\\" + TEAM,
                TeamPathFallback.areaPath(PROJECT, TeamName.of(TEAM)));
    }

    @Test
    @DisplayName("GIVEN una celula que ya empieza por el proyecto WHEN se fabrica el AreaPath THEN se deja intacta")
    void givenTeamAlreadyPrefixed_whenAreaPath_thenItIsKept() {
        // Arrange (GIVEN)
        String fullPath = PROJECT + "\\" + TEAM;

        // Assert (THEN)
        assertEquals(fullPath, TeamPathFallback.areaPath(PROJECT, TeamName.of(fullPath)));
    }

    /**
     * ⚠️ Fija el comportamiento <b>defectuoso</b> que da origen al tablero vacío: para un sprint
     * con la forma {@code Sprint N} se intercala el año. Se conserva por DP-04 §0.1(a), pero ahora
     * queda contado por {@code TeamScopeFallbackMetrics}.
     */
    @Test
    @DisplayName("GIVEN un sprint numerado WHEN se fabrica el IterationPath THEN se intercala el anio")
    void givenNumberedSprint_whenIterationPath_thenYearIsInterleaved() {
        assertEquals(PROJECT + "\\" + YEAR + "\\" + SPRINT,
                TeamPathFallback.iterationPath(PROJECT, SprintName.of(SPRINT), YEAR));
    }

    @Test
    @DisplayName("GIVEN un sprint con nombre propio WHEN se fabrica el IterationPath THEN NO se intercala anio alguno")
    void givenNamedSprint_whenIterationPath_thenNoYearIsInterleaved() {
        // Arrange (GIVEN)
        String namedSprint = "Cierre de Trimestre";

        // Assert (THEN)
        assertEquals(PROJECT + "\\" + namedSprint,
                TeamPathFallback.iterationPath(PROJECT, SprintName.of(namedSprint), YEAR));
    }

    @Test
    @DisplayName("GIVEN un sprint que ya empieza por el proyecto WHEN se fabrica el IterationPath THEN se deja intacto")
    void givenSprintAlreadyPrefixed_whenIterationPath_thenItIsKept() {
        // Arrange (GIVEN)
        String fullPath = PROJECT + "\\2026\\" + SPRINT;

        // Assert (THEN)
        assertEquals(fullPath,
                TeamPathFallback.iterationPath(PROJECT, SprintName.of(fullPath), YEAR));
    }

    @Test
    @DisplayName("GIVEN distintos sprints WHEN se pregunta si intercalan anio THEN solo los numerados lo hacen")
    void givenSprints_whenAskedForYearInterleaving_thenOnlyNumberedOnesDo() {
        assertTrue(TeamPathFallback.interleavesCalendarYear(SprintName.of(SPRINT)));
        assertFalse(TeamPathFallback.interleavesCalendarYear(SprintName.of("Sprint")));
        assertFalse(TeamPathFallback.interleavesCalendarYear(SprintName.of("Cierre de Trimestre")));
        assertFalse(TeamPathFallback.interleavesCalendarYear(SprintName.of("Sprint 247 - Extra")));
    }
}

