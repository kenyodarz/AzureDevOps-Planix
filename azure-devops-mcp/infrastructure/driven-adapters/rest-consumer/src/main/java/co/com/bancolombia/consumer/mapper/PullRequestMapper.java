package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.GitPullRequestChangesResponse;
import co.com.bancolombia.consumer.dto.GitPullRequestResponse;
import co.com.bancolombia.model.pullrequest.GitChange;
import co.com.bancolombia.model.pullrequest.PullRequest;

import java.util.List;
import java.util.Objects;

/**
 * Mapper desacoplado para transformar DTOs de Azure DevOps Git a entidades inmutables de dominio
 * ({@link PullRequest} y {@link GitChange}).
 */
public final class PullRequestMapper {

    private PullRequestMapper() {
        // Mapper estático: no se instancia.
    }

    /**
     * Transforma un {@link GitPullRequestResponse} a la entidad de dominio {@link PullRequest}.
     *
     * @param dto objeto DTO proveniente de la API REST
     * @return entidad inmutable de dominio o {@code null} si el DTO es nulo
     */
    public static PullRequest toDomain(GitPullRequestResponse dto) {
        if (dto == null) {
            return null;
        }

        String repositoryId = null;
        if (dto.getRepository() != null) {
            repositoryId = dto.getRepository().getId() != null
                    ? dto.getRepository().getId()
                    : dto.getRepository().getName();
        }

        String createdBy = null;
        if (dto.getCreatedBy() != null) {
            createdBy = dto.getCreatedBy().getDisplayName() != null
                    ? dto.getCreatedBy().getDisplayName()
                    : dto.getCreatedBy().getUniqueName();
        }

        List<Integer> workItemIds = List.of();
        if (dto.getWorkItemRefs() != null) {
            workItemIds = dto.getWorkItemRefs().stream()
                    .map(ref -> {
                        if (ref == null || ref.getId() == null) {
                            return null;
                        }
                        try {
                            return Integer.valueOf(ref.getId().trim());
                        } catch (NumberFormatException _) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        return PullRequest.builder()
                .pullRequestId(dto.getPullRequestId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .sourceRefName(dto.getSourceRefName())
                .targetRefName(dto.getTargetRefName())
                .repositoryId(repositoryId)
                .createdBy(createdBy)
                .creationDate(dto.getCreationDate())
                .workItemIds(workItemIds)
                .build();
    }

    /**
     * Transforma un {@link GitPullRequestChangesResponse.ChangeEntryDTO} a la entidad de dominio
     * {@link GitChange}.
     *
     * @param changeDto DTO de una entrada de cambio
     * @return entidad inmutable de dominio o {@code null} si el DTO es nulo
     */
    public static GitChange toDomain(GitPullRequestChangesResponse.ChangeEntryDTO changeDto) {
        if (changeDto == null) {
            return null;
        }

        String itemPath = null;
        String originalObjectId = null;
        String newObjectId = null;

        if (changeDto.getItem() != null) {
            itemPath = changeDto.getItem().getPath();
            originalObjectId = changeDto.getItem().getOriginalObjectId();
            newObjectId = changeDto.getItem().getObjectId();
        }

        return GitChange.builder()
                .itemPath(itemPath)
                .changeType(changeDto.getChangeType())
                .originalObjectId(originalObjectId)
                .newObjectId(newObjectId)
                .build();
    }

    /**
     * Transforma un {@link GitPullRequestChangesResponse} a una lista inmutable de
     * {@link GitChange}.
     *
     * @param response DTO con la lista de cambios
     * @return lista inmutable de cambios
     */
    public static List<GitChange> toDomainChanges(GitPullRequestChangesResponse response) {
        if (response == null || response.allChanges() == null) {
            return List.of();
        }

        return response.allChanges().stream()
                .map(PullRequestMapper::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }
}
