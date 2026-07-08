package co.com.bancolombia.usecase.dashboard;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DevOpsDashboardUseCaseTest {

    private ChatGateway chatGateway;
    private DevOpsDashboardUseCase devOpsDashboardUseCase;

    @BeforeEach
    void setUp() {
        chatGateway = mock(ChatGateway.class);
        devOpsDashboardUseCase = new DevOpsDashboardUseCase(chatGateway, "grupobancolombia",
                "Vicepresidencia Servicios de Tecnología", true);
    }

    @Test
    void shouldReturnDashboardDataFromAIWhenSuccessful() {
        // GIVEN
        String expectedResponse = """
                {
                  "metrics": {
                    "totalPoints": 10,
                    "completedPoints": 5,
                    "completedPercentage": 50,
                    "avgQualityScore": 90,
                    "undocumentedCount": 0
                  },
                  "items": []
                }
                """;
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.just(expectedResponse));

        // WHEN
        Mono<String> resultMono = devOpsDashboardUseCase.getDashboardData("Aegis Backend",
                "Sprint 3");

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.contains("totalPoints"));
                    assertTrue(response.contains("completedPoints"));
                    assertTrue(response.contains("completedPercentage\": 50"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnMockDashboardDataWhenAIServiceFailsAndMcpIsDisabled() {
        // GIVEN
        DevOpsDashboardUseCase disabledMcpUseCase = new DevOpsDashboardUseCase(chatGateway,
                "grupobancolombia",
                "Vicepresidencia Servicios de Tecnología", false);
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("AI service unavailable")));

        // WHEN
        Mono<String> resultMono = disabledMcpUseCase.getDashboardData("Aegis Backend",
                "Sprint 3");

        // THEN
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    // Debe retornar los datos mock por defecto
                    assertTrue(response.contains("totalPoints\": 29"));
                    assertTrue(response.contains("completedPoints\": 13"));
                    assertTrue(response.contains("completedPercentage\": 44"));
                    assertTrue(response.contains("BFF Aegis Backend"));
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenAIServiceFailsAndMcpIsEnabled() {
        // GIVEN
        when(chatGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("AI service unavailable")));

        // WHEN
        Mono<String> resultMono = devOpsDashboardUseCase.getDashboardData("Aegis Backend",
                "Sprint 3");

        // THEN
        StepVerifier.create(resultMono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldFailWhenParametersAreEmpty() {
        // GIVEN/WHEN
        Mono<String> resultMono1 = devOpsDashboardUseCase.getDashboardData("", "Sprint 3");
        Mono<String> resultMono2 = devOpsDashboardUseCase.getDashboardData("Arkham", "");

        // THEN
        StepVerifier.create(resultMono1)
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(resultMono2)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
