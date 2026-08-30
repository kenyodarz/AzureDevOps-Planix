package co.com.bancolombia.config;

import co.com.bancolombia.mcp.security.McpRoles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.AnonymousSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.AuthorizeExchangeSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;


@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(McpSecurityProperties.class)
public class McpSecurityConfig {

    private static final String ROLE_PREFIX = McpRoles.ROLE_PREFIX;

    /**
     * Sondas de vida. Permanecen abiertas en los <b>dos</b> modos: son las que usan el orquestador
     * de contenedores y el balanceador, que nunca portarán un token.
     */
    private static final String[] PUBLIC_PATHS = {"/actuator/health", "/actuator/info"};

    private final String issuerUri;
    private final String clientId;
    private final String jsonExpRoles;
    private final JsonMapper mapper;

    public McpSecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.client-id}") String clientId,
            @Value("${jwt.json-exp-roles}") String jsonExpRoles, JsonMapper mapper) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.jsonExpRoles = jsonExpRoles;
        this.mapper = mapper;
    }

    /**
     * Construye la cadena de filtros según la postura declarada en {@code mcp.security.mode}.
     *
     * <p>La postura se recibe como parámetro del bean, no del constructor, para no alterar la firma
     * pública que ya consumen las pruebas existentes.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder, McpSecurityProperties securityProperties) {
        logSecurityMode(securityProperties);

        return http
                .csrf(CsrfSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler((exchange, ex) -> exchange.getPrincipal()
                                .map(java.security.Principal::getName)
                                .defaultIfEmpty("anonymous")
                                .flatMap(user -> {
                                    log.error("⛔ Acceso Denegado: {} | Usuario: {} | Path: {}",
                                            ex.getMessage(), user, exchange.getRequest().getPath());
                                    exchange.getResponse()
                                            .setStatusCode(
                                                    org.springframework.http.HttpStatus.FORBIDDEN);
                                    return exchange.getResponse().setComplete();
                                })))
                // Usar configuración de CORS definida en CorsConfig.java (CorsWebFilter)
                // .cors(CorsSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .anonymous(anonymous -> configureAnonymousIdentity(anonymous, securityProperties))
                .authorizeExchange(exchanges -> configureAuthorization(exchanges,
                        securityProperties))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /**
     * Única diferencia observable entre los dos modos a nivel de transporte.
     */
    private void configureAuthorization(AuthorizeExchangeSpec exchanges,
            McpSecurityProperties securityProperties) {
        exchanges.pathMatchers(PUBLIC_PATHS).permitAll();
        if (securityProperties.isEnforced()) {
            exchanges.anyExchange().authenticated();
        } else {
            exchanges.anyExchange().permitAll();
        }
    }

    /**
     * Concede a la identidad anónima los roles de lectura y escritura <b>solo</b> en modo
     * {@code PERMISSIVE}.
     *
     * <p>Es la pieza que permite que las ocho tools lleven su {@code @PreAuthorize} declarado,
     * compilado y probado <b>sin cambiar el comportamiento observable de la POC</b>: quien llama sin
     * token sigue siendo atendido igual que antes, pero por una autorización concedida
     * explícitamente y no por la ausencia de una anotación.
     *
     * <p>En {@code ENFORCED} se deja el anónimo por defecto (solo {@code ROLE_ANONYMOUS}), que no
     * satisface ninguna de las expresiones de las tools.
     */
    private void configureAnonymousIdentity(AnonymousSpec anonymous,
            McpSecurityProperties securityProperties) {
        if (securityProperties.isEnforced()) {
            return;
        }
        anonymous.authorities(
                AuthorityUtils.createAuthorityList(McpRoles.ROLE_READ, McpRoles.ROLE_WRITE));
    }

    /**
     * Deja constancia inequívoca del modo en el arranque: una postura laxa silenciosa es
     * indistinguible de un olvido.
     */
    private void logSecurityMode(McpSecurityProperties securityProperties) {
        if (securityProperties.isEnforced()) {
            log.info(
                    "🔐 MCP Security en modo ENFORCED: toda petición ajena a las sondas del actuator exige un token válido.");
            return;
        }
        log.warn(
                "🔓 MCP Security en modo PERMISSIVE (mcp.security.mode=PERMISSIVE): NO se exige token y la identidad anónima recibe los roles {} y {}. "
                        + "Es la postura deliberada de la POC mientras no exista el Service Principal. Para exigir token exporte MCP_SECURITY_MODE=ENFORCED; no hace falta recompilar.",
                McpRoles.READ, McpRoles.WRITE);
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.info("Configurando JWT Decoder con issuer: {}", issuerUri);

        // Crear el decoder usando el issuer URI
        var jwtDecoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build();

        // 1. Usar validadores por defecto de Spring (timestamp, etc.) pero SIN issuer
        // estricto
        var defaultValidator = JwtValidators.createDefault();

        // 2. Custom Validator para Audience y AppID (flexible para Graph/Azure AD)
        OAuth2TokenValidator<Jwt> customValidator = jwt -> {
            if (clientId == null || clientId.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }

            // Validar Audience ('aud')
            List<String> audience = jwt.getAudience();
            if (audience != null) {
                for (String aud : audience) {
                    if (aud.contains(clientId)) {
                        log.debug("Token aceptado por 'aud': {}", aud);
                        return OAuth2TokenValidatorResult.success();
                    }
                }
            }

            // Validar App ID ('appid') - Fallback
            String appid = jwt.getClaimAsString("appid");
            if (clientId.equals(appid)) {
                log.debug("Token aceptado por 'appid': {}", clientId);
                return OAuth2TokenValidatorResult.success();
            }

            log.warn(
                    "Token rechazado por mismatch. Esperado ClientID: {}. Recibido aud: {}, appid: {}",
                    clientId,
                    audience, appid);
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "El token no está destinado a esta aplicación (aud/appid mismatch)", null));
        };

        // 3. Combinar validadores
        var combinedValidator = new DelegatingOAuth2TokenValidator<>(defaultValidator,
                customValidator);
        jwtDecoder.setJwtValidator(combinedValidator);

        return jwtDecoder;
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> extractRoles(jwt.getClaims()));

        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }

    private Collection<GrantedAuthority> extractRoles(Map<String, Object> claims) {
        try {
            var json = mapper.writeValueAsString(claims);
            var chunk = mapper.readTree(json).at(jsonExpRoles);
            List<String> roles = mapper.readerFor(new TypeReference<List<String>>() {
            }).readValue(chunk);

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                    .collect(Collectors.toList());
        } catch (JacksonException e) {
            log.error("Error extrayendo roles del token JWT: {}", e.getMessage());
            return List.of();
        }
    }
}
