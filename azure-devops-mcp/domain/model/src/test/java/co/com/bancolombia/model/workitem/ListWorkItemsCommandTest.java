package co.com.bancolombia.model.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Invariantes de {@link ListWorkItemsCommand} y de {@link TeamScope}.
 *
 * <p>Comprueba que la fábrica {@code of(...)} traduce el texto crudo del cable a objetos de valor
 * de dominio aplicando las mismas reglas que antes vivían en el entry-point.
 */
class ListWorkItemsCommandTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";
    private static final String SPRINT = "Sprint 247";
    private static final String API_VERSION = "7.0";

    @Test
    @DisplayName("GIVEN los parametros crudos WHEN se construye el comando THEN se normalizan a objetos de valor")
    void givenRawParameters_whenBuilt_thenTheyBecomeValueObjects() {
        // Act (WHEN)
        ListWorkItemsCommand command = ListWorkItemsCommand.of(ORG, PROJECT,
                PROJECT + "\\\\" + TEAM, SPRINT, null, API_VERSION);

        // Assert (THEN)
        assertEquals(PROJECT + "\\" + TEAM, command.team().value());
        assertEquals(TEAM, command.team().shortName());
        assertEquals(SPRINT, command.sprint().value());
        assertEquals("'Historia de Usuario','Habilitador'",
                command.workItemTypes().toWiqlList());
        assertEquals(API_VERSION, command.apiVersion());
    }

    /**
     * La versión de API es opcional en el contrato MCP y su valor por defecto lo aplica el
     * adaptador, no el comando: aquí se acepta nula sin traducirla.
     */
    @Test
    @DisplayName("GIVEN una version de api nula WHEN se construye el comando THEN se acepta tal cual")
    void givenNullApiVersion_whenBuilt_thenItIsAccepted() {
        ListWorkItemsCommand command = ListWorkItemsCommand.of(ORG, PROJECT, TEAM, SPRINT, null,
                null);

        assertNull(command.apiVersion());
    }

    @Test
    @DisplayName("GIVEN una organizacion en blanco WHEN se construye el comando THEN se rechaza")
    void givenBlankOrganization_whenBuilt_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ListWorkItemsCommand.of("  ", PROJECT, TEAM, SPRINT, null, API_VERSION));
    }

    @Test
    @DisplayName("GIVEN un proyecto nulo WHEN se construye el comando THEN se rechaza")
    void givenNullProject_whenBuilt_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ListWorkItemsCommand.of(ORG, null, TEAM, SPRINT, null, API_VERSION));
    }

    @Test
    @DisplayName("GIVEN una celula nula WHEN se construye el comando THEN se rechaza")
    void givenNullTeam_whenBuilt_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ListWorkItemsCommand.of(ORG, PROJECT, null, SPRINT, null, API_VERSION));
    }

    /**
     * Solo se rechazan los nulos: una ruta de área en blanco es tolerada a propósito, porque el
     * comportamiento heredado ante un {@code defaultValue} vacío era emitir la consulta igualmente
     * en lugar de fallar.
     */
    @Test
    @DisplayName("GIVEN un ambito con rutas nulas WHEN se construye THEN se rechaza")
    void givenNullPaths_whenTeamScopeBuilt_thenItIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TeamScope(null, "iteracion"));
        assertThrows(IllegalArgumentException.class, () -> new TeamScope("area", null));
    }

    @Test
    @DisplayName("GIVEN un ambito con el area en blanco WHEN se construye THEN se tolera")
    void givenBlankAreaPath_whenTeamScopeBuilt_thenItIsTolerated() {
        assertEquals("", new TeamScope("", "iteracion").areaPath());
    }

    @Test
    @DisplayName("GIVEN un ambito valido WHEN se construye THEN conserva ambas rutas")
    void givenValidPaths_whenTeamScopeBuilt_thenBothAreKept() {
        // Act (WHEN)
        TeamScope scope = new TeamScope("area", "iteracion");

        // Assert (THEN)
        assertEquals("area", scope.areaPath());
        assertEquals("iteracion", scope.iterationPath());
    }
}



