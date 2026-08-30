package co.com.bancolombia.consumer.config;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Configuration
public class RestConsumerConfig {

    private static final String BASIC_PREFIX = "Basic ";
    private static final String TOKEN_PROPERTY = "adapter.restconsumer.token";
    private static final String EXPECTED_FORMAT =
            "Se espera Base64(\":\" + PAT): usuario vacio, dos puntos y el Personal Access Token.";
    private static final String ENFORCED_MODE = "ENFORCED";

    private final String url;
    private final int timeout;
    private final String token;

    /**
     * @param token credencial de Azure DevOps. <b>El formato no cambia</b> (decisión B-01): la
     *              propiedad recibe el valor <b>ya codificado en Base64</b>, y este adaptador se
     *              limita a anteponer el prefijo {@code Basic }; no codifica nada. El valor
     *              esperado es {@code Base64(":" + PAT)} — usuario <b>vacío</b>, dos puntos y el
     *              Personal Access Token. El PAT <b>no</b> es la fusión de dos claves: el usuario
     *              se ignora, y por eso {@code cualquiercosa:PAT} también funciona. Nunca se
     *              versiona: llega por variable de entorno o secreto (DP-02).
     * @param securityMode postura declarada en {@code mcp.security.mode}. Determina si un token
     *                     ausente o malformado rompe el arranque o solo deja un aviso.
     */
    public RestConsumerConfig(@Value("${adapter.restconsumer.url}") String url,
                              @Value("${adapter.restconsumer.timeout}") int timeout,
                              @Value("${adapter.restconsumer.token:}") String token,
                              @Value("${mcp.security.mode:PERMISSIVE}") String securityMode) {
        this.url = url;
        this.timeout = timeout;
        this.token = token;
        validateToken(token, securityMode);
    }

    /**
     * Comprueba al arranque lo que antes no comprobaba nadie.
     *
     * <p>La propiedad traía por defecto el literal {@code your-token-here}, así que la aplicación
     * arrancaba siempre y fallaba mucho después con un 401 opaco de Azure DevOps, indistinguible de
     * un problema de permisos. Aquí el diagnóstico se emite en el sitio y en el momento correctos.
     *
     * <p>En {@code ENFORCED} un valor ausente o malformado <b>rompe el arranque</b>; en
     * {@code PERMISSIVE} solo deja un aviso, para no alterar el arranque de la POC ni de los
     * entornos de desarrollo.
     */
    private void validateToken(String rawToken, String securityMode) {
        boolean enforced = ENFORCED_MODE.equalsIgnoreCase(String.valueOf(securityMode).trim());

        if (rawToken == null || rawToken.isBlank()) {
            reportInvalidToken(enforced,
                    "La propiedad " + TOKEN_PROPERTY + " esta vacia. " + EXPECTED_FORMAT);
            return;
        }

        String candidate = stripBasicPrefix(rawToken.trim());
        if (!isBase64ContainingColon(candidate)) {
            reportInvalidToken(enforced, "La propiedad " + TOKEN_PROPERTY
                    + " no es Base64 valido o su contenido no incluye ':'. " + EXPECTED_FORMAT
                    + " Un PAT crudo en esta propiedad produce un 401 mudo.");
            return;
        }
        log.info("Credencial de Azure DevOps validada al arranque: formato correcto.");
    }

    private String stripBasicPrefix(String value) {
        if (value.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            return value.substring(BASIC_PREFIX.length()).trim();
        }
        return value;
    }

    private boolean isBase64ContainingColon(String candidate) {
        try {
            String decoded = new String(Base64.getDecoder().decode(candidate),
                    StandardCharsets.UTF_8);
            return decoded.contains(":");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void reportInvalidToken(boolean enforced, String message) {
        if (enforced) {
            throw new IllegalStateException(message);
        }
        log.warn("⚠️ {} El arranque continua porque mcp.security.mode=PERMISSIVE.", message);
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
                authHeader = BASIC_PREFIX + authHeader;
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
