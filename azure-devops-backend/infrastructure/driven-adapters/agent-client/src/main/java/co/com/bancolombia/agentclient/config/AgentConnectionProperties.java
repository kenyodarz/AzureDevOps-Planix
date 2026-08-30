package co.com.bancolombia.agentclient.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la conexión con el agente autónomo.
 *
 * <p>Sustituye al {@code @Value("${agent.url:http://localhost:8082}")} suelto que vivía dentro del
 * adaptador (deuda D-27): la dependencia del BFF respecto a otro servicio era invisible para
 * cualquiera que leyera `application.yaml`, porque la propiedad no aparecía allí.
 *
 * @param url     URL base del agente
 * @param timeout tiempo máximo de espera de una respuesta; evita que un agente colgado bloquee
 *                indefinidamente un stream SSE del dashboard
 */
@ConfigurationProperties(prefix = "agent")
public record AgentConnectionProperties(String url, Duration timeout) {

    private static final String DEFAULT_URL = "http://localhost:8082";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    public AgentConnectionProperties {
        url = (url == null || url.isBlank()) ? DEFAULT_URL : url;
        timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }
}

