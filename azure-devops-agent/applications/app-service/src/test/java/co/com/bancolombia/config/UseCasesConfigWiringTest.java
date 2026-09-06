package co.com.bancolombia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.AzureDevOpsScope;
import co.com.bancolombia.model.agent.CorporateKnowledge;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import co.com.bancolombia.usecase.chat.handler.ChatFlowDispatcher;
import co.com.bancolombia.usecase.chat.handler.ChatFlowHandler;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Verifica que el contexto de Spring <b>realmente arranca</b> con el cableado de los flujos.
 *
 * <p>Complementa a {@link UseCasesConfigTest}, que captura y descarta
 * {@code UnsatisfiedDependencyException} y por tanto no detecta un cableado roto.
 *
 * <p>Es la red de seguridad de las fases 04 y 05: el {@link ChatFlowDispatcher} valida la
 * exhaustividad de los handlers al construirse, y {@link CorporateKnowledge} valida que los
 * recursos del classpath se hayan cargado. Si falta un handler o un {@code .md}, este test falla
 * en lugar de que lo descubra un usuario en producción.
 */
@DisplayName("UseCasesConfig - Cableado real del contexto de Spring")
class UseCasesConfigWiringTest {

    @Test
    @DisplayName("El contexto arranca y publica el caso de uso con sus seis handlers")
    void givenRealConfiguration_whenContextStarts_thenAllFlowBeansExist() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(WiringTestConfig.class)) {

            // THEN: el caso de uso y el despachador existen
            assertThat(context.getBean(AgentChatUseCase.class)).isNotNull();
            assertThat(context.getBean(ChatFlowDispatcher.class)).isNotNull();

            // THEN: hay exactamente un handler por intención activa (PROGRAM_PLANNING se cablea en Fase 08)
            Map<String, ChatFlowHandler> handlers =
                    context.getBeansOfType(ChatFlowHandler.class);
            AgentIntent[] activeIntents = Arrays.stream(AgentIntent.values())
                    .filter(intent -> intent != AgentIntent.PROGRAM_PLANNING)
                    .toArray(AgentIntent[]::new);
            assertThat(handlers).hasSize(activeIntents.length);
            assertThat(handlers.values())
                    .extracting(ChatFlowHandler::supports)
                    .containsExactlyInAnyOrder(activeIntents);
        }
    }

    @Test
    @DisplayName("La configuración tipada se construye con el contenido real de los recursos")
    void givenRealConfiguration_whenContextStarts_thenTypedConfigIsLoaded() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(WiringTestConfig.class)) {

            // THEN
            AzureDevOpsScope scope = context.getBean(AzureDevOpsScope.class);
            assertThat(scope.organization()).isNotBlank();
            assertThat(scope.project()).isNotBlank();

            CorporateKnowledge knowledge = context.getBean(CorporateKnowledge.class);
            assertThat(knowledge.storyTemplate()).isNotBlank();
            assertThat(knowledge.agileGuide()).isNotBlank();
            assertThat(knowledge.qualityStandards()).isNotBlank();
            assertThat(knowledge.auditStandards())
                    .contains(knowledge.agileGuide())
                    .contains(knowledge.qualityStandards());
        }
    }

    /** Sustituye únicamente los puertos de infraestructura; el resto es la configuración real. */
    @Configuration
    @Import(UseCasesConfig.class)
    static class WiringTestConfig {

        @Bean
        ChatGateway chatGateway() {
            return mock(ChatGateway.class);
        }

        @Bean
        AgentResponseGateway agentResponseGateway() {
            return mock(AgentResponseGateway.class);
        }

        @Bean
        TaskStoreGateway taskStoreGateway() {
            return mock(TaskStoreGateway.class);
        }

        @Bean
        SpecStoragePort specStoragePort() {
            return mock(SpecStoragePort.class);
        }

        @Bean
        PromptTemplatePort promptTemplatePort() {
            return mock(PromptTemplatePort.class);
        }
    }
}

