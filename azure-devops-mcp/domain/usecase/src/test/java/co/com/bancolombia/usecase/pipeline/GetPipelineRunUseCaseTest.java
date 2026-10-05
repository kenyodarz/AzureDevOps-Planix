package co.com.bancolombia.usecase.pipeline;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.gateways.PipelinePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetPipelineRunUseCaseTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final int PIPELINE_ID = 10;
    private static final int RUN_ID = 100;
    private static final String API_VERSION = "7.1";

    @Mock
    private PipelinePort pipelinePort;

    private GetPipelineRunUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPipelineRunUseCase(pipelinePort);
    }

    @Test
    @DisplayName("GIVEN valid arguments WHEN getPipelineRun THEN returns PipelineRun")
    void givenValidArguments_whenGetPipelineRun_thenReturnsPipelineRun() {
        PipelineRun expected = PipelineRun.builder()
                .id(RUN_ID)
                .pipelineId(PIPELINE_ID)
                .name("run-100")
                .status("completed")
                .result("succeeded")
                .build();

        when(pipelinePort.getPipelineRun(ORG, PROJECT, PIPELINE_ID, RUN_ID, API_VERSION))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(useCase.getPipelineRun(ORG, PROJECT, PIPELINE_ID, RUN_ID, API_VERSION))
                .expectNext(expected)
                .verifyComplete();

        verify(pipelinePort).getPipelineRun(ORG, PROJECT, PIPELINE_ID, RUN_ID, API_VERSION);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("GIVEN invalid pipelineId WHEN getPipelineRun THEN returns error")
    void givenInvalidPipelineId_whenGetPipelineRun_thenReturnsError(int invalidId) {
        StepVerifier.create(useCase.getPipelineRun(ORG, PROJECT, invalidId, RUN_ID, API_VERSION))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
