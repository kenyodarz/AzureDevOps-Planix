package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.task.TaskDtoMapper;
import co.com.bancolombia.api.error.ApiErrorTranslator;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Rutas de chat y de tareas del BFF hacia el frontend (DP-07, opción B).
 *
 * <p>Es el único interlocutor del frontend para cuanto el agente ejecuta. Sustituye a las
 * llamadas que el frontend hacía <b>directamente al agente</b> en {@code :8082/message:send}
 * —saltándose el BFF, que así no validaba, no registraba y no podía seguir la tarea (D-21)— y a las
 * dos rutas que llamaba y nadie servía: {@code POST /} con {@code tasks/cancel} (D-25) y
 * {@code GET /.well-known/agent-card.json} (D-26).
 *
 * <p><b>Clase propia, no una ampliación de {@code Handler}.</b> Aquel ya tiene cuatro
 * responsabilidades mezcladas y es objetivo de la Fase 06; añadirle cinco rutas más habría
 * empeorado justo lo que el plan quiere arreglar.
 */
@Slf4j
@Component
public class TaskHandler {

    private static final String CONTEXT_ID_KEY = "contextId";

    private final TrackAgentTaskUseCase trackAgentTaskUseCase;

    public TaskHandler(TrackAgentTaskUseCase trackAgentTaskUseCase) {
        this.trackAgentTaskUseCase = trackAgentTaskUseCase;
    }

    /**
     * {@code POST /api/chat/messages} — envía el mensaje del usuario al agente.
     *
     * <p>Propaga el {@code contextId} recibido hacia el agente y, si el frontend no envía ninguno,
     * <b>genera uno y lo devuelve</b> dentro de la tarea para que lo reutilice en los mensajes
     * siguientes de la misma conversación (D-30).
     */
    public Mono<ServerResponse> handleSendMessage(ServerRequest request) {
        return request.bodyToMono(ChatMessageRequest.class)
                .map(this::toCommand)
                .flatMap(trackAgentTaskUseCase::sendAndTrack)
                .map(TaskDtoMapper::toResponse)
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body))
                .onErrorResume(this::toErrorResponse);
    }

    /**
     * {@code GET /api/tasks} — unión de las tareas del agente y las del BFF (DP-07-bis).
     */
    public Mono<ServerResponse> handleListTasks() {
        return trackAgentTaskUseCase.listTasks()
                .map(TaskDtoMapper::toResponse)
                .collectList()
                .flatMap(tasks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(tasks))
                .onErrorResume(this::toErrorResponse);
    }

    /**
     * {@code GET /api/tasks/&#123;id&#125;} — estado actual de una tarea del agente.
     */
    public Mono<ServerResponse> handleGetTask(ServerRequest request) {
        return Mono.fromCallable(() -> RequestValidator.requireText(request.pathVariable("id"),
                        "id de la tarea"))
                .flatMap(trackAgentTaskUseCase::refreshTask)
                .map(TaskDtoMapper::toResponse)
                .flatMap(task -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(task))
                .onErrorResume(this::toErrorResponse);
    }

    /**
     * {@code POST /api/tasks/&#123;id&#125;/cancel} — cancela una tarea en curso.
     */
    public Mono<ServerResponse> handleCancelTask(ServerRequest request) {
        return Mono.fromCallable(() -> RequestValidator.requireText(request.pathVariable("id"),
                        "id de la tarea"))
                .flatMap(trackAgentTaskUseCase::cancelTask)
                .map(TaskDtoMapper::toResponse)
                .flatMap(task -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(task))
                .onErrorResume(this::toErrorResponse);
    }

    /**
     * {@code GET /api/agent/card} — capacidades del agente, servidas por el BFF.
     */
    public Mono<ServerResponse> handleAgentCard() {
        return trackAgentTaskUseCase.getAgentCard()
                .flatMap(card -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(card))
                .onErrorResume(this::toErrorResponse);
    }

    private AgentCommand toCommand(ChatMessageRequest request) {
        Map<String, Object> message = request.message();
        String prompt = RequestValidator.requirePromptText(message);
        String contextId = resolveContextId(message);
        log.info("Mensaje de chat recibido para el contexto {}", contextId);
        return AgentCommand.nonBlocking(prompt, contextId);
    }

    private String resolveContextId(Map<String, Object> message) {
        if (message.get(CONTEXT_ID_KEY) instanceof String contextId && !contextId.isBlank()) {
            return contextId;
        }
        String generated = UUID.randomUUID().toString();
        log.debug("El frontend no envió contextId; se genera {} y se devuelve en la respuesta",
                generated);
        return generated;
    }

    /**
     * Traducción de errores de estas rutas.
     *
     * <p>Hasta la Fase 07 el mapa vivía aquí, y era el <b>único</b> del BFF que distinguía de
     * quién era la culpa: el resto del entry-point devolvía 400 para cualquier error. La Fase 07 lo
     * saca a {@link ApiErrorTranslator} y lo aplica a todas las rutas, de modo que ya <b>no
     * conviven dos criterios</b> en el mismo entry-point (D-16, DP-06). El mapa que se centralizó
     * es literalmente el que estaba aquí, más {@code TimeoutException → 504}.
     */
    private Mono<ServerResponse> toErrorResponse(Throwable error) {
        return ApiErrorTranslator.toResponse(error);
    }

    /**
     * Cuerpo de {@code POST /api/chat/messages}.
     *
     * <p>Reproduce, simplificada, la forma que el frontend ya envía hoy al agente, de modo que el
     * cambio en el cliente se limita a la URL:
     * {@code { "message": { "contextId": "...", "parts": [ { "text": "..." } ] } }}.
     *
     * @param message bloque del mensaje del usuario
     */
    public record ChatMessageRequest(Map<String, Object> message) {

        public ChatMessageRequest {
            message = message != null ? message : Map.of();
        }
    }
}

