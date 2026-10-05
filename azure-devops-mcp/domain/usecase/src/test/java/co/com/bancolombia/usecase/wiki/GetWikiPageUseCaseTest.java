package co.com.bancolombia.usecase.wiki;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.wiki.WikiPage;
import co.com.bancolombia.model.wiki.gateways.WikiPort;
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
class GetWikiPageUseCaseTest {

    private static final String ORG = "orgTest";
    private static final String PROJECT = "projectTest";
    private static final String WIKI = "wikiTest";
    private static final String PATH = "/test/path";
    private static final String API_VERSION = "7.1";

    @Mock
    private WikiPort wikiPort;

    private GetWikiPageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetWikiPageUseCase(wikiPort);
    }

    @Test
    @DisplayName("GIVEN valid arguments WHEN getWikiPage THEN returns WikiPage")
    void givenValidArguments_whenGetWikiPage_thenReturnsWikiPage() {
        WikiPage expected = WikiPage.builder()
                .id(1)
                .path(PATH)
                .content("# Header")
                .remoteUrl("https://...")
                .version("v1")
                .build();

        when(wikiPort.getWikiPage(ORG, PROJECT, WIKI, PATH, true, API_VERSION))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(useCase.getWikiPage(ORG, PROJECT, WIKI, PATH, true, API_VERSION))
                .expectNext(expected)
                .verifyComplete();

        verify(wikiPort).getWikiPage(ORG, PROJECT, WIKI, PATH, true, API_VERSION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("GIVEN invalid wikiIdentifier WHEN getWikiPage THEN returns error")
    void givenInvalidWikiIdentifier_whenGetWikiPage_thenReturnsError(String invalidWiki) {
        StepVerifier.create(useCase.getWikiPage(ORG, PROJECT, invalidWiki, PATH, true, API_VERSION))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
