package co.com.bancolombia.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.com.bancolombia.config.McpSecurityProperties.SecurityMode;
import co.com.bancolombia.mcp.security.McpRoles;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifica que la postura de acceso sea <b>configuración y no código</b>: el mismo objeto de
 * configuración produce dos comportamientos distintos según {@code mcp.security.mode}, sin
 * recompilar nada.
 *
 * <p>La prueba monta la cadena de filtros real que devuelve {@link McpSecurityConfig} sobre un
 * manejador trivial, de modo que lo que se mide es exactamente lo que se despliega.
 */
class McpSecurityModeTest {

    private static final String ANY_PROTECTED_PATH = "/mcp/McpAzureDevOps";

    @Test
    @DisplayName("GIVEN modo PERMISSIVE WHEN se llama sin token THEN responde 200 (comportamiento historico)")
    void givenPermissiveMode_whenRequestWithoutToken_thenAllowed() {
        // Arrange (GIVEN) + Act (WHEN)
        WebTestClient client = clientFor(SecurityMode.PERMISSIVE, okHandler());

        // Assert (THEN)
        client.get().uri(ANY_PROTECTED_PATH).exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("GIVEN modo ENFORCED WHEN se llama sin token THEN responde 401")
    void givenEnforcedMode_whenRequestWithoutToken_thenUnauthorized() {
        // Arrange (GIVEN) + Act (WHEN)
        WebTestClient client = clientFor(SecurityMode.ENFORCED, okHandler());

        // Assert (THEN)
        client.get().uri(ANY_PROTECTED_PATH).exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GIVEN cualquier modo WHEN se llama a las sondas del actuator THEN siguen abiertas")
    void givenAnyMode_whenActuatorProbes_thenAlwaysOpen() {
        // Arrange (GIVEN)
        WebTestClient permissive = clientFor(SecurityMode.PERMISSIVE, okHandler());
        WebTestClient enforced = clientFor(SecurityMode.ENFORCED, okHandler());

        // Act (WHEN) & Assert (THEN)
        permissive.get().uri("/actuator/health").exchange().expectStatus().isOk();
        enforced.get().uri("/actuator/health").exchange().expectStatus().isOk();
        enforced.get().uri("/actuator/info").exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("GIVEN modo PERMISSIVE WHEN no hay token THEN la identidad anonima porta los dos roles")
    void givenPermissiveMode_whenNoToken_thenAnonymousCarriesBothRoles() {
        // Arrange (GIVEN)
        WebTestClient client = clientFor(SecurityMode.PERMISSIVE, authoritiesHandler());

        // Act (WHEN)
        byte[] body = client.get().uri(ANY_PROTECTED_PATH).exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        // Assert (THEN)
        String authorities = new String(body == null ? new byte[0] : body, StandardCharsets.UTF_8);
        assertEquals(McpRoles.ROLE_READ + "," + McpRoles.ROLE_WRITE, sorted(authorities));
    }

    @Test
    @DisplayName("GIVEN modo ENFORCED WHEN no hay token THEN no se concede ninguna identidad con roles")
    void givenEnforcedMode_whenNoToken_thenNoRolesGranted() {
        // Arrange (GIVEN)
        WebTestClient client = clientFor(SecurityMode.ENFORCED, authoritiesHandler());

        // Act (WHEN) & Assert (THEN): el filtro corta antes de llegar al manejador
        client.get().uri(ANY_PROTECTED_PATH).exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class).value(body ->
                        assertEquals(true, body == null || !body.contains(McpRoles.ROLE_WRITE)));
    }

    private String sorted(String csv) {
        return java.util.Arrays.stream(csv.split(","))
                .filter(value -> !value.isBlank())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private WebTestClient clientFor(SecurityMode mode, WebHandler handler) {
        McpSecurityConfig config = new McpSecurityConfig(
                "https://issuer.test", "test-client-id", "/roles", new JsonMapper());
        ReactiveJwtDecoder decoder = token -> Mono.error(new BadJwtException("token de prueba"));
        SecurityWebFilterChain chain = config.securityWebFilterChain(
                ServerHttpSecurity.http(), decoder, new McpSecurityProperties(mode));

        return WebTestClient.bindToWebHandler(handler)
                .webFilter(new WebFilterChainProxy(chain))
                .build();
    }

    private WebHandler okHandler() {
        return exchange -> {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        };
    }

    /**
     * Devuelve en el cuerpo las autoridades efectivas de quien llama, que es la única forma
     * fiable de comprobar la concesión anónima del modo {@code PERMISSIVE}.
     */
    private WebHandler authoritiesHandler() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(authentication -> authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))
                .defaultIfEmpty("")
                .flatMap(authorities -> write(exchange, authorities));
    }

    private Mono<Void> write(ServerWebExchange exchange, String body) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8))));
    }
}

