package co.com.bancolombia.mcp.error;

/**
 * Lo que finalmente ve el cliente MCP cuando una tool falla (<b>Fase 06</b>, decisión <b>B-08</b>).
 *
 * <p>Su {@code message} tiene <b>siempre</b> la forma {@code CODIGO: mensaje neutro} que fijó
 * <b>DP-06 §0.1(a)</b>. Es la única superficie de error del servidor y, desde esta fase, <b>contrato
 * público</b>: está descrita en {@code CONTRATO-MCP.md} §6.
 *
 * <p>No lleva la excepción original en el mensaje, pero sí como {@code cause}, de modo que la traza
 * completa —y con ella el cuerpo de Azure DevOps que el adaptador ya registró— sigue disponible en
 * el log del servidor sin viajar hacia el cliente.
 */
public class McpToolExecutionException extends RuntimeException {

    public McpToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

