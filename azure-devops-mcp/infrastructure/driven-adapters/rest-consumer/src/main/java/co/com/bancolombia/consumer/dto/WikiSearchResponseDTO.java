package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para deserializar la respuesta de búsqueda de páginas en la Wiki de Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiSearchResponseDTO {

    private Integer count;
    private List<WikiSearchResultDTO> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WikiSearchResultDTO {

        private String path;
        private String summary;
        private String url;
        private WikiReferenceDTO wiki;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WikiReferenceDTO {

        private String id;
        private String name;
    }
}
