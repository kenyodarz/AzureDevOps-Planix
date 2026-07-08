package co.com.bancolombia.consumer.config;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.netty.http.client.HttpClient;

@Configuration
public class RestConsumerConfig {

    private final String url;
    private final int timeout;
    private final String token;

    public RestConsumerConfig(@Value("${adapter.restconsumer.url}") String url,
                              @Value("${adapter.restconsumer.timeout}") int timeout,
                              @Value("${adapter.restconsumer.token:}") String token) {
        this.url = url;
        this.timeout = timeout;
        this.token = token;
    }

    @Bean
    public WebClient getWebClient(ObjectProvider<Builder> builderProvider) {
        WebClient.Builder builder = builderProvider.getIfAvailable(WebClient::builder);
        WebClient.Builder webClientBuilder = builder
            .baseUrl(url)
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (token != null && !token.isBlank()) {
            String authHeader = token.trim();
            if (!authHeader.toLowerCase().startsWith("basic ")) {
                authHeader = "Basic " + authHeader;
            }
            webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }

        return webClientBuilder
            .clientConnector(getClientHttpConnector())
            .build();
    }

    private ClientHttpConnector getClientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create()
                .compress(true)
                .keepAlive(true)
                .option(CONNECT_TIMEOUT_MILLIS, timeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(timeout, MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(timeout, MILLISECONDS));
                }));
    }

}
