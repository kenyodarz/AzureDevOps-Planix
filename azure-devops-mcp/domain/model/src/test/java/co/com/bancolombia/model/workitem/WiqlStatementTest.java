package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Congela la plantilla WIQL <b>carácter a carácter</b> en el sitio donde ahora se construye.
 *
 * <p>Es el gemelo, a nivel de dominio, de {@code WiqlCharacterizationTest}: aquélla comprueba la
 * sentencia que sale del flujo completo con sus ocho ramas; ésta comprueba la plantilla en sí,
 * aislada de la resolución de rutas. La plantilla se duplica aquí <b>a propósito</b>, igual que en
 * la prueba de caracterización: si alguien cambia la del código productivo, esta prueba lo detecta.
 * Una constante compartida no detectaría nada.
 */
class WiqlStatementTest {

    private static final String AREA_PATH =
            "Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA";
    private static final String ITERATION_PATH =
            "Vicepresidencia Servicios de Tecnología\\2025\\Sprint 247";

    private static final String EXPECTED_TEMPLATE =
            "SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project"
                    + " AND [System.IterationPath] = '%s'"
                    + " AND [System.AreaPath] = '%s'"
                    + " AND [System.WorkItemType] IN (%s)"
                    + " ORDER BY [System.Id]";

    @Test
    @DisplayName("GIVEN un ambito y los tipos por defecto WHEN se redacta THEN la sentencia es exactamente la esperada")
    void givenScopeAndDefaultTypes_whenBuilt_thenStatementIsExact() {
        // Arrange (GIVEN)
        TeamScope scope = new TeamScope(AREA_PATH, ITERATION_PATH);

        // Act (WHEN)
        WiqlStatement statement = WiqlStatement.forTeamAndSprint(scope, WorkItemTypes.defaults());

        // Assert (THEN)
        assertEquals(String.format(EXPECTED_TEMPLATE, ITERATION_PATH, AREA_PATH,
                "'Historia de Usuario','Habilitador'"), statement.value());
    }

    /**
     * El orden de los argumentos es la trampa más silenciosa de esta plantilla: intercambiar
     * {@code IterationPath} y {@code AreaPath} produce una consulta válida que devuelve cero
     * elementos.
     */
    @Test
    @DisplayName("GIVEN un ambito WHEN se redacta THEN la iteracion va antes que el area")
    void givenScope_whenBuilt_thenIterationComesBeforeArea() {
        // Arrange (GIVEN)
        TeamScope scope = new TeamScope(AREA_PATH, ITERATION_PATH);

        // Act (WHEN)
        String statement = WiqlStatement.forTeamAndSprint(scope, WorkItemTypes.defaults()).value();

        // Assert (THEN)
        assertEquals("[System.IterationPath] = '" + ITERATION_PATH + "'",
                statement.substring(statement.indexOf("[System.IterationPath]"),
                        statement.indexOf(" AND [System.AreaPath]")));
    }

    @Test
    @DisplayName("GIVEN tipos personalizados WHEN se redacta THEN se insertan en la clausula IN")
    void givenCustomTypes_whenBuilt_thenTheyAreInsertedInTheInClause() {
        // Arrange (GIVEN)
        TeamScope scope = new TeamScope(AREA_PATH, ITERATION_PATH);

        // Act (WHEN)
        String statement = WiqlStatement
                .forTeamAndSprint(scope, WorkItemTypes.parse("User Story, Task, Bug")).value();

        // Assert (THEN)
        assertEquals(String.format(EXPECTED_TEMPLATE, ITERATION_PATH, AREA_PATH,
                "'Historia de Usuario','Task','Bug'"), statement);
    }

    @Test
    @DisplayName("GIVEN una sentencia en blanco WHEN se construye THEN se rechaza")
    void givenBlankStatement_whenCreated_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WiqlStatement("   "));
    }

    @Test
    @DisplayName("GIVEN una sentencia nula WHEN se construye THEN se rechaza")
    void givenNullStatement_whenCreated_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WiqlStatement(null));
    }
}

