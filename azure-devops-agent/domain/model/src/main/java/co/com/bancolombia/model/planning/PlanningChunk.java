package co.com.bancolombia.model.planning;

import java.util.Map;
import lombok.Builder;

/**
 * Entidad de dominio que representa un fragmento de texto extraído del Markdown de planificación.
 */
@Builder
public record PlanningChunk(
        String id,
        String initiativeId,
        String sectionName,
        String content,
        Map<String, Object> metadata
) {

}
