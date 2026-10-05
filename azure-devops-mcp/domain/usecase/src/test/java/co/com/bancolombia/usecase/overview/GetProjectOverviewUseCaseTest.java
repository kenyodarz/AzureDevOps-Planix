package co.com.bancolombia.usecase.overview;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.overview.ProjectOverview;
import co.com.bancolombia.model.overview.gateways.OverviewPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetProjectOverviewUseCaseTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final String API_VERSION = "7.1";

    @Mock
    private OverviewPort overviewPort;

    private GetProjectOverviewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProjectOverviewUseCase(overviewPort);
    }

    @Test
    @DisplayName("GIVEN valid arguments WHEN getProjectOverview THEN returns ProjectOverview")
    void givenValidArguments_whenGetProjectOverview_thenReturnsProjectOverview() {
        ProjectOverview expected = ProjectOverview.builder()
                .id("proj-123")
                .name(PROJECT)
                .description("Desc")
                .state("wellFormed")
                .visibility("private")
                .url("https://dev.azure.com/...")
                .build();

        when(overviewPort.getProjectOverview(ORG, PROJECT, API_VERSION))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(useCase.getProjectOverview(ORG, PROJECT, API_VERSION))
                .expectNext(expected)
                .verifyComplete();

        verify(overviewPort).getProjectOverview(ORG, PROJECT, API_VERSION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("GIVEN invalid organization WHEN getProjectOverview THEN returns error")
    void givenInvalidOrganization_whenGetProjectOverview_thenReturnsError(String invalidOrg) {
        StepVerifier.create(useCase.getProjectOverview(invalidOrg, PROJECT, API_VERSION))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("GIVEN invalid project WHEN getProjectOverview THEN returns error")
    void givenInvalidProject_whenGetProjectOverview_thenReturnsError(String invalidProject) {
        StepVerifier.create(useCase.getProjectOverview(ORG, invalidProject, API_VERSION))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
