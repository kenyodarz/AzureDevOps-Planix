package co.com.bancolombia.model.workitem;

import lombok.Builder;

/**
 * Referencia mínima a un elemento de trabajo: su identificador y su URL.
 *
 * <p>Es lo que devuelve una consulta WIQL: el <i>qué</i>, no el <i>detalle</i>. Quien necesite los
 * campos hace después un {@code getWorkItemsBatch}.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> No lleva colecciones ni rechaza
 * valores: un {@code id} nulo es posible en una respuesta degradada de Azure DevOps y esta fase no
 * convierte eso en un error (DP-07 §0.2(c), precedente de {@link TeamScope}).
 */
@Builder(toBuilder = true)
public record WorkItemReference(Integer id, String url) {
}
