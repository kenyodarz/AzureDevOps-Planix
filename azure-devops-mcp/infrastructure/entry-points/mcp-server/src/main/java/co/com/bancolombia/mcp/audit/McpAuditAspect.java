package co.com.bancolombia.mcp.audit;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Aspecto para auditar el uso de Tools, Resources y Prompts MCP
 * <p>
 * Este aspecto intercepta todas las llamadas a métodos anotados con
 *
 * @McpTool, @McpResource y @McpPrompt para registrar: - Quién (Client ID /
 * User) - Qué
 * (metodo/tool/resource) - Cuándo (timestamp) - Resultado (éxito/fallo) -
 * Tiempo de ejecución
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class McpAuditAspect {



    /*
     * Audita todas las llamadas a Resources MCP
     * Note: Annotation might be different depending on Spring AI version, adjusting relative to scaffold.
     * Assuming standard Spring AI MCP annotations or custom ones. User reference uses org.springframework.ai.mcp.annotation
     * but standard Spring AI uses @Tool.
     * Use user provided annotation package for consistency with their reference if they are using the community starter.
     */
    
    /**
     * Audita todas las llamadas a Tools MCP
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool) || @annotation(org.springframework.ai.mcp.annotation.McpTool)")
    public Object auditToolCall(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditMcpCall(joinPoint, "TOOL");
    }

    /**
     * Audita todas las llamadas a Resources MCP
     */
    @Around("@annotation(org.springframework.ai.mcp.annotation.McpResource)")
    public Object auditResourceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditMcpCall(joinPoint, "RESOURCE");
    }

    /**
     * Audita todas las llamadas a Prompts MCP
     */
    @Around("@annotation(org.springframework.ai.mcp.annotation.McpPrompt)")
    public Object auditPromptCall(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditMcpCall(joinPoint, "PROMPT");
    }

    /**
     * Metodo genérico de auditoría
     *
     * <p><b>D-23, saldada en la Fase 08 (B-15): se retira la rama no reactiva.</b> El enunciado
     * original decía que este aspecto «no audita la rama no reactiva (solo un {@code warn})». Al
     * medirlo se comprobó que <b>esa rama es inalcanzable</b>: las <b>seis</b> tools MCP del
     * servidor devuelven {@code Mono<...>}, sin excepción. No existe ningún método síncrono que
     * auditar, así que el {@code log.warn} nunca se ha ejecutado ni podía ejecutarse.
     *
     * <p>En su lugar queda un fallo explícito: si algún día alguien añade una tool que no devuelva
     * {@code Mono}, <b>el aspecto lo dirá en voz alta</b> en vez de dejar pasar la llamada sin
     * auditar. En una aplicación WebFlux eso es un defecto de diseño, no un caso de uso legítimo.
     *
     * <p><b>Sobre el orden de {@code proceed()}:</b> se invoca antes de resolver el contexto de
     * seguridad, y <b>es correcto</b>. {@code Mono} es <i>lazy</i>: {@code proceed()} solo
     * <b>ensambla</b> el flujo, no ejecuta la tool. La ejecución ocurre al suscribirse, ya dentro
     * del {@code flatMap}, es decir, <b>después</b> de haber resuelto la identidad del llamante. El
     * log de auditoría refleja el orden real de los hechos.
     */
    private Object auditMcpCall(ProceedingJoinPoint joinPoint, String mcpType) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Información del metodo
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String argsString = formatArgs(args);

        // Ensambla el flujo. NO ejecuta la tool: Mono es lazy.
        Object result = joinPoint.proceed();

        if (!(result instanceof Mono)) {
            throw new IllegalStateException(
                    "Una operacion MCP no reactiva no puede auditarse en una aplicacion WebFlux: "
                            + className + "." + methodName + " debe devolver Mono<...>");
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(this::extractClientId)
                .defaultIfEmpty("anonymous")
                .flatMap(clientId -> {
                    log.info("📊 [AUDIT] {} llamado por: {} | Método: {}.{} | Args: {}",
                            mcpType,
                            clientId,
                            className,
                            methodName,
                            argsString);

                    return ((Mono<?>) result)
                            .doOnSuccess(value -> {
                                long executionTime = System.currentTimeMillis() - startTime;
                                log.info(
                                        "✅ [AUDIT] {} exitoso | Client: {} | Método: {}.{} | Tiempo: {}ms",
                                        mcpType,
                                        clientId,
                                        className,
                                        methodName,
                                        executionTime);
                            })
                            .doOnError(error -> {
                                long executionTime = System.currentTimeMillis() - startTime;
                                log.error(
                                        "❌ [AUDIT] {} fallido | Client: {} | Método: {}.{} | Tiempo: {}ms | Error: {}",
                                        mcpType,
                                        clientId,
                                        className,
                                        methodName,
                                        executionTime,
                                        error.getMessage());
                            });
                });
    }


    private String extractClientId(Authentication auth) {
        if (auth == null) {
            return "anonymous";
        }

        if (auth instanceof JwtAuthenticationToken jwtauthenticationtoken) {
            Jwt jwt = jwtauthenticationtoken.getToken();

            // 1. Intentar 'appid' (Azure AD v1/Graph)
            String appid = jwt.getClaimAsString("appid");
            if (appid != null) {
                return appid;
            }

            // 2. Intentar 'azp' (Authorized Party - OIDC standard)
            String azp = jwt.getClaimAsString("azp");
            if (azp != null) {
                return azp;
            }

            // 3. Intentar extraer ClientID del 'aud'
            List<String> aud = jwt.getAudience();
            if (aud != null && !aud.isEmpty()) {
                // Heurística simple: devolver el primer audience
                return aud.getFirst();
            }
        }

        return auth.getName();
    }

    /**
     * Formatea los argumentos para el log (limita el tamaño)
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(args.length, 3); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else {
                String argStr = arg.toString();
                // Limitar tamaño del argumento en el log
                sb.append(argStr.length() > 50 ? argStr.substring(0, 50) + "..." : argStr);
            }
        }

        if (args.length > 3) {
            sb.append(", ... (").append(args.length - 3).append(" more)");
        }

        sb.append("]");
        return sb.toString();
    }
}
