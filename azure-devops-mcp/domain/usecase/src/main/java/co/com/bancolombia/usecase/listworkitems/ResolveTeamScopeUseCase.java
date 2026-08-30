package co.com.bancolombia.usecase.listworkitems;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.workitem.SprintName;
import co.com.bancolombia.model.workitem.TeamName;
import co.com.bancolombia.model.workitem.TeamPathFallback;
import co.com.bancolombia.model.workitem.TeamScope;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Resuelve el ámbito de una consulta: la ruta de área de la célula y la ruta de iteración del
 * sprint.
 *
 * <p>Hasta la Fase 04 esta orquestación vivía dentro de {@code AzureDevOpsTools}, el entry-point
 * MCP, en contra de la regla «cero lógica de negocio» de {@code spring-rules.md} (deudas
 * <b>D-07</b> y <b>D-09</b>).
 *
 * <h2>Qué cambia y qué no</h2>
 *
 * <p><b>No cambia el comportamiento observable.</b> Se pregunta primero por el {@code AreaPath} y
 * después por el {@code IterationPath}, y cada uno cae a su repliegue por concatenación si Azure
 * DevOps no responde, exactamente igual que antes. <b>DP-04 §0.1 eligió la opción (a)</b>:
 * conservar el repliegue, año del calendario incluido.
 *
 * <p><b>Sí cambia que ahora se mide.</b> Cada disparo del repliegue notifica a
 * {@link TeamScopeFallbackMetrics}, que lo cuenta y emite el {@code WARN}. Ése era el vacío de
 * <b>D-09</b>: el repliegue existía, se sabía defectuoso y <b>nadie sabía cuántas veces se
 * usaba</b>, así que no había forma de decidir con datos si podía retirarse.
 *
 * <p><b>Y sí cambia de dónde sale el año.</b> Antes se calculaba con
 * {@code LocalDate.now(ZoneId.systemDefault())} en mitad de la concatenación —número mágico y
 * dependencia oculta del reloj de la máquina, S109—. Ahora el reloj se inyecta, de modo que el
 * repliegue es determinista y comprobable.
 *
 * <p><b>Por qué esta clase no escribe en el log.</b> Porque no puede: el módulo
 * {@code domain/usecase} solo admite {@code :model} como dependencia, y la tarea
 * {@code validateStructure} del plugin de Clean Architecture rechaza el módulo si se le añade
 * cualquier otra —SLF4J incluido—. El aviso se emite en el adaptador, que recibe la excepción
 * original a través del puerto.
 */
@RequiredArgsConstructor
public class ResolveTeamScopeUseCase {

    private final GetTeamFieldValuesUseCase getTeamFieldValuesUseCase;
    private final GetTeamIterationsUseCase getTeamIterationsUseCase;
    private final TeamScopeFallbackMetrics fallbackMetrics;
    private final Clock clock;

    /**
     * Devuelve el ámbito resuelto, replegándose a la concatenación en cada mitad por separado.
     */
    public Mono<TeamScope> resolve(String organization, String project, TeamName team,
            SprintName sprint) {
        return resolveAreaPath(organization, project, team)
                .flatMap(areaPath -> resolveIterationPath(organization, project, team, sprint)
                        .map(iterationPath -> new TeamScope(areaPath, iterationPath)));
    }

    private Mono<String> resolveAreaPath(String organization, String project, TeamName team) {
        return getTeamFieldValuesUseCase
                .getTeamFieldValues(organization, project, team.shortName())
                .map(TeamFieldValues::defaultValue)
                .onErrorResume(error -> Mono.just(fallbackAreaPath(project, team, error)));
    }

    private Mono<String> resolveIterationPath(String organization, String project, TeamName team,
            SprintName sprint) {
        return getTeamIterationsUseCase
                .resolveIterationPath(organization, project, team.shortName(), sprint.value())
                .onErrorResume(error -> Mono.just(fallbackIterationPath(project, sprint, error)));
    }

    private String fallbackAreaPath(String project, TeamName team, Throwable error) {
        fallbackMetrics.areaPathFallbackUsed(team.value(), error);
        return TeamPathFallback.areaPath(project, team);
    }

    /**
     * ⚠️ Aquí es donde nace el tablero vacío: si el sprint tiene la forma {@code Sprint N} se
     * intercala el año en curso, y un sprint que cruza el cambio de ejercicio pertenece al año en
     * que <b>empezó</b>. Se marca explícitamente para que el caso deje de ser indistinguible del
     * repliegue inocuo.
     */
    private String fallbackIterationPath(String project, SprintName sprint, Throwable error) {
        boolean yearInterleaved = TeamPathFallback.interleavesCalendarYear(sprint);
        fallbackMetrics.iterationPathFallbackUsed(sprint.value(), yearInterleaved, error);
        return TeamPathFallback.iterationPath(project, sprint, currentYear());
    }

    private int currentYear() {
        return LocalDate.now(clock).getYear();
    }
}

