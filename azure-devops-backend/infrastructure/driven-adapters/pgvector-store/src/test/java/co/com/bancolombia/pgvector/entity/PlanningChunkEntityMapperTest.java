package co.com.bancolombia.pgvector.entity;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.planning.PlanningChunk;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fija la frontera de persistencia de los fragmentos de planificación (D-14).
 *
 * <p>La prueba que importa es {@link #givenAnEntity_whenSerialized_thenJsonIsTheSameAsBefore()}:
 * la entidad se introdujo para <b>no</b> cambiar lo ya almacenado en {@code planning_chunks}, así
 * que si alguien altera un nombre de campo, esto se pone rojo antes de que se corrompa un solo
 * registro.
 */
class PlanningChunkEntityMapperTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private PlanningChunk domainChunk() {
        return PlanningChunk.builder()
                .id("c1")
                .initiativeId("INI-001")
                .sectionName("Alcance")
                .content("contenido")
                .metadata(Map.of("cell", "EQU1096"))
                .build();
    }

    @Test
    @DisplayName("GIVEN un fragmento de dominio WHEN se persiste y se relee THEN vuelve idéntico")
    void givenDomainChunk_whenRoundTripped_thenItComesBackIdentical() {
        PlanningChunk original = domainChunk();

        PlanningChunk recovered = PlanningChunkEntityMapper.toDomain(
                PlanningChunkEntityMapper.toEntity(original));

        assertThat(recovered).isEqualTo(original);
    }

    /**
     * El formato guardado <b>no cambia</b> respecto al que producía la serialización directa del
     * modelo: mismos nombres, mismos valores. Es la condición que hizo innecesaria cualquier
     * migración ni columna nueva (DP-05).
     */
    @Test
    @DisplayName("GIVEN una entidad WHEN se serializa THEN el JSON es el mismo que antes de la Fase 07")
    void givenAnEntity_whenSerialized_thenJsonIsTheSameAsBefore() {
        String asEntity = jsonMapper.writeValueAsString(
                PlanningChunkEntityMapper.toEntity(domainChunk()));

        assertThat(asEntity)
                .contains("\"id\":\"c1\"")
                .contains("\"initiativeId\":\"INI-001\"")
                .contains("\"sectionName\":\"Alcance\"")
                .contains("\"content\":\"contenido\"")
                .contains("\"metadata\"");
    }

    @Test
    @DisplayName("GIVEN un JSON almacenado WHEN se lee THEN se obtiene el fragmento de dominio")
    void givenStoredJson_whenRead_thenDomainChunkIsObtained() {
        String stored = """
                {"id":"c1","initiativeId":"INI-001","sectionName":"Alcance",
                 "content":"contenido","metadata":{"cell":"EQU1096"}}""";

        PlanningChunk chunk = PlanningChunkEntityMapper.toDomain(
                jsonMapper.readValue(stored, PlanningChunkEntity.class));

        assertThat(chunk.id()).isEqualTo("c1");
        assertThat(chunk.sectionName()).isEqualTo("Alcance");
        assertThat(chunk.metadata()).containsEntry("cell", "EQU1096");
    }

    @Test
    @DisplayName("GIVEN nulos WHEN se traducen THEN no revienta")
    void givenNulls_whenMapped_thenNoFailure() {
        assertThat(PlanningChunkEntityMapper.toEntity(null)).isNull();
        assertThat(PlanningChunkEntityMapper.toDomain(null)).isNull();
    }
}

