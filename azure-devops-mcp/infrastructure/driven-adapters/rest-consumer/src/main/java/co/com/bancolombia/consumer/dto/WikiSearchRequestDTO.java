package co.com.bancolombia.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de búsqueda en Wiki de Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiSearchRequestDTO {

    private String searchText;

    @JsonProperty("$top")
    private Integer top;
}
