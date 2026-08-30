package co.com.bancolombia.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Operación JSON Patch tal y como <b>llega por el cable MCP</b>.
 *
 * <p>Hasta la Fase 03 este hueco lo ocupaba {@code co.com.bancolombia.model.workitem.JsonPatchOperation},
 * un modelo de <b>dominio</b> que Jackson deserializaba directamente desde el payload MCP (D-11).
 * Eso convertía al dominio en el contrato de cable: cualquier cambio en el JSON del cliente
 * repercutía en el núcleo, y al revés.
 *
 * <p>⚠️ <b>Los nombres de campo son contrato público</b> ({@code CONTRATO-MCP.md} §3): {@code op},
 * {@code path}, {@code value} y {@code from} se conservan <b>literalmente</b>. El agente y el BFF
 * son clientes reales y MCP no valida esquemas al vuelo: renombrar uno de estos campos los rompería
 * en silencio. Lo único que cambió es el nombre de la <b>clase</b> (DP-03).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class JsonPatchOperationInput {
    private String op;
    private String path;
    private Object value;
    private String from;
}

