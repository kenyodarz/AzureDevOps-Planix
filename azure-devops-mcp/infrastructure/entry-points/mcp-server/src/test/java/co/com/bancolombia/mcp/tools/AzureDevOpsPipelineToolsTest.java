package co.com.bancolombia.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.mcp.dto.PipelineRunResponse;
import co.com.bancolombia.mcp.dto.PipelineSummaryResponse;
import co.com.bancolombia.model.pipeline.PipelineRun;
import co.com.bancolombia.model.pipeline.PipelineSummary;
import co.com.bancolombia.usecase.pipeline.GetPipelineRunLogsUseCase;
import co.com.bancolombia.usecase.pipeline.GetPipelineRunUseCase;
import co.com.bancolombia.usecase.pipeline.ListPipelinesUseCase;
import co.com.bancolombia.usecase.pipeline.TriggerPipelineRunUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AzureDevOpsPipelineToolsTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final int PIPELINE_ID = 5;
    private static final int RUN_ID = 50;
    private static final String API_VERSION = "7.1";

    @Mock
    private ListPipelinesUseCase listPipelinesUseCase;
    @Mock
    private GetPipelineRunUseCase getPipelineRunUseCase;
    @Mock
    private GetPipelineRunLogsUseCase getPipelineRunLogsUseCase;
    @Mock
    private TriggerPipelineRunUseCase triggerPipelineRunUseCase;

    @InjectMocks
    private AzureDevOpsPipelineTools pipelineTools;

    @Test
    @DisplayName("GIVEN valid params WHEN listPipelines THEN delegates and maps response")
    void givenValidParams_whenListPipelines_thenDelegatesAndMapsResponse() {
        PipelineSummary summary = PipelineSummary.builder()
                .id(PIPELINE_ID)
                .name("CI-Build")
                .folder("\\")
                .revision(1)
                .url("https://...")
                .build();

        when(listPipelinesUseCase.listPipelines(ORG, PROJECT, 10, API_VERSION))
                .thenReturn(Mono.just(List.of(summary)));

        StepVerifier.create(pipelineTools.listPipelines(ORG, PROJECT, 10, API_VERSION))
                .assertNext(list -> {
                    assertNotNull(list);
                    assertEquals(1, list.size());
                    assertEquals("CI-Build", list.get(0).name());
                })
                .verifyComplete();

        verify(listPipelinesUseCase).listPipelines(ORG, PROJECT, 10, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN valid params WHEN getPipelineRun THEN delegates and maps response")
    void givenValidParams_whenGetPipelineRun_thenDelegatesAndMapsResponse() {
        PipelineRun run = PipelineRun.builder()
                .id(RUN_ID)
                .pipelineId(PIPELINE_ID)
                .name("Build-50")
                .status("completed")
                .result("succeeded")
                .sourceBranch("refs/heads/main")
                .build();

        when(getPipelineRunUseCase.getPipelineRun(ORG, PROJECT, PIPELINE_ID, RUN_ID, API_VERSION))
                .thenReturn(Mono.just(run));

        StepVerifier.create(pipelineTools.getPipelineRun(ORG, PROJECT, PIPELINE_ID, RUN_ID, API_VERSION))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(RUN_ID, response.id());
                    assertEquals("completed", response.status());
                    assertEquals("succeeded", response.result());
                })
                .verifyComplete();

        verify(getPipelineRunUseCase).getPipelineRun(ORG, PROJECT, PIPELINE_ID, RUN_ID, API_VERSION);
    }
}
