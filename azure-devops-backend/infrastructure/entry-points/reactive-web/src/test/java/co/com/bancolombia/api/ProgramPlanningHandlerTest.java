package co.com.bancolombia.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.dashboard.DashboardStreamOrchestrator;
import co.com.bancolombia.api.dashboard.DashboardTaskTracker;
import co.com.bancolombia.api.dto.ProgramPlanRequestDTO;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.exceptions.AgentExecutionException;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.planning.TriggerProgramPlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import co.com.bancolombia.usecase.spec.GetSpecDocumentUseCase;
import co.com.bancolombia.usecase.spec.ListAvailableSpecsUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(classes = {
        RouterRest.class,
        Handler.class,
        TaskHandler.class,
        SpecHandler.class,
        ProgramPlanningHandler.class,
        DashboardStreamOrchestrator.class,
        DashboardTaskTracker.class,
        co.com.bancolombia.api.task.TaskStreamOrchestrator.class
})
class ProgramPlanningHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TriggerProgramPlanningUseCase triggerProgramPlanningUseCase;

    // Dependencias necesarias para levantar RouterRest completo
    @MockitoBean
    private GetSpecDocumentUseCase getSpecDocumentUseCase;
    @MockitoBean
    private ListAvailableSpecsUseCase listAvailableSpecsUseCase;
    @MockitoBean
    private TrackAgentTaskUseCase trackAgentTaskUseCase;
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

    private Task sampleTask(String id, String contextId) {
        return Task.builder()
                .id(id)
                .contextId(contextId)
                .status(TaskStatus.builder()
                        .state(TaskState.WORKING)
                        .timestamp("2026-09-06T19:00:00Z")
                        .message(Message.builder()
                                .role("agent")
                                .parts(List.of(Part.ofText("Planeación en proceso")))
                                .build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("GIVEN payload válido WHEN POST /api/planning/program THEN 202 Accepted con tarea asíncrona")
    void givenValidPayload_whenTriggerPlanning_then202AcceptedWithTask() {
        // Arrange
        ProgramPlanRequestDTO requestDto = new ProgramPlanRequestDTO(
                "Q3-2026",
                "Entregar arquitectura Clean y reactiva",
                List.of("Autonomía", "Canales"),
                6,
                40,
                "ctx-planning-1"
        );

        Task task = sampleTask("task-prog-123", "ctx-planning-1");
        AgentInteraction interaction = new AgentInteraction(task, "Tarea de planeación iniciada");

        when(triggerProgramPlanningUseCase.execute(any(), anyString()))
                .thenReturn(Mono.just(interaction));

        // Act & Assert
        webTestClient.post()
                .uri("/api/planning/program")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.quarter").isEqualTo("Q3-2026")
                .jsonPath("$.message").isEqualTo("Tarea de planeación iniciada")
                .jsonPath("$.contextId").isEqualTo("ctx-planning-1")
                .jsonPath("$.task.id").isEqualTo("task-prog-123")
                .jsonPath("$.task.origin").isEqualTo("AGENT")
                .jsonPath("$.task.status.state").isEqualTo("working");

        verify(triggerProgramPlanningUseCase).execute(any(), anyString());
    }

    @Test
    @DisplayName("GIVEN interacción sin tarea WHEN POST /api/planning/program THEN 202 Accepted con task nulo")
    void givenReplyOnlyInteraction_whenTriggerPlanning_then202AcceptedWithNullTask() {
        // Arrange
        ProgramPlanRequestDTO requestDto = new ProgramPlanRequestDTO(
                "Q3-2026",
                "Objetivo general",
                List.of(),
                4,
                30,
                null
        );

        AgentInteraction interaction = AgentInteraction.replyOnly("Respuesta directa sin tarea");
        when(triggerProgramPlanningUseCase.execute(any(), any()))
                .thenReturn(Mono.just(interaction));

        // Act & Assert
        webTestClient.post()
                .uri("/api/planning/program")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.quarter").isEqualTo("Q3-2026")
                .jsonPath("$.message").isEqualTo("Respuesta directa sin tarea")
                .jsonPath("$.task").doesNotExist();
    }

    @Test
    @DisplayName("GIVEN fallo de validación en caso de uso WHEN POST /api/planning/program THEN 400 Bad Request")
    void givenValidationFailure_whenTriggerPlanning_then400BadRequest() {
        // Arrange
        ProgramPlanRequestDTO requestDto = new ProgramPlanRequestDTO(
                "",
                "Objetivo",
                List.of(),
                -1,
                0,
                "ctx-1"
        );

        when(triggerProgramPlanningUseCase.execute(any(), anyString()))
                .thenReturn(Mono.error(
                        new IllegalArgumentException("El trimestre (quarter) es obligatorio")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/planning/program")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El trimestre (quarter) es obligatorio");
    }

    @Test
    @DisplayName("GIVEN agente no disponible WHEN POST /api/planning/program THEN 503 Service Unavailable")
    void givenAgentUnavailable_whenTriggerPlanning_then503ServiceUnavailable() {
        // Arrange
        ProgramPlanRequestDTO requestDto = new ProgramPlanRequestDTO(
                "Q3-2026",
                "Objetivo",
                List.of(),
                5,
                35,
                "ctx-1"
        );

        when(triggerProgramPlanningUseCase.execute(any(), anyString()))
                .thenReturn(Mono.error(
                        new AgentUnavailableException("Agente caído", new RuntimeException())));

        // Act & Assert
        webTestClient.post()
                .uri("/api/planning/program")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error")
                .isEqualTo("No fue posible contactar con el agente: Agente caído");
    }

    @Test
    @DisplayName("GIVEN agente falla ejecución WHEN POST /api/planning/program THEN 502 Bad Gateway")
    void givenAgentExecutionError_whenTriggerPlanning_then502BadGateway() {
        // Arrange
        ProgramPlanRequestDTO requestDto = new ProgramPlanRequestDTO(
                "Q3-2026",
                "Objetivo",
                List.of(),
                5,
                35,
                "ctx-1"
        );

        when(triggerProgramPlanningUseCase.execute(any(), anyString()))
                .thenReturn(
                        Mono.error(new AgentExecutionException("Respuesta errónea del agente")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/planning/program")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.error")
                .isEqualTo("El agente no pudo completar la tarea: Respuesta errónea del agente");
    }

    @Test
    @DisplayName("GIVEN cuerpo vacío WHEN POST /api/planning/program THEN 400 Bad Request")
    void givenEmptyBody_whenTriggerPlanning_then400BadRequest() {
        webTestClient.post()
                .uri("/api/planning/program")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
    }
}
