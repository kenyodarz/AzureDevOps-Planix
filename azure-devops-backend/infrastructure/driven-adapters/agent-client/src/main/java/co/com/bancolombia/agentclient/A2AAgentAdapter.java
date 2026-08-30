package co.com.bancolombia.agentclient;

import co.com.bancolombia.agentclient.config.AgentConnectionProperties;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.agent.exceptions.AgentException;
import co.com.bancolombia.model.agent.exceptions.AgentExecutionException;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.agent.exceptions.InvalidAgentRequestException;
import co.com.bancolombia.model.agent.exceptions.TaskNotFoundException;
import co.com.bancolombia.model.agent.gateways.AgentGateway;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cliente A2A del agente autónomo, sobre JSON-RPC 2.0.
 *
 * <p>Es el único punto por el que el BFF delega trabajo. Sustituye a {@code ChatGatewayAdapter},
 * que llamaba al endpoint <b>legacy</b> {@code POST /message:send} —con fecha de sunset ya vencida
 * en el agente— y que además <b>descartaba la {@link Task}</b> devuelta, dejando al frontend sin
 * forma de seguir el progreso.
 *
 * <p><b>Modo no bloqueante (DP-08).</b> Toda la cadena es reactiva: no hay {@code block()} ni
 * suscripciones manuales. El agente devuelve la tarea en curso y el frontend la sigue.
 *
 * <p><b>Traducción de errores.</b> Ni los códigos JSON-RPC ni las excepciones de {@code WebClient}
 * escapan de esta clase: se convierten en excepciones tipadas del dominio.
 */
@Slf4j
@Component
public class A2AAgentAdapter implements AgentGateway {

    private static final String METHOD_SEND = "message/send";
    private static final String METHOD_GET = "tasks/get";
    private static final String METHOD_CANCEL = "tasks/cancel";
    private static final String JSON_RPC_PATH = "/";

    /**
     * Rutas <b>REST planas</b> del agente. No son JSON-RPC: se consultan con {@code GET}, no con el
     * {@code POST /} del dispatcher.
     */
    private static final String TASKS_PATH = "/api/tasks";
    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

    /**
     * Algunos modelos anteponen su razonamiento entre etiquetas {@code <think>}. Se compila una
     * sola vez: la versión anterior recompilaba el patrón en cada respuesta.
     */
    private static final Pattern REASONING_PATTERN = Pattern.compile("(?s)<think>.*?</think>");

    /**
     * Tamaño máximo de una respuesta del agente que se retiene en memoria. El valor por defecto de
     * WebFlux son 256 KB, insuficiente para un tablero completo.
     *
     * <p><b>Fase 08 (D-32, B-11).</b> Este límite lo fijaba el {@code WebClient.Builder} que
     * publicaba {@code OAuth2WebClientConfig} en el módulo {@code mcp-client}. Ese módulo se
     * eliminó —el BFF no habla MCP con nadie—, así que el ajuste pasa a donde de verdad se usa: el
     * adaptador que hace la llamada. Antes, un módulo de MCP configuraba en la sombra el transporte
     * del cliente A2A; si alguien lo hubiera borrado sin mirar, el agente habría empezado a fallar
     * con respuestas grandes y sin relación aparente con el cambio.
     */
    private static final int MAX_RESPONSE_IN_MEMORY_BYTES = 10 * 1024 * 1024;

    private final WebClient webClient;
    private final AgentConnectionProperties properties;

    public A2AAgentAdapter(WebClient.Builder webClientBuilder,
            AgentConnectionProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .clone()
                .baseUrl(properties.url())
                .codecs(codecs -> codecs.defaultCodecs()
                        .maxInMemorySize(MAX_RESPONSE_IN_MEMORY_BYTES))
                .build();
        log.info("BFF configurado como cliente A2A del agente en {} (timeout {})",
                properties.url(), properties.timeout());
    }

    @Override
    public Mono<AgentInteraction> sendMessage(AgentCommand command) {
        return dispatch(METHOD_SEND, A2APayloadMapper.toSendMessageParams(command))
                .map(result -> new AgentInteraction(
                        A2APayloadMapper.toTask(result),
                        filterReasoning(A2APayloadMapper.toReply(result))))
                .doOnNext(interaction -> log.debug(
                        "Respuesta del agente para el contexto {} · tarea asociada: {}",
                        command.contextId(),
                        interaction.taskAsOptional().map(Task::getId).orElse("ninguna")));
    }

    @Override
    public Mono<Task> getTask(String taskId) {
        return dispatch(METHOD_GET, A2APayloadMapper.toTaskParams(taskId))
                .flatMap(result -> toTaskOrNotFound(result, taskId));
    }

    @Override
    public Mono<Task> cancelTask(String taskId) {
        return dispatch(METHOD_CANCEL, A2APayloadMapper.toTaskParams(taskId))
                .flatMap(result -> toTaskOrNotFound(result, taskId));
    }

    private Mono<Task> toTaskOrNotFound(Map<String, Object> result, String taskId) {
        Task task = A2APayloadMapper.toTask(result);
        if (task == null) {
            return Mono.error(new TaskNotFoundException(taskId));
        }
        return Mono.just(task);
    }

    @Override
    public Flux<Task> listTasks() {
        return get(TASKS_PATH, Object.class)
                .map(A2APayloadMapper::toTasks)
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<AgentCard> getAgentCard() {
        return get(AGENT_CARD_PATH, Map.class)
                .map(A2APayloadMapper::toAgentCard);
    }

    /**
     * Invoca una ruta <b>REST</b> del agente.
     *
     * <p>Existe separada de {@link #dispatch} porque {@code GET /api/tasks} y la agent card no
     * hablan JSON-RPC: no llevan sobre, no devuelven {@code result} y no pueden fallar con los
     * códigos {@code -326xx}. Comparten, eso sí, el mismo timeout y la misma traducción de fallos
     * de transporte a {@link AgentUnavailableException}.
     */
    private <T> Mono<T> get(String path, Class<T> responseType) {
        return webClient.get()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(properties.timeout())
                .onErrorMap(this::isTransportFailure,
                        error -> new AgentUnavailableException(
                                "fallo de transporte al consultar " + path, error));
    }

    /**
     * Envía un sobre JSON-RPC 2.0 al agente y devuelve el bloque {@code result}.
     *
     * <p>Concentra el <b>único</b> punto de traducción de errores del adaptador: cualquier fallo,
     * venga del protocolo o del transporte, sale de aquí como excepción del dominio.
     */
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> dispatch(String method, Map<String, Object> params) {
        String requestId = UUID.randomUUID().toString();

        return webClient.post()
                .uri(JSON_RPC_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JsonRpcEnvelope.request(requestId, method, params))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> (Map<String, Object>) body)
                .timeout(properties.timeout())
                .flatMap(body -> extractResult(body, method))
                .onErrorMap(this::isTransportFailure,
                        error -> new AgentUnavailableException(
                                "fallo de transporte al invocar " + method, error));
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> extractResult(Map<String, Object> body, String method) {
        Object rawError = body.get("error");
        if (rawError instanceof Map<?, ?> errorMap) {
            return Mono.error(toDomainError((Map<String, Object>) errorMap, method));
        }
        Object rawResult = body.get("result");
        if (rawResult instanceof Map<?, ?> resultMap) {
            return Mono.just((Map<String, Object>) resultMap);
        }
        return Mono.just(Map.of());
    }

    /**
     * Traduce el código de error JSON-RPC del agente a la excepción de dominio equivalente.
     *
     * <p>Los códigos son los que el agente declara como intocables en su
     * {@code JsonRpcResponseFactory}.
     */
    private RuntimeException toDomainError(Map<String, Object> errorMap, String method) {
        int code = errorMap.get("code") instanceof Number number ? number.intValue() : 0;
        String message = String.valueOf(errorMap.getOrDefault("message", "sin detalle"));

        log.warn("El agente respondió con error {} al invocar {}: {}", code, method, message);

        return switch (code) {
            case JsonRpcEnvelope.TASK_NOT_FOUND -> new TaskNotFoundException(message);
            case JsonRpcEnvelope.INVALID_REQUEST,
                 JsonRpcEnvelope.METHOD_NOT_FOUND,
                 JsonRpcEnvelope.INVALID_PARAMS -> new InvalidAgentRequestException(message);
            default -> new AgentExecutionException(message);
        };
    }

    /**
     * Un error de dominio ya traducido no debe volver a envolverse; cualquier otro fallo —conexión
     * rechazada, DNS, timeout, 5xx— significa que el agente no está disponible.
     */
    private boolean isTransportFailure(Throwable error) {
        return !(error instanceof AgentException);
    }

    private String filterReasoning(String response) {
        if (response == null) {
            return "";
        }
        return REASONING_PATTERN.matcher(response).replaceAll("").trim();
    }
}



