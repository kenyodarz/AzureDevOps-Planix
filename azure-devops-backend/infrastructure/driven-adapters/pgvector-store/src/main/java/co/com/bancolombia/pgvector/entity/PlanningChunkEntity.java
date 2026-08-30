package co.com.bancolombia.pgvector.entity;

import java.util.Map;

/**
 * Forma en que un fragmento de planificación <b>se guarda</b> en pgvector (D-14).
 *
 * <p>Hasta la Fase 07 el adaptador serializaba el modelo de dominio {@code PlanningChunk} con
 * Jackson directamente: no había entidad ni mapper, así que <b>renombrar un campo del dominio
 * cambiaba en silencio el formato almacenado</b> y dejaba ilegibles los registros anteriores. El
 * dominio tiene que poder cambiar sin arrastrar a la base de datos; para eso está esta clase.
 *
 * <p><b>Compatible con lo ya almacenado.</b> Los nombres de los campos son exactamente los que
 * producía la serialización del modelo, de modo que el JSON guardado en la metadata
 * {@code chunk_json} de {@code planning_chunks} <b>no cambia</b>: no hace falta ninguna migración,
 * ninguna columna nueva y ningún consumidor externo se ve afectado (DP-05, confirmado el
 * 2026-08-29). Si algún día hay que cambiar el formato guardado, se cambia <i>aquí</i>, a la vista,
 * y con una migración explícita.
 *
 * @param id           identificador del fragmento
 * @param initiativeId iniciativa a la que pertenece
 * @param sectionName  sección del Markdown de la que se extrajo
 * @param content      texto del fragmento
 * @param metadata     metadatos asociados
 */
public record PlanningChunkEntity(
        String id,
        String initiativeId,
        String sectionName,
        String content,
        Map<String, Object> metadata
) {

}

