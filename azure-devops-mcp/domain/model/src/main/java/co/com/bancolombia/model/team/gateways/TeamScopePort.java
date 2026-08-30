package co.com.bancolombia.model.team.gateways;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.team.TeamIteration;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Ámbito de un equipo en Azure DevOps: su {@code AreaPath} y sus iteraciones
 * (D-14, decisión <b>DP-05 opción (a)</b>).
 *
 * <p>Funde {@code GetTeamFieldValuesRepository} y {@code GetTeamIterationsRepository}, que estaban
 * en dos paquetes distintos —{@code model.team} y {@code model.iteration}— pese a ser <b>las dos
 * mitades de la misma pregunta</b>: dónde vive el trabajo de esta célula y en qué carpeta vive este
 * sprint. Son exactamente las dos consultas de las que depende {@code ResolveTeamScopeUseCase}, y
 * las dos que sostienen el arreglo del tablero vacío (D-09).
 *
 * <p><b>Ninguna firma cambió al fundirlas.</b> Nótese que ninguna de las dos recibe
 * {@code apiVersion}: sus URLs la llevan fijada literalmente a {@code 7.0} desde antes de este
 * plan, y esta fase <b>no cambia ni una llamada HTTP</b>. Tipar las versiones es D-18, material de
 * la Fase 06.
 */
public interface TeamScopePort {

    Mono<TeamFieldValues> getTeamFieldValues(String organization, String project, String team);

    Mono<List<TeamIteration>> getTeamIterations(String organization, String project, String team);
}

