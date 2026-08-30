package co.com.bancolombia.model.workitem;

import lombok.Builder;

/**
 * Sentencia WIQL lista para enviarse a Azure DevOps.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> No cruza la frontera de salida hacia
 * el cliente MCP —solo hacia Azure DevOps, a través de {@code WiqlQueryRequestDTO} desde la Fase
 * 03—, así que su forma es asunto exclusivo del dominio.
 *
 * <p><b>Invariante — DP-07 §0.2(c): la sentencia no puede ser nula ni estar en blanco.</b> Es una
 * de las dos invariantes de rechazo que esta fase declara, y es segura porque una WIQL vacía nunca
 * se construye en ningún camino real: {@code WiqlStatement} la redacta siempre completa y sus ocho
 * ramas están congeladas carácter a carácter desde la Fase 01. Lo que la invariante impide es que
 * un cambio futuro emita una consulta vacía contra Azure DevOps y se descubra por un tablero vacío
 * en lugar de por un error — exactamente el fallo que originó D-09.
 */
@Builder(toBuilder = true)
public record WiqlQuery(String query) {

    public WiqlQuery {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("La sentencia WIQL no puede ser nula ni estar vacia");
        }
    }
}
