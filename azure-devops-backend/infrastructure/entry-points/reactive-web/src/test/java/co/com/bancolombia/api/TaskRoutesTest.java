package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.dashboard.DashboardStreamOrchestrator;
import co.com.bancolombia.api.dashboard.DashboardTaskTracker;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.TrackedTask;
import co.com.bancolombia.model.agent.exceptions.AgentExecutionException;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.agent.exceptions.InvalidAgentRequestException;
import co.com.bancolombia.model.agent.exceptions.TaskNotFoundException;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contrato HTTP de las cinco rutas nuevas del BFF hacia el frontend (Fase 03, DP-07 opción B).
 *
 * <p>Cubre el caso feliz y el de error de cada ruta, incluido el mapa de excepción de dominio a
 * código HTTP de T-08, que estas rutas estrenan mientras el resto del BFF sigue con el genérico
 * «cualquier error es 400» (D-16, pendiente de DP-06).
 */
@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, Handler.class, TaskHandler.class,
        DashboardStreamOrchestrator.class, DashboardTaskTracker.class})
class TaskRoutesTest {

    private static final String TASK_ID = "task-42";
    private static final String CONTEXT_ID = "ctx-1";
    private static final String PROMPT = "Analiza el sprint 247";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TrackAgentTaskUseCase trackAgentTaskUseCase;

    // Dependencias de Handler: necesarias solo para levantar el RouterRest completo.
    @MockitoBean
    private IngestPlanningSpecUseCase ingestPlanningSpecUseCase;
    @MockitoBean
    private SearchPlanningSpecUseCase searchPlanningSpecUseCase;
    @MockitoBean
    private ManagePlanningUseCase managePlanningUseCase;
    @MockitoBean
    private DevOpsDashboardUseCase devOpsDashboardUseCase;
    @MockitoBean
    private TaskStoreGateway taskStoreGateway;

    private Task task(String id, TaskState state, String text) {
        return Task.builder()
                .id(id)
                .contextId(CONTEXT_ID)
                .status(TaskStatus.builder()
                        .state(state)
                        .timestamp("2026-08-29T00:00:00Z")
                        .message(Message.builder()
                                .role("agent")
                                .parts(List.of(Part.ofText(text)))
                                .build())
                        .build())
                .build();
    }

    private Map<String, Object> chatBody(String contextId) {
        Map<String, Object> message = new LinkedHashMap<>();
        if (contextId != null) {
            message.put("contextId", contextId);
        }
        message.put("parts", List.of(Map.of("text", PROMPT)));
        return Map.of("message", message);
    }

    private AgentCommand captureCommand() {
        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(trackAgentTaskUseCase).sendAndTrack(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------------------------------
    // POST /api/chat/messages — el chat deja de saltarse el BFF (D-21)
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN un mensaje del usuario WHEN POST /api/chat/messages THEN 200 con reply y tarea")
    void givenUserMessage_whenPost_then200WithReplyAndTask() {
        // GIVEN
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.just(new AgentInteraction(
                        task(TASK_ID, TaskState.WORKING, "En marcha"), "Recibido")));

        // WHEN / THEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(CONTEXT_ID))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reply").isEqualTo("Recibido")
                .jsonPath("$.task.id").isEqualTo(TASK_ID)
                .jsonPath("$.task.origin").isEqualTo("AGENT")
                .jsonPath("$.task.status.state").isEqualTo("working")
                .jsonPath("$.task.status.message").isEqualTo("En marcha");
    }

    /**
     * D-30: el contextId recibido se propaga tal cual hacia el agente.
     */
    @Test
    @DisplayName("GIVEN un contextId WHEN POST /api/chat/messages THEN se propaga al agente")
    void givenContextId_whenPost_thenItIsPropagated() {
        // GIVEN
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.just(AgentInteraction.replyOnly("Hola")));

        // WHEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(CONTEXT_ID))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.reply").isEqualTo("Hola");

        // THEN
        AgentCommand command = captureCommand();
        assertThat(command.contextId()).isEqualTo(CONTEXT_ID);
        assertThat(command.prompt()).isEqualTo(PROMPT);
        assertThat(command.blocking())
                .as("DP-08: el BFF no espera a que el agente termine")
                .isFalse();
    }

    /**
     * D-30: si el frontend no envía contextId, el BFF genera uno para que lo reutilice.
     */
    @Test
    @DisplayName("GIVEN sin contextId WHEN POST /api/chat/messages THEN el BFF genera uno")
    void givenNoContextId_whenPost_thenOneIsGenerated() {
        // GIVEN
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.just(AgentInteraction.replyOnly("Hola")));

        // WHEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(null))
                .exchange()
                .expectStatus().isOk();

        // THEN
        assertThat(captureCommand().contextId()).isNotBlank();
    }

    @Test
    @DisplayName("GIVEN un cuerpo sin parts WHEN POST /api/chat/messages THEN 400 (D-28)")
    void givenBodyWithoutParts_whenPost_thenBadRequest() {
        // GIVEN / WHEN / THEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", Map.of("contextId", CONTEXT_ID)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error")
                .isEqualTo("El campo message.parts es obligatorio");
    }

    @Test
    @DisplayName("GIVEN un parts sin texto WHEN POST /api/chat/messages THEN 400 (D-28)")
    void givenPartsWithoutText_whenPost_thenBadRequest() {
        // GIVEN / WHEN / THEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", Map.of("parts", List.of(Map.of("text", "   ")))))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------------------------------------------------------------------------------------
    // GET /api/tasks — unión de fuentes con origen (DP-07-bis)
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN tareas de ambos orígenes WHEN GET /api/tasks THEN 200 con la lista etiquetada")
    void givenTasksFromBothOrigins_whenGet_then200WithOriginLabel() {
        // GIVEN
        when(trackAgentTaskUseCase.listTasks()).thenReturn(Flux.just(
                TrackedTask.fromAgent(task(TASK_ID, TaskState.WORKING, "En marcha")),
                TrackedTask.fromBff(task("dashboard-1", TaskState.COMPLETED, "Listo"))));

        // WHEN / THEN
        webTestClient.get().uri("/api/tasks")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(TASK_ID)
                .jsonPath("$[0].origin").isEqualTo("AGENT")
                .jsonPath("$[0].status.state").isEqualTo("working")
                .jsonPath("$[1].id").isEqualTo("dashboard-1")
                .jsonPath("$[1].origin").isEqualTo("BFF")
                .jsonPath("$[1].status.state").isEqualTo("completed");
    }

    @Test
    @DisplayName("GIVEN el almacén del BFF falla WHEN GET /api/tasks THEN 500 con el mensaje")
    void givenStoreFails_whenGet_thenServerError() {
        // GIVEN
        when(trackAgentTaskUseCase.listTasks())
                .thenReturn(Flux.error(new IllegalStateException("base de datos caída")));

        // WHEN / THEN
        webTestClient.get().uri("/api/tasks")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.error").isEqualTo("base de datos caída");
    }

    // ---------------------------------------------------------------------------------------
    // GET /api/tasks/{id} y POST /api/tasks/{id}/cancel
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN una tarea existente WHEN GET /api/tasks/{id} THEN 200 con su estado")
    void givenExistingTask_whenGet_then200() {
        // GIVEN
        when(trackAgentTaskUseCase.refreshTask(TASK_ID))
                .thenReturn(Mono.just(task(TASK_ID, TaskState.INPUT_REQUIRED, "Falta un dato")));

        // WHEN / THEN
        webTestClient.get().uri("/api/tasks/{id}", TASK_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(TASK_ID)
                .jsonPath("$.contextId").isEqualTo(CONTEXT_ID)
                .jsonPath("$.status.state").isEqualTo("input-required")
                .jsonPath("$.status.timestamp").isEqualTo("2026-08-29T00:00:00Z");
    }

    @Test
    @DisplayName("GIVEN una tarea en curso WHEN POST /api/tasks/{id}/cancel THEN 200 cancelada (D-25)")
    void givenRunningTask_whenCancel_then200Canceled() {
        // GIVEN
        when(trackAgentTaskUseCase.cancelTask(TASK_ID))
                .thenReturn(Mono.just(task(TASK_ID, TaskState.CANCELED, "Cancelada")));

        // WHEN / THEN
        webTestClient.post().uri("/api/tasks/{id}/cancel", TASK_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status.state").isEqualTo("canceled");
    }

    // ---------------------------------------------------------------------------------------
    // GET /api/agent/card (D-26)
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN el agente publica su tarjeta WHEN GET /api/agent/card THEN 200")
    void givenAgentCard_whenGet_then200() {
        // GIVEN
        when(trackAgentTaskUseCase.getAgentCard()).thenReturn(Mono.just(AgentCard.builder()
                .protocolVersion("1.0")
                .name("Agente DevOps")
                .version("2.3.1")
                .build()));

        // WHEN / THEN
        webTestClient.get().uri("/api/agent/card")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Agente DevOps")
                .jsonPath("$.protocolVersion").isEqualTo("1.0");
    }

    // ---------------------------------------------------------------------------------------
    // Mapa de excepción de dominio a código HTTP (T-08)
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN el agente no conoce la tarea WHEN GET /api/tasks/{id} THEN 404")
    void givenUnknownTask_whenGet_thenNotFound() {
        // GIVEN
        when(trackAgentTaskUseCase.refreshTask(anyString()))
                .thenReturn(Mono.error(new TaskNotFoundException(TASK_ID)));

        // WHEN / THEN
        webTestClient.get().uri("/api/tasks/{id}", TASK_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GIVEN el agente rechaza la petición WHEN POST /api/chat/messages THEN 400")
    void givenRejectedRequest_whenPost_thenBadRequest() {
        // GIVEN
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.error(new InvalidAgentRequestException("payload mal formado")));

        // WHEN / THEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(CONTEXT_ID))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GIVEN el agente no está disponible WHEN POST /api/chat/messages THEN 503")
    void givenAgentUnavailable_whenPost_thenServiceUnavailable() {
        // GIVEN
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.error(new AgentUnavailableException("agente caído", null)));

        // WHEN / THEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(CONTEXT_ID))
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    @DisplayName("GIVEN el agente falla ejecutando WHEN POST /api/chat/messages THEN 502")
    void givenAgentExecutionFails_whenPost_thenBadGateway() {
        // GIVEN
        when(trackAgentTaskUseCase.sendAndTrack(any(AgentCommand.class)))
                .thenReturn(Mono.error(new AgentExecutionException("el LLM falló")));

        // WHEN / THEN
        webTestClient.post().uri("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(CONTEXT_ID))
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    @DisplayName("GIVEN el caso de uso rechaza el identificador WHEN POST .../cancel THEN 400")
    void givenInvalidTaskId_whenCancel_thenBadRequest() {
        // GIVEN
        when(trackAgentTaskUseCase.cancelTask(anyString()))
                .thenReturn(Mono.error(new IllegalArgumentException(
                        "El identificador de la tarea es obligatorio")));

        // WHEN / THEN
        webTestClient.post().uri("/api/tasks/{id}/cancel", "desconocida")
                .exchange()
                .expectStatus().isBadRequest();
    }
}

