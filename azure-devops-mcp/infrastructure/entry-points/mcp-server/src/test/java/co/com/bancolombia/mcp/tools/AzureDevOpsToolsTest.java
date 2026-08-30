package co.com.bancolombia.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.workitem.ListWorkItemsCommand;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.listworkitems.ListWorkItemsByTeamAndSprintUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * El entry-point MCP, después de la Fase 04: <b>protocolo y nada más</b>.
 *
 * <p>Estas pruebas <b>sustituyen</b> a las siete que había aquí y que comprobaban la sentencia
 * WIQL, la resolución de rutas y la normalización de tipos. <b>Ninguno de aquellos escenarios se ha
 * perdido</b>: se reubicaron literalmente a
 * {@code co.com.bancolombia.usecase.listworkitems.ListWorkItemsByTeamAndSprintUseCaseTest}, que es
 * donde vive ahora el flujo. Seguir comprobando la consulta aquí significaría que el entry-point
 * todavía sabe cómo se construye, que es exactamente la deuda <b>D-07</b> que esta fase salda.
 *
 * <p>Lo que sí se comprueba aquí, y antes no comprobaba nadie, es que la herramienta <b>traduce el
 * protocolo sin alterarlo</b>: que los seis parámetros MCP llegan al comando de dominio en el orden
 * y con el contenido correctos.
 */
@ExtendWith(MockitoExtension.class)
class AzureDevOpsToolsTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";
    private static final String SPRINT = "Sprint 247";
    private static final String API_VERSION = "7.0";

    @Mock
    private GetWorkItemUseCase getWorkItemUseCase;
    @Mock
    private CreateWorkItemUseCase createWorkItemUseCase;
    @Mock
    private UpdateWorkItemUseCase updateWorkItemUseCase;
    @Mock
    private QueryByWiqlUseCase queryByWiqlUseCase;
    @Mock
    private GetWorkItemsBatchUseCase getWorkItemsBatchUseCase;
    @Mock
    private ListWorkItemsByTeamAndSprintUseCase listWorkItemsByTeamAndSprintUseCase;

    @InjectMocks
    private AzureDevOpsTools azureDevOpsTools;

    private ListWorkItemsCommand capturedCommand() {
        ArgumentCaptor<ListWorkItemsCommand> captor =
                ArgumentCaptor.forClass(ListWorkItemsCommand.class);
        verify(listWorkItemsByTeamAndSprintUseCase).execute(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("GIVEN los parametros MCP WHEN se invoca la tool THEN delega en el caso de uso y devuelve su resultado")
    void givenMcpParameters_whenListing_thenItDelegatesAndReturnsTheResult() {
        // Arrange (GIVEN)
        WiqlResult expected = WiqlResult.builder().build();
        when(listWorkItemsByTeamAndSprintUseCase.execute(any()))
                .thenReturn(Mono.just(expected));

        // Act (WHEN)
        Mono<WiqlResult> result = azureDevOpsTools.listWorkItemsByTeamAndSprint(ORG, PROJECT, TEAM,
                SPRINT, null, API_VERSION);

        // Assert (THEN)
        StepVerifier.create(result).expectNext(expected).verifyComplete();
    }

    /**
     * El contrato MCP público no cambió: los seis parámetros se siguen recibiendo sueltos y en el
     * mismo orden. Esta prueba lo fija.
     */
    @Test
    @DisplayName("GIVEN los seis parametros MCP WHEN se invoca la tool THEN se trasladan al comando sin alterarse")
    void givenSixMcpParameters_whenListing_thenTheyAreMappedIntoTheCommand() {
        // Arrange (GIVEN)
        when(listWorkItemsByTeamAndSprintUseCase.execute(any()))
                .thenReturn(Mono.just(WiqlResult.builder().build()));

        // Act (WHEN)
        azureDevOpsTools.listWorkItemsByTeamAndSprint(ORG, PROJECT, TEAM, SPRINT, "Bug",
                API_VERSION).block();

        // Assert (THEN)
        ListWorkItemsCommand command = capturedCommand();
        assertEquals(ORG, command.organization());
        assertEquals(PROJECT, command.project());
        assertEquals(TEAM, command.team().value());
        assertEquals(SPRINT, command.sprint().value());
        assertEquals("'Bug'", command.workItemTypes().toWiqlList());
        assertEquals(API_VERSION, command.apiVersion());
    }

    @Test
    @DisplayName("GIVEN un id WHEN se invoca getWorkItem THEN delega en su caso de uso")
    void givenId_whenGetWorkItem_thenItDelegates() {
        // Arrange (GIVEN)
        WorkItem expected = WorkItem.builder().build();
        when(getWorkItemUseCase.getWorkItem(ORG, PROJECT, 1, null))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(azureDevOpsTools.getWorkItem(ORG, PROJECT, 1, null))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un parche WHEN se invoca createWorkItem THEN delega traduciendo el DTO a dominio")
    void givenPatch_whenCreateWorkItem_thenItDelegatesWithDomainTypes() {
        // Arrange (GIVEN)
        WorkItem expected = WorkItem.builder().build();
        when(createWorkItemUseCase.createWorkItem(eq(ORG), eq(PROJECT), eq("Task"), any(),
                eq(null))).thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(
                        azureDevOpsTools.createWorkItem(ORG, PROJECT, "Task", List.of(), null))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un parche WHEN se invoca updateWorkItem THEN delega traduciendo el DTO a dominio")
    void givenPatch_whenUpdateWorkItem_thenItDelegatesWithDomainTypes() {
        // Arrange (GIVEN)
        WorkItem expected = WorkItem.builder().build();
        when(updateWorkItemUseCase.updateWorkItem(eq(ORG), eq(PROJECT), eq(1), any(), eq(null)))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(azureDevOpsTools.updateWorkItem(ORG, PROJECT, 1, List.of(), null))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN una sentencia WHEN se invoca queryByWiql THEN delega envolviendola en el modelo de dominio")
    void givenStatement_whenQueryByWiql_thenItDelegates() {
        // Arrange (GIVEN)
        WiqlResult expected = WiqlResult.builder().build();
        when(queryByWiqlUseCase.queryByWiql(eq(ORG), eq(PROJECT), any(WiqlQuery.class), eq(null)))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(azureDevOpsTools.queryByWiql(ORG, PROJECT, "SELECT 1", null))
                .expectNext(expected)
                .verifyComplete();
    }

    /**
     * Los valores por defecto {@code None} y {@code Omit} son contrato público
     * ({@code CONTRATO-MCP.md} §2.5) y los aplica el entry-point, no el dominio: son opcionalidad
     * del protocolo, no una regla de negocio de Azure DevOps.
     */
    @Test
    @DisplayName("GIVEN expand y errorPolicy en blanco WHEN se invoca getWorkItemsBatch THEN se aplican None y Omit")
    void givenBlankOptionalParameters_whenBatch_thenDefaultsAreApplied() {
        // Arrange (GIVEN)
        when(getWorkItemsBatchUseCase.getWorkItemsBatch(eq(ORG), eq(PROJECT), any(), eq(null)))
                .thenReturn(Mono.just(List.of(WorkItem.builder().build())));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(azureDevOpsTools.getWorkItemsBatch(ORG, PROJECT, List.of(1), null, "  ",
                        "  ", null))
                .expectNextCount(1)
                .verifyComplete();
    }
}

