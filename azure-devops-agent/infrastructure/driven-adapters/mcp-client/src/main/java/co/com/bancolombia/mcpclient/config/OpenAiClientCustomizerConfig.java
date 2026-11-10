package co.com.bancolombia.mcpclient.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customizer for the OpenAI OkHttpClient used by Spring AI.
 * <p>
 * Ensures compatibility with proxies and gateways (such as LiteLLM, local gateways, or non-OpenAI
 * model providers) that may omit the required 'index' field in ChatCompletion choices.
 */
@Slf4j
@Configuration
public class OpenAiClientCustomizerConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHOICES_KEY = "choices";
    private static final String INDEX_KEY = "index";

    @Bean
    public OpenAiHttpClientBuilderCustomizer openAiHttpClientBuilderCustomizer() {
        log.info("=== Registering ChoicesIndexSanitizerInterceptor for OpenAI HTTP Client ===");
        return builder -> builder.interceptor(new ChoicesIndexSanitizerInterceptor());
    }

    static class ChoicesIndexSanitizerInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);

            ResponseBody body = response.body();
            if (!shouldProcess(response, body)) {
                return response;
            }

            MediaType contentType = body.contentType();
            String bodyString = body.string();
            if (shouldSkipBody(bodyString)) {
                return buildResponse(response, bodyString, contentType);
            }

            String finalBodyString = sanitizeJsonPayload(bodyString);
            return buildResponse(response, finalBodyString, contentType);
        }

        private boolean shouldProcess(Response response, ResponseBody body) {
            if (!response.isSuccessful() || body == null) {
                return false;
            }
            MediaType contentType = body.contentType();
            return contentType == null || contentType.toString().contains("json");
        }

        private boolean shouldSkipBody(String bodyString) {
            return bodyString.isBlank() || !bodyString.contains(CHOICES_KEY);
        }

        private String sanitizeJsonPayload(String bodyString) {
            try {
                JsonNode rootNode = OBJECT_MAPPER.readTree(bodyString);
                boolean modified = sanitizeChoices(rootNode);
                if (modified) {
                    log.info("✅ Choices sanitized in OpenAI response: injected missing '{}' field",
                            INDEX_KEY);
                    return OBJECT_MAPPER.writeValueAsString(rootNode);
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to parse/sanitize OpenAI JSON response: {}", e.getMessage());
            }
            return bodyString;
        }

        private boolean sanitizeChoices(JsonNode rootNode) {
            if (!rootNode.has(CHOICES_KEY) || !rootNode.get(CHOICES_KEY).isArray()) {
                return false;
            }
            ArrayNode choices = (ArrayNode) rootNode.get(CHOICES_KEY);
            boolean modified = false;
            for (int i = 0; i < choices.size(); i++) {
                if (sanitizeChoice(choices.get(i), i)) {
                    modified = true;
                }
            }
            return modified;
        }

        private boolean sanitizeChoice(JsonNode choice, int index) {
            if (choice.isObject() && (!choice.has(INDEX_KEY) || choice.get(INDEX_KEY).isNull())) {
                ((ObjectNode) choice).put(INDEX_KEY, index);
                return true;
            }
            return false;
        }

        private Response buildResponse(Response originalResponse, String bodyString,
                MediaType contentType) {
            ResponseBody newBody = ResponseBody.create(bodyString, contentType);
            return originalResponse.newBuilder().body(newBody).build();
        }
    }
}
