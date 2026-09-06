package co.com.bancolombia.usecase.planning;

import static java.util.logging.Level.WARNING;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.planning.ActivityType;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.planning.ProgramPlanResult;
import co.com.bancolombia.model.planning.SprintAllocation;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Caso de uso puro de orquestación para Program Planning (planeación trimestral macro).
 *
 * <p>Integra {@link SpecStoragePort} para la inyección de contexto documental,
 * {@link PromptTemplatePort} para renderizar la plantilla externalizada {@code PROGRAM_PLANNING}, y
 * {@link ChatGateway} para interactuar con el modelo de lenguaje.
 *
 * <p>Aplica rigurosamente las reglas rectoras:
 * <ul>
 *   <li><b>DP-PL-02:</b> Separación estricta entre Historias de Usuario (HU, alcance hasta QA) e
 *       Historias Habilitadoras (HA, setups técnicos y paso a producción HyMS).</li>
 *   <li><b>DP-PL-04:</b> Persistencia del roadmap estructurado maestro {@code ideas_planning_{quarter}.md}
 *       mediante {@link SpecStoragePort}.</li>
 * </ul>
 */
@Log
@RequiredArgsConstructor
public class ProgramPlanningUseCase {

    private static final String DEFAULT_FALLBACK_SPEC = "ideas_planning_q3.md";
    private static final String NO_SPEC_CONTEXT = "No hay especificaciones técnicas previas disponibles.";
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private static final Pattern HYMS_KEYWORDS = Pattern.compile(
            "\\b(?:hyms|paso a producci[oó]n|runbook|despliegue a producci[oó]n|producci[oó]n)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SPEC_FILE_PATTERN = Pattern.compile("[\\w\\-]++\\.md");
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^[|:\\-\\s]+$");

    private static final Set<String> EMPTY_DEP_VALUES = Set.of(
            "ninguna", "ninguno", "n/a", "na", "none", "-", "sin dependencias", "");

    private final PromptTemplatePort promptTemplatePort;
    private final SpecStoragePort specStoragePort;
    private final ChatGateway chatGateway;

    /**
     * Ejecuta el flujo de planeación con un identificador de contexto derivado del trimestre.
     *
     * @param request petición estructurada de planeación
     * @return Mono con el resultado de la planeación
     */
    public Mono<ProgramPlanResult> plan(ProgramPlanRequest request) {
        Objects.requireNonNull(request, "La solicitud de planeación es obligatoria");
        String contextId = "planning-" + normalizeSlug(request.quarter());
        return plan(request, contextId);
    }

    /**
     * Ejecuta el flujo de planeación con un identificador de contexto específico.
     *
     * @param request   petición estructurada de planeación
     * @param contextId identificador de correlación o contexto
     * @return Mono con el resultado estructurado de la planeación
     */
    public Mono<ProgramPlanResult> plan(ProgramPlanRequest request, String contextId) {
        Objects.requireNonNull(request, "La solicitud de planeación es obligatoria");

        return loadSpecsContext(request)
                .flatMap(specsContext -> {
                    String prompt = renderPrompt(request, specsContext);
                    return chatGateway.sendMessage(prompt, contextId);
                })
                .flatMap(llmResponse -> {
                    if (llmResponse == null || llmResponse.isBlank()) {
                        log.log(WARNING, "Respuesta vacía o nula del LLM para el trimestre {0}",
                                request.quarter());
                        return buildEmptyResult(request);
                    }
                    return processAndPersistPlan(request, llmResponse);
                });
    }

    /**
     * Alias de ejecución estándar para interoperabilidad.
     */
    public Mono<ProgramPlanResult> execute(ProgramPlanRequest request) {
        return plan(request);
    }

    private Mono<String> loadSpecsContext(ProgramPlanRequest request) {
        List<String> fronts = request.targetFronts();
        if (fronts != null && !fronts.isEmpty()) {
            return Flux.fromIterable(fronts)
                    .flatMap(front -> {
                        String specFileName = resolveFrontSpecFileName(front);
                        return specStoragePort.getSpec(specFileName)
                                .map(doc -> "### Frente: " + front + "\n" + doc.content())
                                .onErrorResume(err -> {
                                    log.log(WARNING,
                                            "No se encontró spec para el frente [{0}]: {1}",
                                            new Object[]{front, err.getMessage()});
                                    return Mono.empty();
                                });
                    })
                    .collectList()
                    .flatMap(specs -> {
                        if (specs.isEmpty()) {
                            return loadMasterOrFallbackSpec(request.quarter());
                        }
                        return Mono.just(String.join("\n\n", specs));
                    });
        }
        return loadMasterOrFallbackSpec(request.quarter());
    }

    private Mono<String> loadMasterOrFallbackSpec(String quarter) {
        String masterSpecName = resolveMasterSpecFileName(quarter);
        return specStoragePort.getSpec(masterSpecName)
                .map(SpecDocument::content)
                .onErrorResume(err -> specStoragePort.getSpec(DEFAULT_FALLBACK_SPEC)
                        .map(SpecDocument::content)
                        .onErrorResume(e -> Mono.just(NO_SPEC_CONTEXT)))
                .defaultIfEmpty(NO_SPEC_CONTEXT);
    }

    private String renderPrompt(ProgramPlanRequest request, String specsContext) {
        String frontsDescription =
                (request.targetFronts() == null || request.targetFronts().isEmpty())
                        ? "Todos los frentes"
                        : String.join(", ", request.targetFronts());

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("trimestre", request.quarter());
        variables.put("capacidadSprint", request.maxCapacityPerSprint());
        variables.put("frentes", frontsDescription);
        variables.put("objetivos", request.objectives());
        variables.put("specsContexto",
                specsContext != null && !specsContext.isBlank() ? specsContext : NO_SPEC_CONTEXT);

        return promptTemplatePort.render(PromptTemplateId.PROGRAM_PLANNING, variables);
    }

    private Mono<ProgramPlanResult> processAndPersistPlan(ProgramPlanRequest request,
            String llmResponse) {
        String masterSpecFileName = resolveMasterSpecFileName(request.quarter());
        String executiveSummary = extractExecutiveSummary(llmResponse, request);
        List<SprintAllocation> allocations = parseAllocations(llmResponse, request);
        List<String> generatedSpecNames = extractGeneratedSpecNames(llmResponse, masterSpecFileName,
                request.targetFronts());

        ProgramPlanResult result = ProgramPlanResult.builder()
                .quarter(request.quarter())
                .executiveSummary(executiveSummary)
                .allocations(allocations)
                .generatedSpecNames(generatedSpecNames)
                .build();

        return specStoragePort.saveSpec(masterSpecFileName, llmResponse)
                .thenReturn(result)
                .onErrorResume(err -> {
                    log.log(WARNING, "Error al persistir el spec maestro [{0}]: {1}",
                            new Object[]{masterSpecFileName, err.getMessage()});
                    return Mono.just(result);
                });
    }

    private Mono<ProgramPlanResult> buildEmptyResult(ProgramPlanRequest request) {
        String masterSpecFileName = resolveMasterSpecFileName(request.quarter());
        ProgramPlanResult emptyResult = ProgramPlanResult.builder()
                .quarter(request.quarter())
                .executiveSummary("No se generó planeación para " + request.quarter()
                        + " por respuesta vacía del modelo.")
                .allocations(List.of())
                .generatedSpecNames(List.of(masterSpecFileName))
                .build();
        return Mono.just(emptyResult);
    }

    private String extractExecutiveSummary(String markdown, ProgramPlanRequest request) {
        int summaryHeaderIdx = markdown.indexOf("## 1. Resumen Ejecutivo");
        if (summaryHeaderIdx != -1) {
            int contentStart = summaryHeaderIdx + "## 1. Resumen Ejecutivo".length();
            int nextSectionIdx = markdown.indexOf("## 2.", contentStart);
            if (nextSectionIdx == -1) {
                nextSectionIdx = markdown.indexOf("##", contentStart);
            }
            String summary = (nextSectionIdx != -1)
                    ? markdown.substring(contentStart, nextSectionIdx).trim()
                    : markdown.substring(contentStart).trim();
            if (!summary.isBlank()) {
                return summary;
            }
        }
        return "Planeación estratégica para " + request.quarter() + " orientada a: "
                + request.objectives();
    }

    private List<SprintAllocation> parseAllocations(String markdown, ProgramPlanRequest request) {
        List<SprintAllocation> allocations = new ArrayList<>();
        String[] lines = markdown.split("\\r?\\n");
        boolean inRoadmapSection = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## 2. Tabla de Roadmap")) {
                inRoadmapSection = true;
            } else if (inRoadmapSection && trimmed.startsWith("## 3.")) {
                inRoadmapSection = false;
            } else if (isCandidateTableRow(trimmed)) {
                parseTableRow(trimmed, request).ifPresent(allocations::add);
            }
        }

        return allocations;
    }

    private boolean isCandidateTableRow(String trimmed) {
        if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
            return false;
        }
        if (trimmed.contains("Sprint") && trimmed.contains("Tipo")) {
            return false;
        }
        return !TABLE_SEPARATOR_PATTERN.matcher(trimmed).matches();
    }

    private java.util.Optional<SprintAllocation> parseTableRow(String row,
            ProgramPlanRequest request) {
        String[] parts = row.split("\\|");
        if (parts.length < 5) {
            return java.util.Optional.empty();
        }

        List<String> cols = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            cols.add(parts[i].trim());
        }

        if (cols.size() < 4) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(buildAllocationFromCols(cols, request));
    }

    private SprintAllocation buildAllocationFromCols(List<String> cols,
            ProgramPlanRequest request) {
        int sprintNumber = parseNumber(getCol(cols, 0), 1);
        String rawType = getCol(cols, 1);

        boolean hasUnescapedPipe = cols.size() >= 5
                && !hasDigits(getCol(cols, 3))
                && hasDigits(getCol(cols, 4));
        int titleOffset = hasUnescapedPipe ? 1 : 0;

        String title = hasUnescapedPipe
                ? getCol(cols, 2) + " | " + getCol(cols, 3)
                : getCol(cols, 2);
        if (title.isBlank()) {
            title = "Actividad Sprint " + sprintNumber;
        }

        int storyPoints = parseNumber(getCol(cols, 3 + titleOffset), 0);
        String front = getCol(cols, 4 + titleOffset);
        if (front.isBlank()) {
            front = resolveDefaultFront(request);
        }

        String rawDeps = getCol(cols, 5 + titleOffset);
        String description = getCol(cols, 6 + titleOffset);

        ActivityType type = resolveAndEnforceActivityType(rawType, title, description);
        List<String> dependencies = parseDependencies(rawDeps);

        return SprintAllocation.builder()
                .sprintNumber(sprintNumber)
                .type(type)
                .title(title)
                .description(description)
                .storyPoints(Math.max(0, storyPoints))
                .front(front)
                .dependencies(dependencies)
                .build();
    }

    private String getCol(List<String> cols, int index) {
        return index < cols.size() ? cols.get(index) : "";
    }

    private boolean hasDigits(String text) {
        return text != null && DIGITS_PATTERN.matcher(text).find();
    }

    /**
     * Resuelve el tipo de actividad validando y reforzando la regla rectora DP-PL-02. Si una HU
     * incluye alcance de HyMS, Runbook o paso a producción, se clasifica como ENABLER (HA).
     */
    private ActivityType resolveAndEnforceActivityType(String rawType, String title,
            String description) {
        ActivityType declaredType;
        try {
            declaredType = ActivityType.fromTag(rawType);
        } catch (IllegalArgumentException _) {
            declaredType = rawType.toUpperCase(Locale.ROOT).contains("HA")
                    ? ActivityType.ENABLER
                    : ActivityType.USER_STORY;
        }

        // DP-PL-02: si la actividad tiene alcance de HyMS o paso a producción, debe ser obligatoriamente HA
        boolean containsHyMSScope = HYMS_KEYWORDS.matcher(title).find()
                || HYMS_KEYWORDS.matcher(description).find();

        if (containsHyMSScope && declaredType == ActivityType.USER_STORY) {
            log.log(WARNING,
                    "Regla DP-PL-02: re-clasificando HU a HA por contener alcance HyMS/Producción: [{0}]",
                    title);
            return ActivityType.ENABLER;
        }

        return declaredType;
    }

    private List<String> parseDependencies(String rawDeps) {
        if (rawDeps == null || rawDeps.isBlank()) {
            return List.of();
        }
        String[] tokens = rawDeps.split("[,;]");
        List<String> list = new ArrayList<>();
        for (String token : tokens) {
            String clean = token.trim();
            if (!clean.isEmpty() && !EMPTY_DEP_VALUES.contains(clean.toLowerCase(Locale.ROOT))) {
                list.add(clean);
            }
        }
        return list;
    }

    private List<String> extractGeneratedSpecNames(String markdown, String masterSpecName,
            List<String> targetFronts) {
        Set<String> specs = new LinkedHashSet<>();
        specs.add(masterSpecName);

        int section3Idx = markdown.indexOf("## 3. Especificaciones y Entregables por Frente");
        if (section3Idx != -1) {
            String section3Text = markdown.substring(section3Idx);
            Matcher matcher = SPEC_FILE_PATTERN.matcher(section3Text);
            while (matcher.find()) {
                specs.add(matcher.group().toLowerCase(Locale.ROOT));
            }
        }

        if (targetFronts != null) {
            for (String front : targetFronts) {
                specs.add(resolveFrontSpecFileName(front));
            }
        }

        return new ArrayList<>(specs);
    }

    private int parseNumber(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        Matcher matcher = DIGITS_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException _) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String resolveDefaultFront(ProgramPlanRequest request) {
        if (request.targetFronts() != null && !request.targetFronts().isEmpty()) {
            return request.targetFronts().get(0);
        }
        return "General";
    }

    private String resolveMasterSpecFileName(String quarter) {
        return "ideas_planning_" + normalizeSlug(quarter) + ".md";
    }

    private String resolveFrontSpecFileName(String front) {
        return "frente_" + normalizeSlug(front) + ".md";
    }

    private String normalizeSlug(String text) {
        if (text == null || text.isBlank()) {
            return "general";
        }
        String normalized = NON_ALPHANUMERIC.matcher(text.trim().toLowerCase(Locale.ROOT))
                .replaceAll("_");
        int start = 0;
        while (start < normalized.length() && normalized.charAt(start) == '_') {
            start++;
        }
        int end = normalized.length();
        while (end > start && normalized.charAt(end - 1) == '_') {
            end--;
        }
        normalized = normalized.substring(start, end);
        return normalized.isEmpty() ? "general" : normalized;
    }
}
