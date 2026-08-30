package co.com.bancolombia.config.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.prompt.exceptions.PromptTemplateException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato del adaptador de plantillas de prompt (Fase 04, T-06).
 *
 * <p>Lo que se fija aquí: que las variables sean <b>obligatorias</b>, que las líneas de metadato
 * {@code #~} no lleguen nunca al modelo y que la caché evite releer el classpath. Lo tres son las
 * decisiones que hacen seguro sacar los prompts del dominio.
 */
class ClasspathPromptTemplateAdapterTest {

    private static final String TEMPLATE = "dashboard";

    private CountingReader readerWith(String content) {
        return new CountingReader(Map.of("prompts/" + TEMPLATE + ".txt", content));
    }

    @Test
    @DisplayName("GIVEN una plantilla con variables WHEN render THEN las sustituye todas")
    void givenTemplateWithVariables_whenRender_thenAllAreSubstituted() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter = new ClasspathPromptTemplateAdapter(
                readerWith("Hola ${nombre}, bienvenido a ${lugar}.\n"));

        // WHEN
        String rendered = adapter.render(TEMPLATE, Map.of("nombre", "Jorge", "lugar", "Medellín"));

        // THEN
        assertThat(rendered).isEqualTo("Hola Jorge, bienvenido a Medellín.\n");
    }

    @Test
    @DisplayName("GIVEN una variable sin valor WHEN render THEN falla en vez de dejar el hueco vacío")
    void givenMissingVariable_whenRender_thenFails() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter = new ClasspathPromptTemplateAdapter(
                readerWith("Hola ${nombre}, tu sprint es ${sprint}.\n"));
        Map<String, String> soloNombre = Map.of("nombre", "Jorge");

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render(TEMPLATE, soloNombre))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("sprint");
    }

    @Test
    @DisplayName("GIVEN ninguna variable WHEN render con mapa nulo THEN falla igual")
    void givenNullVariables_whenRender_thenFails() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter =
                new ClasspathPromptTemplateAdapter(readerWith("Hola ${nombre}.\n"));

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render(TEMPLATE, null))
                .isInstanceOf(PromptTemplateException.class);
    }

    @Test
    @DisplayName("GIVEN una plantilla inexistente WHEN render THEN excepción de dominio, no IOException")
    void givenUnknownTemplate_whenRender_thenDomainException() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter =
                new ClasspathPromptTemplateAdapter(readerWith("cualquier cosa"));

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render("no-existe", Map.of()))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    @DisplayName("GIVEN un nombre vacío WHEN render THEN excepción de dominio")
    void givenBlankName_whenRender_thenDomainException() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter =
                new ClasspathPromptTemplateAdapter(readerWith("x"));

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render("  ", Map.of()))
                .isInstanceOf(PromptTemplateException.class);
    }

    @Test
    @DisplayName("GIVEN líneas de metadato WHEN render THEN no llegan al prompt")
    void givenMetadataLines_whenRender_thenTheyAreStripped() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter = new ClasspathPromptTemplateAdapter(readerWith("""
                #~ PLANTILLA: dashboard
                #~ [MCP] esto migra en D-35
                Instrucción real.
                """));

        // WHEN
        String rendered = adapter.render(TEMPLATE, Map.of());

        // THEN
        assertThat(rendered).isEqualTo("Instrucción real.\n");
    }

    @Test
    @DisplayName("GIVEN saltos de línea de Windows WHEN render THEN se normalizan a \\n")
    void givenWindowsLineEndings_whenRender_thenNormalized() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter =
                new ClasspathPromptTemplateAdapter(readerWith("una\r\ndos\r\n"));

        // WHEN
        String rendered = adapter.render(TEMPLATE, Map.of());

        // THEN
        assertThat(rendered).isEqualTo("una\ndos\n");
    }

    @Test
    @DisplayName("GIVEN dos renderizados WHEN se pide la misma plantilla THEN el recurso se lee una sola vez")
    void givenTwoRenders_whenSameTemplate_thenResourceIsReadOnce() {
        // GIVEN
        CountingReader reader = readerWith("Hola ${nombre}.\n");
        ClasspathPromptTemplateAdapter adapter = new ClasspathPromptTemplateAdapter(reader);

        // WHEN
        adapter.render(TEMPLATE, Map.of("nombre", "uno"));
        adapter.render(TEMPLATE, Map.of("nombre", "dos"));

        // THEN
        assertThat(reader.reads.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN el classpath real WHEN render THEN encuentra las tres plantillas de la Fase 04")
    void givenRealClasspath_whenRender_thenFindsTheThreeTemplates() {
        // GIVEN
        ClasspathPromptTemplateAdapter adapter = new ClasspathPromptTemplateAdapter();
        Map<String, String> backlog = Map.of("org", "o", "project", "p", "cell", "c",
                "sprint", "s");

        // WHEN / THEN
        assertThat(adapter.render("dashboard", backlog)).contains("REGLAS CRÍTICAS");
        assertThat(adapter.render("dashboard-initial", backlog)).contains("NO analices la calidad");
        assertThat(adapter.render("batch-audit",
                Map.of("batchSize", "3", "project", "p", "org", "o", "idsCsv", "1,2,3")))
                .contains("IDs a analizar: 1,2,3");
    }

    /**
     * Lector de prueba que sirve un texto en memoria y cuenta cuántas veces se le pide.
     */
    private static final class CountingReader
            implements ClasspathPromptTemplateAdapter.ResourceReader {

        private final Map<String, String> resources;
        private final AtomicInteger reads = new AtomicInteger();

        private CountingReader(Map<String, String> resources) {
            this.resources = resources;
        }

        @Override
        public InputStream open(String path) {
            reads.incrementAndGet();
            String content = resources.get(path);
            return content == null
                    ? null
                    : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}

