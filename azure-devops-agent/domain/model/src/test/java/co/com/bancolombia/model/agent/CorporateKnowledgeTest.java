package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Contrato del {@link CorporateKnowledge}.
 */
@DisplayName("CorporateKnowledge - Documentación corporativa de los prompts")
class CorporateKnowledgeTest {

    private static final String STORY_TEMPLATE = "## Plantilla HU/HA corporativa";
    private static final String AGILE_GUIDE = "## Guía de agilidad";
    private static final String QUALITY_STANDARDS = "## Estándares de auditoría";

    @Test
    @DisplayName("Conserva los tres documentos tal como se recibieron")
    void givenValidDocuments_whenBuild_thenKeepsThem() {
        // WHEN
        CorporateKnowledge knowledge = validKnowledge();

        // THEN
        assertThat(knowledge.storyTemplate()).isEqualTo(STORY_TEMPLATE);
        assertThat(knowledge.agileGuide()).isEqualTo(AGILE_GUIDE);
        assertThat(knowledge.qualityStandards()).isEqualTo(QUALITY_STANDARDS);
    }

    @Test
    @DisplayName("auditStandards() combina la guía de agilidad con los estándares de auditoría")
    void givenKnowledge_whenAuditStandards_thenCombinesBothDocuments() {
        // WHEN
        String combined = validKnowledge().auditStandards();

        // THEN
        assertThat(combined)
                .isEqualTo(AGILE_GUIDE + "\n\n" + QUALITY_STANDARDS)
                .startsWith(AGILE_GUIDE)
                .endsWith(QUALITY_STANDARDS);
    }

    @ParameterizedTest(name = "plantilla \"{0}\" es inválida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("La plantilla de historia es obligatoria")
    void givenBlankStoryTemplate_whenBuild_thenThrows(String storyTemplate) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new CorporateKnowledge(storyTemplate, AGILE_GUIDE, QUALITY_STANDARDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plantilla de historia");
    }

    @ParameterizedTest(name = "guía \"{0}\" es inválida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("La guía de agilidad es obligatoria")
    void givenBlankAgileGuide_whenBuild_thenThrows(String agileGuide) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new CorporateKnowledge(STORY_TEMPLATE, agileGuide, QUALITY_STANDARDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("guía de agilidad");
    }

    @ParameterizedTest(name = "estándares \"{0}\" son inválidos")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Los estándares de auditoría son obligatorios")
    void givenBlankQualityStandards_whenBuild_thenThrows(String qualityStandards) {
        // WHEN / THEN
        assertThatThrownBy(
                () -> new CorporateKnowledge(STORY_TEMPLATE, AGILE_GUIDE, qualityStandards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estándares de auditoría");
    }

    private static CorporateKnowledge validKnowledge() {
        return new CorporateKnowledge(STORY_TEMPLATE, AGILE_GUIDE, QUALITY_STANDARDS);
    }
}

