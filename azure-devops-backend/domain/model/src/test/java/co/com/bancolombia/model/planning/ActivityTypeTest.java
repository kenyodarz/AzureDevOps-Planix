package co.com.bancolombia.model.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ActivityType - Tipificación de actividades HU vs HA")
class ActivityTypeTest {

    @Test
    @DisplayName("Valida los valores y atributos constantes de cada tipo de actividad")
    void givenEnumValues_whenInspectProperties_thenMatchExpectedTagsAndDescriptions() {
        // GIVEN / WHEN / THEN
        assertThat(ActivityType.HU.code()).isEqualTo("HU");
        assertThat(ActivityType.HU.getCode()).isEqualTo("HU");
        assertThat(ActivityType.HU.tag()).isEqualTo("HU");
        assertThat(ActivityType.HU.getTag()).isEqualTo("HU");
        assertThat(ActivityType.HU.description()).isEqualTo("Historia de Usuario");
        assertThat(ActivityType.HU.getDescription()).isEqualTo("Historia de Usuario");

        assertThat(ActivityType.HA.code()).isEqualTo("HA");
        assertThat(ActivityType.HA.getCode()).isEqualTo("HA");
        assertThat(ActivityType.HA.tag()).isEqualTo("HA");
        assertThat(ActivityType.HA.getTag()).isEqualTo("HA");
        assertThat(ActivityType.HA.description()).isEqualTo("Habilitador de Arquitectura");
        assertThat(ActivityType.HA.getDescription()).isEqualTo("Habilitador de Arquitectura");
    }

    @ParameterizedTest(name = "Tag \"{0}\" resuelve a HU")
    @ValueSource(strings = {"HU", "hu", "Hu", "  hu  ", "USER_STORY", "user_story",
            "Historia de Usuario", "historia de usuario"})
    @DisplayName("Resuelve HU a partir de tags y variantes comunes")
    void givenUserStoryVariants_whenFromTag_thenResolvesHU(String variant) {
        // WHEN / THEN
        assertThat(ActivityType.fromTag(variant)).isEqualTo(ActivityType.HU);
        assertThat(ActivityType.parse(variant)).isEqualTo(ActivityType.HU);
        assertThat(ActivityType.fromString(variant)).isEqualTo(ActivityType.HU);
    }

    @ParameterizedTest(name = "Tag \"{0}\" resuelve a HA")
    @ValueSource(strings = {"HA", "ha", "Ha", "  ha  ", "ENABLER", "enabler",
            "Habilitador de Arquitectura", "Habilitador"})
    @DisplayName("Resuelve HA a partir de tags y variantes comunes")
    void givenEnablerVariants_whenFromTag_thenResolvesHA(String variant) {
        // WHEN / THEN
        assertThat(ActivityType.fromTag(variant)).isEqualTo(ActivityType.HA);
        assertThat(ActivityType.parse(variant)).isEqualTo(ActivityType.HA);
        assertThat(ActivityType.fromString(variant)).isEqualTo(ActivityType.HA);
    }

    @ParameterizedTest(name = "Tag inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException cuando el tag es nulo o blanco")
    void givenBlankOrNullTag_whenFromTag_thenThrowsIllegalArgumentException(String invalidTag) {
        // WHEN / THEN
        assertThatThrownBy(() -> ActivityType.fromTag(invalidTag))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tag de actividad es obligatorio");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException cuando el tag no es reconocido")
    void givenUnrecognizedTag_whenFromTag_thenThrowsIllegalArgumentException() {
        // WHEN / THEN
        assertThatThrownBy(() -> ActivityType.fromTag("UNKNOWN_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag de actividad no reconocido: UNKNOWN_TYPE");
    }
}
