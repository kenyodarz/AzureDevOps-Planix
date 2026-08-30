package co.com.bancolombia.mcp.error;

import java.util.function.Function;

import co.com.bancolombia.model.exception.AzureDevOpsException;
import lombok.extern.slf4j.Slf4j;

/**
 * Segunda mitad del reparto de <b>B-08</b>: convierte una excepción de <b>dominio</b> en el mensaje
 * exacto que ve el cliente MCP.
 *
 * <h2>Por qué hacen falta dos piezas y no una</h2>
 *
 * <p><b>B-08</b> descartó concentrar la traducción en un solo sitio, y la razón es que son dos
 * traducciones distintas con dos conocimientos distintos:
 *
 * <table border="1">
 *   <caption>Reparto</caption>
 *   <tr><th>Pieza</th><th>Sabe</th><th>De</th><th>A</th></tr>
 *   <tr><td>{@code AzureDevOpsErrorTranslator} <i>(rest-consumer)</i></td>
 *       <td>qué es un 404, un timeout, un cortacircuito abierto</td>
 *       <td>excepción técnica</td><td>excepción de dominio</td></tr>
 *   <tr><td><b>esta clase</b> <i>(mcp-server)</i></td>
 *       <td>qué forma tiene un error en el protocolo MCP</td>
 *       <td>excepción de dominio</td><td>mensaje con código estable</td></tr>
 * </table>
 *
 * <p>Ponerlo todo en el adaptador habría obligado al {@code driven-adapter} a conocer el protocolo
 * MCP —una dependencia hacia el entry-point que la arquitectura prohíbe—. Ponerlo todo en el
 * entry-point habría dejado al adaptador incumpliendo {@code spring-rules.md}, que le exige
 * expresamente traducir las excepciones técnicas. Cada capa traduce lo que le corresponde y
 * <b>ninguna duplica a la otra</b>.
 *
 * <h2>La red de seguridad</h2>
 *
 * <p>Un fallo que <b>no</b> sea de dominio —un defecto del propio entry-point, un mapeo roto— no
 * puede escaparse crudo solo porque nadie lo previera. Cae en {@link #INTERNAL_CODE}, con mensaje
 * genérico y traza completa en el log. Esa es la diferencia entre «0 errores técnicos crudos» y «0
 * errores técnicos crudos de los que se me ocurrieron».
 */
@Slf4j
public final class McpErrorTranslator {

    /** Código para todo lo que no sea un fallo de dominio previsto. <b>Contrato público.</b> */
    public static final String INTERNAL_CODE = "MCP_INTERNAL_ERROR";

    private static final String INTERNAL_MESSAGE =
            "Ocurrio un error interno al ejecutar la herramienta";

    private McpErrorTranslator() {
    }

    /**
     * Función lista para encadenarse con {@code onErrorMap} al final de cada tool.
     *
     * @param toolName nombre de la tool MCP. Ya es contrato público, así que aparece en el mensaje
     *                 sin filtrar nada nuevo, y hace el error accionable sin necesidad de correlar
     *                 con el log.
     */
    public static Function<Throwable, Throwable> forTool(String toolName) {
        return error -> translate(toolName, error);
    }

    static Throwable translate(String toolName, Throwable error) {
        if (error instanceof McpToolExecutionException alreadyTranslated) {
            return alreadyTranslated;
        }
        if (error instanceof AzureDevOpsException domainError) {
            log.warn("La tool MCP [{}] fallo con {}: {}", toolName, domainError.getErrorCode(),
                    domainError.getMessage());
            return new McpToolExecutionException(domainError.toClientMessage(), domainError);
        }
        log.error("La tool MCP [{}] fallo con un error no clasificado.", toolName, error);
        return new McpToolExecutionException(
                INTERNAL_CODE + ": " + INTERNAL_MESSAGE + " '" + toolName + "'.", error);
    }
}

