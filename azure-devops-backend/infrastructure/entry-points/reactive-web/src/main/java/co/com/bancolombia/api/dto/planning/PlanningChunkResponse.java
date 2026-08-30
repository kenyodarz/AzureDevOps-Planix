package co.com.bancolombia.api.dto.planning;

import java.util.Map;

/**
 * Fragmento de planificación tal como lo publica el BFF por HTTP.
 *
 * <p>Frontera de salida de {@code GET /api/planning/search} y de
 * {@code GET /api/planning/initiatives/&#123;id&#125;/chunks} (D-15). Hasta la Fase 07 esas dos
 * rutas devolvían el modelo de dominio {@code PlanningChunk} <b>directamente</b>: cualquier
 * renombrado en el dominio rompía el frontend en silencio.
 *
 * <p><b>El JSON no cambia:</b> los nombres y el orden de los campos son exactamente los del
 * modelo, así que el contrato con el frontend queda intacto. Lo que cambia es que ahora hay un
 * sitio donde decidirlo.
 *
 * @param id           identificador del fragmento
 * @param initiativeId iniciativa a la que pertenece
 * @param sectionName  sección del Markdown de la que se extrajo
 * @param content      texto del fragmento
 * @param metadata     metadatos asociados al fragmento
 */
public record PlanningChunkResponse(
        String id,
        String initiativeId,
        String sectionName,
        String content,
        Map<String, Object> metadata
) {

}

