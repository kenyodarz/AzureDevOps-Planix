package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.WikiPageDTO;
import co.com.bancolombia.consumer.dto.WikiPagePutRequestDTO;
import co.com.bancolombia.consumer.dto.WikiSearchRequestDTO;
import co.com.bancolombia.consumer.dto.WikiSearchResponseDTO;
import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.model.wiki.WikiSearchResult;
import java.util.List;
import java.util.Objects;

/**
 * Mapper para transformar DTOs de Wiki de Azure DevOps a entidades de dominio {@link WikiPage} y {@link WikiSearchResult}.
 */
public final class WikiMapper {

    private WikiMapper() {
    }

    public static WikiPage toDomain(WikiPageDTO dto) {
        if (dto == null) {
            return null;
        }
        return WikiPage.builder()
                .id(dto.getId())
                .path(dto.getPath())
                .content(dto.getContent())
                .remoteUrl(dto.getRemoteUrl())
                .version(dto.getETag())
                .build();
    }

    public static List<WikiSearchResult> toDomainSearchResults(WikiSearchResponseDTO dto) {
        if (dto == null || dto.getResults() == null) {
            return List.of();
        }
        return dto.getResults().stream()
                .filter(Objects::nonNull)
                .map(item -> WikiSearchResult.builder()
                        .path(item.getPath())
                        .summary(item.getSummary())
                        .url(item.getUrl())
                        .wikiName(item.getWiki() != null ? item.getWiki().getName() : null)
                        .build())
                .toList();
    }

    public static WikiPagePutRequestDTO toPutRequest(String content) {
        return WikiPagePutRequestDTO.builder()
                .content(content != null ? content : "")
                .build();
    }

    public static WikiSearchRequestDTO toSearchRequest(String query, int top) {
        return WikiSearchRequestDTO.builder()
                .searchText(query)
                .top(top)
                .build();
    }
}
