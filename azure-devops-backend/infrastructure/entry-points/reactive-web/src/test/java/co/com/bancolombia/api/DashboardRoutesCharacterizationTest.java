package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.dashboard.DashboardStreamOrchestrator;
import co.com.bancolombia.api.dashboard.DashboardTaskTracker;
import co.com.bancolombia.api.dto.dashboard.DashboardResponse;
import co.com.bancolombia.api.dto.event.DashboardUpdateEvent;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.dashboard.BacklogAudit;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.planning.TriggerProgramPlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import co.com.bancolombia.usecase.spec.GetSpecDocumentUseCase;
import co.com.bancolombia.usecase.spec.ListAvailableSpecsUseCase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
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
import reactor.test.StepVerifier;

/**
 * Pruebas de caracterización del flujo de auditoría del backlog (Fase 01).
 *
 * <p>Éste es el flujo más frágil del BFF y el que más se moverá en las fases 05 y 06: hoy el
 * entry-point parte los ítems en lotes, parsea la respuesta del agente, muta los modelos in-place
 * dentro de la cadena reactiva y recalcula métricas de negocio. Estas pruebas son la <b>definición
 * operativa</b> de lo que esas fases no pueden romper.
 *
 * <p><b>No modificar estas pruebas para hacerlas pasar.</b>
 *
 * <p><b>Excepción S-2, autorizada por escrito el 2026-08-29 (Fase 05).</b> Se cambiaron
 * exclusivamente <b>dos líneas de tipos</b>: el {@code import} de {@code DashboardResponse}, que
 * pasó de {@code model.dashboard} a {@code api.dto.dashboard}, y el {@code any(...)} de
 * {@code saveDashboardReport}, que ahora recibe {@code BacklogAudit}. <b>Ninguna aserción, ningún
 * dato de entrada y ningún comportamiento esperado se tocaron.</b> El cambio era inevitable: estas
 * pruebas anclaban el nombre y el paquete de los tipos de dominio, y sin liberarlos D-13 no podía
 * cerrarse en ninguna fase, ni ahora ni después.
 *
 * <p><b>Excepción S-3, autorizada por escrito el 2026-08-29 (Fase 06).</b> Una línea de
 * <i>stub</i> en {@code setUp}: {@code saveDashboardReport} devuelve {@code Mono<Void>} desde que
 * D-10 sacó la escritura del caso de uso, y Mockito devuelve {@code null} para los métodos que
 * retornan {@code Mono}. Sin el stub el stream muere con un NPE contra un doble de prueba, no
 * contra el código. <b>Ninguna aserción se tocó.</b>
 *
 * <p><b>Excepción S-4, autorizada por escrito el 2026-08-29 (Fase 06).</b> Dos pruebas cambian
 * porque el comportamiento que fijaban <b>deja de existir</b>:
 * <ul>
 *   <li>{@code givenInitialFailsWithMcpEnabled_...} — B-06 sustituye el 500 mudo por un evento SSE
 *       {@code ERROR}. Fijar el 500 era fijar precisamente el fallo que D-31 describe.</li>
 *   <li>{@code givenMcpDisabledAndFailure_...} — B-07 saca el repliegue a mock del entry-point y lo
 *       unifica en el dominio, así que {@code isMcpEnabled()} desaparece de la API pública del caso
 *       de uso y esta prueba ni siquiera compilaba. Se reescribe al contrato nuevo: el repliegue se
 *       observa donde ahora ocurre.</li>
 * </ul>
 * Las diez restantes siguen intactas, y son las que garantizan que no se rompió nada.
 *
 * <p><b>Excepción S-5, autorizada por escrito el 2026-08-29 (Fase 07, B-09).</b> Una prueba
 * cambia, {@code givenUseCaseFails_whenGetDashboard_...}: D-16 sustituye el «400 para cualquier
 * error» por el
 * mapa único de DP-06, y un {@link IllegalStateException} pasa a ser 500. <b>Ni el cuerpo de la
 * respuesta ni ninguna otra aserción se tocaron</b>, y las demás pruebas siguen intactas.
 *
 * <p><b>Excepción S-6, autorizada por escrito el 2026-08-29 (Fase 08, B-13).</b>
 * <ul>
 *   <li><b>Qué se cambió:</b> sólo el {@code @ContextConfiguration}. Se le añaden
 *       {@link DashboardStreamOrchestrator} y {@link DashboardTaskTracker}.</li>
 *   <li><b>Por qué era inevitable:</b> D-42. Hasta la Fase 07 el {@code Handler} construía esos dos
 *       colaboradores con {@code new} <b>precisamente para no tocar este fichero</b>. Era un andamio
 *       que se sostenía sobre la prueba, no sobre el diseño: el entry-point declaraba
 *       {@code audit.batch-size} y {@code audit.concurrency}, que no usa, para poder pasárselos al
 *       orquestador. Al convertirlos en beans, Spring deja de encontrarlos si no se declaran aquí.
 *       No hay forma de cerrar D-42 sin tocar esta línea.</li>
 *   <li><b>Qué NO se tocó:</b> ninguna aserción, ningún cuerpo de respuesta, ningún código de
 *       estado, ningún nombre de prueba. El comportamiento observable que este fichero fija sigue
 *       siendo exactamente el mismo, y por eso sigue sirviendo de red.</li>
 * </ul>
 */
@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, Handler.class, TaskHandler.class,
        SpecHandler.class, ProgramPlanningHandler.class,
        DashboardStreamOrchestrator.class, DashboardTaskTracker.class})
class DashboardRoutesCharacterizationTest {

    private static final String CELL = "EQU1096 - EXODIA";
    private static final String SPRINT = "Sprint 247";
    private static final String REQUIRED_PARAMS_MESSAGE =
            "La célula y el sprint son parámetros requeridos";
    private static final int BATCH_SIZE = 10;

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

    /**
     * {@code RouterRest} monta también las rutas de chat y tareas desde la Fase 03; el contexto
     * necesita el bean aunque este test no lo ejercite.
     */
    @MockitoBean
    private TrackAgentTaskUseCase trackAgentTaskUseCase;

    @MockitoBean
    private TaskStoreGateway taskStoreGateway;

    @MockitoBean
    private GetSpecDocumentUseCase getSpecDocumentUseCase;

    @MockitoBean
    private ListAvailableSpecsUseCase listAvailableSpecsUseCase;

    @MockitoBean
    private TriggerProgramPlanningUseCase triggerProgramPlanningUseCase;

    @BeforeEach
    void setUp() {
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
        // Excepción S-3: sin este stub el doble devuelve null y el stream muere con un NPE.
        when(devOpsDashboardUseCase.saveDashboardReport(any(), anyString(), anyString()))
                .thenReturn(Mono.empty());
    }

    /**
     * Construye la respuesta inicial del agente con {@code count} ítems sin auditar.
     */
    private String initialPayloadWith(int count) {
        String items = IntStream.rangeClosed(1, count)
                .mapToObj(index -> """
                        {"id":"%d","title":"Historia %d","points":3,"state":"Active",
                         "assignedMember":"Alguien","hasAcceptanceCriteria":false,"hasDoD":false,
                         "qualityScore":0,"linkedTasksCount":0,"feedback":""}
                        """.formatted(index, index))
                .collect(Collectors.joining(","));
        return """
                {"metrics":{"totalPoints":%d,"completedPoints":0,"completedPercentage":0,
                            "avgQualityScore":0,"undocumentedCount":0},
                 "items":[%s]}
                """.formatted(count * 3, items);
    }

    /**
     * Construye la respuesta de auditoría de un lote de IDs, todos con el mismo score.
     */
    private String auditPayloadFor(String idsCsv, int qualityScore, boolean documented) {
        String updates = java.util.Arrays.stream(idsCsv.split(","))
                .map(id -> """
                        {"id":"%s","hasAcceptanceCriteria":%b,"hasDoD":%b,"qualityScore":%d,
                         "linkedTasksCount":2,"feedback":"revisado"}
                        """.formatted(id, documented, documented, qualityScore))
                .collect(Collectors.joining(","));
        return "{\"updates\":[%s]}".formatted(updates);
    }

    private Flux<DashboardUpdateEvent> streamEvents() {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/devops/dashboard/stream")
                        .queryParam("cell", CELL)
                        .queryParam("sprint", SPRINT)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(DashboardUpdateEvent.class)
                .getResponseBody();
    }

    // ---------------------------------------------------------------------------------------
    // GET /api/devops/dashboard
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN falta la célula o el sprint WHEN GET /api/devops/dashboard THEN 400 con el mensaje exacto")
    void givenMissingParams_whenGetDashboard_then400WithExactMessage() {
        // GIVEN / WHEN / THEN
        webTestClient.get().uri("/api/devops/dashboard?cell=&sprint=" + SPRINT)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo(REQUIRED_PARAMS_MESSAGE);

        webTestClient.get().uri("/api/devops/dashboard?cell=" + CELL + "&sprint=")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo(REQUIRED_PARAMS_MESSAGE);

        verify(devOpsDashboardUseCase, never()).getDashboardData(anyString(), anyString());
    }

    /**
     * El handler devuelve el JSON del caso de uso <b>tal cual</b>, sin reempaquetarlo ni
     * validarlo.
     */
    @Test
    @DisplayName("GIVEN parámetros válidos WHEN GET /api/devops/dashboard THEN 200 con el JSON del caso de uso")
    void givenValidParams_whenGetDashboard_then200WithRawJson() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardData(anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(1)));

        // WHEN / THEN
        webTestClient.get().uri("/api/devops/dashboard?cell=" + CELL + "&sprint=" + SPRINT)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.items[0].id").isEqualTo("1");
    }

    /**
     * <b>Invertida en la Fase 07 (T-01, D-16) — excepción S-5.</b> Hasta ahora <b>cualquier</b>
     * fallo del caso de uso salía como <b>400</b>, es decir «te equivocaste tú», aunque el que se
     * hubiera caído fuera el agente. Con el mapa único de DP-06, un {@link IllegalStateException}
     * —que no es culpa del cliente— sale como <b>500</b>. El cuerpo no cambia: sigue siendo
     * {@code {"error": "<mensaje>"}}.
     *
     * <p>Fijar el 400 era fijar exactamente el fallo que D-16 describe, igual que en S-4 la Fase
     * 06 tuvo que dejar de fijar el 500 mudo del stream.
     */
    @Test
    @DisplayName("GIVEN el caso de uso falla WHEN GET /api/devops/dashboard THEN 500 (D-16)")
    void givenUseCaseFails_whenGetDashboard_then500() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardData(anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("agente caído")));

        // WHEN / THEN
        webTestClient.get().uri("/api/devops/dashboard?cell=" + CELL + "&sprint=" + SPRINT)
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody().jsonPath("$.error").isEqualTo("agente caído");
    }

    // ---------------------------------------------------------------------------------------
    // GET /api/devops/dashboard/stream
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN falta la célula o el sprint WHEN GET .../stream THEN 400 y no abre el stream")
    void givenMissingParams_whenGetStream_then400() {
        // GIVEN / WHEN / THEN
        webTestClient.get().uri("/api/devops/dashboard/stream?cell=&sprint=" + SPRINT)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo(REQUIRED_PARAMS_MESSAGE);

        verify(taskStoreGateway, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN 3 ítems WHEN GET .../stream THEN emite INITIAL y luego un BATCH_UPDATE")
    void givenThreeItems_whenGetStream_thenEmitsInitialThenOneBatchUpdate() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(3)));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> Mono.just(
                        auditPayloadFor(invocation.getArgument(1), 80, true)));

        // WHEN / THEN
        StepVerifier.create(streamEvents())
                .assertNext(event -> {
                    assertThat(event.getEvent()).isEqualTo("INITIAL");
                    assertThat(event.getData().getItems()).hasSize(3);
                })
                .assertNext(event -> assertThat(event.getEvent()).isEqualTo("BATCH_UPDATE"))
                .verifyComplete();
    }

    /**
     * <b>Regla de negocio hoy alojada en el entry-point (D-07, D-19).</b> El tamaño de lote es el
     * literal {@code 10} dentro de {@code Handler.partitionItems}. Con 25 ítems deben salir tres
     * lotes: 10, 10 y 5. La Fase 05 mueve esta regla al dominio; esta prueba la fija antes.
     */
    @Test
    @DisplayName("GIVEN 25 ítems WHEN GET .../stream THEN 3 lotes de 10, 10 y 5 con sus CSV de IDs")
    void givenTwentyFiveItems_whenGetStream_thenThreeBatchesOfTenTenAndFive() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(25)));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> Mono.just(
                        auditPayloadFor(invocation.getArgument(1), 80, true)));

        // WHEN
        StepVerifier.create(streamEvents())
                .expectNextCount(1 + 3L)
                .verifyComplete();

        // THEN
        verify(devOpsDashboardUseCase).getBatchAudit(eq(BATCH_SIZE),
                eq("1,2,3,4,5,6,7,8,9,10"), anyString());
        verify(devOpsDashboardUseCase).getBatchAudit(eq(BATCH_SIZE),
                eq("11,12,13,14,15,16,17,18,19,20"), anyString());
        verify(devOpsDashboardUseCase).getBatchAudit(eq(5),
                eq("21,22,23,24,25"), anyString());
    }

    /**
     * <b>Cálculo de negocio hoy alojado en el entry-point (D-07).</b>
     * {@code avgQualityScore} es la media <i>entera</i> y {@code undocumentedCount} cuenta los
     * ítems sin criterios de aceptación <i>o</i> sin DoD.
     */
    @Test
    @DisplayName("GIVEN una auditoría con scores WHEN termina el stream THEN recalcula media y no documentados")
    void givenAuditWithScores_whenStreamCompletes_thenRecalculatesMetrics() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(4)));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ids = invocation.getArgument(1);
                    // Dos documentados con 90 y dos sin documentar con 30.
                    String updates = """
                            {"updates":[
                              {"id":"1","hasAcceptanceCriteria":true,"hasDoD":true,"qualityScore":90,"linkedTasksCount":1,"feedback":"ok"},
                              {"id":"2","hasAcceptanceCriteria":true,"hasDoD":true,"qualityScore":90,"linkedTasksCount":1,"feedback":"ok"},
                              {"id":"3","hasAcceptanceCriteria":true,"hasDoD":false,"qualityScore":30,"linkedTasksCount":0,"feedback":"falta DoD"},
                              {"id":"4","hasAcceptanceCriteria":false,"hasDoD":false,"qualityScore":30,"linkedTasksCount":0,"feedback":"falta todo"}
                            ]}""";
                    assertThat(ids).isEqualTo("1,2,3,4");
                    return Mono.just(updates);
                });

        // WHEN
        List<DashboardUpdateEvent> received = new ArrayList<>();
        StepVerifier.create(streamEvents())
                .recordWith(() -> received)
                .expectNextCount(2)
                .verifyComplete();

        // THEN
        DashboardResponse audited = received.get(1).getData();
        assertThat(audited.getMetrics().getAvgQualityScore())
                .as("(90+90+30+30)/4 = 60, con división entera")
                .isEqualTo(60);
        assertThat(audited.getMetrics().getUndocumentedCount())
                .as("los ítems 3 y 4 no tienen CA o no tienen DoD")
                .isEqualTo(2);
        assertThat(audited.getItems().getFirst().getFeedback()).isEqualTo("ok");
    }

    @Test
    @DisplayName("GIVEN un stream correcto WHEN termina THEN la tarea pasa de WORKING a COMPLETED")
    void givenSuccessfulStream_whenCompletes_thenTaskGoesFromWorkingToCompleted() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(1)));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(auditPayloadFor("1", 100, true)));

        // WHEN
        StepVerifier.create(streamEvents()).expectNextCount(2).verifyComplete();

        // THEN
        org.mockito.ArgumentCaptor<Task> taskCaptor =
                org.mockito.ArgumentCaptor.forClass(Task.class);
        verify(taskStoreGateway, times(2)).save(taskCaptor.capture());

        List<Task> saved = taskCaptor.getAllValues();
        assertThat(saved.get(0).getStatus().getState()).isEqualTo(TaskState.WORKING);
        assertThat(saved.get(1).getStatus().getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(saved.get(0).getId()).isEqualTo(saved.get(1).getId());
    }

    @Test
    @DisplayName("GIVEN un stream correcto WHEN termina THEN guarda el reporte Markdown")
    void givenSuccessfulStream_whenCompletes_thenSavesMarkdownReport() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(1)));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(auditPayloadFor("1", 100, true)));

        // WHEN
        StepVerifier.create(streamEvents()).expectNextCount(2).verifyComplete();

        // THEN
        verify(devOpsDashboardUseCase)
                .saveDashboardReport(any(BacklogAudit.class), eq(CELL), eq(SPRINT));
    }

    /**
     * El fallo de <b>un</b> lote no rompe el stream: se emite el estado actual y se continúa.
     */
    @Test
    @DisplayName("GIVEN falla la auditoría de un lote WHEN GET .../stream THEN el stream no se rompe")
    void givenOneBatchFails_whenGetStream_thenStreamSurvives() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(initialPayloadWith(3)));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("el agente no responde")));

        // WHEN / THEN
        StepVerifier.create(streamEvents())
                .assertNext(event -> assertThat(event.getEvent()).isEqualTo("INITIAL"))
                .assertNext(event -> {
                    assertThat(event.getEvent()).isEqualTo("BATCH_UPDATE");
                    assertThat(event.getData().getItems().getFirst().getQualityScore()).isZero();
                })
                .verifyComplete();
    }

    /**
     * <b>D-31 cerrada (B-06).</b> Antes, un fallo al obtener los datos iniciales ocurría
     * <i>antes</i>
     * del primer elemento, así que WebFlux no llegaba a escribir la cabecera
     * {@code text/event-stream} y el cliente recibía un <b>500</b> con cuerpo JSON. El front
     * (`EventSource.onerror`) cerraba la conexión y completaba el observable en silencio: el
     * usuario no distinguía «no hay datos» de «el agente se cayó».
     *
     * <p>Ahora el stream <b>nunca termina en error</b>: el fallo viaja como un evento
     * {@code ERROR} con su mensaje, y la respuesta es un 200 con la cabecera SSE. La tarea se sigue
     * marcando como {@code FAILED} en el almacén, igual que antes.
     */
    @Test
    @DisplayName("GIVEN falla el arranque WHEN GET .../stream THEN emite un evento ERROR y la tarea queda FAILED")
    void givenInitialFails_whenGetStream_thenEmitsErrorEventAndTaskIsFailed() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("agente caído")));

        // WHEN
        StepVerifier.create(streamEvents())
                .assertNext(event -> {
                    assertThat(event.getEvent()).isEqualTo("ERROR");
                    assertThat(event.getMessage()).isEqualTo("agente caído");
                    assertThat(event.getData()).isNull();
                })
                .verifyComplete();

        // THEN
        org.mockito.ArgumentCaptor<Task> taskCaptor =
                org.mockito.ArgumentCaptor.forClass(Task.class);
        verify(taskStoreGateway, times(2)).save(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues().get(1).getStatus().getState())
                .isEqualTo(TaskState.FAILED);
    }

    /**
     * <b>Repliegue unificado (D-11, B-07).</b> El entry-point ya no sabe si el canal de IA está
     * habilitado: si el repliegue está activo, es el <b>dominio</b> quien devuelve el tablero
     * simulado por el mismo camino que devolvería el real. Desde aquí eso se ve como un stream
     * normal, que es justo la idea.
     */
    @Test
    @DisplayName("GIVEN el dominio repliega al tablero simulado WHEN GET .../stream THEN el stream es uno normal")
    void givenDomainFallsBackToMock_whenGetStream_thenStreamLooksNormal() {
        // GIVEN
        when(devOpsDashboardUseCase.getDashboardInitialData(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(mockDashboardPayload()));
        when(devOpsDashboardUseCase.getBatchAudit(anyInt(), anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("el agente no responde")));

        // WHEN / THEN
        StepVerifier.create(streamEvents().timeout(Duration.ofSeconds(10)))
                .assertNext(event -> {
                    assertThat(event.getEvent()).isEqualTo("INITIAL");
                    assertThat(event.getData().getMetrics().getAvgQualityScore()).isEqualTo(78);
                    assertThat(event.getData().getItems().getFirst().isHasAcceptanceCriteria())
                            .isTrue();
                })
                .assertNext(event -> {
                    assertThat(event.getEvent()).isEqualTo("BATCH_UPDATE");
                    assertThat(event.getData().getMetrics().getAvgQualityScore()).isEqualTo(78);
                })
                .verifyComplete();
    }

    private String mockDashboardPayload() {
        return """
                {"metrics":{"totalPoints":29,"completedPoints":13,"completedPercentage":44,
                            "avgQualityScore":78,"undocumentedCount":2},
                 "items":[{"id":"1001","title":"Historia mock","points":8,"state":"Approved",
                           "assignedMember":"Alguien","hasAcceptanceCriteria":true,"hasDoD":false,
                           "qualityScore":66,"linkedTasksCount":6,"feedback":"falta DoD"}]}
                """;
    }
}



