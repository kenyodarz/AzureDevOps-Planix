package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte para deserializar la respuesta de la API REST de Azure DevOps correspondiente a
 * los cambios de archivos (diffs) asociados a un Pull Request o iteración.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitPullRequestChangesResponse {

    private List<ChangeEntryDTO> changeEntries;
    private List<ChangeEntryDTO> changes;

    /**
     * Retorna la lista consolidada de cambios tolerando la respuesta por 'changeEntries' o
     * 'changes'.
     */
    public List<ChangeEntryDTO> allChanges() {
        if (changeEntries != null && !changeEntries.isEmpty()) {
            return changeEntries;
        }
        return changes != null ? changes : List.of();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeEntryDTO {

        private Integer changeTrackingId;
        private Integer changeId;
        private String changeType;
        private GitItemDTO item;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitItemDTO {

        private String objectId;
        private String originalObjectId;
        private String path;
        private String url;
    }
}
