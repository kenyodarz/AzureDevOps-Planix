package co.com.bancolombia.usecase.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TriggerProgramPlanningUseCaseTest {

    private static final String QUARTER = "Q3-2026";
    private static final String OBJECTIVES = "Modernización Cloud y Arquitectura Limpia";
    private static final List<String> FRONTS = List.of("Canales", "Core");
    private static final int SPRINTS = 6;
    private static final int CAPACITY = 34;
    private static final String CONTEXT_ID = "session-plan-789";

    @Mock
    private TrackAgentTaskUseCase trackAgentTaskUseCase;

    private TriggerProgramPlanningUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TriggerProgramPlanningUseCase(trackAgentTaskUseCase);
    }

    private ProgramPlanRequest validRequest() {
        return ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .targetFronts(FRONTS)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();
    }

    private Task mockTask() {
        return Task.builder()
                .id("task-123")
                .contextId(CONTEXT_ID)
                .status(TaskStatus.builder().state(TaskState.WORKING).build())
                .build();
    }

    @Test
    @DisplayName("GIVEN request válido y contextId WHEN execute THEN despacha AgentCommand no bloqueante canónico")
    void givenValidRequestAndContextId_whenExecute_thenDelegatesToTrackAgentTaskWithCanonicalCommand() {
        // GIVEN
        ProgramPlanRequest request = validRequest();
        AgentInteraction expectedInteraction = new AgentInteraction(mockTask(),
                "Planeación en progreso");
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.just(expectedInteraction));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(request, CONTEXT_ID))
                .assertNext(interaction -> {
                    assertThat(interaction.hasTask()).isTrue();
                    assertThat(interaction.task().getId()).isEqualTo("task-123");
                    assertThat(interaction.reply()).isEqualTo("Planeación en progreso");
                })
                .verifyComplete();

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(trackAgentTaskUseCase).sendAndTrack(captor.capture());

        AgentCommand sentCommand = captor.getValue();
        assertThat(sentCommand.contextId()).isEqualTo(CONTEXT_ID);
        assertThat(sentCommand.blocking()).isFalse();
        assertThat(sentCommand.prompt())
                .isEqualTo(
                        "/plan Q3-2026 6 sprints 34 sp [Canales, Core] Objetivos: Modernización Cloud y Arquitectura Limpia");
    }

    @Test
    @DisplayName("GIVEN request válido sin contextId WHEN execute THEN genera contextId UUID y despacha comando")
    void givenValidRequestWithoutContextId_whenExecute_thenGeneratesUuidContextIdAndDispatches() {
        // GIVEN
        ProgramPlanRequest request = validRequest();
        AgentInteraction expectedInteraction = new AgentInteraction(mockTask(),
                "Planeación iniciada");
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.just(expectedInteraction));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(request))
                .assertNext(interaction -> assertThat(interaction.hasTask()).isTrue())
                .verifyComplete();

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(trackAgentTaskUseCase).sendAndTrack(captor.capture());

        AgentCommand sentCommand = captor.getValue();
        assertThat(sentCommand.contextId()).isNotBlank();
        assertThat(sentCommand.blocking()).isFalse();
        assertThat(sentCommand.prompt()).startsWith("/plan Q3-2026 6 sprints 34 sp");
    }

    @Test
    @DisplayName("GIVEN request con contextId en blanco WHEN execute con sobrecarga THEN genera contextId UUID")
    void givenRequestWithBlankContextId_whenExecute_thenGeneratesUuidContextId() {
        // GIVEN
        ProgramPlanRequest request = validRequest();
        AgentInteraction expectedInteraction = new AgentInteraction(mockTask(),
                "Planeación iniciada");
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.just(expectedInteraction));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(request, "   "))
                .assertNext(interaction -> assertThat(interaction.hasTask()).isTrue())
                .verifyComplete();

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(trackAgentTaskUseCase).sendAndTrack(captor.capture());

        AgentCommand sentCommand = captor.getValue();
        assertThat(sentCommand.contextId()).isNotBlank().isNotEqualTo("   ");
    }

    @Test
    @DisplayName("GIVEN request nulo WHEN execute THEN emite IllegalArgumentException sin llamar al agente")
    void givenNullRequest_whenExecute_thenReturnsIllegalArgumentException() {
        // WHEN / THEN
        StepVerifier.create(useCase.execute(null))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage()
                        .contains("El request de planeación no puede ser nulo"))
                .verify();

        verify(trackAgentTaskUseCase, never()).sendAndTrack(any());
    }

    @Test
    @DisplayName("GIVEN request con formato de quarter inválido WHEN execute THEN propaga IllegalArgumentException reactivamente")
    void givenRequestWithInvalidQuarter_whenExecute_thenPropagatesIllegalArgumentException() {
        // GIVEN: ProgramPlanRequest permite cualquier String no vacío, pero ProgramPlanningCommand valida QX-YYYY
        ProgramPlanRequest requestWithBadQuarter = ProgramPlanRequest.builder()
                .quarter("2026-INVALID")
                .objectives(OBJECTIVES)
                .targetFronts(FRONTS)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        // WHEN / THEN
        StepVerifier.create(useCase.execute(requestWithBadQuarter, CONTEXT_ID))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage()
                        .contains("El trimestre debe cumplir el formato QX-YYYY"))
                .verify();

        verify(trackAgentTaskUseCase, never()).sendAndTrack(any());
    }

    @Test
    @DisplayName("GIVEN el agente falla WHEN execute THEN propaga la excepción recibida")
    void givenAgentFails_whenExecute_thenPropagatesAgentException() {
        // GIVEN
        ProgramPlanRequest request = validRequest();
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.error(new IllegalStateException("Agente no disponible")));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(request, CONTEXT_ID))
                .expectError(IllegalStateException.class)
                .verify();

        verify(trackAgentTaskUseCase).sendAndTrack(any(AgentCommand.class));
    }
}
