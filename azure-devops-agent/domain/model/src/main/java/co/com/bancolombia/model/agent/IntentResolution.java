package co.com.bancolombia.model.agent;

import java.util.Map;
import java.util.Optional;

/**
 * Resultado de resolver la intención de un mensaje: qué flujo debe atenderlo y con qué datos.
 *
 * <p>Value Object inmutable. Los parámetros se copian de forma defensiva, de modo que el mapa
 * devuelto nunca puede modificarse desde fuera.
 *
 * @param intent intención detectada; obligatoria
 * @param params datos extraídos del texto del usuario, por ejemplo el identificador de Work Item
 * @see IntentResolver
 */
public record IntentResolution(AgentIntent intent, Map<String, String> params) {

    /** Clave del identificador de Work Item dentro de {@link #params()}. */
    public static final String PARAM_WORK_ITEM_ID = "workItemId";

    public IntentResolution {
        if (intent == null) {
            throw new IllegalArgumentException("La intención resuelta es obligatoria");
        }
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    /**
     * Resolución sin parámetros asociados.
     *
     * @param intent intención detectada
     * @return la resolución correspondiente
     */
    public static IntentResolution of(AgentIntent intent) {
        return new IntentResolution(intent, Map.of());
    }

    /**
     * Resolución que arrastra el identificador de Work Item extraído del texto.
     *
     * @param intent     intención detectada
     * @param workItemId identificador del Work Item; obligatorio y no vacío
     * @return la resolución correspondiente
     */
    public static IntentResolution withWorkItem(AgentIntent intent, String workItemId) {
        if (workItemId == null || workItemId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de Work Item es obligatorio para la intención " + intent);
        }
        return new IntentResolution(intent, Map.of(PARAM_WORK_ITEM_ID, workItemId));
    }

    /**
     * Identificador de Work Item asociado a la intención, si la señal estaba presente.
     *
     * @return el identificador, o vacío cuando la intención no lo requiere
     */
    public Optional<String> workItemId() {
        return Optional.ofNullable(params.get(PARAM_WORK_ITEM_ID));
    }
}

