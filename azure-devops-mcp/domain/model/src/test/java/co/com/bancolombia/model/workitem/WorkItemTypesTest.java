package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes y casos borde de {@link WorkItemTypes}.
 *
 * <p>Las tres reglas que cubre esta clase —tipos por defecto, traducción de {@code User Story} y
 * entrecomillado— son <b>contrato de facto</b> ({@code CONTRATO-MCP.md} §2.4, puntos 3 y 4), así
 * que sus resultados están fijados literalmente.
 */
class WorkItemTypesTest {

    private static final String DEFAULT_TYPES = "'Historia de Usuario','Habilitador'";

    @Test
    @DisplayName("GIVEN ningun tipo WHEN se piden los de por defecto THEN son Historia de Usuario y Habilitador")
    void givenNothing_whenDefaults_thenTheTwoExpectedTypesAreReturned() {
        assertEquals(DEFAULT_TYPES, WorkItemTypes.defaults().toWiqlList());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("GIVEN un csv nulo o en blanco WHEN se interpreta THEN se usan los tipos por defecto")
    void givenBlankCsv_whenParsed_thenDefaultsAreUsed(String csv) {
        assertEquals(DEFAULT_TYPES, WorkItemTypes.parse(csv).toWiqlList());
    }

    @Test
    @DisplayName("GIVEN User Story WHEN se interpreta THEN se traduce a Historia de Usuario")
    void givenUserStory_whenParsed_thenItIsTranslated() {
        assertEquals("'Historia de Usuario','Bug'",
                WorkItemTypes.parse("User Story, Bug").toWiqlList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user story", "USER STORY", "UsEr StOrY"})
    @DisplayName("GIVEN User Story en cualquier caja WHEN se interpreta THEN igualmente se traduce")
    void givenUserStoryInAnyCase_whenParsed_thenItIsTranslated(String type) {
        assertEquals("'Historia de Usuario'", WorkItemTypes.parse(type).toWiqlList());
    }

    @Test
    @DisplayName("GIVEN tipos ya entrecomillados WHEN se interpretan THEN no se duplican las comillas")
    void givenAlreadyQuotedTypes_whenParsed_thenQuotesAreNotDuplicated() {
        assertEquals("'Task'", WorkItemTypes.parse("'Task'").toWiqlList());
    }

    @Test
    @DisplayName("GIVEN tipos con espacios sobrantes WHEN se interpretan THEN se recortan")
    void givenPaddedTypes_whenParsed_thenTheyAreTrimmed() {
        assertEquals("'Task','Bug'", WorkItemTypes.parse("  Task ,  Bug  ").toWiqlList());
    }

    /**
     * §4.1 del plan maestro sugería deduplicar, pero el código original <b>no</b> lo hacía. Esta
     * prueba fija esa ausencia a propósito: deduplicar cambiaría la sentencia WIQL emitida, y la
     * regla de oro de la Fase 04 es que salga idéntica.
     */
    @Test
    @DisplayName("GIVEN tipos repetidos WHEN se interpretan THEN NO se deduplican")
    void givenRepeatedTypes_whenParsed_thenTheyAreNotDeduplicated() {
        assertEquals("'Bug','Bug'", WorkItemTypes.parse("Bug, Bug").toWiqlList());
    }

    @Test
    @DisplayName("GIVEN una lista vacia WHEN se construye THEN se rechaza")
    void givenEmptyList_whenCreated_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WorkItemTypes(List.of()));
    }

    @Test
    @DisplayName("GIVEN una lista nula WHEN se construye THEN se rechaza")
    void givenNullList_whenCreated_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WorkItemTypes(null));
    }

    @Test
    @DisplayName("GIVEN un objeto de valor WHEN se intenta mutar su lista THEN es inmutable")
    void givenValueObject_whenMutatingItsList_thenItIsImmutable() {
        // Arrange (GIVEN)
        WorkItemTypes types = WorkItemTypes.defaults();

        // Assert (THEN)
        assertThrows(UnsupportedOperationException.class, () -> types.values().add("'Bug'"));
    }
}

