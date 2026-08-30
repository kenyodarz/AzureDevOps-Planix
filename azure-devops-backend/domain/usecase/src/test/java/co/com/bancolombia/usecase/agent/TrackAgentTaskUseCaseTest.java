package co.com.bancolombia.usecase.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.TaskOrigin;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.agent.exceptions.TaskNotFoundException;
import co.com.bancolombia.model.agent.gateways.AgentGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas del seguimiento de tareas del agente.
 *
 * <p>Ajustadas en la Fase 03: las que verificaban la réplica local de las tareas del agente se
 * <b>invierten</b> a {@code never()}, porque el BFF dejó de escribir tareas ajenas (D-24).
 */
@ExtendWith(MockitoExtension.class)
class TrackAgentTaskUseCaseTest {

    private static final String TASK_ID = "task-42";
    private static final String CONTEXT_ID = "ctx-1";
    private static final String PROMPT = "Analiza el sprint 247";

    @Mock
    private AgentGateway agentGateway;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    private TrackAgentTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TrackAgentTaskUseCase(agentGateway, taskStoreGateway);
    }

    private Task task(TaskState state) {
        return task(TASK_ID, state);
    }

    private Task task(String id, TaskState state) {
        return Task.builder()
                .id(id)
                .contextId(CONTEXT_ID)
                .status(TaskStatus.builder().state(state).build())
                .build();
    }

    @Test
    @DisplayName("GIVEN el agente abre una tarea WHEN sendAndTrack THEN la devuelve sin persistirla")
    void givenAgentOpensTask_whenSendAndTrack_thenItIsReturnedWithoutBeingStored() {
        // GIVEN
        Task working = task(TaskState.WORKING);
        when(agentGateway.sendMessage(any(AgentCommand.class)))
                .thenReturn(Mono.just(new AgentInteraction(working, "En marcha")));

        // WHEN / THEN
        StepVerifier.create(useCase.sendAndTrack(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction -> {
                    assertThat(interaction.hasTask()).isTrue();
                    assertThat(interaction.task().getId()).isEqualTo(TASK_ID);
                    assertThat(interaction.reply()).isEqualTo("En marcha");
                })
                .verifyComplete();

        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN respuesta sin tarea WHEN sendAndTrack THEN no toca el almacén")
    void givenReplyWithoutTask_whenSendAndTrack_thenStoreIsNotTouched() {
        // GIVEN
        when(agentGateway.sendMessage(any(AgentCommand.class)))
                .thenReturn(Mono.just(AgentInteraction.replyOnly("Hola")));

        // WHEN / THEN
        StepVerifier.create(useCase.sendAndTrack(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction -> assertThat(interaction.hasTask()).isFalse())
                .verifyComplete();

        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN el agente falla WHEN sendAndTrack THEN el error se propaga sin registrar nada")
    void givenAgentFails_whenSendAndTrack_thenErrorPropagates() {
        // GIVEN
        when(agentGateway.sendMessage(any(AgentCommand.class)))
                .thenReturn(Mono.error(new IllegalStateException("agente caído")));

        // WHEN / THEN
        StepVerifier.create(useCase.sendAndTrack(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectError(IllegalStateException.class)
                .verify();

        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    /**
     * <b>Invertida en la Fase 03.</b> Antes verificaba que el estado se reflejaba en el almacén
     * local; ahora exige lo contrario: la fuente de verdad es el agente y el BFF no escribe tareas
     * ajenas (D-24).
     */
    @Test
    @DisplayName("GIVEN una tarea en curso WHEN refreshTask THEN consulta al agente y NO persiste")
    void givenRunningTask_whenRefreshTask_thenAgentIsTheSourceOfTruthAndNothingIsStored() {
        // GIVEN
        when(agentGateway.getTask(TASK_ID)).thenReturn(Mono.just(task(TaskState.COMPLETED)));

        // WHEN / THEN
        StepVerifier.create(useCase.refreshTask(TASK_ID))
                .assertNext(refreshed ->
                        assertThat(refreshed.getStatus().getState()).isEqualTo(TaskState.COMPLETED))
                .verifyComplete();

        verify(agentGateway).getTask(TASK_ID);
        verify(taskStoreGateway, never()).findById(anyString());
        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN el agente no conoce la tarea WHEN refreshTask THEN propaga TaskNotFoundException")
    void givenUnknownTask_whenRefreshTask_thenTaskNotFoundIsPropagated() {
        // GIVEN
        when(agentGateway.getTask(TASK_ID))
                .thenReturn(Mono.error(new TaskNotFoundException(TASK_ID)));

        // WHEN / THEN
        StepVerifier.create(useCase.refreshTask(TASK_ID))
                .expectError(TaskNotFoundException.class)
                .verify();

        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    /**
     * <b>Invertida en la Fase 03.</b> Ídem: cancelar es una orden al agente, no una escritura
     * local.
     */
    @Test
    @DisplayName("GIVEN una tarea en curso WHEN cancelTask THEN delega en el agente y NO persiste")
    void givenRunningTask_whenCancelTask_thenAgentCancelsAndNothingIsStored() {
        // GIVEN
        when(agentGateway.cancelTask(TASK_ID)).thenReturn(Mono.just(task(TaskState.CANCELED)));

        // WHEN / THEN
        StepVerifier.create(useCase.cancelTask(TASK_ID))
                .assertNext(canceled ->
                        assertThat(canceled.getStatus().getState()).isEqualTo(TaskState.CANCELED))
                .verifyComplete();

        verify(agentGateway).cancelTask(TASK_ID);
        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN identificador vacío o nulo WHEN refreshTask o cancelTask THEN falla sin llamar al agente")
    void givenBlankTaskId_whenRefreshOrCancel_thenItFailsWithoutCallingTheAgent() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(useCase.refreshTask(null))
                .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(useCase.refreshTask("  "))
                .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(useCase.cancelTask(null))
                .expectError(IllegalArgumentException.class).verify();

        verify(agentGateway, never()).getTask(anyString());
        verify(agentGateway, never()).cancelTask(anyString());
    }

    // ---------------------------------------------------------------------------------------
    // listTasks: unión de fuentes con origen (DP-07-bis)
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN tareas de ambas fuentes WHEN listTasks THEN se combinan etiquetadas con su origen")
    void givenBothSources_whenListTasks_thenTheyAreMergedWithOrigin() {
        // GIVEN
        when(agentGateway.listTasks()).thenReturn(Flux.just(task(TaskState.WORKING)));
        when(taskStoreGateway.findAll())
                .thenReturn(Flux.just(task("dashboard-1", TaskState.COMPLETED)));

        // WHEN / THEN
        StepVerifier.create(useCase.listTasks())
                .assertNext(tracked -> {
                    assertThat(tracked.origin()).isEqualTo(TaskOrigin.AGENT);
                    assertThat(tracked.id()).isEqualTo(TASK_ID);
                })
                .assertNext(tracked -> {
                    assertThat(tracked.origin()).isEqualTo(TaskOrigin.BFF);
                    assertThat(tracked.id()).isEqualTo("dashboard-1");
                })
                .verifyComplete();
    }

    /**
     * Mientras la tabla siga siendo compartida (DP-05, Fase 07), la misma tarea puede llegar por
     * ambas vías. Prevalece la del agente, que es la fuente de verdad.
     */
    @Test
    @DisplayName("GIVEN la misma tarea en ambas fuentes WHEN listTasks THEN se deduplica y gana la del agente")
    void givenDuplicatedTask_whenListTasks_thenAgentWins() {
        // GIVEN
        when(agentGateway.listTasks()).thenReturn(Flux.just(task(TaskState.WORKING)));
        when(taskStoreGateway.findAll()).thenReturn(Flux.just(task(TaskState.FAILED)));

        // WHEN / THEN
        StepVerifier.create(useCase.listTasks())
                .assertNext(tracked -> {
                    assertThat(tracked.origin()).isEqualTo(TaskOrigin.AGENT);
                    assertThat(tracked.task().getStatus().getState())
                            .isEqualTo(TaskState.WORKING);
                })
                .verifyComplete();
    }

    /**
     * El panel de progreso es informativo: que desaparezca entero porque el agente esté caído sería
     * peor que mostrar datos parciales.
     */
    @Test
    @DisplayName("GIVEN el agente no responde WHEN listTasks THEN degrada a las tareas del BFF")
    void givenAgentDown_whenListTasks_thenItDegradesToBffTasks() {
        // GIVEN
        when(agentGateway.listTasks())
                .thenReturn(Flux.error(new AgentUnavailableException("caído", null)));
        when(taskStoreGateway.findAll())
                .thenReturn(Flux.just(task("dashboard-1", TaskState.WORKING)));

        // WHEN / THEN
        StepVerifier.create(useCase.listTasks())
                .assertNext(tracked -> {
                    assertThat(tracked.origin()).isEqualTo(TaskOrigin.BFF);
                    assertThat(tracked.id()).isEqualTo("dashboard-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN el almacén local falla WHEN listTasks THEN el error sí se propaga")
    void givenStoreFails_whenListTasks_thenErrorPropagates() {
        // GIVEN
        when(agentGateway.listTasks()).thenReturn(Flux.empty());
        when(taskStoreGateway.findAll())
                .thenReturn(Flux.error(new IllegalStateException("base de datos caída")));

        // WHEN / THEN
        StepVerifier.create(useCase.listTasks())
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN el agente publica su tarjeta WHEN getAgentCard THEN el BFF la expone")
    void givenAgentCard_whenGetAgentCard_thenItIsExposed() {
        // GIVEN
        when(agentGateway.getAgentCard())
                .thenReturn(Mono.just(AgentCard.builder().name("Agente DevOps").build()));

        // WHEN / THEN
        StepVerifier.create(useCase.getAgentCard())
                .assertNext(card -> assertThat(card.getName()).isEqualTo("Agente DevOps"))
                .verifyComplete();
    }
}
