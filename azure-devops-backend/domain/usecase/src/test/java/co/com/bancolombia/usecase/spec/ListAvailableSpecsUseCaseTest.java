package co.com.bancolombia.usecase.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ListAvailableSpecsUseCaseTest {

    @Mock
    private SpecStoragePort specStoragePort;

    private ListAvailableSpecsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAvailableSpecsUseCase(specStoragePort);
    }

    @Test
    @DisplayName("GIVEN especificaciones disponibles WHEN execute THEN emite todos los nombres encontrados")
    void givenAvailableSpecs_whenExecute_thenEmitsAllSpecNames() {
        // GIVEN
        when(specStoragePort.listAvailableSpecs())
                .thenReturn(Flux.just("frente_canales.md", "ideas_planning_q3.md"));

        // WHEN / THEN
        StepVerifier.create(useCase.execute())
                .assertNext(name -> assertThat(name).isEqualTo("frente_canales.md"))
                .assertNext(name -> assertThat(name).isEqualTo("ideas_planning_q3.md"))
                .verifyComplete();

        verify(specStoragePort).listAvailableSpecs();
    }

    @Test
    @DisplayName("GIVEN un repositorio vacío WHEN execute THEN completa sin emitir elementos")
    void givenNoSpecs_whenExecute_thenEmitsEmpty() {
        // GIVEN
        when(specStoragePort.listAvailableSpecs()).thenReturn(Flux.empty());

        // WHEN / THEN
        StepVerifier.create(useCase.execute())
                .verifyComplete();

        verify(specStoragePort).listAvailableSpecs();
    }

    @Test
    @DisplayName("GIVEN un error en el puerto WHEN execute THEN propaga la excepción")
    void givenStorageError_whenExecute_thenPropagatesError() {
        // GIVEN
        when(specStoragePort.listAvailableSpecs())
                .thenReturn(Flux.error(new IllegalStateException("Directorio inaccesible")));

        // WHEN / THEN
        StepVerifier.create(useCase.execute())
                .expectError(IllegalStateException.class)
                .verify();

        verify(specStoragePort).listAvailableSpecs();
    }
}
