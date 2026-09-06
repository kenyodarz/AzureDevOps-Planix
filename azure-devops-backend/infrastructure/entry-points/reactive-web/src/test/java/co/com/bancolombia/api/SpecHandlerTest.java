package co.com.bancolombia.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.dashboard.DashboardStreamOrchestrator;
import co.com.bancolombia.api.dashboard.DashboardTaskTracker;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.planning.TriggerProgramPlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import co.com.bancolombia.usecase.spec.GetSpecDocumentUseCase;
import co.com.bancolombia.usecase.spec.ListAvailableSpecsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(classes = {
        RouterRest.class,
        Handler.class,
        TaskHandler.class,
        SpecHandler.class,
        ProgramPlanningHandler.class,
        DashboardStreamOrchestrator.class,
        DashboardTaskTracker.class
})
class SpecHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GetSpecDocumentUseCase getSpecDocumentUseCase;

    @MockitoBean
    private ListAvailableSpecsUseCase listAvailableSpecsUseCase;

    // Dependencias necesarias para levantar RouterRest completo
    @MockitoBean
    private TriggerProgramPlanningUseCase triggerProgramPlanningUseCase;
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

    @Test
    @DisplayName("GIVEN specs disponibles WHEN GET /api/planning/specs THEN 200 OK con lista estructurada")
    void givenAvailableSpecs_whenListSpecs_then200OkWithList() {
        // Arrange
        when(listAvailableSpecsUseCase.execute())
                .thenReturn(Flux.just("ideas_planning_q3.md", "frente_autonomia.md"));

        // Act & Assert
        webTestClient.get()
                .uri("/api/planning/specs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.count").isEqualTo(2)
                .jsonPath("$.specs[0]").isEqualTo("ideas_planning_q3.md")
                .jsonPath("$.specs[1]").isEqualTo("frente_autonomia.md");

        verify(listAvailableSpecsUseCase).execute();
    }

    @Test
    @DisplayName("GIVEN sin specs disponibles WHEN GET /api/planning/specs THEN 200 OK con lista vacía")
    void givenNoSpecs_whenListSpecs_then200OkWithEmptyList() {
        // Arrange
        when(listAvailableSpecsUseCase.execute()).thenReturn(Flux.empty());

        // Act & Assert
        webTestClient.get()
                .uri("/api/planning/specs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(0)
                .jsonPath("$.specs").isArray();
    }

    @Test
    @DisplayName("GIVEN error al listar specs WHEN GET /api/planning/specs THEN 500 Internal Server Error")
    void givenErrorListingSpecs_whenListSpecs_then500InternalServerError() {
        // Arrange
        when(listAvailableSpecsUseCase.execute())
                .thenReturn(Flux.error(new IllegalStateException("Error I/O en disco")));

        // Act & Assert
        webTestClient.get()
                .uri("/api/planning/specs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Error I/O en disco");
    }

    @Test
    @DisplayName("GIVEN spec existente WHEN GET /api/planning/specs/{specName} THEN 200 OK con contenido")
    void givenExistingSpec_whenGetSpec_then200OkWithContent() {
        // Arrange
        String specName = "ideas_planning_q3.md";
        SpecDocument doc = new SpecDocument(specName, "# Roadmap Q3\nObjetivos",
                "docs/specs/" + specName);
        when(getSpecDocumentUseCase.execute(specName)).thenReturn(Mono.just(doc));

        // Act & Assert
        webTestClient.get()
                .uri("/api/planning/specs/{specName}", specName)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.name").isEqualTo(specName)
                .jsonPath("$.content").isEqualTo("# Roadmap Q3\nObjetivos")
                .jsonPath("$.path").isEqualTo("docs/specs/ideas_planning_q3.md");

        verify(getSpecDocumentUseCase).execute(specName);
    }

    @Test
    @DisplayName("GIVEN spec inexistente WHEN GET /api/planning/specs/{specName} THEN 404 Not Found")
    void givenNonExistingSpec_whenGetSpec_then404NotFound() {
        // Arrange
        String specName = "spec_inexistente.md";
        when(getSpecDocumentUseCase.execute(specName))
                .thenReturn(Mono.error(new SpecNotFoundException(specName)));

        // Act & Assert
        webTestClient.get()
                .uri("/api/planning/specs/{specName}", specName)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error")
                .isEqualTo("No se encontró el documento de especificación: " + specName);
    }

    @Test
    @DisplayName("GIVEN parámetro inválido WHEN GET /api/planning/specs/{specName} THEN 400 Bad Request")
    void givenInvalidSpecName_whenGetSpec_then400BadRequest() {
        // Arrange
        String specName = "   ";
        when(getSpecDocumentUseCase.execute(anyString()))
                .thenReturn(Mono.error(
                        new IllegalArgumentException("El nombre no puede estar en blanco")));

        // Act & Assert
        webTestClient.get()
                .uri("/api/planning/specs/{specName}", specName)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
    }
}
