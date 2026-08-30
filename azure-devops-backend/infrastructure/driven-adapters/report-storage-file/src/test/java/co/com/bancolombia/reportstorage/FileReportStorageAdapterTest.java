package co.com.bancolombia.reportstorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

/**
 * Pruebas del adaptador de disco de los reportes (D-10, DP-04 opción A).
 *
 * <p>Escriben en un directorio temporal: la versión anterior de estas comprobaciones vivía en el
 * test del caso de uso y ensuciaba {@code reports/} del propio repositorio.
 */
class FileReportStorageAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("GIVEN un reporte WHEN save THEN lo escribe en el directorio configurado")
    void givenReport_whenSave_thenWritesItInTheConfiguredDirectory() {
        // GIVEN
        FileReportStorageAdapter adapter =
                new FileReportStorageAdapter(tempDir.resolve("reportes").toString());

        // WHEN
        StepVerifier.create(adapter.save("reporte_Celula_Sprint_1.md", "# Hola"))
                .verifyComplete();

        // THEN
        Path expected = tempDir.resolve("reportes").resolve("reporte_Celula_Sprint_1.md");
        assertThat(expected).exists()
                .content(StandardCharsets.UTF_8).isEqualTo("# Hola");
    }

    @Test
    @DisplayName("GIVEN acentos y emojis WHEN save THEN conserva el contenido en UTF-8")
    void givenAccents_whenSave_thenKeepsUtf8() throws Exception {
        // GIVEN
        FileReportStorageAdapter adapter = new FileReportStorageAdapter(tempDir.toString());
        String content = "- **Célula/Equipo:** Análisis ✅";

        // WHEN
        StepVerifier.create(adapter.save("acentos.md", content)).verifyComplete();

        // THEN
        assertThat(Files.readString(tempDir.resolve("acentos.md"), StandardCharsets.UTF_8))
                .isEqualTo(content);
    }

    @Test
    @DisplayName("GIVEN un nombre que sale del directorio WHEN save THEN falla y no escribe nada")
    void givenEscapingName_whenSave_thenFails() {
        // GIVEN
        FileReportStorageAdapter adapter =
                new FileReportStorageAdapter(tempDir.resolve("reportes").toString());

        // WHEN / THEN
        StepVerifier.create(adapter.save("../fuera.md", "contenido"))
                .expectError(IllegalArgumentException.class)
                .verify();
        assertThat(tempDir.resolve("fuera.md")).doesNotExist();
    }

    @Test
    @DisplayName("GIVEN un nombre vacío WHEN save THEN falla sin tocar el disco")
    void givenBlankName_whenSave_thenFails() {
        // GIVEN
        FileReportStorageAdapter adapter = new FileReportStorageAdapter(tempDir.toString());

        // WHEN / THEN
        StepVerifier.create(adapter.save("  ", "contenido"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN contenido nulo WHEN save THEN escribe un fichero vacío")
    void givenNullContent_whenSave_thenWritesEmptyFile() throws Exception {
        // GIVEN
        FileReportStorageAdapter adapter = new FileReportStorageAdapter(tempDir.toString());

        // WHEN
        StepVerifier.create(adapter.save("vacio.md", null)).verifyComplete();

        // THEN
        assertThat(Files.readString(tempDir.resolve("vacio.md"))).isEmpty();
    }
}

