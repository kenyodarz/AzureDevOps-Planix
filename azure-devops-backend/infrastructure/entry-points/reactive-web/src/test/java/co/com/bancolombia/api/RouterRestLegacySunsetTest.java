package co.com.bancolombia.api;

import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, Handler.class})
@TestPropertySource(properties = {
        "a2a.legacy-message-send-enabled=true",
        "a2a.legacy.disable-after-sunset=true",
        "a2a.legacy.sunset-date=2000-01-01"
})
class RouterRestLegacySunsetTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AgentChatUseCase agentChatUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase ingestPlanningSpecUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase searchPlanningSpecUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase managePlanningUseCase;

    @MockitoBean
    private co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase devOpsDashboardUseCase;

    @MockitoBean
    private co.com.bancolombia.model.chat.gateways.TaskStoreGateway taskStoreGateway;

    @Test
    void shouldDisableLegacyEndpointWhenSunsetDateIsReached() {
        webTestClient.post()
                .uri("/message:send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isNotFound();
    }
}

