package co.com.bancolombia.mcpclient.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(McpConnectionHeadersProperties.class)
public class OAuth2WebClientConfig {

    private final McpConnectionHeadersProperties connectionHeadersProperties;

    /**
     * OAuth2 Authorized Client Manager for client_credentials flow WITHOUT web context.
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {

        log.info("=== Configuring OAuth2 Authorized Client Manager (Service-based) ===");

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
                .builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        log.info("✅ OAuth2 client_credentials flow configured (no web context required)");
        return authorizedClientManager;
    }

    /**
     * WebClient.Builder bean that Spring AI MCP will use automatically. This builder includes
     * OAuth2 support and dynamically injects extra headers per MCP server, as declared in:
     * <pre>{@code
     * mcp:
     *   connection-headers:
     *     "http://server-url": { header-name: header-value }
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${spring.security.oauth2.client.registration.mcp-server.client-id:your-client-id}") String clientId) {
        log.info("=== Configuring WebClient.Builder for MCP ===");

        Map<String, Map<String, String>> headersMap = connectionHeadersProperties.getConnectionHeaders();
        logConnectionHeaders(headersMap);

        WebClient.Builder builder = WebClient.builder();
        builder.codecs(
                codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10 MB buffer

        // Omitimos el filtro OAuth2 si no hay credenciales de Entra ID configuradas
        boolean isSecurityEnabled =
                clientId != null && !clientId.isBlank() && !"your-client-id".equals(clientId);

        if (isSecurityEnabled) {
            log.info("🔒 OAuth2 Security habilitado para MCP Client. Client ID: {}", clientId);
            ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                    authorizedClientManager);
            oauth2.setDefaultClientRegistrationId("mcp-server");
            builder.filter(oauth2);
        } else {
            log.warn(
                    "⚠️ OAuth2 Security DESHABILITADO para MCP Client en entorno local (ClientID de prueba detectado)");
        }
        builder
                .filter(injectExtraHeadersFilter(headersMap))
                .filter((request, next) -> {
                    log.debug("MCP Request: {} {}", request.method(), request.url());
                    return next.exchange(request)
                            .map(response -> normalizeMcpPlainTextAcceptedResponse(
                                    request.method().name(), response))
                            .doOnNext(response -> log.debug("MCP Response: {}",
                                    response.statusCode()));
                });

        log.info("✅ WebClient.Builder configured with OAuth2");
        log.info("   Registration ID: mcp-server");
        log.info("   Token will be sent as: Authorization: Bearer <token>");
        log.info("   Spring AI MCP will use this builder automatically");

        return builder;
    }

    private void logConnectionHeaders(Map<String, Map<String, String>> headersMap) {
        if (headersMap.isEmpty()) {
            log.info("   No extra MCP connection headers configured (mcp.connection-headers)");
        } else {
            headersMap.forEach((url, headers) ->
                    log.info("   Extra headers for [{}]: {}", url, headers.keySet()));
        }
    }

    private ExchangeFilterFunction injectExtraHeadersFilter(
            Map<String, Map<String, String>> headersMap) {
        return (request, next) -> {
            String requestUrl = request.url().toString();
            Map<String, String> extraHeaders = findExtraHeaders(requestUrl,
                    request.url().getAuthority(), headersMap);

            if (extraHeaders != null && !extraHeaders.isEmpty()) {
                log.debug("Injecting extra headers {} for MCP request: {}",
                        extraHeaders.keySet(), requestUrl);
                ClientRequest.Builder reqBuilder = ClientRequest.from(request);
                extraHeaders.forEach(reqBuilder::header);
                return next.exchange(reqBuilder.build());
            }
            return next.exchange(request);
        };
    }

    private Map<String, String> findExtraHeaders(String requestUrl, String hostPort,
            Map<String, Map<String, String>> headersMap) {
        String cleanHostPort =
                hostPort != null ? hostPort.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : "";
        return headersMap.entrySet().stream()
                .filter(e -> {
                    String key = e.getKey();
                    if (requestUrl.startsWith(key)) {
                        return true;
                    }
                    if (cleanHostPort.isEmpty()) {
                        return false;
                    }
                    String cleanKey = key.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    return cleanKey.contains(cleanHostPort);
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient.Builder chatClientBuilder(
            org.springframework.ai.chat.model.ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    private ClientResponse normalizeMcpPlainTextAcceptedResponse(String method,
            ClientResponse response) {
        var contentType = response.headers().contentType();
        boolean isCompatibleTextPlain = contentType
                .map(type -> type.isCompatibleWith(MediaType.TEXT_PLAIN))
                .orElse(false);
        boolean isAcceptedPost = "POST".equals(method) && response.statusCode().is2xxSuccessful();

        if (isAcceptedPost && isCompatibleTextPlain) {
            return response.mutate()
                    .headers(headers -> headers.remove(HttpHeaders.CONTENT_TYPE))
                    .build();
        }
        return response;
    }
}
