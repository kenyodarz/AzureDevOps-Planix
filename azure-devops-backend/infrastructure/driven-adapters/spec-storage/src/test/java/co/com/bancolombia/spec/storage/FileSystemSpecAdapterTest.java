package co.com.bancolombia.spec.storage;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.spec.SpecNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

/**
 * Pruebas unitarias reactivas para {@link FileSystemSpecAdapter}.
 */
class FileSystemSpecAdapterTest {

    @TempDir
    Path tempDir;

    private FileSystemSpecAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileSystemSpecAdapter(tempDir);
    }

    @Test
    @DisplayName("GIVEN un spec válido WHEN saveSpec y getSpec THEN persiste y recupera el documento correctamente")
    void givenValidSpec_whenSaveSpecAndGetSpec_thenPersistsAndRetrievesDocument() {
        // GIVEN
        String specName = "ideas_planning_q3.md";
        String content = "# Roadmap Q3\n- **Iniciativa:** Modernización Arquitectura 🚀\n- Detalle con tildes y caracteres UTF-8.";

        // WHEN & THEN (Save)
        StepVerifier.create(adapter.saveSpec(specName, content))
                .verifyComplete();

        // WHEN & THEN (Get)
        StepVerifier.create(adapter.getSpec(specName))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(specName);
                    assertThat(doc.content()).isEqualTo(content);
                    assertThat(doc.path()).isEqualTo(
                            tempDir.resolve(specName).toAbsolutePath().toString());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN contenido nulo WHEN saveSpec THEN almacena contenido vacío")
    void givenNullContent_whenSaveSpec_thenPersistsEmptyContent() {
        // GIVEN
        String specName = "empty_spec.md";

        // WHEN & THEN
        StepVerifier.create(adapter.saveSpec(specName, null))
                .verifyComplete();

        StepVerifier.create(adapter.getSpec(specName))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(specName);
                    assertThat(doc.content()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un archivo inexistente WHEN getSpec THEN emite SpecNotFoundException")
    void givenNonExistentFile_whenGetSpec_thenEmitsSpecNotFoundException() {
        // GIVEN
        String missingSpec = "non_existent.md";

        // WHEN & THEN
        StepVerifier.create(adapter.getSpec(missingSpec))
                .expectErrorMatches(throwable -> throwable instanceof SpecNotFoundException
                        && throwable.getMessage().contains(missingSpec))
                .verify();
    }

    @Test
    @DisplayName("GIVEN una ruta que es directorio WHEN getSpec THEN emite SpecNotFoundException")
    void givenDirectoryTarget_whenGetSpec_thenEmitsSpecNotFoundException() throws IOException {
        // GIVEN
        String dirName = "subdir.md";
        Files.createDirectory(tempDir.resolve(dirName));

        // WHEN & THEN
        StepVerifier.create(adapter.getSpec(dirName))
                .expectError(SpecNotFoundException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret.txt", "..\\secret.txt", "sub/secret.md", "sub\\secret.md",
            "..", "/"})
    @DisplayName("GIVEN un nombre con path traversal WHEN saveSpec o getSpec THEN lanza IllegalArgumentException")
    void givenPathTraversalName_whenSaveOrGet_thenFailsWithIllegalArgumentException(
            String maliciousName) {
        // WHEN & THEN saveSpec
        StepVerifier.create(adapter.saveSpec(maliciousName, "hack"))
                .expectError(IllegalArgumentException.class)
                .verify();

        // WHEN & THEN getSpec
        StepVerifier.create(adapter.getSpec(maliciousName))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("GIVEN un nombre vacío o en blanco WHEN saveSpec o getSpec THEN lanza IllegalArgumentException")
    void givenBlankName_whenSaveOrGet_thenFailsWithIllegalArgumentException(String blankName) {
        // WHEN & THEN saveSpec
        StepVerifier.create(adapter.saveSpec(blankName, "content"))
                .expectError(IllegalArgumentException.class)
                .verify();

        // WHEN & THEN getSpec
        StepVerifier.create(adapter.getSpec(blankName))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN un nombre nulo WHEN saveSpec o getSpec THEN lanza IllegalArgumentException")
    void givenNullName_whenSaveOrGet_thenFailsWithIllegalArgumentException() {
        // WHEN & THEN saveSpec
        StepVerifier.create(adapter.saveSpec(null, "content"))
                .expectError(IllegalArgumentException.class)
                .verify();

        // WHEN & THEN getSpec
        StepVerifier.create(adapter.getSpec(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN un directorio con múltiples archivos WHEN listAvailableSpecs THEN retorna solo specs .md ordenados")
    void givenDirectoryWithMixedFiles_whenListAvailableSpecs_thenReturnsOnlyMarkdownSorted()
            throws IOException {
        // GIVEN
        Files.writeString(tempDir.resolve("z_last.md"), "Z", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("a_first.md"), "A", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("m_middle.MD"), "M", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("ignore.txt"), "TXT", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("ignore.json"), "JSON", StandardCharsets.UTF_8);
        Files.createDirectory(tempDir.resolve("nested_dir.md"));

        // WHEN & THEN
        StepVerifier.create(adapter.listAvailableSpecs())
                .expectNext("a_first.md")
                .expectNext("m_middle.MD")
                .expectNext("z_last.md")
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un directorio base inexistente WHEN listAvailableSpecs THEN emite Flux vacío sin fallar")
    void givenNonExistentDirectory_whenListAvailableSpecs_thenEmitsEmptyFlux() {
        // GIVEN
        Path nonExistent = tempDir.resolve("does_not_exist");
        FileSystemSpecAdapter nonExistentAdapter = new FileSystemSpecAdapter(nonExistent);

        // WHEN & THEN
        StepVerifier.create(nonExistentAdapter.listAvailableSpecs())
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN una ruta base que es un archivo regular WHEN listAvailableSpecs THEN emite Flux vacío")
    void givenBaseDirectoryIsFile_whenListAvailableSpecs_thenEmitsEmptyFlux() throws IOException {
        // GIVEN
        Path fileAsBase = tempDir.resolve("not_a_dir.txt");
        Files.writeString(fileAsBase, "test");
        FileSystemSpecAdapter fileBaseAdapter = new FileSystemSpecAdapter(fileAsBase);

        // WHEN & THEN
        StepVerifier.create(fileBaseAdapter.listAvailableSpecs())
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN constructor con String WHEN instanciado THEN resuelve correctamente")
    void givenStringConstructor_whenInstantiated_thenResolvesProperly() {
        // GIVEN
        String pathString = tempDir.resolve("from_string").toString();
        FileSystemSpecAdapter stringAdapter = new FileSystemSpecAdapter(pathString);

        // WHEN & THEN
        StepVerifier.create(stringAdapter.saveSpec("test.md", "hello"))
                .verifyComplete();

        StepVerifier.create(stringAdapter.getSpec("test.md"))
                .assertNext(doc -> assertThat(doc.content()).isEqualTo("hello"))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN constructor con Path nulo WHEN instanciado THEN lanza IllegalArgumentException")
    void givenNullPathConstructor_whenInstantiated_thenThrowsIllegalArgumentException() {
        // WHEN & THEN
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FileSystemSpecAdapter((Path) null)
        );
    }
}
