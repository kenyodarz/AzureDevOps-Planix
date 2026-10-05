package co.com.bancolombia.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.mcp.dto.ProjectOverviewResponse;
import co.com.bancolombia.mcp.dto.WikiPageResponse;
import co.com.bancolombia.model.overview.ProjectOverview;
import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.usecase.overview.GetProjectOverviewUseCase;
import co.com.bancolombia.usecase.wiki.CreateOrUpdateWikiPageUseCase;
import co.com.bancolombia.usecase.wiki.GetWikiPageUseCase;
import co.com.bancolombia.usecase.wiki.SearchWikiUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AzureDevOpsOverviewToolsTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final String WIKI = "wikiTest";
    private static final String PATH = "/test/path";
    private static final String API_VERSION = "7.1";

    @Mock
    private GetProjectOverviewUseCase getProjectOverviewUseCase;
    @Mock
    private GetWikiPageUseCase getWikiPageUseCase;
    @Mock
    private SearchWikiUseCase searchWikiUseCase;
    @Mock
    private CreateOrUpdateWikiPageUseCase createOrUpdateWikiPageUseCase;

    @InjectMocks
    private AzureDevOpsOverviewTools overviewTools;

    @Test
    @DisplayName("GIVEN valid params WHEN getProjectOverview THEN delegates and maps response")
    void givenValidParams_whenGetProjectOverview_thenDelegatesAndMapsResponse() {
        ProjectOverview domainModel = ProjectOverview.builder()
                .id("guid-123")
                .name(PROJECT)
                .description("Test Description")
                .state("wellFormed")
                .visibility("private")
                .url("https://...")
                .build();

        when(getProjectOverviewUseCase.getProjectOverview(ORG, PROJECT, API_VERSION))
                .thenReturn(Mono.just(domainModel));

        StepVerifier.create(overviewTools.getProjectOverview(ORG, PROJECT, API_VERSION))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("guid-123", response.id());
                    assertEquals(PROJECT, response.name());
                })
                .verifyComplete();

        verify(getProjectOverviewUseCase).getProjectOverview(ORG, PROJECT, API_VERSION);
    }

    @Test
    @DisplayName("GIVEN valid params WHEN getWikiPage THEN delegates and maps response")
    void givenValidParams_whenGetWikiPage_thenDelegatesAndMapsResponse() {
        WikiPage domainModel = WikiPage.builder()
                .id(42)
                .path(PATH)
                .content("# Wiki Title")
                .remoteUrl("https://...")
                .version("eTag1")
                .build();

        when(getWikiPageUseCase.getWikiPage(ORG, PROJECT, WIKI, PATH, true, API_VERSION))
                .thenReturn(Mono.just(domainModel));

        StepVerifier.create(overviewTools.getWikiPage(ORG, PROJECT, WIKI, PATH, true, API_VERSION))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(42, response.id());
                    assertEquals(PATH, response.path());
                    assertEquals("# Wiki Title", response.content());
                })
                .verifyComplete();

        verify(getWikiPageUseCase).getWikiPage(ORG, PROJECT, WIKI, PATH, true, API_VERSION);
    }
}
