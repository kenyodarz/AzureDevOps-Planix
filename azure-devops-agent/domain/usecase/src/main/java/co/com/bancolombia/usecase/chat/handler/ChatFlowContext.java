package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.IntentResolution;

/**
 * Datos que necesita un flujo de conversación para ejecutarse.
 *
 * <p>Deliberadamente no contiene nada del transporte A2A: construir la {@code SendMessageResponse},
 * medir el tiempo de ejecución y manejar el error siguen siendo responsabilidad del caso de uso.
 *
 * @param userText   texto original escrito por el usuario; nunca vacío
 * @param contextId  identificador de contexto enviado por el cliente; admite nulo
 * @param resolution intención ya resuelta por el dominio, con sus parámetros
 */
public record ChatFlowContext(String userText, String contextId, IntentResolution resolution) {

    public ChatFlowContext {
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("El texto del usuario es obligatorio");
        }
        if (resolution == null) {
            throw new IllegalArgumentException("La resolución de intención es obligatoria");
        }
    }

    /** Intención que debe atender este contexto. */
    public AgentIntent intent() {
        return resolution.intent();
    }

    /**
     * Identificador de Work Item de una intención que, por construcción, siempre lo lleva.
     *
     * @return el identificador
     * @throws IllegalStateException si falta, lo que sería un defecto del resolutor y no una
     *                               entrada inválida del usuario
     */
    public String requireWorkItemId() {
        return resolution.workItemId().orElseThrow(() -> new IllegalStateException(
                "La intención " + intent() + " exige un identificador de Work Item"));
    }
}

