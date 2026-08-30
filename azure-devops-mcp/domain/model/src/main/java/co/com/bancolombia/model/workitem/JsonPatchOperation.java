package co.com.bancolombia.model.workitem;

import lombok.Builder;

/**
 * Operación JSON Patch sobre un elemento de trabajo: el <i>qué</i> se quiere cambiar.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> Los nombres de sus componentes
 * —{@code op}, {@code path}, {@code value}, {@code from}— coinciden con los del cable en las dos
 * fronteras, pero desde la Fase 03 ya <b>no es</b> el cable: lo son
 * {@code JsonPatchOperationInput} en la entrada y {@code JsonPatchOperationRequestDTO} en la salida.
 *
 * <p><b>Invariante — DP-07 §0.2(c): {@code op} y {@code path} no pueden ser nulos ni estar en
 * blanco.</b> Una operación JSON Patch sin verbo o sin destino no es una operación: es un objeto sin
 * significado que hoy viajaría hasta Azure DevOps para volver como un 400 opaco. Rechazarla aquí no
 * cambia el desenlace —seguía siendo un fallo—, solo lo hace inmediato y comprensible. {@code value}
 * y {@code from} sí pueden faltar: {@code remove} no lleva valor y solo {@code move} y {@code copy}
 * llevan origen.
 */
@Builder(toBuilder = true)
public record JsonPatchOperation(String op, String path, Object value, String from) {

    public JsonPatchOperation {
        if (op == null || op.isBlank()) {
            throw new IllegalArgumentException(
                    "La operacion JSON Patch requiere un 'op' no vacio");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "La operacion JSON Patch requiere un 'path' no vacio");
        }
    }
}
