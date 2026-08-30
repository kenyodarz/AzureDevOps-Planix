package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de la consulta en lote <b>tal y como lo espera la API REST de Azure DevOps</b>:
 * {@code {"ids": [...], "fields": [...], "expand": "None", "errorPolicy": "Omit"}}.
 *
 * <p>Este DTO es el <b>destino legítimo</b> del sufijo {@code Request}: aquí sí describe un cuerpo
 * HTTP y vive en el adaptador, no en el dominio. Su antecesor
 * {@code co.com.bancolombia.model.workitem.WorkItemsBatchRequest} llevaba el mismo sufijo dentro de
 * {@code domain/model} y era la única violación real de {@code Rule_2.2} (D-17); por <b>DP-03</b>
 * el dominio pasó a llamarse {@code WorkItemBatchCriteria}.
 *
 * <p>⚠️ <b>El JSON emitido no cambia ni un byte:</b> mismos nombres de campo, mismo orden, mismos
 * valores por defecto ({@code None} / {@code Omit}, resueltos en el entry-point como siempre).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkItemsBatchRequestDTO {
    private List<Integer> ids;
    private List<String> fields;
    private String expand;
    private String errorPolicy;
}

