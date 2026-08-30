package co.com.bancolombia.pgvector.entity;

import co.com.bancolombia.model.planning.PlanningChunk;

/**
 * Traduce entre el modelo de dominio y la entidad de persistencia de pgvector (D-14).
 *
 * <p>Es la frontera que faltaba: a partir de aquí, el adaptador serializa
 * {@link PlanningChunkEntity} y nunca el modelo. El dominio no sabe que existe Jackson, que es la
 * regla que {@code rules/spring-rules.md} impone y que D-14 incumplía.
 */
public final class PlanningChunkEntityMapper {

    private PlanningChunkEntityMapper() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * @param chunk fragmento de dominio
     * @return la entidad que se guarda en la base de datos, o {@code null} si no había fragmento
     */
    public static PlanningChunkEntity toEntity(PlanningChunk chunk) {
        if (chunk == null) {
            return null;
        }
        return new PlanningChunkEntity(
                chunk.id(),
                chunk.initiativeId(),
                chunk.sectionName(),
                chunk.content(),
                chunk.metadata());
    }

    /**
     * @param entity entidad leída de la base de datos
     * @return el fragmento de dominio, o {@code null} si no había entidad
     */
    public static PlanningChunk toDomain(PlanningChunkEntity entity) {
        if (entity == null) {
            return null;
        }
        return PlanningChunk.builder()
                .id(entity.id())
                .initiativeId(entity.initiativeId())
                .sectionName(entity.sectionName())
                .content(entity.content())
                .metadata(entity.metadata())
                .build();
    }
}

