package co.com.bancolombia.api.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.dto.event.TaskUpdateEvent;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.TrackedTask;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TaskStreamOrchestratorTest {

    @Mock
    private TrackAgentTaskUseCase trackAgentTaskUseCase;

    private TaskStreamOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new TaskStreamOrchestrator(trackAgentTaskUseCase, 500, 10);
    }

    private TrackedTask sampleTask(String id, TaskState state) {
        return TrackedTask.fromAgent(Task.builder()
                .id(id)
                .contextId("ctx-1")
                .status(TaskStatus.builder()
                        .state(state)
                        .timestamp("2026-09-08T00:00:00Z")
                        .message(Message.builder().role("agent").parts(List.of(Part.ofText("test")))
                                .build())
                        .build())
                .build());
    }

    @Test
    @DisplayName("GIVEN tareas existentes WHEN stream THEN emite evento INITIAL")
    void givenTasks_whenStream_thenEmitsInitialEvent() {
        when(trackAgentTaskUseCase.listTasks()).thenReturn(Flux.just(
                sampleTask("task-1", TaskState.COMPLETED)));

        Flux<ServerSentEvent<TaskUpdateEvent>> stream = orchestrator.stream();

        StepVerifier.create(stream.take(1))
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo(TaskUpdateEvent.EVENT_INITIAL);
                    assertThat(sse.data()).isNotNull();
                    assertThat(sse.data().getData()).hasSize(1);
                    assertThat(sse.data().getData().getFirst().id()).isEqualTo("task-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN stream activo WHEN notifyTaskChange THEN emite evento TASKS_UPDATE")
    void givenActiveStream_whenNotifyTaskChange_thenEmitsTasksUpdate() {
        when(trackAgentTaskUseCase.listTasks()).thenReturn(Flux.just(
                sampleTask("task-1", TaskState.COMPLETED)));

        StepVerifier.create(orchestrator.stream().take(2))
                .assertNext(sse -> assertThat(sse.event()).isEqualTo(TaskUpdateEvent.EVENT_INITIAL))
                .then(() -> orchestrator.notifyTaskChange())
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo(TaskUpdateEvent.EVENT_TASKS_UPDATE);
                    assertThat(sse.data().getData()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN tarea activa WHEN stream THEN ticker emite TASKS_UPDATE periódicamente")
    void givenActiveTask_whenStream_thenActiveTickerEmits() {
        when(trackAgentTaskUseCase.listTasks()).thenReturn(Flux.just(
                sampleTask("task-active", TaskState.WORKING)));

        StepVerifier.withVirtualTime(() -> orchestrator.stream().take(2))
                .expectSubscription()
                .assertNext(sse -> assertThat(sse.event()).isEqualTo(TaskUpdateEvent.EVENT_INITIAL))
                .thenAwait(Duration.ofMillis(600))
                .assertNext(sse -> assertThat(sse.event()).isEqualTo(
                        TaskUpdateEvent.EVENT_TASKS_UPDATE))
                .verifyComplete();
    }
}
