package co.com.bancolombia.model.dashboard.gateways;

/**
 * Datos de respaldo del tablero, para cuando no hay canal de IA disponible.
 *
 * <p>Existe porque el modo de trabajo local de la POC arranca con el cliente MCP deshabilitado
 * ({@code spring.ai.mcp.client.enabled=false}) y el tablero debe seguir mostrando algo. En
 * producción esa bandera no existe: el despliegue va contra el MCP de forma obligatoria y este
 * puerto no se ejerce.
 *
 * <p><b>Por qué es síncrono.</b> La implementación resuelve su contenido <b>una sola vez al
 * construirse</b> y lo conserva en memoria, así que en tiempo de petición no hay entrada/salida
 * alguna. Devolver {@code Mono<String>} solo añadiría ceremonia sobre un valor ya calculado.
 */
public interface DashboardFallbackPort {

    /**
     * @return el JSON de tablero simulado, en el mismo formato que devolvería el agente
     */
    String mockDashboard();
}

