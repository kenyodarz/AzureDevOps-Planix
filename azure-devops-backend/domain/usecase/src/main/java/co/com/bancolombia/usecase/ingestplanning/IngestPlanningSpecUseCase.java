package co.com.bancolombia.usecase.ingestplanning;

import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso encargado de ingerir un archivo de planeación Markdown, fragmentarlo por secciones
 * lógicas y guardarlo vectorizado en pgvector.
 */
@RequiredArgsConstructor
public class IngestPlanningSpecUseCase {

    private final PlanningVectorStorePort vectorStorePort;

    public Mono<Void> ingestMarkdown(String initiativeId, String title, String markdownContent) {
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "El contenido de la planeación no puede estar vacío"));
        }

        // Dividir el markdown en secciones lógicas utilizando expresiones regulares
        // Divide en cada título de segundo nivel "## " o primer nivel "# " para mantener contexto
        String[] sections = markdownContent.split("(?=\\n##\\s+)|(?=\\r\\n##\\s+)");
        List<PlanningChunk> chunks = new ArrayList<>();

        for (int i = 0; i < sections.length; i++) {
            String sectionContent = sections[i].trim();
            if (sectionContent.isEmpty()) {
                continue;
            }

            // Intentar extraer el título de la sección de la primera línea
            String sectionName = "Introducción / Contexto";
            String[] lines = sectionContent.split("\\r?\\n", 2);
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                if (firstLine.startsWith("#")) {
                    sectionName = firstLine.replace("#", "").trim();
                }
            }

            // Generar un UUID determinista para evitar duplicidades si se vuelve a subir
            String key = String.format("%s:%s", initiativeId, sectionName);
            String chunkId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8))
                    .toString();

            PlanningChunk chunk = PlanningChunk.builder()
                    .id(chunkId)
                    .initiativeId(initiativeId)
                    .sectionName(sectionName)
                    .content(sectionContent)
                    .metadata(Map.of(
                            "initiative_id", initiativeId,
                            "section_name", sectionName,
                            "initiative_title", title,
                            "chunk_index", i
                    ))
                    .build();

            chunks.add(chunk);
        }

        // Limpiar vectores existentes de la iniciativa para evitar huérfanos si es una actualización
        return vectorStorePort.deleteInitiative(initiativeId)
                .then(vectorStorePort.saveChunks(chunks));
    }
}
