package co.com.bancolombia.model.agent;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de dominio que decide <b>qué</b> flujo debe atender un mensaje del usuario.
 *
 * <p>Es puro: no conoce gateways, ni Spring, ni reactividad. Recibe texto y devuelve una decisión,
 * por lo que se prueba sin un solo mock.
 *
 * <h2>Tabla de precedencia (DP-01)</h2>
 * <ol>
 *   <li>{@link AgentIntent#QUALITY_AUDIT}: verbo de auditoría <b>+</b> {@code (ID: n)}</li>
 *   <li>{@link AgentIntent#REFINEMENT}: marcador «analicemos y refinemos» <b>+</b> {@code (ID: n)}</li>
 *   <li>{@link AgentIntent#APPROVAL} / {@link AgentIntent#DIVISION}: comando corto exacto</li>
 *   <li>{@link AgentIntent#PROGRAM_PLANNING}: comando o frase clave de planeación de programa</li>
 *   <li>{@link AgentIntent#GENERAL}: {@code contextId} general/dashboard, o palabra clave genérica</li>
 *   <li>{@link AgentIntent#PLANNING_DRAFT}: texto libre suficientemente largo</li>
 * </ol>
 *
 * <p>Un {@code (ID: n)} explícito junto a un verbo de intención <b>siempre</b> gana sobre las
 * palabras clave genéricas: el identificador es una señal fuerte y «reporte» es una señal débil.
 *
 * <p>Las palabras clave genéricas se evalúan como <b>palabra completa</b> y descartando las que van
 * gobernadas por un pronombre relativo («…un servicio <i>que consulta</i> el saldo…»), porque en esa
 * posición describen el sistema que se quiere construir, no una acción que se le pide al agente.
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
 */
public final class IntentResolver {

    /**
     * Longitud mínima para tratar un texto como idea de planeación y no como consulta suelta.
     * Un texto de exactamente esta longitud ya se considera idea, tal como venía comportándose el
     * enrutamiento anterior.
     */
    public static final int MIN_PLANNING_TEXT_LENGTH = 25;

    private static final String REFINEMENT_MARKER = "analicemos y refinemos";

    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{M}");

    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,;:!?]");

    private static final Pattern AUDIT_REQUEST_PATTERN = Pattern.compile(
            "(?:audita|evalua|revisa|calidad).*?\\(ID:\\s*(\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern WORK_ITEM_ID_PATTERN = Pattern.compile("\\(ID:\\s*(\\d+)\\)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PROGRAM_PLANNING_COMMAND_PATTERN = Pattern.compile(
            "^/(?:plan|roadmap|program-planning)(?:\\s.*)?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROGRAM_PLANNING_PHRASE_PATTERN = Pattern.compile(
            "\\b(?:planear|planificar|planeacion|planificacion)\\s+(?:q\\d*|trimestre)\\b"
                    + "|\\broadmap\\s+q\\d*\\b"
                    + "|\\bgenerar\\s+roadmap\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Palabras que redirigen la conversación al flujo general de consulta.
     *
     * <p>Los lookbehind descartan las apariciones gobernadas por un pronombre relativo. Solo se
     * incluyen las formas nominales e imperativas; las formas de infinitivo («consultar»,
     * «buscar», «listar») quedan fuera de forma deliberada, porque aparecen de manera natural
     * dentro de una idea de desarrollo y secuestrarían el flujo de planeación (DP-01, regla 2).
     */
    private static final Pattern GENERAL_KEYWORD_PATTERN = Pattern.compile(
            "(?<!\\bque\\s)(?<!\\bquien\\s)(?<!\\bcual\\s)"
                    + "\\b(?:listas?|busca|consultas?|reportes?)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final String GENERAL_CONTEXT_PREFIX = "general";

    private static final String DASHBOARD_CONTEXT_PREFIX = "dashboard";

    /** Comandos cortos que nunca deben interpretarse como una idea de planeación. */
    private static final List<String> SHORT_COMMANDS = List.of("aprobado", "crear", "si", "no",
            "procede", "dividela", "divide", "de acuerdo", "dividir");

    private static final List<String> APPROVAL_COMMANDS = List.of("aprobado", "si", "de acuerdo");

    private static final List<String> DIVISION_COMMANDS = List.of("dividir", "dividela", "divide");

    /**
     * Resuelve la intención del mensaje aplicando la tabla de precedencia.
     *
     * @param userText  texto escrito por el usuario; admite nulo
     * @param contextId identificador de contexto enviado por el cliente; admite nulo
     * @return la intención detectada junto con los parámetros extraídos; nunca nulo
     */
    public IntentResolution resolve(String userText, String contextId) {
        String sanitized = sanitize(userText);
        String command = asCommand(sanitized);
        return asQualityAudit(sanitized)
                .or(() -> asRefinement(sanitized))
                .or(() -> asShortCommand(command))
                .or(() -> asProgramPlanning(sanitized, command))
                .or(() -> asGeneral(sanitized, contextId))
                .orElseGet(() -> IntentResolution.of(planningOrGeneral(sanitized)));
    }

    private Optional<IntentResolution> asQualityAudit(String sanitized) {
        Matcher auditMatcher = AUDIT_REQUEST_PATTERN.matcher(sanitized);
        if (auditMatcher.find()) {
            return Optional.of(
                    IntentResolution.withWorkItem(AgentIntent.QUALITY_AUDIT, auditMatcher.group(1)));
        }
        return Optional.empty();
    }

    private Optional<IntentResolution> asRefinement(String sanitized) {
        if (!sanitized.contains(REFINEMENT_MARKER)) {
            return Optional.empty();
        }
        Matcher idMatcher = WORK_ITEM_ID_PATTERN.matcher(sanitized);
        if (idMatcher.find()) {
            return Optional.of(
                    IntentResolution.withWorkItem(AgentIntent.REFINEMENT, idMatcher.group(1)));
        }
        return Optional.empty();
    }

    private Optional<IntentResolution> asShortCommand(String command) {
        if (APPROVAL_COMMANDS.contains(command)) {
            return Optional.of(IntentResolution.of(AgentIntent.APPROVAL));
        }
        if (DIVISION_COMMANDS.contains(command)) {
            return Optional.of(IntentResolution.of(AgentIntent.DIVISION));
        }
        return Optional.empty();
    }

    private Optional<IntentResolution> asProgramPlanning(String sanitized, String command) {
        if (PROGRAM_PLANNING_COMMAND_PATTERN.matcher(command).matches()
                || PROGRAM_PLANNING_COMMAND_PATTERN.matcher(sanitized).matches()
                || PROGRAM_PLANNING_PHRASE_PATTERN.matcher(sanitized).find()) {
            return Optional.of(IntentResolution.of(AgentIntent.PROGRAM_PLANNING));
        }
        return Optional.empty();
    }

    private Optional<IntentResolution> asGeneral(String sanitized, String contextId) {
        if (isGeneralContext(contextId) || GENERAL_KEYWORD_PATTERN.matcher(sanitized).find()) {
            return Optional.of(IntentResolution.of(AgentIntent.GENERAL));
        }
        return Optional.empty();
    }

    private boolean isGeneralContext(String contextId) {
        return contextId != null
                && (contextId.startsWith(GENERAL_CONTEXT_PREFIX)
                || contextId.startsWith(DASHBOARD_CONTEXT_PREFIX));
    }

    /**
     * Último escalón de la tabla: texto libre suficientemente largo que no sea un comando corto.
     * Cualquier otra cosa se atiende como consulta general, replicando el comportamiento de la
     * antigua rama por defecto del caso de uso.
     */
    private AgentIntent planningOrGeneral(String sanitized) {
        boolean isFreeText = sanitized.length() >= MIN_PLANNING_TEXT_LENGTH
                && !SHORT_COMMANDS.contains(asCommand(sanitized));
        return isFreeText ? AgentIntent.PLANNING_DRAFT : AgentIntent.GENERAL;
    }

    /**
     * Descompone los diacríticos y pasa a minúsculas, conservando la puntuación necesaria para
     * reconocer el patrón {@code (ID: n)}.
     */
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return DIACRITICAL_MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** Forma canónica para comparar contra los comandos cortos: sin puntuación ni espacios extra. */
    private String asCommand(String sanitized) {
        return TRAILING_PUNCTUATION.matcher(sanitized).replaceAll("").trim();
    }
}

