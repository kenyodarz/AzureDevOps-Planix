package co.com.bancolombia.mcp.dto;

/**
 * DTO de respuesta que representa un comentario o hilo de feedback en un Pull Request hacia el
 * cliente MCP.
 *
 * @param id      identificador numérico del comentario o hilo en Azure DevOps
 * @param content contenido textual del comentario técnico o revisión de código
 * @param status  estado del hilo (ej. "active", "fixed", "closed")
 * @param author  nombre descriptivo o identificador del autor del comentario
 */
public record PullRequestCommentResponse(
        Integer id,
        String content,
        String status,
        String author) {

}
