package co.com.bancolombia.prompttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.exceptions.PromptTemplateException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del mecanismo de resolución de plantillas.
 *
 * <p>Se usan recursos de prueba propios (carpetas {@code test-prompts} y {@code test-fragmentos})
 * para verificar la mecánica de forma aislada. El contenido real de las plantillas de producción se
 * valida en {@code PromptTemplateContentTest}, dentro del módulo que las aloja.
 */
@DisplayName("ClasspathPromptTemplateAdapter")
class ClasspathPromptTemplateAdapterTest {

    private static final String TEST_TEMPLATES = "test-prompts/";
    private static final String TEST_FRAGMENTS = "test-fragmentos/";

    private static final String WORK_ITEM_ID = "workItemId";
    private static final String ORGANIZATION = "organizacion";
    private static final String PROJECT = "proyecto";
    private static final String AGILE_GUIDE = "guiaAgilidad";
    private static final String STANDARDS = "estandares";

    private ClasspathPromptTemplateAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ClasspathPromptTemplateAdapter(TEST_TEMPLATES, TEST_FRAGMENTS);
    }

    @Test
    @DisplayName("Sustituye cada marcador por el valor indicado")
    void givenVariables_whenRender_thenReplacesEveryPlaceholder() {
        // GIVEN
        Map<String, Object> variables = Map.of(
                "ideaOriginal", "Construir un servicio de notificaciones",
                "contextoRag", "Sin contexto adicional");

        // WHEN
        String result = adapter.render(PromptTemplateId.PLANNING_DRAFT, variables);

        // THEN
        assertThat(result).contains("Construir un servicio de notificaciones")
                .contains("Sin contexto adicional")
                .doesNotContain("{{");
    }

    @Test
    @DisplayName("Un mismo marcador repetido se sustituye en todas sus apariciones")
    void givenRepeatedPlaceholder_whenRender_thenReplacesAllOccurrences() {
        // GIVEN
        Map<String, Object> variables = Map.of(
                WORK_ITEM_ID, "12345",
                ORGANIZATION, "grupobancolombia",
                PROJECT, "VST",
                STANDARDS, "Estándares corporativos");

        // WHEN
        String result = adapter.render(PromptTemplateId.QUALITY_AUDIT, variables);

        // THEN
        assertThat(result).doesNotContain("{{workItemId}}");
        assertThat(result.split("12345", -1)).hasSize(3); // dos apariciones sustituidas
    }

    @Test
    @DisplayName("El fragmento compartido de estimación se inyecta sin declararlo")
    void givenTemplateWithSharedFragment_whenRender_thenInjectsEstimationTable() {
        // GIVEN
        Map<String, Object> variables = Map.of(
                "plantillaCorporativa", "Plantilla",
                AGILE_GUIDE, "Guía");

        // WHEN
        String result = adapter.render(PromptTemplateId.STRUCTURED_STORY, variables);

        // THEN
        assertThat(result).contains("| Story Point | Esfuerzo (Horas) |")
                .doesNotContain("{{tablaEstimacion}}");
    }

    @Test
    @DisplayName("Una variable faltante aborta el renderizado con excepción de dominio")
    void givenMissingVariable_whenRender_thenThrowsDomainException() {
        // GIVEN
        Map<String, Object> incomplete = Map.of("ideaOriginal", "Solo una de las dos variables");

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render(PromptTemplateId.PLANNING_DRAFT, incomplete))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("contextoRag");
    }

    @Test
    @DisplayName("El identificador nulo es rechazado")
    void givenNullId_whenRender_thenThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> variables = Map.of();

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render(null, variables))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("El mapa de variables nulo es rechazado")
    void givenNullVariables_whenRender_thenThrowsNullPointerException() {
        // WHEN / THEN
        assertThatThrownBy(() -> adapter.render(PromptTemplateId.STORY_DIVISION, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Un valor nulo se sustituye por cadena vacía sin romper el renderizado")
    void givenNullValue_whenRender_thenReplacesWithEmptyString() {
        // GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put(AGILE_GUIDE, null);

        // WHEN
        String result = adapter.render(PromptTemplateId.STORY_DIVISION, variables);

        // THEN
        assertThat(result).doesNotContain("{{guiaAgilidad}}").contains("Guía de agilidad:");
    }

    @Test
    @DisplayName("Un valor con caracteres especiales de regex se inserta literalmente")
    void givenValueWithRegexCharacters_whenRender_thenInsertsLiterally() {
        // GIVEN
        String tricky = "costo $100 \\ 50% {llaves} $1";
        Map<String, Object> variables = Map.of(AGILE_GUIDE, tricky);

        // WHEN
        String result = adapter.render(PromptTemplateId.STORY_DIVISION, variables);

        // THEN
        assertThat(result).contains(tricky);
    }

    @Test
    @DisplayName("Una carpeta inexistente falla al construir el bean, no en tiempo de petición")
    void givenMissingFolder_whenConstruct_thenThrowsDomainException() {
        // WHEN / THEN
        assertThatThrownBy(() -> new ClasspathPromptTemplateAdapter("no-existe/", TEST_FRAGMENTS))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("No se pudo cargar la plantilla");
    }

    @Test
    @DisplayName("Todas las plantillas del catálogo se resuelven")
    void givenEveryTemplateId_whenRender_thenNoneIsMissing() {
        // GIVEN
        Map<PromptTemplateId, Map<String, Object>> variablesById = Map.of(
                PromptTemplateId.PLANNING_DRAFT,
                Map.of("ideaOriginal", "idea", "contextoRag", "contexto"),
                PromptTemplateId.STRUCTURED_STORY,
                Map.of("plantillaCorporativa", "plantilla", AGILE_GUIDE, "guia"),
                PromptTemplateId.STORY_DIVISION,
                Map.of(AGILE_GUIDE, "guia"),
                PromptTemplateId.STORY_REFINEMENT,
                Map.of(WORK_ITEM_ID, "1", ORGANIZATION, "org", PROJECT, "proj", AGILE_GUIDE, "guia"),
                PromptTemplateId.QUALITY_AUDIT,
                Map.of(WORK_ITEM_ID, "1", ORGANIZATION, "org", PROJECT, "proj", STANDARDS, "std"));

        // WHEN / THEN
        for (PromptTemplateId id : PromptTemplateId.values()) {
            String rendered = adapter.render(id, variablesById.get(id));
            assertThat(rendered).as("Plantilla %s", id).isNotBlank().doesNotContain("{{");
        }
    }
}

