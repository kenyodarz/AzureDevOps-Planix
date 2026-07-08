package co.com.bancolombia.pgvector;

import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.pgvector.exceptions.PlanningVectorStoreException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador que conecta el puerto PlanningVectorStorePort con la abstracción de VectorStore de
 * Spring AI.
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class PgVectorPlanningAdapter implements PlanningVectorStorePort {

    private static final String CHUNK_JSON_KEY = "chunk_json";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    @Value("${spring.ai.vectorstore.pgvector.table-name:planning_chunks}")
    private String tableName;

    @Value("${spring.ai.vectorstore.pgvector.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Override
    public Mono<Void> saveChunks(List<PlanningChunk> chunks) {
        return Mono.fromRunnable(() -> {
                    log.info("Persistiendo {} fragmentos vectoriales en la tabla: {}", chunks.size(),
                            tableName);
                    List<Document> documents = chunks.stream()
                            .map(chunk -> {
                                // Inyectar initiative_id directamente en metadata para búsquedas filtradas
                                Map<String, Object> metadata = Map.of(
                                        "initiative_id", chunk.initiativeId(),
                                        "section_name", chunk.sectionName(),
                                        CHUNK_JSON_KEY, serializeChunk(chunk)
                                );
                                return new Document(chunk.id(), chunk.content(), metadata);
                            })
                            .toList();

                    try {
                        vectorStore.add(documents);
                        log.info("Fragmentos persistidos exitosamente en pgvector.");
                    } catch (Exception e) {
                        log.error("Error al persistir fragmentos en pgvector", e);
                        throw new PlanningVectorStoreException("Fallo al guardar vectores en pgvector", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Flux<PlanningChunk> searchSimilarity(String query, String initiativeId, int maxResults) {
        return Mono.fromCallable(() -> {
                    log.info("Ejecutando similitud para la iniciativa: {} con query: {}", initiativeId,
                            query);

                    org.springframework.ai.vectorstore.SearchRequest.Builder builder = SearchRequest.builder()
                            .query(query)
                            .similarityThreshold(similarityThreshold)
                            .topK(maxResults);

                    if (initiativeId != null && !initiativeId.trim().isEmpty()) {
                        Filter.Expression filterExpression = new Filter.Expression(
                                Filter.ExpressionType.EQ,
                                new Filter.Key("initiative_id"),
                                new Filter.Value(initiativeId)
                        );
                        builder.filterExpression(filterExpression);
                    }

                    SearchRequest searchRequest = builder.build();

                    try {
                        List<Document> documents = vectorStore.similaritySearch(searchRequest);
                        return documents.stream()
                                .map(this::deserializeChunk)
                                .toList();
                    } catch (Exception e) {
                        log.error("Error consultando similitud semántica en pgvector", e);
                        throw new PlanningVectorStoreException("Fallo al realizar la búsqueda vectorial",
                                e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    @SuppressWarnings("java:S2077")
    public Mono<Void> deleteInitiative(String initiativeId) {
        return Mono.fromRunnable(() -> {
                    validateTableName();
                    // Eliminación directa de todos los registros de una iniciativa usando su metadata JSONB
                    String sql = String.format("DELETE FROM %s WHERE metadata->>'initiative_id' = ?",
                            tableName);
                    try {
                        int deletedRows = jdbcTemplate.update(sql, initiativeId);
                        log.info("Eliminados {} fragmentos previos para la iniciativa: {}", deletedRows,
                                initiativeId);
                    } catch (Exception e) {
                        log.error("Error al limpiar fragmentos de iniciativa en la tabla {}", tableName, e);
                        throw new PlanningVectorStoreException(
                                "Fallo al limpiar la iniciativa en base de datos", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    @SuppressWarnings("java:S2077")
    public Flux<Map<String, Object>> findAllInitiatives() {
        return Mono.fromCallable(() -> {
                    validateTableName();
                    String sql = String.format("SELECT DISTINCT " +
                            "metadata->>'initiative_id' as initiative_id, " +
                            "metadata->>'initiative_title' as initiative_title, " +
                            "metadata->>'cell' as cell " +
                            "FROM %s", tableName);
                    try {
                        return jdbcTemplate.queryForList(sql);
                    } catch (Exception e) {
                        log.error("Error al obtener iniciativas de la tabla {}", tableName, e);
                        throw new PlanningVectorStoreException(
                                "Fallo al obtener iniciativas en base de datos", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    @SuppressWarnings("java:S2077")
    public Mono<Void> updateInitiativeCell(String initiativeId, String cell) {
        return Mono.fromRunnable(() -> {
                    validateTableName();
                    String sql = String.format("UPDATE %s " +
                            "SET metadata = jsonb_set(metadata, '{cell}', CAST(? AS jsonb)) " +
                            "WHERE metadata->>'initiative_id' = ?", tableName);
                    try {
                        String cellJson = "\"" + cell + "\"";
                        int updatedRows = jdbcTemplate.update(sql, cellJson, initiativeId);
                        log.info("Actualizada propiedad cell para {} fragmentos de la iniciativa: {}", updatedRows,
                                initiativeId);
                    } catch (Exception e) {
                        log.error("Error al actualizar la celula de iniciativa en la tabla {}", tableName, e);
                        throw new PlanningVectorStoreException(
                                "Fallo al actualizar la celula en base de datos", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    @SuppressWarnings("java:S2077")
    public Flux<PlanningChunk> findChunksByInitiative(String initiativeId) {
        return Mono.fromCallable(() -> {
                    validateTableName();
                    String sql = String.format(
                            "SELECT metadata->>'" + CHUNK_JSON_KEY + "' as " + CHUNK_JSON_KEY
                                    + " FROM %s WHERE metadata->>'initiative_id' = ?",
                            tableName);
                    try {
                        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, initiativeId);
                        return rows.stream()
                                .map(row -> {
                                    String chunkJsonValue = (String) row.get(CHUNK_JSON_KEY);
                                    try {
                                        return jsonMapper.readValue(chunkJsonValue, PlanningChunk.class);
                                    } catch (Exception e) {
                                        throw new IllegalStateException(
                                                "Error al deserializar PlanningChunk desde la base de datos",
                                                e);
                                    }
                                })
                                .toList();
                    } catch (Exception e) {
                        log.error("Error al obtener chunks para la iniciativa {} en la tabla {}",
                                initiativeId, tableName, e);
                        throw new PlanningVectorStoreException(
                                "Fallo al obtener chunks en base de datos", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    private void validateTableName() {
        if (tableName == null || !tableName.matches("^[a-zA-Z_]\\w*$")) {
            throw new IllegalArgumentException(
                    "Nombre de tabla inválido o potencialmente inseguro: " + tableName);
        }
    }

    private String serializeChunk(PlanningChunk chunk) {
        try {
            return jsonMapper.writeValueAsString(chunk);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al serializar el chunk a JSON string", e);
        }
    }

    private PlanningChunk deserializeChunk(Document document) {
        Object chunkJson = document.getMetadata().get(CHUNK_JSON_KEY);
        if (!(chunkJson instanceof String chunkJsonValue)) {
            throw new IllegalStateException(
                    "Metadata '" + CHUNK_JSON_KEY + "' no encontrada en el documento vectorial");
        }
        try {
            return jsonMapper.readValue(chunkJsonValue, PlanningChunk.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al deserializar PlanningChunk desde base de datos", e);
        }
    }
}
