package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Cubre el listado de tareas ({@code GET /api/tasks}), la única ruta del {@link Handler} que
 * ninguna de las cuatro suites del contrato ejercita.
 *
 * <p>Se instancia el handler directamente, sin levantar el contexto web: la ruta no recibe cuerpo
 * ni parámetros, así que no hace falta un {@code ServerRequest}. Las suites del contrato quedan
 * intactas, tal y como exige el plan.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Handler - Listado de tareas")
class HandlerTaskListingTest {

    @Mock
    private AgentChatUseCase agentChatUseCase;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    private Handler handler;

    private static Task taskWith(String id, TaskState state) {
        return Task.builder()
                .id(id)
                .status(TaskStatus.builder().state(state).build())
                .build();
    }

    @BeforeEach
    void setUp() {
        handler = new Handler(agentChatUseCase, taskStoreGateway, "2026-06-30", "/");
    }

    @Test
    @DisplayName("GIVEN tareas almacenadas WHEN se listan THEN responde 200 con JSON")
    void givenStoredTasks_whenListTasks_thenRespondsOkWithJson() {
        when(taskStoreGateway.findAll()).thenReturn(Flux.just(
                taskWith("task-1", TaskState.COMPLETED),
                taskWith("task-2", TaskState.WORKING)));

        StepVerifier.create(handler.handleListTasks())
                .assertNext(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.headers().getContentType())
                            .isEqualTo(MediaType.APPLICATION_JSON);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN el almacén vacío WHEN se listan THEN responde 200 igualmente")
    void givenEmptyStore_whenListTasks_thenRespondsOk() {
        when(taskStoreGateway.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(handler.handleListTasks())
                .assertNext(response ->
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un fallo del almacén WHEN se listan THEN responde 400 en lugar de propagar")
    void givenStoreFailure_whenListTasks_thenRespondsBadRequest() {
        when(taskStoreGateway.findAll())
                .thenReturn(Flux.error(new IllegalStateException("almacén caído")));

        StepVerifier.create(handler.handleListTasks())
                .assertNext(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.headers().getContentType())
                            .isEqualTo(MediaType.APPLICATION_JSON);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN la tarjeta del agente WHEN se solicita THEN responde 200 con JSON")
    void givenAgentCard_whenRequested_thenRespondsOkWithJson() {
        StepVerifier.create(handler.handleAgentCard())
                .assertNext(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.headers().getContentType())
                            .isEqualTo(MediaType.APPLICATION_JSON);
                })
                .verifyComplete();
    }
}

