package co.com.bancolombia.usecase.iteration;

import co.com.bancolombia.model.iteration.TeamIteration;
import co.com.bancolombia.model.iteration.gateways.GetTeamIterationsRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Resuelve el {@code IterationPath} de un sprint preguntándoselo a Azure DevOps.
 *
 * <p>Es el gemelo de {@code GetTeamFieldValuesUseCase}, que hace lo propio con el
 * {@code AreaPath}. La comparación por nombre es laxa a propósito —sin distinguir mayúsculas y sin
 * espacios sobrantes— porque el nombre llega escrito a mano por una persona en un formulario del
 * frontend. Si además viniera con la ruta pegada, se toma solo el último tramo.
 */
@RequiredArgsConstructor
public class GetTeamIterationsUseCase {

    private static final String PATH_SEPARATOR = "\\";

    private final GetTeamIterationsRepository repository;

    public Mono<List<TeamIteration>> getTeamIterations(String organization, String project,
            String team) {
        return repository.getTeamIterations(organization, project, team);
    }

    /**
     * Devuelve el {@code IterationPath} real del sprint indicado.
     *
     * @return el path de la iteración; error {@link NoSuchElementException} si el equipo no tiene
     * ninguna iteración con ese nombre, para que quien llame decida su repliegue
     */
    public Mono<String> resolveIterationPath(String organization, String project, String team,
            String sprintName) {
        String wanted = shortName(sprintName);
        return getTeamIterations(organization, project, team)
                .flatMapIterable(iterations -> iterations == null ? List.of() : iterations)
                .filter(iteration -> matches(iteration, wanted))
                .next()
                .map(TeamIteration::getPath)
                .filter(path -> path != null && !path.isBlank())
                .switchIfEmpty(Mono.error(new NoSuchElementException(
                        "El equipo '" + team + "' no tiene una iteración llamada '" + wanted
                                + "'")));
    }

    private boolean matches(TeamIteration iteration, String wanted) {
        return iteration != null && iteration.getName() != null
                && iteration.getName().trim().equalsIgnoreCase(wanted);
    }

    /**
     * Se queda con el último tramo si el sprint llega como ruta en lugar de como nombre.
     */
    private String shortName(String sprintName) {
        if (sprintName == null) {
            return "";
        }
        String trimmed = sprintName.trim();
        int lastSeparator = trimmed.lastIndexOf(PATH_SEPARATOR);
        return lastSeparator < 0 ? trimmed : trimmed.substring(lastSeparator + 1).trim();
    }
}

