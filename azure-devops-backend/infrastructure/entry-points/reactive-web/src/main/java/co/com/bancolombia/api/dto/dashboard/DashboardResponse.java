package co.com.bancolombia.api.dto.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrato JSON del tablero: lo que el agente devuelve y lo que el frontend consume.
 *
 * <p><b>Fase 05 (D-13).</b> Esta clase vivía en {@code domain/model}, donde ArchUnit la señalaba
 * en cada build —<i>Rule_2.2: Domain classes should not be named with technology suffixes</i>— con
 * el aviso de que acabaría rompiendo la compilación. Aquí el sufijo {@code Response} no sobra:
 * describe exactamente lo que es, un DTO de la capa web.
 *
 * <p>Conserva los setters y el constructor vacío <b>a propósito</b>: Jackson los necesita para
 * deserializar la respuesta del agente. Ese es justamente el motivo por el que no podía ser
 * inmutable mientras estuviera en el dominio; hacerlo allí habría exigido meter anotaciones de
 * Jackson en {@code domain/model} y romper su pureza. El modelo de negocio inmutable es
 * {@link co.com.bancolombia.model.dashboard.BacklogAudit}, y {@link DashboardDtoMapper} traduce
 * entre ambos.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    private DashboardMetricsResponse metrics;
    private List<DashboardStoryItemResponse> items;

}

