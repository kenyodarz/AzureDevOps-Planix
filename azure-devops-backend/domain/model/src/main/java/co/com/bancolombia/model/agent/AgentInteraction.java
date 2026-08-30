package co.com.bancolombia.model.agent;

import co.com.bancolombia.model.a2a.Task;
import java.util.Optional;

/**
 * Resultado de una interacción con el agente: <b>la respuesta y la tarea</b>.
 *
 * <p>Es la pieza que salda la deuda D-23. El puerto anterior, {@code ChatGateway}, devolvía
 * {@code Mono<String>}, de modo que el adaptador se quedaba con el texto y <b>descartaba la
 * {@link Task}</b> que el agente sí devolvía. Sin esa tarea el frontend no podía mostrar el estado
 * de lo que el agente estaba haciendo, y el BFF acababa supliéndolo leyendo la base de datos que
 * ambos servicios compartían.
 *
 * <p>La tarea es opcional: el agente puede responder a una pregunta conversacional sin abrir
 * ninguna tarea.
 *
 * @param task  tarea abierta o actualizada por el agente; puede ser {@code null}
 * @param reply texto de la respuesta, ya libre de bloques de razonamiento; nunca {@code null}
 */
public record AgentInteraction(Task task, String reply) {

    public AgentInteraction {
        reply = reply != null ? reply : "";
    }

    /**
     * Interacción sin tarea asociada, para respuestas puramente conversacionales.
     *
     * @param reply texto de la respuesta
     * @return la interacción
     */
    public static AgentInteraction replyOnly(String reply) {
        return new AgentInteraction(null, reply);
    }

    /**
     * Indica si el agente abrió o actualizó una tarea que el frontend pueda seguir.
     *
     * @return {@code true} si hay tarea con identificador
     */
    public boolean hasTask() {
        return task != null && task.getId() != null;
    }

    /**
     * Tarea asociada, si la hay.
     *
     * @return la tarea envuelta en {@link Optional}
     */
    public Optional<Task> taskAsOptional() {
        return hasTask() ? Optional.of(task) : Optional.empty();
    }
}

