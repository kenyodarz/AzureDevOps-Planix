package co.com.bancolombia.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de la consulta WIQL <b>tal y como lo espera la API REST de Azure DevOps</b>:
 * {@code {"query": "SELECT ..."}}.
 *
 * <p>Sustituye a la serialización directa de {@code co.com.bancolombia.model.workitem.WiqlQuery},
 * que era un modelo de dominio viajando por el cable de salida (D-11).
 *
 * <p>⚠️ <b>El JSON emitido no cambia ni un byte.</b> La sentencia WIQL sigue congelada carácter a
 * carácter en {@code WiqlCharacterizationTest} y esta clase no la toca: solo la transporta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WiqlQueryRequestDTO {
    private String query;
}

