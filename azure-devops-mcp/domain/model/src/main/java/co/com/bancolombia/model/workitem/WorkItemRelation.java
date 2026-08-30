package co.com.bancolombia.model.workitem;

import co.com.bancolombia.model.common.DomainCollections;
import java.util.Map;
import lombok.Builder;

/**
 * Relación (vínculo) de un elemento de trabajo con otro: jerarquía, dependencia o adjunto.
 *
 * <p><b>Objeto de valor inmutable desde la Fase 07 (D-16).</b> Sus {@code @Setter} desaparecen y
 * {@code attributes} se copia de forma defensiva, preservando el orden de iteración porque es el
 * que determina el orden de los campos del JSON emitido.
 *
 * <p>No se rechaza ningún valor: Azure DevOps puede devolver una relación sin atributos y
 * convertirlo en un error sería un cambio de comportamiento observable no autorizado
 * (DP-07 §0.2(c), precedente de {@link TeamScope}).
 */
@Builder(toBuilder = true)
public record WorkItemRelation(String rel, String url, Map<String, Object> attributes) {

    public WorkItemRelation {
        attributes = DomainCollections.immutableCopy(attributes);
    }
}
