package co.com.bancolombia.api;

import co.com.bancolombia.api.dashboard.DashboardStreamOrchestrator;
import co.com.bancolombia.api.dto.planning.PlanningDtoMapper;
import co.com.bancolombia.api.error.ApiErrorTranslator;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class Handler {

    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_KEY = "error";
    private static final int DEFAULT_MAX_RESULTS = 3;

    private final IngestPlanningSpecUseCase ingestPlanningSpecUseCase;
    private final SearchPlanningSpecUseCase searchPlanningSpecUseCase;
    private final ManagePlanningUseCase managePlanningUseCase;
    private final DevOpsDashboardUseCase devOpsDashboardUseCase;

    /**
     * Orquestador del stream SSE (D-05).
     *
     * <p><b>Fase 08 (D-42, B-13).</b> Se <b>inyecta</b>. Hasta la Fase 07 se construía aquí con
     * {@code new}, junto con su propio {@code DashboardTaskTracker}, y el javadoc lo justificaba
     * diciendo que no tenía configuración propia. Sí la tenía —{@code audit.batch-size} y
     * {@code audit.concurrency}—, sólo que la declaraba este {@code Handler} en su nombre. El
     * motivo real de aquel {@code new} era no tener que tocar el {@code @ContextConfiguration} de
     * tres pruebas; B-13 autoriza hacerlo y el andamio desaparece.
     */
    private final DashboardStreamOrchestrator dashboardStream;

    public Handler(IngestPlanningSpecUseCase ingestPlanningSpecUseCase,
            SearchPlanningSpecUseCase searchPlanningSpecUseCase,
            ManagePlanningUseCase managePlanningUseCase,
            DevOpsDashboardUseCase devOpsDashboardUseCase,
            DashboardStreamOrchestrator dashboardStream) {
        this.ingestPlanningSpecUseCase = ingestPlanningSpecUseCase;
        this.searchPlanningSpecUseCase = searchPlanningSpecUseCase;
        this.managePlanningUseCase = managePlanningUseCase;
        this.devOpsDashboardUseCase = devOpsDashboardUseCase;
        this.dashboardStream = dashboardStream;
    }


    /**
     * POST /api/planning/ingest – Ingesta de especificaciones de planificación desde el Frontend.
     */
    public Mono<ServerResponse> handleIngestPlanning(ServerRequest request) {
        return request.bodyToMono(IngestPlanningRequest.class)
                .flatMap(req -> ingestPlanningSpecUseCase.ingestMarkdown(req.initiativeId(),
                                req.title(), req.markdownContent())
                        .then(ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(MESSAGE_KEY,
                                        "Planeación ingesta y vectorizada exitosamente"))))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    /**
     * GET /api/planning/search – Búsqueda semántica de planeación.
     *
     * <p>{@code maxResults} se valida <b>antes</b> de entrar en la cadena reactiva: hasta la Fase
     * 03 un valor no numérico reventaba en {@code Integer.parseInt} y escapaba como 500 (D-28).
     *
     * <p>Devuelve {@link co.com.bancolombia.api.dto.planning.PlanningChunkResponse}, no el modelo
     * de dominio (D-15). El JSON es el mismo.
     */
    public Mono<ServerResponse> handleSearchPlanning(ServerRequest request) {
        return Mono.fromCallable(() -> RequestValidator.maxResults(request, DEFAULT_MAX_RESULTS))
                .flatMapMany(maxResults -> searchPlanningSpecUseCase.search(
                        request.queryParam("query").orElse(""),
                        request.queryParam("initiativeId").orElse(""),
                        maxResults))
                .map(PlanningDtoMapper::toResponse)
                .collectList()
                .flatMap(chunks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(chunks))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    public Mono<ServerResponse> handleListInitiatives() {
        return managePlanningUseCase.getInitiatives()
                .collectList()
                .flatMap(initiatives -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(initiatives))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    /**
     * Devuelve DTOs, no modelos de dominio (D-15).
     */
    public Mono<ServerResponse> handleGetInitiativeChunks(ServerRequest request) {
        String id = request.pathVariable("id");
        return managePlanningUseCase.getInitiativeChunks(id)
                .map(PlanningDtoMapper::toResponse)
                .collectList()
                .flatMap(chunks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(chunks))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    public Mono<ServerResponse> handleDeleteInitiative(ServerRequest request) {
        String id = request.pathVariable("id");
        return managePlanningUseCase.deleteInitiative(id)
                .then(ServerResponse.ok().build())
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    public Mono<ServerResponse> handleUpdateCell(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(UpdateCellRequest.class)
                .flatMap(body -> managePlanningUseCase.updateCell(id, body.cell()))
                .then(ServerResponse.ok().build())
                .onErrorResume(ApiErrorTranslator::toResponse);
    }


    public Mono<ServerResponse> handleDevOpsDashboard(ServerRequest request) {
        String cell = request.queryParam("cell").orElse("").trim();
        String sprint = request.queryParam("sprint").orElse("").trim();

        if (cell.isEmpty() || sprint.isEmpty()) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                            Map.of(ERROR_KEY, "La célula y el sprint son parámetros requeridos"));
        }

        return devOpsDashboardUseCase.getDashboardData(cell, sprint)
                .flatMap(json -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(json))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    public Mono<ServerResponse> handleDevOpsDashboardStream(ServerRequest request) {
        String cell = request.queryParam("cell").orElse("").trim();
        String sprint = request.queryParam("sprint").orElse("").trim();

        if (cell.isEmpty() || sprint.isEmpty()) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                            Map.of(ERROR_KEY, "La célula y el sprint son parámetros requeridos"));
        }

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(dashboardStream.stream(cell, sprint), ServerSentEvent.class);
    }

    public record IngestPlanningRequest(String initiativeId, String title, String markdownContent) {

    }

    public record UpdateCellRequest(String cell) {

    }
}
