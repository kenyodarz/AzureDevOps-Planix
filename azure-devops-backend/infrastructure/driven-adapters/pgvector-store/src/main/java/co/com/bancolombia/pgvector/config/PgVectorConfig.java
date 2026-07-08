package co.com.bancolombia.pgvector.config;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuración explícita y manual para PostgreSQL con pgvector y el VectorStore de Spring AI.
 * Sigue el patrón establecido en AgentRegistry para consumir el modelo de embeddings local.
 */
@Configuration
public class PgVectorConfig {

    @Value("${spring.ai.vectorstore.pgvector.table-name:planning_chunks}")
    private String tableName;

    /**
     * Bean de EmbeddingModel personalizado que invoca al servicio localmente en
     * http://localhost:4141.
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(
            @Value("${spring.ai.openai.api-key:dummy}") String apiKey,
            @Value("${spring.ai.openai.base-url:http://localhost:4141}") String baseUrl,
            @Value("${spring.ai.openai.embedding.options.model:text-embedding-ada-002}") String modelName,
            RestClient.Builder restClientBuilder) {

        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();

        return new EmbeddingModel() {
            @Override
            public float[] embed(String text) {
                List<float[]> result = embed(List.of(text));
                return result.isEmpty() ? new float[0] : result.getFirst();
            }

            @Override
            public List<float[]> embed(List<String> texts) {
                EmbeddingResponse response = embedForResponse(texts);
                return response.getResults().stream()
                        .map(Embedding::getOutput)
                        .toList();
            }

            @Override
            public float[] embed(Document document) {
                return embed(document.getFormattedContent());
            }

            @Override
            public EmbeddingResponse embedForResponse(List<String> texts) {
                java.util.Map<String, Object> requestBody = java.util.Map.of(
                        "input", texts,
                        "model", modelName
                );

                try {
                    OpenAIEmbeddingResponse response = restClient.post()
                            .uri("/v1/embeddings")
                            .header("Authorization", "Bearer " + apiKey)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(OpenAIEmbeddingResponse.class);

                    if (response == null) {
                        throw new IllegalStateException(
                                "Respuesta vacía del servidor de embeddings local");
                    }

                    List<Embedding> embeddings = response.data().stream()
                            .map(data -> {
                                if (!"embedding".equalsIgnoreCase(data.object())) {
                                    throw new IllegalArgumentException(
                                            "Formato de embedding no soportado: "
                                                    + data.object());
                                }
                                return new Embedding(toFloatArray(data.embedding()), data.index());
                            })
                            .toList();

                    return new EmbeddingResponse(embeddings);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Error llamando al servicio local de embeddings: " + e.getMessage(), e);
                }
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                return embedForResponse(request.getInstructions());
            }
        };
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    /**
     * Bean de JsonMapper de Jackson para mapeo de payloads de metadatos.
     */
    @Bean
    @ConditionalOnMissingBean(JsonMapper.class)
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

    /**
     * VectorStore utilizando la abstracción oficial de PgVectorStore de Spring AI con patrón
     * Builder.
     */
    @Bean
    @Primary
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .initializeSchema(
                        false) // Deshabilitado para crear la tabla de forma controlada vía DDL sql
                .build();
    }

    private record OpenAIEmbeddingResponse(List<EmbeddingData> data, String model) {

    }

    private record EmbeddingData(List<Float> embedding, int index, String object) {

    }
}
