package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.WiqlQueryRequestDTO;
import co.com.bancolombia.model.workitem.WiqlQuery;

/**
 * Mapper de la <b>frontera de salida</b>: dominio → cuerpo de la consulta WIQL de Azure DevOps.
 *
 * <p>Sustituye al antiguo {@code .bodyValue(query)}, que serializaba el modelo de dominio
 * {@code WiqlQuery} directamente hacia el cable (D-11).
 */
public final class WiqlQueryMapper {

    private WiqlQueryMapper() {
        // Mapper estático: no se instancia.
    }

    public static WiqlQueryRequestDTO toRequest(WiqlQuery query) {
        if (query == null) {
            return null;
        }
        return WiqlQueryRequestDTO.builder()
                .query(query.getQuery())
                .build();
    }
}

