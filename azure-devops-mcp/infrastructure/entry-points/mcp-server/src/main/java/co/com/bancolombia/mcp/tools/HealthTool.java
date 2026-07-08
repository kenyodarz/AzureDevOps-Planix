package co.com.bancolombia.mcp.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Health check tool for MCP server.
 * This tool provides basic health status information.
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
  public Mono<String> getServerInfo() {
    return Mono.just("Server:  v1.0.0 - Package: co.com.bancolombia");
  }
}
