package co.com.bancolombia.consumer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para deserializar la línea de tiempo (timeline) de una ejecución en Azure DevOps Build API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTimelineResponseDTO {

    private List<TimelineRecordDTO> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineRecordDTO {

        private String id;
        private String name;
        private String type;
        private String state;
        private String result;
        private Integer errorCount;
        private Integer warningCount;
        private List<TimelineIssueDTO> issues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineIssueDTO {

        private String type;
        private String message;
    }
}
