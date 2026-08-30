package co.com.bancolombia.mcp.tools;

import co.com.bancolombia.mcp.security.McpRoles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Health check tool for MCP server.
 * This tool provides basic health status information.
 *
 * <p><b>Política de acceso:</b> las dos tools son <b>deliberadamente públicas</b> —son sondas de
 * vida, equivalentes a {@code /actuator/health}— y así se declara con una anotación explícita en
 * lugar de dejarlo a la omisión: una tool sin anotación no se distingue de un descuido. Nótese que
 * en modo {@code ENFORCED} el acceso al endpoint MCP sigue exigiendo un token válido a nivel de
 * transporte; esta anotación solo dice que, una vez dentro, no se exige ningún rol.
 */
@Component
@Slf4j
public class HealthTool {

  /**
   * Checks the health status of the MCP server.
   *
   * @return health status message
   */
  @McpTool(
      name = "checkHealth",
      description = "Check the health status of the MCP server")
  @PreAuthorize(McpRoles.PUBLIC)
  public Mono<String> checkHealth() {
    log.debug("Health check requested");
    return Mono.just("MCP Server is healthy and running");
  }

  /**
   * Gets server information.
   *
   * @return server information
   */
  @McpTool(
      name = "getServerInfo",
      description = "Get MCP server information")
  @PreAuthorize(McpRoles.PUBLIC)
  public Mono<String> getServerInfo() {
    return Mono.just("Server:  v1.0.0 - Package: co.com.bancolombia");
  }
}
