package co.com.bancolombia.model.agent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estimación de complejidad de una historia, expresada en Story Points.
 *
 * <p>Value Object inmutable con comportamiento de negocio propio: es la estimación quien sabe si la
 * historia debe dividirse, no el caso de uso.
 *
 * <p>El umbral procede de la guía corporativa {@code HISTORIA_USUARIO.md}, cuya tabla oficial
 * indica «&gt;8 Requiere Dividir», y es coherente con la equivalencia por horas acordada
 * (8 puntos = hasta 30 horas, una semana).
 *
 * @param points    esfuerzo estimado en Story Points; siempre mayor que cero
 * @param level     nivel de incertidumbre asociado
 * @param rationale justificación de la incertidumbre
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-02, DP-03, DP-06
 */
public record ComplexityEstimation(int points, UncertaintyLevel level, String rationale) {

    /**
     * Máximo de Story Points admitido para una única historia (DP-02). Por encima de este valor la
     * guía corporativa exige dividirla.
     */
    public static final int MAX_STORY_POINTS_PER_STORY = 8;

    private static final Pattern POINTS_PATTERN =
            Pattern.compile("\"puntos\"\\s*:\\s*(\\d+)");
    private static final Pattern LEVEL_PATTERN =
            Pattern.compile("\"incertidumbre_nivel\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RATIONALE_PATTERN =
            Pattern.compile("\"incertidumbre_justificacion\"\\s*:\\s*\"([^\"]+)\"");

    private static final UncertaintyLevel FALLBACK_LEVEL = UncertaintyLevel.MEDIA;
    private static final String FALLBACK_RATIONALE = "El modelo no entregó una justificación.";

    public ComplexityEstimation {
        if (points <= 0) {
            throw new IllegalArgumentException(
                    "Los Story Points deben ser mayores que cero, pero se recibió: " + points);
        }
        if (level == null) {
            throw new IllegalArgumentException("El nivel de incertidumbre es obligatorio");
        }
        if (rationale == null || rationale.isBlank()) {
            rationale = FALLBACK_RATIONALE;
        }
    }

    /**
     * Indica si la historia supera el estándar de la célula y debe dividirse.
     *
     * <p>La comparación es {@code > 8} y no {@code >= 13} de forma deliberada: el modelo no siempre
     * respeta la escala Fibonacci, y una estimación de 9, 10 o 12 puntos también supera el máximo
     * admitido.
     */
    public boolean requiresSplit() {
        return points > MAX_STORY_POINTS_PER_STORY;
    }

    /**
     * Extrae la estimación del bloque JSON que el modelo añade al final de su respuesta.
     *
     * <p>Devuelve {@link Optional#empty()} cuando no hay un valor de puntos válido. <b>Nunca</b> se
     * inventa una estimación por defecto: informar de la ausencia es responsabilidad de quien
     * compone la respuesta al usuario (DP-03).
     *
     * @param llmResponse respuesta completa del modelo; admite nulo
     * @return la estimación encontrada, o vacío si el modelo no estimó
     */
    public static Optional<ComplexityEstimation> parseFrom(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return Optional.empty();
        }
        Optional<Integer> points = extractPoints(llmResponse);
        if (points.isEmpty()) {
            return Optional.empty();
        }
        UncertaintyLevel level = extractGroup(LEVEL_PATTERN, llmResponse)
                .flatMap(UncertaintyLevel::fromText)
                .orElse(FALLBACK_LEVEL);
        String rationale = extractGroup(RATIONALE_PATTERN, llmResponse)
                .orElse(FALLBACK_RATIONALE);
        return Optional.of(new ComplexityEstimation(points.get(), level, rationale));
    }

    private static Optional<Integer> extractPoints(String llmResponse) {
        return extractGroup(POINTS_PATTERN, llmResponse).flatMap(ComplexityEstimation::toPositiveInt);
    }

    private static Optional<Integer> toPositiveInt(String rawValue) {
        try {
            int parsed = Integer.parseInt(rawValue);
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> extractGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }
        return Optional.empty();
    }
}

