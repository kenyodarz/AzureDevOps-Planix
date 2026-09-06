package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Índice de handlers por intención.
 *
 * <p>Valida la exhaustividad <b>al construirse</b>: si falta un handler para alguna intención o hay
 * dos que declaran la misma, el arranque de la aplicación falla. Es preferible un fallo inmediato y
 * ruidoso al arrancar que un {@code NullPointerException} en producción la primera vez que un
 * usuario escribe la frase adecuada.
 */
public final class ChatFlowDispatcher {

    private final Map<AgentIntent, ChatFlowHandler> handlers;

    public ChatFlowDispatcher(Collection<ChatFlowHandler> availableHandlers) {
        this.handlers = index(availableHandlers);
        requireExhaustive(this.handlers);
    }

    /**
     * Ejecuta el handler que corresponde a la intención del contexto.
     *
     * @param context datos de la conversación
     * @return el texto listo para presentar al usuario
     */
    public Mono<String> dispatch(ChatFlowContext context) {
        return handlers.get(context.intent()).handle(context);
    }

    private static Map<AgentIntent, ChatFlowHandler> index(
            Collection<ChatFlowHandler> availableHandlers) {
        if (availableHandlers == null || availableHandlers.isEmpty()) {
            throw new IllegalStateException("No se registró ningún ChatFlowHandler");
        }
        Map<AgentIntent, ChatFlowHandler> indexed = new EnumMap<>(AgentIntent.class);
        for (ChatFlowHandler handler : availableHandlers) {
            ChatFlowHandler previous = indexed.put(handler.supports(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Hay más de un ChatFlowHandler para la intención " + handler.supports());
            }
        }
        return indexed;
    }

    private static void requireExhaustive(Map<AgentIntent, ChatFlowHandler> indexed) {
        List<AgentIntent> missing = new ArrayList<>();
        for (AgentIntent intent : AgentIntent.values()) {
            if (intent == AgentIntent.PROGRAM_PLANNING) {
                // El handler formal (ProgramPlanningFlowHandler) se introduce en Fase 06 y se cablea en Fase 08
                continue;
            }
            if (!indexed.containsKey(intent)) {
                missing.add(intent);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Faltan handlers para las intenciones: " + missing);
        }
    }
}

