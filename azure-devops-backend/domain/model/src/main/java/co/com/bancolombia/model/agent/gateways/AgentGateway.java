package co.com.bancolombia.model.agent.gateways;

import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia el agente autónomo.
 *
 * <p>Es el <b>único</b> canal por el que el BFF delega trabajo. Sustituye al antiguo
 * {@code ChatGateway}, cuya firma {@code Mono<String> sendMessage(...)} obligaba a descartar la
 * {@link Task} que el agente devolvía (deuda D-23) y dejaba al frontend sin forma de seguir el
 * progreso.
 *
 * <p><b>Contrato con el agente.</b> Las tres operaciones {@code sendMessage}, {@code getTask} y
 * {@code cancelTask} se corresponden con los métodos JSON-RPC 2.0 que el agente expone en
 * {@code POST /}: {@code message/send}, {@code tasks/get} y {@code tasks/cancel}. En cambio
 * {@link #listTasks()} y {@link #getAgentCard()} son <b>REST plano</b> en el agente
 * ({@code GET /api/tasks} y {@code GET /.well-known/agent-card.json}). El detalle del transporte es
 * responsabilidad exclusiva del adaptador.
 *
 * <p><b>Propiedad de las tareas.</b> Las tareas del agente son del agente: el BFF no las inventa
 * ni las modifica, solo las recibe y las proyecta hacia el frontend.
 *
 * <p>Todas las operaciones son reactivas y no bloqueantes (DP-08).
 */
public interface AgentGateway {

    /**
     * Envía una orden al agente y devuelve su respuesta junto con la tarea abierta.
     *
     * @param command orden a ejecutar, ya validada
     * @return la respuesta y, si la hay, la tarea que el frontend podrá seguir
     */
    Mono<AgentInteraction> sendMessage(AgentCommand command);

    /**
     * Consulta el estado actual de una tarea del agente.
     *
     * @param taskId identificador de la tarea
     * @return la tarea; error {@code TaskNotFoundException} si el agente no la conoce
     */
    Mono<Task> getTask(String taskId);

    /**
     * Solicita al agente la cancelación de una tarea en curso.
     *
     * @param taskId identificador de la tarea
     * @return la tarea ya cancelada; error {@code TaskNotFoundException} si no existe
     */
    Mono<Task> cancelTask(String taskId);

    /**
     * Lista las tareas que el agente conoce.
     *
     * <p>Corresponde a {@code GET /api/tasks} del agente, que es <b>REST plano, no JSON-RPC</b>.
     * Es la fuente de verdad del panel de progreso del frontend para las tareas de origen
     * {@code AGENT}.
     *
     * @return las tareas del agente; flujo vacío si no hay ninguna
     */
    Flux<Task> listTasks();

    /**
     * Publica las capacidades del agente hacia el frontend.
     *
     * <p>Corresponde a {@code GET /.well-known/agent-card.json} del agente, también REST plano.
     * Sustituye a la llamada que el frontend hacía a esa ruta sin que nadie la sirviera (D-26).
     *
     * @return la tarjeta de presentación del agente
     */
    Mono<AgentCard> getAgentCard();
}

