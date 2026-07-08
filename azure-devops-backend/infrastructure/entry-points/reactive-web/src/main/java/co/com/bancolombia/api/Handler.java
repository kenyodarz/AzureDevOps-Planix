package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.event.BatchUpdatesEvent;
import co.com.bancolombia.api.dto.event.DashboardUpdateEvent;
import co.com.bancolombia.api.dto.event.StoryUpdateEvent;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.dashboard.DashboardResponse;
import co.com.bancolombia.model.dashboard.DashboardStoryItemResponse;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Log4j2
@Component
public class Handler {

    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_KEY = "error";
    private static final String EVENT_BATCH_UPDATE = "BATCH_UPDATE";
    private static final String ROLE_AGENT = "agent";

    private final IngestPlanningSpecUseCase ingestPlanningSpecUseCase;
    private final SearchPlanningSpecUseCase searchPlanningSpecUseCase;
    private final ManagePlanningUseCase managePlanningUseCase;
    private final DevOpsDashboardUseCase devOpsDashboardUseCase;
    private final TaskStoreGateway taskStoreGateway;
    private final JsonMapper jsonMapper;

    public Handler(IngestPlanningSpecUseCase ingestPlanningSpecUseCase,
            SearchPlanningSpecUseCase searchPlanningSpecUseCase,
            ManagePlanningUseCase managePlanningUseCase,
            DevOpsDashboardUseCase devOpsDashboardUseCase,
            TaskStoreGateway taskStoreGateway,
            JsonMapper jsonMapper) {
        this.ingestPlanningSpecUseCase = ingestPlanningSpecUseCase;
        this.searchPlanningSpecUseCase = searchPlanningSpecUseCase;
        this.managePlanningUseCase = managePlanningUseCase;
        this.devOpsDashboardUseCase = devOpsDashboardUseCase;
        this.taskStoreGateway = taskStoreGateway;
        this.jsonMapper = jsonMapper;
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
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    /**
     * GET /api/planning/search – Búsqueda semántica de planeación.
     */
    public Mono<ServerResponse> handleSearchPlanning(ServerRequest request) {
        String query = request.queryParam("query").orElse("");
        String initiativeId = request.queryParam("initiativeId").orElse("");
        int maxResults = request.queryParam("maxResults").map(Integer::parseInt).orElse(3);

        return searchPlanningSpecUseCase.search(query, initiativeId, maxResults)
                .collectList()
                .flatMap(chunks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(chunks))
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    public Mono<ServerResponse> handleListInitiatives() {
        return managePlanningUseCase.getInitiatives()
                .collectList()
                .flatMap(initiatives -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(initiatives))
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    public Mono<ServerResponse> handleGetInitiativeChunks(ServerRequest request) {
        String id = request.pathVariable("id");
        return managePlanningUseCase.getInitiativeChunks(id)
                .collectList()
                .flatMap(chunks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(chunks))
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    public Mono<ServerResponse> handleDeleteInitiative(ServerRequest request) {
        String id = request.pathVariable("id");
        return managePlanningUseCase.deleteInitiative(id)
                .then(ServerResponse.ok().build())
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    public Mono<ServerResponse> handleUpdateCell(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(UpdateCellRequest.class)
                .flatMap(body -> managePlanningUseCase.updateCell(id, body.cell()))
                .then(ServerResponse.ok().build())
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
    }

    public Mono<ServerResponse> handleListTasks() {
        return taskStoreGateway.findAll()
                .collectList()
                .flatMap(tasks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(tasks))
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
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
                .onErrorResume(e -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(ERROR_KEY, e.getMessage())));
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

        String sessionKey = "dashboard-" + java.util.UUID.randomUUID();

        // 1. Crear Tarea A2A en Estado WORKING con mensaje explicativo
        String taskId = java.util.UUID.randomUUID().toString();
        Task dashboardTask = Task.builder()
                .id(taskId)
                .contextId(sessionKey)
                .status(TaskStatus.builder()
                        .state(TaskState.WORKING)
                        .message(Message.builder()
                                .role(ROLE_AGENT)
                                .contextId(sessionKey)
                                .messageId(java.util.UUID.randomUUID().toString())
                                .parts(List.of(Part.ofText(
                                        "Analizando calidad del backlog: Célula " + cell
                                                + " | Sprint " + sprint)))
                                .build())
                        .timestamp(java.time.Instant.now().toString())
                        .build())
                .build();

        final DashboardResponse[] sharedData = new DashboardResponse[1];

        Flux<ServerSentEvent<DashboardUpdateEvent>> sseFlux = taskStoreGateway.save(dashboardTask)
                .then(devOpsDashboardUseCase.getDashboardInitialData(cell, sprint, sessionKey))
                .flatMapMany(responseStr -> {
                    try {
                        DashboardResponse initialData = jsonMapper.readValue(responseStr,
                                DashboardResponse.class);
                        sharedData[0] = initialData; // Almacenar para grabarlo al completar
                        DashboardUpdateEvent initialEvent = new DashboardUpdateEvent("INITIAL",
                                initialData);

                        List<DashboardStoryItemResponse> allItems = initialData.getItems();
                        List<List<DashboardStoryItemResponse>> batches = partitionItems(allItems);

                        Flux<DashboardUpdateEvent> updatesFlux = Flux.fromIterable(batches)
                                .flatMapSequential(
                                        batch -> auditBatchAndEmit(batch, allItems, initialData,
                                                sessionKey));

                        return Flux.concat(Flux.just(initialEvent), updatesFlux);

                    } catch (Exception e) {
                        log.error("Failed parsing initial dashboard data.", e);
                        if (!devOpsDashboardUseCase.isMcpEnabled()) {
                            log.info("MCP is disabled. Falling back to mock dashboard stream.");
                            return getMockDashboardStream();
                        }
                        return Flux.error(e);
                    }
                })
                .onErrorResume(error -> {
                    log.warn("Error in dashboard stream.", error);
                    if (!devOpsDashboardUseCase.isMcpEnabled()) {
                        log.info("MCP is disabled. Falling back to mock dashboard stream.");
                        return getMockDashboardStream();
                    }
                    return Flux.error(error); // Propaga el error del stream reactivo
                })
                .concatWith(Mono.defer(() -> {
                    // 2. Al completar con éxito, actualizar Tarea a COMPLETED con mensaje de forma reactiva
                    Task completedTask = dashboardTask.toBuilder()
                            .status(TaskStatus.builder()
                                    .state(TaskState.COMPLETED)
                                    .message(Message.builder()
                                            .role(ROLE_AGENT)
                                            .contextId(sessionKey)
                                            .messageId(java.util.UUID.randomUUID().toString())
                                            .parts(List.of(Part.ofText(
                                                    "Auditoría de backlog finalizada con éxito. Reporte generado.")))
                                            .build())
                                    .timestamp(java.time.Instant.now().toString())
                                    .build())
                            .build();

                    if (sharedData[0] != null) {
                        devOpsDashboardUseCase.saveDashboardReport(sharedData[0], cell, sprint);
                    }
                    return taskStoreGateway.save(completedTask).then(Mono.empty());
                }))
                .onErrorResume(error -> {
                    // 3. Al ocurrir error, marcar Tarea como FAILED con detalle del error de forma reactiva
                    Task failedTask = dashboardTask.toBuilder()
                            .status(TaskStatus.builder()
                                    .state(TaskState.FAILED)
                                    .message(Message.builder()
                                            .role(ROLE_AGENT)
                                            .contextId(sessionKey)
                                            .messageId(java.util.UUID.randomUUID().toString())
                                            .parts(List.of(Part.ofText(
                                                    "Fallo en auditoría: " + error.getMessage())))
                                            .build())
                                    .timestamp(java.time.Instant.now().toString())
                                    .build())
                            .build();
                    return taskStoreGateway.save(failedTask).then(Mono.error(error));
                })
                .map(update -> ServerSentEvent.<DashboardUpdateEvent>builder()
                        .event(update.getEvent())
                        .data(update)
                        .build());

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(sseFlux, ServerSentEvent.class);
    }

    private List<List<DashboardStoryItemResponse>> partitionItems(
            List<DashboardStoryItemResponse> allItems) {
        List<List<DashboardStoryItemResponse>> batches = new ArrayList<>();
        if (allItems != null) {
            for (int i = 0; i < allItems.size(); i += 10) {
                batches.add(allItems.subList(i, Math.min(i + 10, allItems.size())));
            }
        }
        return batches;
    }

    private Mono<DashboardUpdateEvent> auditBatchAndEmit(
            List<DashboardStoryItemResponse> batch,
            List<DashboardStoryItemResponse> allItems,
            DashboardResponse initialData,
            String sessionKey) {

        String idsCsv = batch.stream()
                .map(DashboardStoryItemResponse::getId)
                .collect(Collectors.joining(","));

        return devOpsDashboardUseCase.getBatchAudit(batch.size(), idsCsv, sessionKey)
                .map(auditResponse -> applyBatchAuditUpdates(auditResponse, allItems, initialData))
                .onErrorResume(e -> {
                    log.warn("Failed to audit batch, continuing with existing states", e);
                    return Mono.just(new DashboardUpdateEvent(EVENT_BATCH_UPDATE, initialData));
                });
    }

    private DashboardUpdateEvent applyBatchAuditUpdates(
            String auditResponse,
            List<DashboardStoryItemResponse> allItems,
            DashboardResponse initialData) {
        try {
            BatchUpdatesEvent batchUpdates = jsonMapper.readValue(auditResponse,
                    BatchUpdatesEvent.class);
            if (batchUpdates.getUpdates() != null && allItems != null) {
                updateOriginalItems(batchUpdates.getUpdates(), allItems);
            }
            recalculateMetrics(allItems, initialData);
            return new DashboardUpdateEvent(EVENT_BATCH_UPDATE, initialData);
        } catch (Exception e) {
            log.warn("Error parsing batch quality audits, returning current state", e);
            return new DashboardUpdateEvent(EVENT_BATCH_UPDATE, initialData);
        }
    }

    private void updateOriginalItems(List<StoryUpdateEvent> updates,
            List<DashboardStoryItemResponse> allItems) {
        for (StoryUpdateEvent update : updates) {
            for (DashboardStoryItemResponse originalItem : allItems) {
                if (originalItem.getId().equals(update.getId())) {
                    originalItem.setHasAcceptanceCriteria(update.isHasAcceptanceCriteria());
                    originalItem.setHasDoD(update.isHasDoD());
                    originalItem.setQualityScore(update.getQualityScore());
                    originalItem.setLinkedTasksCount(update.getLinkedTasksCount());
                    originalItem.setFeedback(update.getFeedback());
                }
            }
        }
    }

    private void recalculateMetrics(List<DashboardStoryItemResponse> allItems,
            DashboardResponse initialData) {
        if (allItems != null && initialData.getMetrics() != null) {
            int totalQuality = 0;
            int undocumentedCount = 0;
            for (DashboardStoryItemResponse item : allItems) {
                totalQuality += item.getQualityScore();
                if (!item.isHasAcceptanceCriteria() || !item.isHasDoD()) {
                    undocumentedCount++;
                }
            }
            initialData.getMetrics()
                    .setAvgQualityScore(totalQuality / Math.max(1, allItems.size()));
            initialData.getMetrics().setUndocumentedCount(undocumentedCount);
        }
    }

    private Flux<DashboardUpdateEvent> getMockDashboardStream() {
        return Flux.defer(() -> {
            try {
                String mockDataStr = devOpsDashboardUseCase.getMockDashboardData();
                // Parse original mock fully
                DashboardResponse fullMockData = jsonMapper.readValue(mockDataStr,
                        DashboardResponse.class);

                // Create a clone/copy for the initial state where quality / DoD checks are 0/false
                DashboardResponse initialMockData = jsonMapper.readValue(mockDataStr,
                        DashboardResponse.class);
                if (initialMockData.getMetrics() != null) {
                    initialMockData.getMetrics().setAvgQualityScore(0);
                    initialMockData.getMetrics().setUndocumentedCount(0);
                }
                if (initialMockData.getItems() != null) {
                    for (DashboardStoryItemResponse item : initialMockData.getItems()) {
                        item.setQualityScore(0);
                        item.setHasAcceptanceCriteria(false);
                        item.setHasDoD(false);
                        item.setLinkedTasksCount(0);
                    }
                }

                DashboardUpdateEvent initialEvent = new DashboardUpdateEvent("INITIAL",
                        initialMockData);
                DashboardUpdateEvent finalEvent = new DashboardUpdateEvent(EVENT_BATCH_UPDATE,
                        fullMockData);

                // Emit INITIAL immediately, and then final after 800ms
                return Flux.concat(
                        Flux.just(initialEvent),
                        Flux.just(finalEvent).delayElements(java.time.Duration.ofMillis(800))
                );
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }

    public record IngestPlanningRequest(String initiativeId, String title, String markdownContent) {

    }

    public record UpdateCellRequest(String cell) {

    }
}
