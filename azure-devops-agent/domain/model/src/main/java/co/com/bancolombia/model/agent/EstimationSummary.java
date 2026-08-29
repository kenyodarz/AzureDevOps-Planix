package co.com.bancolombia.model.agent;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Compone el texto final que se muestra al usuario a partir de la respuesta cruda del modelo.
 *
 * <p>Concentra en un único lugar lo que antes estaba duplicado entre el flujo de Refinamiento y el
 * de Aprobación: retirar el bloque JSON técnico, añadir el resumen de estimación y cerrar con la
 * llamada a la acción que corresponda.
 *
 * <p>Cuando el modelo no entrega una estimación válida se informa de forma explícita en lugar de
 * mostrar un número inventado (DP-03).
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-02, DP-03
 */
public final class EstimationSummary {

    private static final Pattern FENCED_ESTIMATION_JSON =
            Pattern.compile("(?s)```json\\s*\\{\\s*\"puntos\"[^}]+}\\s*```");
    private static final Pattern PLAIN_ESTIMATION_JSON =
            Pattern.compile("(?s)\\{\\s*\"puntos\"[^}]+}");

    private static final String INFO_BLOCK_TEMPLATE = """
            

            ---
            📊 **Estimación de Complejidad (Story Points):** %d
            ⚠️ **Nivel de Incertidumbre:** %s
            🔍 **Justificación:** %s""";

    private static final String MISSING_ESTIMATION_NOTICE = """
            

            ---
            ⚠️ **No se pudo obtener la estimación:** el modelo no devolvió una estimación válida.
            Por favor, estima manualmente esta historia en Azure DevOps.""";

    private static final String HIGH_COMPLEXITY_ALERT = """
            

            🚨 **Alerta de Complejidad:** Esta historia supera el estándar de la célula de 8 puntos. \
            Sugiero dividirla. Responde 'Dividir' para desglosarla automáticamente en historias de \
            máximo 8 puntos, o 'Crear' para registrarla completa.""";

    private EstimationSummary() {
        // Servicio de dominio sin estado
    }

    /**
     * Construye la respuesta definitiva para el usuario.
     *
     * @param llmResponse         texto devuelto por el modelo
     * @param estimation          estimación extraída, posiblemente ausente
     * @param confirmationMessage llamada a la acción propia del flujo, usada cuando la historia no
     *                            necesita dividirse
     * @return el texto listo para presentar
     */
    public static String compose(String llmResponse,
            Optional<ComplexityEstimation> estimation,
            String confirmationMessage) {
        String cleanedResponse = stripEstimationJson(llmResponse);
        if (estimation.isEmpty()) {
            return cleanedResponse + MISSING_ESTIMATION_NOTICE + confirmationMessage;
        }
        ComplexityEstimation complexity = estimation.get();
        String summary = cleanedResponse + describe(complexity);
        return complexity.requiresSplit()
                ? summary + HIGH_COMPLEXITY_ALERT
                : summary + confirmationMessage;
    }

    /**
     * Retira el bloque JSON de estimación, tanto si viene dentro de una cerca de código como suelto.
     */
    public static String stripEstimationJson(String llmResponse) {
        if (llmResponse == null) {
            return "";
        }
        String withoutFenced = FENCED_ESTIMATION_JSON.matcher(llmResponse).replaceAll("").trim();
        return PLAIN_ESTIMATION_JSON.matcher(withoutFenced).replaceAll("").trim();
    }

    private static String describe(ComplexityEstimation estimation) {
        return String.format(INFO_BLOCK_TEMPLATE,
                estimation.points(),
                estimation.level().displayName(),
                estimation.rationale());
    }
}

