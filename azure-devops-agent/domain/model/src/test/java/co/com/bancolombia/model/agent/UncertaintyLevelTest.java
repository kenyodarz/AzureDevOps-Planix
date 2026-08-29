package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UncertaintyLevel")
class UncertaintyLevelTest {

    private static Stream<Arguments> recognizableValues() {
        return Stream.of(
                Arguments.of("Nula", UncertaintyLevel.NULA),
                Arguments.of("Baja", UncertaintyLevel.BAJA),
                Arguments.of("MEDIA", UncertaintyLevel.MEDIA),
                Arguments.of("alta", UncertaintyLevel.ALTA),
                Arguments.of("Critica", UncertaintyLevel.CRITICA),
                Arguments.of("Crítica", UncertaintyLevel.CRITICA),
                Arguments.of("CRÍTICA", UncertaintyLevel.CRITICA),
                Arguments.of("  media  ", UncertaintyLevel.MEDIA));
    }

    @ParameterizedTest(name = "\"{0}\" se interpreta como {1}")
    @MethodSource("recognizableValues")
    @DisplayName("Interpreta el nivel ignorando mayúsculas, tildes y espacios")
    void givenRecognizableText_whenFromText_thenReturnsLevel(String rawValue,
            UncertaintyLevel expected) {
        // WHEN
        Optional<UncertaintyLevel> result = UncertaintyLevel.fromText(rawValue);

        // THEN
        assertThat(result).contains(expected);
    }

    @ParameterizedTest(name = "\"{0}\" no es reconocible")
    @ValueSource(strings = {"desconocido", "High", "5", "   ", "medio"})
    @DisplayName("Un valor no reconocible devuelve vacío en lugar de un nivel por defecto")
    void givenUnknownText_whenFromText_thenReturnsEmpty(String rawValue) {
        // WHEN / THEN
        assertThat(UncertaintyLevel.fromText(rawValue)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Texto nulo o vacío devuelve vacío")
    void givenNullOrEmptyText_whenFromText_thenReturnsEmpty(String rawValue) {
        // WHEN / THEN
        assertThat(UncertaintyLevel.fromText(rawValue)).isEmpty();
    }

    @Test
    @DisplayName("La etiqueta legible capitaliza solo la primera letra")
    void givenLevel_whenDisplayName_thenCapitalizesFirstLetterOnly() {
        // WHEN / THEN
        assertThat(UncertaintyLevel.CRITICA.displayName()).isEqualTo("Critica");
        assertThat(UncertaintyLevel.MEDIA.displayName()).isEqualTo("Media");
        assertThat(UncertaintyLevel.NULA.displayName()).isEqualTo("Nula");
    }
}

