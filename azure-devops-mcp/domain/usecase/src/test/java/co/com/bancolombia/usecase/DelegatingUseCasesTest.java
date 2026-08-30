package co.com.bancolombia.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.createworkitem.gateways.CreateWorkItemRepository;
import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.querybywiql.gateways.QueryByWiqlRepository;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.gateways.GetTeamFieldValuesRepository;
import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Cubre los seis casos de uso delegantes (deuda <b>D-12</b>).
 *
 * <p>Son delegantes de 17–18 líneas y seguirán siéndolo: <b>no todo caso de uso tiene que tener
 * lógica</b>. Lo que no era aceptable es que la capa estuviera vacía <i>y</i> sin probar, de modo
 * que nadie pudiera detectar si un día uno de ellos dejaba de llamar a su gateway o le cambiaba un
 * argumento por el camino. Estas pruebas fijan exactamente eso: qué se llama y con qué.
 */
@ExtendWith(MockitoExtension.class)
class DelegatingUseCasesTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String API_VERSION = "7.1";

    @Mock
    private GetWorkItemRepository getWorkItemRepository;
    @Mock
    private CreateWorkItemRepository createWorkItemRepository;
    @Mock
    private UpdateWorkItemRepository updateWorkItemRepository;
    @Mock
    private QueryByWiqlRepository queryByWiqlRepository;
    @Mock
    private GetWorkItemsBatchRepository getWorkItemsBatchRepository;
    @Mock
    private GetTeamFieldValuesRepository getTeamFieldValuesRepository;

    @Test
    @DisplayName("GIVEN un id WHEN se ejecuta GetWorkItemUseCase THEN delega en su gateway sin alterar los argumentos")
    void givenId_whenGetWorkItem_thenItDelegates() {
        // Arrange (GIVEN)
        WorkItem expected = WorkItem.builder().build();
        when(getWorkItemRepository.getWorkItem(ORG, PROJECT, 42, API_VERSION))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(new GetWorkItemUseCase(getWorkItemRepository)
                        .getWorkItem(ORG, PROJECT, 42, API_VERSION))
                .expectNext(expected)
                .verifyComplete();
        verify(getWorkItemRepository).getWorkItem(ORG, PROJECT, 42, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN un parche WHEN se ejecuta CreateWorkItemUseCase THEN delega en su gateway")
    void givenPatch_whenCreateWorkItem_thenItDelegates() {
        // Arrange (GIVEN)
        WorkItem expected = WorkItem.builder().build();
        List<JsonPatchOperation> patch = List.of(JsonPatchOperation.builder().op("add").build());
        when(createWorkItemRepository.createWorkItem(ORG, PROJECT, "Task", patch, API_VERSION))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(new CreateWorkItemUseCase(createWorkItemRepository)
                        .createWorkItem(ORG, PROJECT, "Task", patch, API_VERSION))
                .expectNext(expected)
                .verifyComplete();
        verify(createWorkItemRepository).createWorkItem(ORG, PROJECT, "Task", patch, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN un parche WHEN se ejecuta UpdateWorkItemUseCase THEN delega en su gateway")
    void givenPatch_whenUpdateWorkItem_thenItDelegates() {
        // Arrange (GIVEN)
        WorkItem expected = WorkItem.builder().build();
        List<JsonPatchOperation> patch = List.of(JsonPatchOperation.builder().op("replace").build());
        when(updateWorkItemRepository.updateWorkItem(ORG, PROJECT, 42, patch, API_VERSION))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(new UpdateWorkItemUseCase(updateWorkItemRepository)
                        .updateWorkItem(ORG, PROJECT, 42, patch, API_VERSION))
                .expectNext(expected)
                .verifyComplete();
        verify(updateWorkItemRepository).updateWorkItem(ORG, PROJECT, 42, patch, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN una sentencia WHEN se ejecuta QueryByWiqlUseCase THEN delega en su gateway")
    void givenStatement_whenQueryByWiql_thenItDelegates() {
        // Arrange (GIVEN)
        WiqlResult expected = WiqlResult.builder().build();
        WiqlQuery query = WiqlQuery.builder().query("SELECT [System.Id] FROM workitems").build();
        when(queryByWiqlRepository.queryByWiql(ORG, PROJECT, query, API_VERSION))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(new QueryByWiqlUseCase(queryByWiqlRepository)
                        .queryByWiql(ORG, PROJECT, query, API_VERSION))
                .expectNext(expected)
                .verifyComplete();
        verify(queryByWiqlRepository).queryByWiql(ORG, PROJECT, query, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN unos criterios WHEN se ejecuta GetWorkItemsBatchUseCase THEN delega en su gateway")
    void givenCriteria_whenBatch_thenItDelegates() {
        // Arrange (GIVEN)
        List<WorkItem> expected = List.of(WorkItem.builder().build());
        WorkItemBatchCriteria criteria = WorkItemBatchCriteria.builder().ids(List.of(1)).build();
        when(getWorkItemsBatchRepository.getWorkItemsBatch(ORG, PROJECT, criteria, API_VERSION))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(new GetWorkItemsBatchUseCase(getWorkItemsBatchRepository)
                        .getWorkItemsBatch(ORG, PROJECT, criteria, API_VERSION))
                .expectNext(expected)
                .verifyComplete();
        verify(getWorkItemsBatchRepository).getWorkItemsBatch(ORG, PROJECT, criteria, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN una celula WHEN se ejecuta GetTeamFieldValuesUseCase THEN delega en su gateway")
    void givenTeam_whenTeamFieldValues_thenItDelegates() {
        // Arrange (GIVEN)
        TeamFieldValues expected = TeamFieldValues.builder().defaultValue("area").build();
        when(getTeamFieldValuesRepository.getTeamFieldValues(ORG, PROJECT, "EQU1096 - EXODIA"))
                .thenReturn(Mono.just(expected));

        // Act (WHEN) + Assert (THEN)
        StepVerifier.create(new GetTeamFieldValuesUseCase(getTeamFieldValuesRepository)
                        .getTeamFieldValues(ORG, PROJECT, "EQU1096 - EXODIA"))
                .expectNext(expected)
                .verifyComplete();
        verify(getTeamFieldValuesRepository).getTeamFieldValues(ORG, PROJECT, "EQU1096 - EXODIA");
    }
}




