package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.PipelineDTO;
import co.com.bancolombia.consumer.dto.PipelineListResponseDTO;
import co.com.bancolombia.consumer.dto.PipelineRunDTO;
import co.com.bancolombia.consumer.dto.PipelineRunRequestDTO;
import co.com.bancolombia.consumer.dto.PipelineTimelineResponseDTO;
import co.com.bancolombia.model.pipeline.PipelineLogSummary;
import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.PipelineSummary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mapper para transformar DTOs de Pipelines de Azure DevOps a entidades de dominio.
 */
public final class PipelineMapper {

    private PipelineMapper() {
    }

    public static PipelineSummary toDomain(PipelineDTO dto) {
        if (dto == null) {
            return null;
        }
        return PipelineSummary.builder()
                .id(dto.getId())
                .name(dto.getName())
                .folder(dto.getFolder())
                .revision(dto.getRevision())
                .url(dto.getUrl())
                .build();
    }

    public static List<PipelineSummary> toDomainList(PipelineListResponseDTO dto) {
        if (dto == null || dto.getValue() == null) {
            return List.of();
        }
        return dto.getValue().stream()
                .filter(Objects::nonNull)
                .map(PipelineMapper::toDomain)
                .toList();
    }

    public static PipelineRun toDomain(PipelineRunDTO dto) {
        if (dto == null) {
            return null;
        }

        Integer pipelineId = null;
        String pipelineName = null;
        if (dto.getPipeline() != null) {
            pipelineId = dto.getPipeline().getId();
            pipelineName = dto.getPipeline().getName();
        }

        String sourceBranch = null;
        String sourceCommit = null;
        if (dto.getResources() != null && dto.getResources().getRepositories() != null) {
            var repo = dto.getResources().getRepositories().get("self");
            if (repo != null) {
                sourceBranch = repo.getRefName();
                sourceCommit = repo.getVersion();
            }
        }

        String webUrl = null;
        if (dto.getLinks() != null && dto.getLinks().containsKey("web")) {
            webUrl = dto.getLinks().get("web").getHref();
        }

        return PipelineRun.builder()
                .id(dto.getId())
                .name(dto.getName())
                .status(dto.getStatus())
                .result(dto.getResult())
                .createdDate(dto.getCreatedDate())
                .finishedDate(dto.getFinishedDate())
                .pipelineId(pipelineId)
                .pipelineName(pipelineName)
                .sourceBranch(sourceBranch)
                .sourceCommit(sourceCommit)
                .webUrl(webUrl)
                .build();
    }

    public static PipelineLogSummary toDomain(PipelineTimelineResponseDTO dto, int runId, boolean onlyErrors, int maxLines) {
        if (dto == null || dto.getRecords() == null) {
            return PipelineLogSummary.builder()
                    .runId(runId)
                    .hasErrors(false)
                    .errorCount(0)
                    .warningCount(0)
                    .errorLines(List.of())
                    .build();
        }

        int totalErrors = 0;
        int totalWarnings = 0;
        List<String> collectedLines = new ArrayList<>();

        for (var record : dto.getRecords()) {
            if (record == null) {
                continue;
            }
            if (record.getErrorCount() != null) {
                totalErrors += record.getErrorCount();
            }
            if (record.getWarningCount() != null) {
                totalWarnings += record.getWarningCount();
            }

            if (record.getIssues() != null) {
                for (var issue : record.getIssues()) {
                    if (issue == null || issue.getMessage() == null) {
                        continue;
                    }
                    boolean isError = "error".equalsIgnoreCase(issue.getType());
                    boolean isWarning = "warning".equalsIgnoreCase(issue.getType());

                    if (isError || (!onlyErrors && isWarning)) {
                        String prefix = isError ? "[ERROR] " : "[WARNING] ";
                        String context = (record.getName() != null ? "(" + record.getName() + ") " : "");
                        if (collectedLines.size() < maxLines) {
                            collectedLines.add(prefix + context + issue.getMessage());
                        }
                    }
                }
            }
        }

        return PipelineLogSummary.builder()
                .runId(runId)
                .hasErrors(totalErrors > 0)
                .errorCount(totalErrors)
                .warningCount(totalWarnings)
                .errorLines(collectedLines)
                .build();
    }

    public static PipelineRunRequestDTO toRunRequest(String branch, Map<String, String> variables) {
        String refName = branch.startsWith("refs/") ? branch : "refs/heads/" + branch;

        var repoRef = PipelineRunRequestDTO.RepositoryRefRequestDTO.builder()
                .refName(refName)
                .build();

        var repos = Map.of("self", repoRef);
        var resources = PipelineRunRequestDTO.PipelineResourcesRequestDTO.builder()
                .repositories(repos)
                .build();

        Map<String, PipelineRunRequestDTO.VariableValueDTO> varMap = null;
        if (variables != null && !variables.isEmpty()) {
            varMap = new HashMap<>();
            for (var entry : variables.entrySet()) {
                varMap.put(entry.getKey(), PipelineRunRequestDTO.VariableValueDTO.builder()
                        .value(entry.getValue())
                        .build());
            }
        }

        return PipelineRunRequestDTO.builder()
                .resources(resources)
                .variables(varMap)
                .build();
    }
}
