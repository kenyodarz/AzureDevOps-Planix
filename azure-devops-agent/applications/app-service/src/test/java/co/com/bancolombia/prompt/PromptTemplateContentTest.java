package co.com.bancolombia.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.prompttemplate.ClasspathPromptTemplateAdapter;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Valida el contenido <b>real</b> de las plantillas de prompt que se despliegan en producción.
 *
 * <p>Vive en {@code app-service} porque es el módulo que aloja los recursos
 * {@code resources/prompts/}. Complementa a {@code ClasspathPromptTemplateAdapterTest}, que valida
 * el mecanismo de sustitución con recursos sintéticos.
 *
 * <p>Cubre dos garantías:
 * <ol>
 *   <li><b>Integridad estructural:</b> las cinco plantillas existen, se renderizan y no dejan
 *       marcadores sin resolver.</li>
 *   <li><b>Contenido normativo (DP-02 y DP-06):</b> la tabla oficial de equivalencia Story Points a
 *       horas está presente en todas las plantillas que estiman, y la «Regla de Mínimos de 5
 *       puntos» —que contradecía al prompt de auditoría— ya no aparece en ninguna.</li>
 * </ol>
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-02, DP-06
 */
@DisplayName("Contenido real de las plantillas de prompt")
class PromptTemplateContentTest {

    private static final String WORK_ITEM_ID = "workItemId";
    private static final String ORGANIZATION = "organizacion";
    private static final String PROJECT = "proyecto";
    private static final String AGILE_GUIDE = "guiaAgilidad";
    private static final String STANDARDS = "estandares";
    private static final String CORPORATE_TEMPLATE = "plantillaCorporativa";

    private static final String ESTIMATION_TABLE_HEADER = "| Story Point |";
    private static final String MINIMUM_RULE_FRAGMENT = "Regla de Mínimos";

    private static ClasspathPromptTemplateAdapter adapter;

    @BeforeAll
    static void setUp() {
        adapter = new ClasspathPromptTemplateAdapter();
    }

    private static Map<String, Object> variablesFor(PromptTemplateId id) {
        return switch (id) {
            case PLANNING_DRAFT -> Map.of("ideaOriginal", "idea", "contextoRag", "contexto");
            case STRUCTURED_STORY -> Map.of(CORPORATE_TEMPLATE, "plantilla", AGILE_GUIDE, "guia");
            case STORY_DIVISION -> Map.of(AGILE_GUIDE, "guia");
            case STORY_REFINEMENT -> Map.of(WORK_ITEM_ID, "12345", ORGANIZATION, "org",
                    PROJECT, "proj", AGILE_GUIDE, "guia");
            case QUALITY_AUDIT -> Map.of(WORK_ITEM_ID, "12345", ORGANIZATION, "org",
                    PROJECT, "proj", STANDARDS, "estandares");
        };
    }

    @Test
    @DisplayName("Las cinco plantillas se renderizan sin marcadores pendientes")
    void givenEveryTemplate_whenRender_thenNoPlaceholderRemains() {
        for (PromptTemplateId id : PromptTemplateId.values()) {
            String rendered = adapter.render(id, variablesFor(id));
            assertThat(rendered).as("Plantilla %s", id).isNotBlank().doesNotContain("{{");
        }
    }

    @Test
    @DisplayName("DP-06: la tabla de estimación por horas está en las plantillas que estiman")
    void givenEstimatingTemplates_whenRender_thenContainOfficialEstimationTable() {
        PromptTemplateId[] estimating = {
                PromptTemplateId.STRUCTURED_STORY,
                PromptTemplateId.STORY_DIVISION,
                PromptTemplateId.STORY_REFINEMENT,
                PromptTemplateId.QUALITY_AUDIT};

        for (PromptTemplateId id : estimating) {
            String rendered = adapter.render(id, variablesFor(id));
            assertThat(rendered).as("Plantilla %s", id)
                    .contains(ESTIMATION_TABLE_HEADER)
                    .contains("< 1 hora")
                    .contains("24 - 30 horas (1 semana)")
                    .contains("> 30 horas (> 1 semana)");
        }
    }

    @Test
    @DisplayName("DP-06: ninguna plantilla conserva la Regla de Mínimos de 5 puntos")
    void givenEveryTemplate_whenRender_thenMinimumPointsRuleIsGone() {
        for (PromptTemplateId id : PromptTemplateId.values()) {
            String rendered = adapter.render(id, variablesFor(id));
            assertThat(rendered).as("Plantilla %s", id).doesNotContain(MINIMUM_RULE_FRAGMENT);
        }
    }

    @Test
    @DisplayName("DP-02: las plantillas anuncian la división por encima de 8 puntos")
    void givenStructuredStoryTemplate_whenRender_thenAnnouncesEightPointThreshold() {
        String rendered = adapter.render(PromptTemplateId.STRUCTURED_STORY,
                variablesFor(PromptTemplateId.STRUCTURED_STORY));

        assertThat(rendered).contains("supera los **8 puntos**");
    }

    @Test
    @DisplayName("La plantilla de auditoría conserva los marcadores del bloque JSON de resultado")
    void givenQualityAuditTemplate_whenRender_thenKeepsAuditJsonMarkers() {
        String rendered = adapter.render(PromptTemplateId.QUALITY_AUDIT,
                variablesFor(PromptTemplateId.QUALITY_AUDIT));

        assertThat(rendered).contains("AUDIT_JSON_START")
                .contains("AUDIT_JSON_END")
                .contains("\"workItemId\": \"12345\"");
    }

    @Test
    @DisplayName("La tabla de puntajes usa porcentajes simples, sin escapado de String.format")
    void givenQualityAuditTemplate_whenRender_thenPercentagesAreNotEscaped() {
        String rendered = adapter.render(PromptTemplateId.QUALITY_AUDIT,
                variablesFor(PromptTemplateId.QUALITY_AUDIT));

        assertThat(rendered).contains("| 10% |").doesNotContain("%%");
    }

    @Test
    @DisplayName("La plantilla de refinamiento sigue excluyendo el DoR")
    void givenRefinementTemplate_whenRender_thenExcludesDefinitionOfReady() {
        String rendered = adapter.render(PromptTemplateId.STORY_REFINEMENT,
                variablesFor(PromptTemplateId.STORY_REFINEMENT));

        assertThat(rendered).contains("No incluyas ni evalúes el DoR");
    }
}

