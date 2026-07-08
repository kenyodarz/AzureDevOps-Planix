package co.com.bancolombia.api.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@SuppressWarnings("java:S5689")
public class SecurityHeadersConfig implements WebFilter {

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self' 'unsafe-inline'; frame-ancestors 'self'; form-action 'self'");
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set(HttpHeaders.SERVER, "");
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        return chain.filter(exchange);
    }
}
