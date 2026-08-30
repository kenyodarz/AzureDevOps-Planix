package co.com.bancolombia.api.dto.task;

/**
 * Respuesta de {@code POST /api/chat/messages}.
 *
 * <p>Devuelve las dos cosas que el frontend necesita de una sola vez: el texto para pintar en el
 * chat y la tarea que debe empezar a seguir. Es la materialización, en el contrato HTTP, de la
 * deuda D-23: antes la tarea se perdía dentro del BFF y el frontend tenía que adivinarla leyendo la
 * base de datos compartida.
 *
 * @param reply texto de la respuesta del agente; nunca nulo, puede ser vacío
 * @param task  tarea abierta por el agente; {@code null} en respuestas puramente conversacionales
 */
public record ChatMessageResponse(String reply, TaskResponse task) {

}

