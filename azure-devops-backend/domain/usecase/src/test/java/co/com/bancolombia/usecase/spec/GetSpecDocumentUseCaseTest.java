package co.com.bancolombia.usecase.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetSpecDocumentUseCaseTest {

    private static final String SPEC_NAME = "ideas_planning_q3.md";
    private static final String MARKDOWN_CONTENT = "# Ideas Planning Q3\n- Objetivo 1";
    private static final String SPEC_PATH = "/workspace/specs/" + SPEC_NAME;

    @Mock
    private SpecStoragePort specStoragePort;

    private GetSpecDocumentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetSpecDocumentUseCase(specStoragePort);
    }

    @Test
    @DisplayName("GIVEN un specName válido WHEN execute THEN retorna el documento de especificación")
    void givenValidSpecName_whenExecute_thenReturnsSpecDocument() {
        // GIVEN
        SpecDocument expectedDoc = new SpecDocument(SPEC_NAME, MARKDOWN_CONTENT, SPEC_PATH);
        when(specStoragePort.getSpec(SPEC_NAME)).thenReturn(Mono.just(expectedDoc));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(SPEC_NAME))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(SPEC_NAME);
                    assertThat(doc.content()).isEqualTo(MARKDOWN_CONTENT);
                    assertThat(doc.path()).isEqualTo(SPEC_PATH);
                })
                .verifyComplete();

        verify(specStoragePort).getSpec(SPEC_NAME);
    }

    @Test
    @DisplayName("GIVEN un specName con espacios en blanco WHEN execute THEN recorta espacios y consulta el puerto")
    void givenSpecNameWithWhitespace_whenExecute_thenTrimsAndReturnsDocument() {
        // GIVEN
        SpecDocument expectedDoc = new SpecDocument(SPEC_NAME, MARKDOWN_CONTENT, SPEC_PATH);
        when(specStoragePort.getSpec(SPEC_NAME)).thenReturn(Mono.just(expectedDoc));

        // WHEN / THEN
        StepVerifier.create(useCase.execute("   " + SPEC_NAME + "  "))
                .assertNext(doc -> assertThat(doc.name()).isEqualTo(SPEC_NAME))
                .verifyComplete();

        verify(specStoragePort).getSpec(SPEC_NAME);
    }

    @Test
    @DisplayName("GIVEN un spec inexistente WHEN execute THEN propaga SpecNotFoundException")
    void givenNonExistentSpec_whenExecute_thenThrowsSpecNotFoundException() {
        // GIVEN
        when(specStoragePort.getSpec(SPEC_NAME))
                .thenReturn(Mono.error(new SpecNotFoundException("No existe el archivo")));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(SPEC_NAME))
                .expectError(SpecNotFoundException.class)
                .verify();

        verify(specStoragePort).getSpec(SPEC_NAME);
    }

    @Test
    @DisplayName("GIVEN specName nulo WHEN execute THEN emite IllegalArgumentException sin llamar al puerto")
    void givenNullSpecName_whenExecute_thenThrowsIllegalArgumentException() {
        // WHEN / THEN
        StepVerifier.create(useCase.execute(null))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("no puede ser nulo o vacío"))
                .verify();

        verify(specStoragePort, never()).getSpec(anyString());
    }

    @Test
    @DisplayName("GIVEN specName vacío WHEN execute THEN emite IllegalArgumentException sin llamar al puerto")
    void givenBlankSpecName_whenExecute_thenThrowsIllegalArgumentException() {
        // WHEN / THEN
        StepVerifier.create(useCase.execute("   "))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().contains("no puede ser nulo o vacío"))
                .verify();

        verify(specStoragePort, never()).getSpec(anyString());
    }

    @Test
    @DisplayName("GIVEN falla de almacenamiento WHEN execute THEN propaga la excepción")
    void givenStorageError_whenExecute_thenPropagatesError() {
        // GIVEN
        when(specStoragePort.getSpec(SPEC_NAME))
                .thenReturn(Mono.error(new IllegalStateException("Error I/O de disco")));

        // WHEN / THEN
        StepVerifier.create(useCase.execute(SPEC_NAME))
                .expectError(IllegalStateException.class)
                .verify();

        verify(specStoragePort).getSpec(SPEC_NAME);
    }
}
