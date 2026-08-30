package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes y casos borde de {@link SprintName}.
 */
class SprintNameTest {

    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String SPRINT = "Sprint 247";

    @Test
    @DisplayName("GIVEN un nombre simple WHEN se crea THEN se conserva tal cual")
    void givenPlainName_whenCreated_thenValueIsKept() {
        assertEquals(SPRINT, SprintName.of(SPRINT).value());
    }

    @Test
    @DisplayName("GIVEN un nombre con barras dobles WHEN se crea THEN se colapsan a una")
    void givenDoubleBackslashes_whenCreated_thenTheyAreCollapsed() {
        // Arrange (GIVEN)
        String raw = PROJECT + "\\\\2026\\\\" + SPRINT;

        // Act (WHEN)
        SprintName sprintName = SprintName.of(raw);

        // Assert (THEN)
        assertEquals(PROJECT + "\\2026\\" + SPRINT, sprintName.value());
    }

    /**
     * A diferencia de {@link TeamName}, el sprint <b>no</b> se recorta al último tramo: el caso de
     * uso de iteraciones ya lo hace por su cuenta y el repliegue necesita la ruta entera.
     */
    @Test
    @DisplayName("GIVEN un sprint con ruta WHEN se crea THEN NO se recorta al ultimo tramo")
    void givenPathLikeSprint_whenCreated_thenItIsNotShortened() {
        // Arrange (GIVEN)
        String raw = PROJECT + "\\2026\\" + SPRINT;

        // Assert (THEN)
        assertEquals(raw, SprintName.of(raw).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("GIVEN un nombre vacio o en blanco WHEN se crea THEN se rechaza")
    void givenBlankName_whenCreated_thenItIsRejected(String blank) {
        assertThrows(IllegalArgumentException.class, () -> SprintName.of(blank));
    }

    @Test
    @DisplayName("GIVEN un nombre nulo WHEN se crea THEN se rechaza")
    void givenNullName_whenCreated_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SprintName.of(null));
    }
}

