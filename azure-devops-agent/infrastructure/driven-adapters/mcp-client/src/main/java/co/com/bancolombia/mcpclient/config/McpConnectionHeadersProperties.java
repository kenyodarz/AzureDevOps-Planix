package co.com.bancolombia.mcpclient.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Custom properties that allow defining extra HTTP headers per MCP server URL. Spring AI does not
 * support connection-level headers natively, so this property fills that gap in a declarative,
 * YAML-driven way.
 *
 * <p>Example configuration in {@code application.yaml}:
 * <pre>
 * mcp:
 *   connection-headers:
 *     "http://localhost:8082":
 *       resource: local
 *     "https://other-server:9090":
 *       x-custom-header: value
 * </pre>
 *
 * <p>The key is the base URL of the MCP server (must match the start of the request URL).
 * The value is a map of {@code header-name → header-value} to inject on every request to that
 * server.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mcp")
public class McpConnectionHeadersProperties {

    /**
     * Map of MCP server base URL → extra headers to inject on every request to that server.
     */
    private Map<String, Map<String, String>> connectionHeaders = new HashMap<>();
}

