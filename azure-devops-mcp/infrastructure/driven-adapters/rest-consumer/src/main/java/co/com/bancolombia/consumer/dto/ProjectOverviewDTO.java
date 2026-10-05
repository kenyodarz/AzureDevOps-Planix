package co.com.bancolombia.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para deserializar la respuesta de detalles de un proyecto en Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOverviewDTO {

    private String id;
    private String name;
    private String description;
    private String state;
    private String visibility;
    private String url;
}
