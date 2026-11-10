package co.com.bancolombia.mcpclient.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

class OpenAiClientCustomizerConfigTest {

    private final OpenAiClientCustomizerConfig config = new OpenAiClientCustomizerConfig();

    @Test
    void shouldRegisterInterceptor() {
        var customizer = config.openAiHttpClientBuilderCustomizer();
        assertNotNull(customizer);
    }

    @Test
    void shouldInjectMissingIndexInChoices() throws IOException {
        var interceptor = new OpenAiClientCustomizerConfig.ChoicesIndexSanitizerInterceptor();
        var chain = mock(Interceptor.Chain.class);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .build();

        String rawJsonWithoutIndex = "{\"id\":\"chatcmpl-123\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hello\"},\"finish_reason\":\"stop\"}]}";

        Response originalResponse = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(rawJsonWithoutIndex, MediaType.get("application/json")))
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(originalResponse);

        Response interceptedResponse = interceptor.intercept(chain);

        assertNotNull(interceptedResponse.body());
        String body = interceptedResponse.body().string();
        assertTrue(body.contains("\"index\":0"), "Expected injected index field: " + body);
    }

    @Test
    void shouldPreserveExistingIndexInChoices() throws IOException {
        var interceptor = new OpenAiClientCustomizerConfig.ChoicesIndexSanitizerInterceptor();
        var chain = mock(Interceptor.Chain.class);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .build();

        String rawJsonWithIndex = "{\"id\":\"chatcmpl-123\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"hello\"},\"finish_reason\":\"stop\"}]}";

        Response originalResponse = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(rawJsonWithIndex, MediaType.get("application/json")))
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(originalResponse);

        Response interceptedResponse = interceptor.intercept(chain);

        assertNotNull(interceptedResponse.body());
        String body = interceptedResponse.body().string();
        assertTrue(body.contains("\"index\":0"));
    }

    @Test
    void shouldHandleNonSuccessfulOrNonJsonResponseGracefully() throws IOException {
        var interceptor = new OpenAiClientCustomizerConfig.ChoicesIndexSanitizerInterceptor();
        var chain = mock(Interceptor.Chain.class);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .build();

        Response errorResponse = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Server Error")
                .body(ResponseBody.create("Internal Server Error", MediaType.get("text/plain")))
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(errorResponse);

        Response interceptedResponse = interceptor.intercept(chain);

        assertEquals(500, interceptedResponse.code());
        assertEquals("Internal Server Error", interceptedResponse.body().string());
    }
}
