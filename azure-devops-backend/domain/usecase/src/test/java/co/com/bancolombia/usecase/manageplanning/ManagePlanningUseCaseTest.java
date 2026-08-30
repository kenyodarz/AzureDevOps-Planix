package co.com.bancolombia.usecase.manageplanning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas de caracterización de la gestión de iniciativas (Fase 01).
 */
@ExtendWith(MockitoExtension.class)
class ManagePlanningUseCaseTest {

    private static final String INITIATIVE_ID = "INI-001";
    private static final String CELL = "EQU1096 - EXODIA";

    @Mock
    private PlanningVectorStorePort vectorStorePort;

    private ManagePlanningUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManagePlanningUseCase(vectorStorePort);
    }

    @Test
    @DisplayName("GIVEN iniciativas en el almacén WHEN getInitiatives THEN las devuelve tal cual")
    void givenInitiatives_whenGetInitiatives_thenReturnsThemAsIs() {
        // GIVEN
        Map<String, Object> row = Map.of("initiative_id", INITIATIVE_ID, "cell", CELL);
        when(vectorStorePort.findAllInitiatives()).thenReturn(Flux.just(row));

        // WHEN / THEN
        StepVerifier.create(useCase.getInitiatives())
                .assertNext(result -> assertThat(result).containsEntry("cell", CELL))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN id válido WHEN getInitiativeChunks THEN delega en el puerto")
    void givenValidId_whenGetInitiativeChunks_thenDelegates() {
        // GIVEN
        PlanningChunk chunk = PlanningChunk.builder().id("c1").initiativeId(INITIATIVE_ID).build();
        when(vectorStorePort.findChunksByInitiative(INITIATIVE_ID)).thenReturn(Flux.just(chunk));

        // WHEN / THEN
        StepVerifier.create(useCase.getInitiativeChunks(INITIATIVE_ID))
                .expectNextMatches(c -> "c1".equals(c.id()))
                .verifyComplete();

        verify(vectorStorePort).findChunksByInitiative(INITIATIVE_ID);
    }

    @Test
    @DisplayName("GIVEN id válido WHEN deleteInitiative THEN delega en el puerto")
    void givenValidId_whenDeleteInitiative_thenDelegates() {
        // GIVEN
        when(vectorStorePort.deleteInitiative(INITIATIVE_ID)).thenReturn(Mono.empty());

        // WHEN / THEN
        StepVerifier.create(useCase.deleteInitiative(INITIATIVE_ID)).verifyComplete();

        verify(vectorStorePort).deleteInitiative(INITIATIVE_ID);
    }

    @Test
    @DisplayName("GIVEN id y célula válidos WHEN updateCell THEN delega en el puerto")
    void givenValidIdAndCell_whenUpdateCell_thenDelegates() {
        // GIVEN
        when(vectorStorePort.updateInitiativeCell(INITIATIVE_ID, CELL)).thenReturn(Mono.empty());

        // WHEN / THEN
        StepVerifier.create(useCase.updateCell(INITIATIVE_ID, CELL)).verifyComplete();

        verify(vectorStorePort).updateInitiativeCell(INITIATIVE_ID, CELL);
    }

    /**
     * Caracteriza que una célula nula se convierte en cadena vacía en lugar de rechazarse. Es una
     * decisión silenciosa del caso de uso actual: se congela, no se juzga.
     */
    @Test
    @DisplayName("GIVEN célula nula WHEN updateCell THEN la convierte en cadena vacía (comportamiento actual)")
    void givenNullCell_whenUpdateCell_thenItBecomesEmptyString() {
        // GIVEN
        when(vectorStorePort.updateInitiativeCell(INITIATIVE_ID, "")).thenReturn(Mono.empty());

        // WHEN / THEN
        StepVerifier.create(useCase.updateCell(INITIATIVE_ID, null)).verifyComplete();

        verify(vectorStorePort).updateInitiativeCell(INITIATIVE_ID, "");
    }

    @Test
    @DisplayName("GIVEN id vacío o nulo WHEN deleteInitiative THEN falla sin tocar el almacén")
    void givenBlankId_whenDeleteInitiative_thenFailsWithoutTouchingStore() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(useCase.deleteInitiative(null))
                .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(useCase.deleteInitiative("  "))
                .expectError(IllegalArgumentException.class).verify();

        verify(vectorStorePort, never()).deleteInitiative(anyString());
    }

    @Test
    @DisplayName("GIVEN id vacío o nulo WHEN updateCell THEN falla sin tocar el almacén")
    void givenBlankId_whenUpdateCell_thenFailsWithoutTouchingStore() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(useCase.updateCell(null, CELL))
                .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(useCase.updateCell("  ", CELL))
                .expectError(IllegalArgumentException.class).verify();

        verify(vectorStorePort, never()).updateInitiativeCell(anyString(), anyString());
    }

    @Test
    @DisplayName("GIVEN id vacío o nulo WHEN getInitiativeChunks THEN falla sin tocar el almacén")
    void givenBlankId_whenGetInitiativeChunks_thenFailsWithoutTouchingStore() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(useCase.getInitiativeChunks(null))
                .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(useCase.getInitiativeChunks("  "))
                .expectError(IllegalArgumentException.class).verify();

        verify(vectorStorePort, never()).findChunksByInitiative(anyString());
    }

    @Test
    @DisplayName("GIVEN el almacén falla WHEN getInitiatives THEN el error se propaga")
    void givenStoreFails_whenGetInitiatives_thenErrorIsPropagated() {
        // GIVEN
        when(vectorStorePort.findAllInitiatives())
                .thenReturn(Flux.error(new IllegalStateException("pgvector caído")));

        // WHEN / THEN
        StepVerifier.create(useCase.getInitiatives())
                .expectError(IllegalStateException.class)
                .verify();
    }
}

