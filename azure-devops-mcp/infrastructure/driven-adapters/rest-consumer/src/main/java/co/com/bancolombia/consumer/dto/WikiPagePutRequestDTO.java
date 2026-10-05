package co.com.bancolombia.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de creación o actualización de página en Azure DevOps Wiki.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPagePutRequestDTO {

    private String content;
}
