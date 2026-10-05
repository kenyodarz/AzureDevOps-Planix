package co.com.bancolombia.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para deserializar una página de Wiki de Azure DevOps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPageDTO {

    private Integer id;
    private String path;
    private String content;
    private String remoteUrl;
    private String eTag;
}
