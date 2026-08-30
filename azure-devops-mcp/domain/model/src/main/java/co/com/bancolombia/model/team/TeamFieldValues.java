package co.com.bancolombia.model.team;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.List;
import lombok.Builder;

/**
 * Valores del campo de equipo que Azure DevOps asocia a una célula: su {@code AreaPath} por defecto
 * y el resto de rutas que tiene asignadas.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> Era una de las dos clases del modelo
 * anotadas con {@code @Data}, la forma más agresiva del modelo anémico: además de los
 * {@code @Setter} generaba {@code equals} y {@code hashCode} sobre campos mutables, de modo que un
 * objeto podía cambiar de identidad después de haberse usado como clave. {@code values} se copia
 * ahora de forma defensiva.
 *
 * <p>⚠️ <b>No se rechaza un {@code defaultValue} en blanco</b>, y no es un olvido: es el precedente
 * que dejó escrito el javadoc de {@code TeamScope}. Azure DevOps devuelve un {@code defaultValue}
 * vacío para una célula mal configurada, y rechazarlo aquí convertiría ese tablero vacío en un
 * error — <b>exactamente</b> el cambio de comportamiento observable que DP-04 §0.1 descartó al
 * elegir la opción (a) y que DP-06 §0.1(d) volvió a ratificar.
 */
@Builder(toBuilder = true)
public record TeamFieldValues(String defaultValue, List<String> values) {

    public TeamFieldValues {
        values = DomainCollections.immutableCopy(values);
    }
}
