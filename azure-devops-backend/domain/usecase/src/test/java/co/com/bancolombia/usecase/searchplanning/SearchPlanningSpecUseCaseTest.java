package co.com.bancolombia.usecase.searchplanning;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Pruebas de caracterización de la búsqueda semántica (Fase 01).
 */
@ExtendWith(MockitoExtension.class)
class SearchPlanningSpecUseCaseTest {

    private static final String QUERY = "reglas de commit";
    private static final String INITIATIVE_ID = "INI-001";
    private static final int DEFAULT_MAX_RESULTS = 3;

    @Mock
    private PlanningVectorStorePort vectorStorePort;

    private SearchPlanningSpecUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchPlanningSpecUseCase(vectorStorePort);
    }

    private PlanningChunk chunk(String id) {
        return PlanningChunk.builder()
                .id(id)
                .initiativeId(INITIATIVE_ID)
                .sectionName("Alcance")
                .content("contenido " + id)
                .metadata(Map.of())
                .build();
    }

    @Test
    @DisplayName("GIVEN consulta válida WHEN search THEN delega en el puerto y devuelve los chunks")
    void givenValidQuery_whenSearch_thenDelegatesAndReturnsChunks() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .thenReturn(Flux.just(chunk("c1"), chunk("c2")));

        // WHEN / THEN
        StepVerifier.create(useCase.search(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .expectNextMatches(c -> "c1".equals(c.id()))
                .expectNextMatches(c -> "c2".equals(c.id()))
                .verifyComplete();

        verify(vectorStorePort).searchSimilarity(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS);
    }

    @Test
    @DisplayName("GIVEN maxResults no positivo WHEN search THEN aplica el valor por defecto 3")
    void givenNonPositiveMaxResults_whenSearch_thenAppliesDefaultOfThree() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.empty());

        // WHEN
        StepVerifier.create(useCase.search(QUERY, INITIATIVE_ID, 0)).verifyComplete();
        StepVerifier.create(useCase.search(QUERY, INITIATIVE_ID, -5)).verifyComplete();

        // THEN
        verify(vectorStorePort, times(2))
                .searchSimilarity(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS);
    }

    @Test
    @DisplayName("GIVEN maxResults positivo WHEN search THEN respeta el valor recibido")
    void givenPositiveMaxResults_whenSearch_thenHonoursTheValue() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(QUERY, INITIATIVE_ID, 25)).thenReturn(Flux.empty());

        // WHEN
        StepVerifier.create(useCase.search(QUERY, INITIATIVE_ID, 25)).verifyComplete();

        // THEN
        verify(vectorStorePort).searchSimilarity(QUERY, INITIATIVE_ID, 25);
    }

    @Test
    @DisplayName("GIVEN consulta vacía o nula WHEN search THEN falla y no toca el almacén")
    void givenBlankOrNullQuery_whenSearch_thenFailsWithoutTouchingStore() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(useCase.search(null, INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(useCase.search("   ", INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(vectorStorePort, never()).searchSimilarity(anyString(), anyString(), anyInt());
    }

    /**
     * Caracteriza que hoy un {@code initiativeId} vacío se propaga tal cual al adaptador, que es
     * quien decide si filtra o no. El caso de uso no valida ese parámetro.
     */
    @Test
    @DisplayName("GIVEN initiativeId vacío WHEN search THEN se propaga sin validar (comportamiento actual)")
    void givenBlankInitiativeId_whenSearch_thenItIsPropagatedUnvalidated() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(QUERY, "", DEFAULT_MAX_RESULTS))
                .thenReturn(Flux.empty());

        // WHEN
        StepVerifier.create(useCase.search(QUERY, "", DEFAULT_MAX_RESULTS)).verifyComplete();

        // THEN
        verify(vectorStorePort).searchSimilarity(QUERY, "", DEFAULT_MAX_RESULTS);
    }

    @Test
    @DisplayName("GIVEN el almacén falla WHEN search THEN el error se propaga")
    void givenStoreFails_whenSearch_thenErrorIsPropagated() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.error(new IllegalStateException("pgvector caído")));

        // WHEN / THEN
        StepVerifier.create(useCase.search(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN el almacén no devuelve nada WHEN search THEN completa vacío")
    void givenEmptyStore_whenSearch_thenCompletesEmpty() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .thenReturn(Flux.empty());

        // WHEN / THEN
        StepVerifier.create(useCase.search(QUERY, INITIATIVE_ID, DEFAULT_MAX_RESULTS))
                .expectNextCount(0)
                .verifyComplete();
    }
}


