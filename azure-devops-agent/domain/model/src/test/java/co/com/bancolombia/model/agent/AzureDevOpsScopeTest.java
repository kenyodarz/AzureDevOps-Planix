package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Contrato del {@link AzureDevOpsScope}.
 */
@DisplayName("AzureDevOpsScope - Ámbito de trabajo en Azure DevOps")
class AzureDevOpsScopeTest {

    private static final String ORGANIZATION = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";

    @Test
    @DisplayName("Conserva organización y proyecto tal como se recibieron")
    void givenValidValues_whenBuild_thenKeepsThem() {
        // WHEN
        AzureDevOpsScope scope = new AzureDevOpsScope(ORGANIZATION, PROJECT);

        // THEN
        assertThat(scope.organization()).isEqualTo(ORGANIZATION);
        assertThat(scope.project()).isEqualTo(PROJECT);
    }

    @ParameterizedTest(name = "organización \"{0}\" es inválida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("La organización es obligatoria")
    void givenBlankOrganization_whenBuild_thenThrows(String organization) {
        // WHEN / THEN
        assertThatThrownBy(() -> new AzureDevOpsScope(organization, PROJECT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organización");
    }

    @ParameterizedTest(name = "proyecto \"{0}\" es inválido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("El proyecto es obligatorio")
    void givenBlankProject_whenBuild_thenThrows(String project) {
        // WHEN / THEN
        assertThatThrownBy(() -> new AzureDevOpsScope(ORGANIZATION, project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proyecto");
    }

    @Test
    @DisplayName("Dos ámbitos con los mismos valores son iguales")
    void givenSameValues_whenCompare_thenAreEqual() {
        // WHEN / THEN
        assertThat(new AzureDevOpsScope(ORGANIZATION, PROJECT))
                .isEqualTo(new AzureDevOpsScope(ORGANIZATION, PROJECT))
                .hasSameHashCodeAs(new AzureDevOpsScope(ORGANIZATION, PROJECT));
    }
}

