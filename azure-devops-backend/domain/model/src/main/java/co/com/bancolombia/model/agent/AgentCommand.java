package co.com.bancolombia.model.agent;

import java.util.Objects;
import java.util.UUID;

/**
 * Orden que el BFF envía al agente para que ejecute una tarea.
 *
 * <p>Value Object inmutable: valida en construcción, de modo que un comando mal formado no llega
 * nunca al adaptador. Sustituye a la pareja de {@code String} sueltos ({@code prompt},
 * {@code sessionKey}) que viajaba por el antiguo {@code ChatGateway}.
 *
 * <p><b>Modo no bloqueante por defecto (DP-08).</b> El sistema es reactivo de extremo a extremo:
 * el BFF no espera a que el agente termine, sino que recibe la tarea en curso y deja que el front
 * siga su progreso. Por eso {@link #blocking()} es {@code false} salvo petición explícita.
 *
 * @param prompt    instrucción para el agente; obligatoria y no vacía
 * @param contextId agrupa los mensajes de una misma conversación o sesión; obligatorio
 * @param messageId identificador único de este mensaje
 * @param blocking  si el agente debe responder solo al terminar; {@code false} por defecto
 */
public record AgentCommand(String prompt, String contextId, String messageId, boolean blocking) {

    public AgentCommand {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("El prompt para el agente no puede estar vacío");
        }
        if (contextId == null || contextId.isBlank()) {
            throw new IllegalArgumentException(
                    "El contextId es obligatorio para agrupar la sesión");
        }
        messageId = Objects.requireNonNullElseGet(messageId, () -> UUID.randomUUID().toString());
    }

    /**
     * Crea un comando no bloqueante, que es el modo estándar del sistema (DP-08).
     *
     * @param prompt    instrucción para el agente
     * @param contextId sesión sobre la que agrupar la tarea
     * @return el comando listo para enviar
     */
    public static AgentCommand nonBlocking(String prompt, String contextId) {
        return new AgentCommand(prompt, contextId, null, false);
    }

    /**
     * Crea un comando bloqueante. Reservado para los flujos que necesitan la respuesta completa en
     * la misma llamada, como la auditoría del backlog, que consume el texto del agente de
     * inmediato.
     *
     * @param prompt    instrucción para el agente
     * @param contextId sesión sobre la que agrupar la tarea
     * @return el comando listo para enviar
     */
    public static AgentCommand blocking(String prompt, String contextId) {
        return new AgentCommand(prompt, contextId, null, true);
    }
}

