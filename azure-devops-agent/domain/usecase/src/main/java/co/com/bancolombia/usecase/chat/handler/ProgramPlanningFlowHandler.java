package co.com.bancolombia.usecase.chat.handler;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.planning.ActivityType;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.planning.ProgramPlanResult;
import co.com.bancolombia.model.planning.SprintAllocation;
import co.com.bancolombia.usecase.planning.ProgramPlanningUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

/**
 * Handler conversacional para la intención {@link AgentIntent#PROGRAM_PLANNING}.
 *
 * <p>Extrae los parámetros de planeación a partir del mensaje del usuario (comandos como
 * {@code /plan}, {@code /roadmap} o lenguaje natural), delega la orquestación en
 * {@link ProgramPlanningUseCase} y produce una respuesta consolidada en Markdown para el chat.
 */
@Log
@RequiredArgsConstructor
public class ProgramPlanningFlowHandler implements ChatFlowHandler {

    private static final String DEFAULT_QUARTER = "Q3-2026";
    private static final int DEFAULT_SPRINT_COUNT = 6;
    private static final int DEFAULT_CAPACITY_PER_SPRINT = 34;

    private static final Pattern QUARTER_PATTERN = Pattern.compile(
            "\\b(Q[1-4](?:[-_ ]?20\\d\\d)?)\\b|\\btrimestre\\s+([a-z0-9_-]+)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SPRINTS_PATTERN = Pattern.compile(
            "\\b(\\d+)\\s+sprints?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACITY_KEYWORD_PATTERN = Pattern.compile(
            "\\bcapacidad(?:\\s+m[aá]xima)?(?:\\s+por\\s+sprint)?[:\\s]+(\\d+)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern STORY_POINTS_PATTERN = Pattern.compile(
            "\\b(\\d+)\\s*(?:sp|pts|puntos|story points)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BRACKETED_FRONT_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern FRONTS_PREFIX_PATTERN = Pattern.compile(
            "\\bfrentes?\\s*:\\s*([^\\n.;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_PREFIX_PATTERN = Pattern.compile(
            "^/(?:plan|roadmap|program-planning)\\s*", Pattern.CASE_INSENSITIVE);

    private final ProgramPlanningUseCase programPlanningUseCase;

    @Override
    public AgentIntent supports() {
        return AgentIntent.PROGRAM_PLANNING;
    }

    @Override
    public Mono<String> handle(ChatFlowContext context) {
        String userText = context.userText();
        ProgramPlanRequest request = parsePlanRequest(userText);

        return programPlanningUseCase.plan(request, context.contextId())
                .map(this::formatChatResponse);
    }

    private ProgramPlanRequest parsePlanRequest(String userText) {
        String quarter = extractQuarter(userText);
        int sprintCount = extractSprintCount(userText);
        int capacity = extractCapacity(userText);
        List<String> fronts = extractFronts(userText);
        String objectives = extractObjectives(userText);

        return ProgramPlanRequest.builder()
                .quarter(quarter)
                .sprintCount(sprintCount)
                .maxCapacityPerSprint(capacity)
                .targetFronts(fronts)
                .objectives(objectives)
                .build();
    }

    private String extractQuarter(String userText) {
        if (userText == null) {
            return DEFAULT_QUARTER;
        }
        Matcher matcher = QUARTER_PATTERN.matcher(userText);
        if (matcher.find()) {
            String q = matcher.group(matcher.group(1) != null ? 1 : 2);
            return q.toUpperCase(Locale.ROOT).replace(" ", "-");
        }
        return DEFAULT_QUARTER;
    }

    private int extractSprintCount(String userText) {
        if (userText == null) {
            return DEFAULT_SPRINT_COUNT;
        }
        Matcher matcher = SPRINTS_PATTERN.matcher(userText);
        if (matcher.find()) {
            return parsePositiveInt(matcher.group(1), DEFAULT_SPRINT_COUNT);
        }
        return DEFAULT_SPRINT_COUNT;
    }

    private int extractCapacity(String userText) {
        if (userText == null) {
            return DEFAULT_CAPACITY_PER_SPRINT;
        }
        Matcher keywordMatcher = CAPACITY_KEYWORD_PATTERN.matcher(userText);
        if (keywordMatcher.find()) {
            return parsePositiveInt(keywordMatcher.group(1), DEFAULT_CAPACITY_PER_SPRINT);
        }
        Matcher spMatcher = STORY_POINTS_PATTERN.matcher(userText);
        if (spMatcher.find()) {
            return parsePositiveInt(spMatcher.group(1), DEFAULT_CAPACITY_PER_SPRINT);
        }
        return DEFAULT_CAPACITY_PER_SPRINT;
    }

    private int parsePositiveInt(String text, int defaultValue) {
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }

    private List<String> extractFronts(String userText) {
        List<String> fronts = new ArrayList<>();
        if (userText == null) {
            return fronts;
        }

        // 1. Frente entre corchetes [Frente]
        Matcher bracketMatcher = BRACKETED_FRONT_PATTERN.matcher(userText);
        while (bracketMatcher.find()) {
            String front = bracketMatcher.group(1).trim();
            if (!front.isEmpty()) {
                fronts.add(front);
            }
        }
        if (!fronts.isEmpty()) {
            return fronts;
        }

        // 2. Frentes: A, B, C
        Matcher frontsMatcher = FRONTS_PREFIX_PATTERN.matcher(userText);
        if (frontsMatcher.find()) {
            String rawFronts = frontsMatcher.group(1);
            String[] tokens = rawFronts.split("[,;]");
            for (String token : tokens) {
                String clean = token.trim();
                if (!clean.isEmpty() && !fronts.contains(clean)) {
                    fronts.add(clean);
                }
            }
        }

        return fronts;
    }

    private String extractObjectives(String userText) {
        if (userText == null || userText.isBlank()) {
            return "Planeación macro del trimestre y definición de roadmap técnico y funcional.";
        }
        String cleaned = COMMAND_PREFIX_PATTERN.matcher(userText.trim()).replaceFirst("").trim();
        return cleaned.isBlank()
                ? "Planeación macro del trimestre y definición de roadmap técnico y funcional."
                : cleaned;
    }

    private String formatChatResponse(ProgramPlanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Roadmap de Planeación — ").append(result.quarter()).append("\n\n");
        sb.append("## Resumen Ejecutivo\n\n").append(result.executiveSummary()).append("\n\n");

        long huCount = result.allocations().stream()
                .filter(a -> a.type() == ActivityType.USER_STORY)
                .count();
        long haCount = result.allocations().stream()
                .filter(a -> a.type() == ActivityType.ENABLER)
                .count();

        sb.append("## Métricas del Plan\n\n");
        sb.append("- **Total Story Points:** ").append(result.totalStoryPoints()).append(" pts\n");
        sb.append("- **Total Actividades:** ").append(result.allocations().size())
                .append(" (").append(huCount).append(" HUs hasta QA, ")
                .append(haCount).append(" HAs HyMS/Setups)\n");

        if (!result.generatedSpecNames().isEmpty()) {
            sb.append("- **Especificaciones Almacenadas:** ")
                    .append(String.join(", ", result.generatedSpecNames())).append("\n");
        }
        sb.append("\n");

        if (!result.allocations().isEmpty()) {
            sb.append("## Tabla de Asignaciones por Sprint\n\n");
            sb.append(
                    "| Sprint | Tipo | Título | Story Points | Frente | Dependencias | Criterio de Entrega |\n");
            sb.append(
                    "|:------:|:----:|:-------|:------------:|:-------|:-------------|:--------------------|\n");
            for (SprintAllocation alloc : result.allocations()) {
                String deps = alloc.dependencies().isEmpty() ? "Ninguna"
                        : String.join(", ", alloc.dependencies());
                sb.append("| ").append(alloc.sprintNumber()).append(" | ")
                        .append(alloc.type().tag()).append(" | ")
                        .append(alloc.title()).append(" | ")
                        .append(alloc.storyPoints()).append(" | ")
                        .append(alloc.front()).append(" | ")
                        .append(deps).append(" | ")
                        .append(alloc.description()).append(" |\n");
            }
        }

        return sb.toString();
    }
}
