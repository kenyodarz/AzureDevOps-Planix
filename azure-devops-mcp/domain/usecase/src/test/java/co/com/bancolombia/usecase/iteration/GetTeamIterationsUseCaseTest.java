package co.com.bancolombia.usecase.iteration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.iteration.TeamIteration;
import co.com.bancolombia.model.iteration.gateways.GetTeamIterationsRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas de la resolución del {@code IterationPath} por consulta.
 *
 * <p>El caso que da sentido a toda la clase es
 * {@link #givenSprintFromAPreviousYear_whenResolve_thenReturnsItsRealPath()}: el sprint vive en la
 * carpeta del año en que empezó, y ese año no se puede deducir del calendario.
 */
class GetTeamIterationsUseCaseTest {

    private static final String ORG = "grupobancolombia";
    private static final String PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String TEAM = "EQU1096 - EXODIA";

    private GetTeamIterationsRepository repository;
    private GetTeamIterationsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(GetTeamIterationsRepository.class);
        useCase = new GetTeamIterationsUseCase(repository);
    }

    private void givenIterations(TeamIteration... iterations) {
        when(repository.getTeamIterations(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(List.of(iterations)));
    }

    private TeamIteration iteration(String name, String path) {
        return TeamIteration.builder().id(name).name(name).path(path).build();
    }

    @Test
    @DisplayName("GIVEN el equipo tiene el sprint WHEN resolveIterationPath THEN devuelve su path real")
    void givenTeamHasSprint_whenResolve_thenReturnsRealPath() {
        // GIVEN
        givenIterations(iteration("Sprint 246", PROJECT + "\\2025\\Sprint 246"),
                iteration("Sprint 247", PROJECT + "\\2025\\Sprint 247"));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "Sprint 247"))
                .expectNext(PROJECT + "\\2025\\Sprint 247")
                .verifyComplete();
    }

    /**
     * El sprint empezó el año pasado y sigue vivo. Calcular el año habría apuntado a una carpeta
     * inexistente; preguntando, se obtiene la ruta correcta.
     */
    @Test
    @DisplayName("GIVEN un sprint que arrancó el año anterior WHEN resolveIterationPath THEN devuelve ese año, no el actual")
    void givenSprintFromAPreviousYear_whenResolve_thenReturnsItsRealPath() {
        // GIVEN
        int lastYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).getYear() - 1;
        String realPath = PROJECT + "\\" + lastYear + "\\Sprint 247";
        givenIterations(iteration("Sprint 247", realPath));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "Sprint 247"))
                .expectNext(realPath)
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN el nombre llega con otra caja o con espacios WHEN resolveIterationPath THEN lo reconoce igual")
    void givenLooselyWrittenName_whenResolve_thenStillMatches() {
        // GIVEN
        givenIterations(iteration("Sprint 247", PROJECT + "\\2025\\Sprint 247"));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "  sprint 247 "))
                .expectNext(PROJECT + "\\2025\\Sprint 247")
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN el sprint llega como ruta completa WHEN resolveIterationPath THEN usa su último tramo")
    void givenFullPathAsSprint_whenResolve_thenUsesLastSegment() {
        // GIVEN
        givenIterations(iteration("Sprint 247", PROJECT + "\\2025\\Sprint 247"));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM,
                        PROJECT + "\\2026\\Sprint 247"))
                .expectNext(PROJECT + "\\2025\\Sprint 247")
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN el equipo no tiene ese sprint WHEN resolveIterationPath THEN falla para que quien llame repliegue")
    void givenUnknownSprint_whenResolve_thenFails() {
        // GIVEN
        givenIterations(iteration("Sprint 246", PROJECT + "\\2025\\Sprint 246"));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "Sprint 999"))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN la iteración existe pero sin path WHEN resolveIterationPath THEN también falla")
    void givenIterationWithoutPath_whenResolve_thenFails() {
        // GIVEN
        givenIterations(iteration("Sprint 247", "  "));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "Sprint 247"))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN el equipo no tiene iteraciones WHEN resolveIterationPath THEN falla")
    void givenNoIterations_whenResolve_thenFails() {
        // GIVEN
        when(repository.getTeamIterations(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(List.of()));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "Sprint 247"))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN Azure DevOps falla WHEN resolveIterationPath THEN propaga el error")
    void givenRepositoryFails_whenResolve_thenPropagates() {
        // GIVEN
        when(repository.getTeamIterations(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("API caída")));

        // WHEN / THEN
        StepVerifier.create(useCase.resolveIterationPath(ORG, PROJECT, TEAM, "Sprint 247"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN se piden las iteraciones WHEN getTeamIterations THEN delega en el repositorio")
    void givenRequest_whenGetTeamIterations_thenDelegates() {
        // GIVEN
        TeamIteration only = iteration("Sprint 247", PROJECT + "\\2025\\Sprint 247");
        givenIterations(only);

        // WHEN / THEN
        StepVerifier.create(useCase.getTeamIterations(ORG, PROJECT, TEAM))
                .expectNext(List.of(only))
                .verifyComplete();
    }
}

