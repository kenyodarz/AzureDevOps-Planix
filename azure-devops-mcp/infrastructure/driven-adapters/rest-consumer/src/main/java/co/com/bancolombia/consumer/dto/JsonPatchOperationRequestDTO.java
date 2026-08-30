package co.com.bancolombia.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de una operación JSON Patch <b>tal y como lo espera la API REST de Azure DevOps</b>.
 *
 * <p>Hasta la Fase 03 este cuerpo lo serializaba el {@code WebClient} directamente desde
 * {@code co.com.bancolombia.model.workitem.JsonPatchOperation}, un modelo de <b>dominio</b> (D-11).
 * No había ni un solo mapper en la frontera de salida, pese a que {@code spring-rules.md} los
 * declara <b>obligatorios</b> en {@code driven-adapters}.
 *
 * <p>⚠️ <b>El JSON emitido no cambia ni un byte.</b> Los nombres, el orden de declaración y la
 * inclusión de nulos son idénticos a los del modelo que sustituye; {@code RestConsumerTest} lo
 * verifica contra {@code MockWebServer}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class JsonPatchOperationRequestDTO {
    private String op;
    private String path;
    private Object value;
    private String from;
}

