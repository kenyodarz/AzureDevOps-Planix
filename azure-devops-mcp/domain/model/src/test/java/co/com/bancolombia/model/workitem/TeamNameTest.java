package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes y casos borde de {@link TeamName}.
 *
 * <p>Primera prueba de {@code domain/model}: hasta la Fase 04 el módulo <b>no tenía carpeta
 * {@code src/test}</b> y por tanto ni siquiera participaba en la medición de cobertura
 * (deuda <b>D-26</b>).
 */
class TeamNameTest {

    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";

    @Test
    @DisplayName("GIVEN un nombre simple WHEN se crea THEN se conserva tal cual")
    void givenPlainName_whenCreated_thenValueIsKept() {
        // Arrange (GIVEN) + Act (WHEN)
        TeamName teamName = TeamName.of(TEAM);

        // Assert (THEN)
        assertEquals(TEAM, teamName.value());
        assertEquals(TEAM, teamName.shortName());
    }

    @Test
    @DisplayName("GIVEN un nombre con barras dobles WHEN se crea THEN se colapsan a una")
    void givenDoubleBackslashes_whenCreated_thenTheyAreCollapsed() {
        // Arrange (GIVEN)
        String raw = PROJECT + "\\\\" + TEAM;

        // Act (WHEN)
        TeamName teamName = TeamName.of(raw);

        // Assert (THEN)
        assertEquals(PROJECT + "\\" + TEAM, teamName.value());
    }

    @Test
    @DisplayName("GIVEN un nombre con ruta WHEN se pide el nombre corto THEN devuelve el ultimo tramo")
    void givenPathLikeName_whenShortName_thenLastSegmentIsReturned() {
        // Arrange (GIVEN)
        TeamName teamName = TeamName.of(PROJECT + "\\\\" + TEAM);

        // Assert (THEN)
        assertEquals(TEAM, teamName.shortName());
    }

    /**
     * La ruta completa <b>debe</b> seguir disponible: es la que alimenta el repliegue por
     * concatenación del {@code AreaPath}. Fundirla con el nombre corto cambiaría la sentencia WIQL
     * de esa rama.
     */
    @Test
    @DisplayName("GIVEN un nombre con ruta WHEN se consulta el valor THEN conserva la ruta completa")
    void givenPathLikeName_whenValue_thenFullPathIsKept() {
        // Arrange (GIVEN)
        TeamName teamName = TeamName.of(PROJECT + "\\" + TEAM);

        // Assert (THEN)
        assertEquals(PROJECT + "\\" + TEAM, teamName.value());
    }

    @Test
    @DisplayName("GIVEN un ultimo tramo con espacios WHEN se pide el nombre corto THEN se recortan")
    void givenPaddedLastSegment_whenShortName_thenItIsTrimmed() {
        // Arrange (GIVEN)
        TeamName teamName = TeamName.of(PROJECT + "\\   " + TEAM + "   ");

        // Assert (THEN)
        assertEquals(TEAM, teamName.shortName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("GIVEN un nombre vacio o en blanco WHEN se crea THEN se rechaza")
    void givenBlankName_whenCreated_thenItIsRejected(String blank) {
        assertThrows(IllegalArgumentException.class, () -> TeamName.of(blank));
    }

    @Test
    @DisplayName("GIVEN un nombre nulo WHEN se crea THEN se rechaza")
    void givenNullName_whenCreated_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> TeamName.of(null));
    }
}

