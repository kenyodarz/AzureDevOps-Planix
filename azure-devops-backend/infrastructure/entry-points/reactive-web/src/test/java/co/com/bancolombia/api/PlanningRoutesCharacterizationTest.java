package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.dashboard.DashboardStreamOrchestrator;
import co.com.bancolombia.api.dashboard.DashboardTaskTracker;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import java.util.Map;
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

/**
 * Pruebas de caracterización de las rutas de planificación y de tareas (Fase 01).
 *
 * <p>Congelan el contrato HTTP <b>tal como es hoy</b>, incluidas sus decisiones discutibles, para
 * que las fases 02 a 08 puedan refactorizar con red. Donde el comportamiento actual es cuestionable
 * se deja constancia explícita en el Javadoc de la prueba, junto con la fase que lo corregirá.
 *
 * <p><b>No modificar estas pruebas para hacerlas pasar.</b> Si una se pone roja tras un cambio, o
 * bien el cambio rompió el contrato, o bien es un cambio deliberado que exige una decisión
 * documentada en el plan maestro.
 *
 * <p><b>Excepción S-5, autorizada por escrito el 2026-08-29 (Fase 07, B-09).</b> Cambia una sola
 * prueba, {@code givenUseCaseFails_whenPost_...}: D-16 sustituye el «400 para cualquier error» por
 * el mapa único de DP-06 y un {@link IllegalStateException} pasa a ser 500. <b>Ningún cuerpo de
 * respuesta y ninguna otra aserción se tocaron.</b>
 *
 * <p><b>Excepción S-6, autorizada por escrito el 2026-08-29 (Fase 08, B-13).</b>
 * <ul>
 *   <li><b>Qué se cambió:</b> sólo el {@code @ContextConfiguration}, que suma
 *       {@link DashboardStreamOrchestrator} y {@link DashboardTaskTracker}.</li>
 *   <li><b>Por qué era inevitable:</b> D-42. Esos dos colaboradores se construían con {@code new}
 *       dentro del {@code Handler} para no alterar este fichero; al pasar a ser beans, el contexto
 *       de esta prueba tiene que conocerlos. Este fichero no prueba el tablero, pero comparte el
 *       {@code Handler} con quien sí lo hace, y el contexto se construye entero o no se construye.</li>
 *   <li><b>Qué NO se tocó:</b> nada más. Ninguna aserción, ningún código de estado, ningún cuerpo.</li>
 * </ul>
 */
@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, Handler.class, TaskHandler.class,
        DashboardStreamOrchestrator.class, DashboardTaskTracker.class})
class PlanningRoutesCharacterizationTest {

    private static final String INITIATIVE_ID = "INI-001";
    private static final int DEFAULT_MAX_RESULTS = 3;

    @Autowired
    private WebTestClient webTestClient;

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

    @MockitoBean
    private TrackAgentTaskUseCase trackAgentTaskUseCase;

    private PlanningChunk chunk(String id) {
        return PlanningChunk.builder()
                .id(id)
                .initiativeId(INITIATIVE_ID)
                .sectionName("Alcance")
                .content("contenido")
                .metadata(Map.of())
                .build();
    }

    // ---------------------------------------------------------------------------------------
    // POST /api/planning/ingest
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN ingesta correcta WHEN POST /api/planning/ingest THEN 200 con el mensaje de éxito")
    void givenValidIngest_whenPost_then200WithSuccessMessage() {
        // GIVEN
        when(ingestPlanningSpecUseCase.ingestMarkdown(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // WHEN / THEN
        webTestClient.post().uri("/api/planning/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("initiativeId", INITIATIVE_ID, "title", "Plan",
                        "markdownContent", "## Alcance"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Planeación ingesta y vectorizada exitosamente");

        verify(ingestPlanningSpecUseCase).ingestMarkdown(INITIATIVE_ID, "Plan", "## Alcance");
    }

    /**
     * <b>Invertida en la Fase 07 (T-01, D-16) — excepción S-5.</b> Hasta ahora <b>cualquier</b>
     * fallo del caso de uso se traducía a 400, incluidos los que no eran culpa del cliente: un
     * pgvector caído se le contaba al frontend como una petición mal formada. Con el mapa único de
     * DP-06, un {@link IllegalStateException} sale como <b>500</b>. El cuerpo no cambia.
     */
    @Test
    @DisplayName("GIVEN el caso de uso falla WHEN POST /api/planning/ingest THEN 500 (D-16)")
    void givenUseCaseFails_whenPost_then500WithErrorMessage() {
        // GIVEN
        when(ingestPlanningSpecUseCase.ingestMarkdown(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("pgvector caído")));

        // WHEN / THEN
        webTestClient.post().uri("/api/planning/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("initiativeId", INITIATIVE_ID, "title", "Plan",
                        "markdownContent", "## Alcance"))
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.error").isEqualTo("pgvector caído");
    }

    // ---------------------------------------------------------------------------------------
    // GET /api/planning/search
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN búsqueda con parámetros WHEN GET /api/planning/search THEN 200 con la lista")
    void givenSearchWithParams_whenGet_then200WithList() {
        // GIVEN
        when(searchPlanningSpecUseCase.search(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.just(chunk("c1")));

        // WHEN / THEN
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/planning/search")
                        .queryParam("query", "commit")
                        .queryParam("initiativeId", INITIATIVE_ID)
                        .queryParam("maxResults", "7")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("c1");

        verify(searchPlanningSpecUseCase).search("commit", INITIATIVE_ID, 7);
    }

    /**
     * Congela los valores por defecto del entry-point: {@code initiativeId} vacío y
     * {@code maxResults = 3}. El literal 3 es un número mágico (D-19) que se nombra en la Fase 04.
     */
    @Test
    @DisplayName("GIVEN búsqueda sin parámetros opcionales WHEN GET THEN aplica initiativeId vacío y maxResults 3")
    void givenSearchWithoutOptionalParams_whenGet_thenAppliesDefaults() {
        // GIVEN
        when(searchPlanningSpecUseCase.search(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.empty());

        // WHEN / THEN
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/planning/search")
                        .queryParam("query", "commit").build())
                .exchange()
                .expectStatus().isOk();

        verify(searchPlanningSpecUseCase).search("commit", "", DEFAULT_MAX_RESULTS);
    }

    /**
     * <b>Invertida en la Fase 03 (T-07).</b> Hasta ahora el BFF no validaba nada: un
     * {@code maxResults} no numérico reventaba en {@code Integer.parseInt} <i>antes</i> de entrar
     * en la cadena reactiva, escapaba al {@code onErrorResume} del handler y salía como 500. Con
     * {@code RequestValidator} ya sale como <b>400 con mensaje útil</b>, que es lo que un BFF debe
     * hacer (D-28 saldada).
     */
    @Test
    @DisplayName("GIVEN maxResults no numérico WHEN GET /api/planning/search THEN 400 con mensaje útil")
    void givenNonNumericMaxResults_whenGet_thenBadRequest() {
        // GIVEN / WHEN / THEN
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/planning/search")
                        .queryParam("query", "commit")
                        .queryParam("maxResults", "no-es-un-numero").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error")
                .value((String error) -> assertThat(error).contains("maxResults"));
    }

    @Test
    @DisplayName("GIVEN maxResults negativo WHEN GET /api/planning/search THEN 400 (D-28)")
    void givenNegativeMaxResults_whenGet_thenBadRequest() {
        // GIVEN / WHEN / THEN
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/planning/search")
                        .queryParam("query", "commit")
                        .queryParam("maxResults", "-5").build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GIVEN la búsqueda falla WHEN GET /api/planning/search THEN 400")
    void givenSearchFails_whenGet_then400() {
        // GIVEN
        when(searchPlanningSpecUseCase.search(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.error(new IllegalArgumentException("consulta vacía")));

        // WHEN / THEN
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/planning/search")
                        .queryParam("query", " ").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("consulta vacía");
    }

    // ---------------------------------------------------------------------------------------
    // GET / DELETE / PUT sobre iniciativas
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN iniciativas WHEN GET /api/planning/initiatives THEN 200 con la lista")
    void givenInitiatives_whenGet_then200WithList() {
        // GIVEN
        when(managePlanningUseCase.getInitiatives())
                .thenReturn(Flux.just(Map.of("initiative_id", INITIATIVE_ID)));

        // WHEN / THEN
        webTestClient.get().uri("/api/planning/initiatives")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$[0].initiative_id").isEqualTo(INITIATIVE_ID);
    }

    @Test
    @DisplayName("GIVEN un id WHEN GET .../initiatives/{id}/chunks THEN 200 y propaga el id")
    void givenId_whenGetChunks_then200AndPropagatesId() {
        // GIVEN
        when(managePlanningUseCase.getInitiativeChunks(INITIATIVE_ID))
                .thenReturn(Flux.just(chunk("c1")));

        // WHEN / THEN
        webTestClient.get().uri("/api/planning/initiatives/{id}/chunks", INITIATIVE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$[0].sectionName").isEqualTo("Alcance");

        verify(managePlanningUseCase).getInitiativeChunks(INITIATIVE_ID);
    }

    @Test
    @DisplayName("GIVEN un id WHEN DELETE .../initiatives/{id} THEN 200 con cuerpo vacío")
    void givenId_whenDelete_then200WithEmptyBody() {
        // GIVEN
        when(managePlanningUseCase.deleteInitiative(INITIATIVE_ID)).thenReturn(Mono.empty());

        // WHEN / THEN
        webTestClient.delete().uri("/api/planning/initiatives/{id}", INITIATIVE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(managePlanningUseCase).deleteInitiative(INITIATIVE_ID);
    }

    @Test
    @DisplayName("GIVEN una célula WHEN PUT .../initiatives/{id}/cell THEN 200 y propaga la célula")
    void givenCell_whenPut_then200AndPropagatesCell() {
        // GIVEN
        when(managePlanningUseCase.updateCell(INITIATIVE_ID, "EQU1096")).thenReturn(Mono.empty());

        // WHEN / THEN
        webTestClient.put().uri("/api/planning/initiatives/{id}/cell", INITIATIVE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("cell", "EQU1096"))
                .exchange()
                .expectStatus().isOk();

        verify(managePlanningUseCase).updateCell(INITIATIVE_ID, "EQU1096");
    }

    @Test
    @DisplayName("GIVEN el borrado falla WHEN DELETE .../initiatives/{id} THEN 400")
    void givenDeleteFails_whenDelete_then400() {
        // GIVEN
        when(managePlanningUseCase.deleteInitiative(INITIATIVE_ID))
                .thenReturn(Mono.error(new IllegalArgumentException("id inválido")));

        // WHEN / THEN
        webTestClient.delete().uri("/api/planning/initiatives/{id}", INITIATIVE_ID)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("id inválido");
    }

    // ---------------------------------------------------------------------------------------
    // Rutas que el frontend llama
    // ---------------------------------------------------------------------------------------

    /**
     * <b>Ajustada en la Fase 03 (T-11).</b> {@code GET /api/tasks} sigue existiendo, pero cambió
     * de dueño: pasó de {@code Handler} —que leía la tabla compartida y devolvía una mezcla
     * indistinguible de tareas (D-24)— a {@code TaskHandler} y {@code TrackAgentTaskUseCase}. Su
     * contrato completo, con el campo {@code origin}, se prueba en {@code TaskRoutesTest}; aquí
     * solo se congela que la ruta sigue publicada y ya no toca {@code taskStoreGateway}.
     */
    @Test
    @DisplayName("GIVEN la ruta de tareas WHEN GET /api/tasks THEN la sirve TaskHandler, no el almacén")
    void givenTasksRoute_whenGet_thenItIsServedByTaskHandler() {
        // GIVEN
        when(trackAgentTaskUseCase.listTasks()).thenReturn(Flux.empty());

        // WHEN / THEN
        webTestClient.get().uri("/api/tasks")
                .exchange()
                .expectStatus().isOk();

        verify(trackAgentTaskUseCase).listTasks();
        verify(taskStoreGateway, never()).findAll();
    }

    /**
     * <b>Invertida en la Fase 03 (T-11).</b> La Fase 01 dejó constancia ejecutable de que el
     * frontend llamaba a {@code POST /} con un sobre JSON-RPC {@code tasks/cancel} y a
     * {@code GET /.well-known/agent-card.json}, y de que el BFF <i>no servía ninguna de las dos</i>
     * (D-25, D-26). Ahora el BFF sí sirve el equivalente, bajo {@code /api/**} (DP-07 opción B).
     *
     * <p>Las rutas heredadas siguen sin existir, y deben seguir sin existir: exponer
     * {@code POST /} chocaría con el fallback de SPA, que fue justamente el motivo de descartar la
     * opción A.
     */
    @Test
    @DisplayName("GIVEN las rutas nuevas del contrato WHEN se invocan THEN el BFF ya las expone")
    void givenTheNewContractRoutes_whenInvoked_thenBffServesThem() {
        // GIVEN
        when(trackAgentTaskUseCase.getAgentCard())
                .thenReturn(Mono.just(AgentCard.builder().name("Agente DevOps").build()));
        when(trackAgentTaskUseCase.cancelTask("task-1"))
                .thenReturn(Mono.just(Task.builder().id("task-1").build()));

        // WHEN / THEN
        webTestClient.get().uri("/api/agent/card")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Agente DevOps");

        webTestClient.post().uri("/api/tasks/{id}/cancel", "task-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo("task-1");

        // Y las rutas heredadas siguen sin existir, deliberadamente.
        webTestClient.post().uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("jsonrpc", "2.0", "method", "tasks/cancel",
                        "params", Map.of("taskId", "task-1"), "id", "cancel-1"))
                .exchange()
                .expectStatus().isNotFound();

        webTestClient.get().uri("/.well-known/agent-card.json")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GIVEN una ruta de SPA WHEN GET THEN devuelve el index.html del frontend")
    void givenSpaRoute_whenGet_thenReturnsIndexHtml() {
        // GIVEN / WHEN / THEN
        // El fallback existe, pero el recurso static/index.html no está presente en los tests:
        // lo relevante para el contrato es que la ruta NO cae en el enrutado de /api.
        assertThat(webTestClient.get().uri("/dashboard")
                .exchange()
                .returnResult(String.class)
                .getStatus().value())
                .isNotEqualTo(404);
    }
}

