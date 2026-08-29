package co.com.bancolombia.model.agent;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Nivel de incertidumbre asociado a la estimación de una historia.
 *
 * <p>Los valores provienen de texto libre generado por un modelo de lenguaje, por lo que el parseo
 * es tolerante: ignora mayúsculas, espacios sobrantes y tildes. Un valor no reconocible produce
 * {@link Optional#empty()} en lugar de un valor por defecto inventado.
 */
public enum UncertaintyLevel {

    NULA, BAJA, MEDIA, ALTA, CRITICA;

    private static final String DIACRITICAL_MARKS = "\\p{M}";

    /**
     * Interpreta el nivel de incertidumbre devuelto por el modelo.
     *
     * @param rawValue texto tal cual lo devolvió el modelo; admite nulo
     * @return el nivel reconocido, o vacío si el texto no corresponde a ningún nivel conocido
     */
    public static Optional<UncertaintyLevel> fromText(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(rawValue);
        for (UncertaintyLevel level : values()) {
            if (level.name().equals(normalized)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    /**
     * Elimina tildes y diacríticos mediante descomposición Unicode, de modo que «Crítica»,
     * «critica» y «CRITICA» se resuelvan al mismo valor.
     */
    private static String normalize(String rawValue) {
        String decomposed = Normalizer.normalize(rawValue.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll(DIACRITICAL_MARKS, "").toUpperCase(Locale.ROOT);
    }

    /**
     * Etiqueta legible para mostrar al usuario, por ejemplo {@code "Critica"}.
     */
    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}

