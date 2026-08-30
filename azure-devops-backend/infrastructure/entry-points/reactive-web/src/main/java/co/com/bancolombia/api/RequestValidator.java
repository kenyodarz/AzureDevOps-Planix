package co.com.bancolombia.api;

import java.util.Map;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Validación de la entrada HTTP, dentro de la cadena reactiva.
 *
 * <p>Es la misión declarada del BFF y hasta la Fase 03 no se cumplía (deuda D-28): un
 * {@code maxResults} no numérico reventaba en {@code Integer.parseInt} <b>antes</b> de entrar en la
 * cadena, de modo que el {@code onErrorResume} del handler no lo capturaba y la excepción escapaba
 * como error genérico 500 en vez de un 400 con mensaje útil.
 *
 * <p>Todos los métodos lanzan {@link IllegalArgumentException}, que el mapa de errores traduce a
 * <b>400</b>. Se lanza en vez de devolver un {@code Mono.error} para que el fallo también quede
 * capturado cuando la validación ocurre al construir la cadena.
 */
public final class RequestValidator {

    private static final int MIN_RESULTS = 1;

    private RequestValidator() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Valida y convierte {@code maxResults}.
     *
     * @param request      petición entrante
     * @param defaultValue valor a aplicar si el parámetro no viene
     * @return el número de resultados solicitado
     * @throws IllegalArgumentException si no es numérico o no es positivo
     */
    public static int maxResults(ServerRequest request, int defaultValue) {
        String raw = request.queryParam("maxResults").orElse(null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El parámetro maxResults debe ser un número entero: " + raw, e);
        }
        if (parsed < MIN_RESULTS) {
            throw new IllegalArgumentException(
                    "El parámetro maxResults debe ser mayor que cero: " + parsed);
        }
        return parsed;
    }

    /**
     * Exige que un texto obligatorio venga informado.
     *
     * @param value     valor recibido
     * @param fieldName nombre del campo, para el mensaje de error
     * @return el valor sin espacios sobrantes
     * @throws IllegalArgumentException si es nulo o está en blanco
     */
    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El campo " + fieldName + " es obligatorio");
        }
        return value.trim();
    }

    /**
     * Extrae y valida el texto del cuerpo de {@code POST /api/chat/messages}.
     *
     * <p>El cuerpo que envía el frontend tiene la forma
     * {@code { "message": { "contextId": "...", "parts": [ { "text": "..." } ] } }}.
     *
     * @param message bloque {@code message} del cuerpo
     * @return el texto del primer {@code part} que lo tenga
     * @throws IllegalArgumentException si no hay {@code parts} o ninguno tiene texto
     */
    public static String requirePromptText(Map<String, Object> message) {
        if (message == null) {
            throw new IllegalArgumentException("El campo message es obligatorio");
        }
        if (!(message.get("parts") instanceof java.util.List<?> parts) || parts.isEmpty()) {
            throw new IllegalArgumentException("El campo message.parts es obligatorio");
        }
        for (Object part : parts) {
            if (part instanceof Map<?, ?> rawPart && rawPart.get("text") instanceof String text
                    && !text.isBlank()) {
                return text;
            }
        }
        throw new IllegalArgumentException("El mensaje no contiene texto que enviar al agente");
    }
}

