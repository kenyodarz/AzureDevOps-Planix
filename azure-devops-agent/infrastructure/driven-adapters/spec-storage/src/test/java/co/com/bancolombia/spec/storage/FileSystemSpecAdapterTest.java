package co.com.bancolombia.spec.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.spec.SpecNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

class FileSystemSpecAdapterTest {

    @TempDir
    Path tempDir;

    private FileSystemSpecAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileSystemSpecAdapter(tempDir);
    }

    @Test
    void givenExistingSpec_whenGetSpec_thenReturnsSpecDocument() throws IOException {
        // Arrange
        String specName = "ideas_planning_q3.md";
        String expectedContent = "# Planning Q3\n- Item 1\n- Item 2";
        Path specFile = tempDir.resolve(specName);
        Files.writeString(specFile, expectedContent, StandardCharsets.UTF_8);

        // Act & Assert
        StepVerifier.create(adapter.getSpec(specName))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(specName);
                    assertThat(doc.content()).isEqualTo(expectedContent);
                    assertThat(doc.path()).isEqualTo(specFile.normalize().toString());
                })
                .verifyComplete();
    }

    @Test
    void givenNonExistingSpec_whenGetSpec_thenEmitsSpecNotFoundException() {
        // Arrange
        String specName = "archivo_inexistente.md";

        // Act & Assert
        StepVerifier.create(adapter.getSpec(specName))
                .expectError(SpecNotFoundException.class)
                .verify();
    }

    @Test
    void givenDirectoryInsteadOfFile_whenGetSpec_thenEmitsSpecNotFoundException()
            throws IOException {
        // Arrange
        String folderName = "directorio_simulado.md";
        Files.createDirectory(tempDir.resolve(folderName));

        // Act & Assert
        StepVerifier.create(adapter.getSpec(folderName))
                .expectError(SpecNotFoundException.class)
                .verify();
    }

    @Test
    void givenMaliciousPathTraversal_whenGetSpec_thenEmitsIllegalArgumentException() {
        // Act & Assert
        StepVerifier.create(adapter.getSpec("../secreto.md"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenNullOrBlankSpecName_whenGetSpec_thenEmitsIllegalArgumentException() {
        // Act & Assert
        StepVerifier.create(adapter.getSpec(""))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(adapter.getSpec("   "))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenSpecDirectoryWithFiles_whenListAvailableSpecs_thenListsAllMarkdownFilesSorted()
            throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("frente_b.md"), "Contenido B");
        Files.writeString(tempDir.resolve("frente_a.md"), "Contenido A");
        Files.writeString(tempDir.resolve("documento.txt"), "No es markdown");
        Files.createDirectory(tempDir.resolve("subcarpeta"));

        // Act & Assert
        StepVerifier.create(adapter.listAvailableSpecs())
                .expectNext("frente_a.md")
                .expectNext("frente_b.md")
                .verifyComplete();
    }

    @Test
    void givenNonExistingDirectory_whenListAvailableSpecs_thenEmitsEmpty() {
        // Arrange
        Path nonExistingDir = tempDir.resolve("no_existe");
        FileSystemSpecAdapter adapterWithoutDir = new FileSystemSpecAdapter(nonExistingDir);

        // Act & Assert
        StepVerifier.create(adapterWithoutDir.listAvailableSpecs())
                .verifyComplete();
    }

    @Test
    void givenNewContent_whenSaveSpec_thenPersistsAndCanBeRetrieved() {
        // Arrange
        String specName = "nuevo_spec.md";
        String content = "# Nuevo Documento\nTexto de especificación funcional.";

        // Act
        StepVerifier.create(adapter.saveSpec(specName, content))
                .verifyComplete();

        // Assert
        StepVerifier.create(adapter.getSpec(specName))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(specName);
                    assertThat(doc.content()).isEqualTo(content);
                })
                .verifyComplete();
    }

    @Test
    void givenNullContent_whenSaveSpec_thenSavesEmptyString() {
        // Arrange
        String specName = "vacio.md";

        // Act
        StepVerifier.create(adapter.saveSpec(specName, null))
                .verifyComplete();

        // Assert
        StepVerifier.create(adapter.getSpec(specName))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(specName);
                    assertThat(doc.content()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void givenNestedPath_whenSaveSpec_thenCreatesDirectoriesAndPersists() {
        // Arrange
        String nestedSpec = "subdirectorio/frente_c.md";
        String content = "# Nested Spec";

        // Act
        StepVerifier.create(adapter.saveSpec(nestedSpec, content))
                .verifyComplete();

        // Assert
        StepVerifier.create(adapter.getSpec(nestedSpec))
                .assertNext(doc -> {
                    assertThat(doc.name()).isEqualTo(nestedSpec);
                    assertThat(doc.content()).isEqualTo(content);
                })
                .verifyComplete();
    }

    @Test
    void givenMaliciousPathTraversal_whenSaveSpec_thenEmitsIllegalArgumentException() {
        // Act & Assert
        StepVerifier.create(adapter.saveSpec("../ataque.md", "malicious content"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenNullSpecName_whenGetSpec_thenEmitsIllegalArgumentException() {
        // Act & Assert
        StepVerifier.create(adapter.getSpec(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenAbsolutePathOutsideBasePath_whenGetSpec_thenEmitsIllegalArgumentException() {
        // Arrange
        Path outsidePath = tempDir.getRoot().resolve("outside_spec.md");

        // Act & Assert
        StepVerifier.create(adapter.getSpec(outsidePath.toString()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenFileAsBasePath_whenListAvailableSpecs_thenEmitsEmpty() throws IOException {
        // Arrange
        Path regularFile = tempDir.resolve("un_archivo.txt");
        Files.writeString(regularFile, "solo un archivo");
        FileSystemSpecAdapter adapterOnFile = new FileSystemSpecAdapter(regularFile);

        // Act & Assert
        StepVerifier.create(adapterOnFile.listAvailableSpecs())
                .verifyComplete();
    }

    @Test
    void givenExistingDirectoryAtTargetPath_whenSaveSpec_thenEmitsIllegalStateException()
            throws IOException {
        // Arrange
        String folderConflict = "carpeta_en_conflicto.md";
        Files.createDirectory(tempDir.resolve(folderConflict));

        // Act & Assert
        StepVerifier.create(adapter.saveSpec(folderConflict, "contenido"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void givenExistingDirectoryAtParentPath_whenSaveSpec_thenDoesNotFailCreatingDirectories() {
        // Arrange
        String fileInBase = "directo_en_base.md";

        // Act & Assert
        StepVerifier.create(adapter.saveSpec(fileInBase, "contenido directo"))
                .verifyComplete();
    }

    @Test
    void givenStringBasePathConstructor_whenInstantiated_thenResolvesSuccessfully() {
        // Arrange & Act
        FileSystemSpecAdapter stringAdapter = new FileSystemSpecAdapter(tempDir.toString());

        // Assert
        assertThat(stringAdapter).isNotNull();
    }

    @Test
    void givenNullBasePath_whenInstantiated_thenThrowsNullPointerException() {
        assertThatThrownBy(() -> new FileSystemSpecAdapter((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("La ruta base no puede ser nula");

        assertThatThrownBy(() -> new FileSystemSpecAdapter((Path) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("La ruta base no puede ser nula");
    }
}
