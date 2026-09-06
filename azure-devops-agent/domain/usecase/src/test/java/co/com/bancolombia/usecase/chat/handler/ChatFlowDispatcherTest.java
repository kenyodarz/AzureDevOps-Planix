package co.com.bancolombia.usecase.chat.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.IntentResolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

/**
 * Pruebas del {@link ChatFlowDispatcher}.
 *
 * <p>Sin mocks: se usan handlers de prueba que devuelven el nombre de su intención, de modo que la
 * aserción comprueba directamente que se ejecutó el handler correcto.
 */
@DisplayName("ChatFlowDispatcher - Índice de flujos por intención")
class ChatFlowDispatcherTest {

    private static final String USER_TEXT = "Texto de la conversación";
    private static final String CONTEXT_ID = "ctx-1";

    @ParameterizedTest(name = "{0} se despacha a su propio handler")
    @EnumSource(AgentIntent.class)
    @DisplayName("Cada intención llega al handler que la declara")
    void givenAllHandlers_whenDispatch_thenExecutesTheMatchingOne(AgentIntent intent) {
        // GIVEN
        ChatFlowDispatcher dispatcher = new ChatFlowDispatcher(allHandlers());

        // WHEN
        String result = dispatcher.dispatch(contextOf(intent)).block();

        // THEN
        assertThat(result).isEqualTo(intent.name());
    }

    @Test
    @DisplayName("Falla al construirse si falta el handler de alguna intención")
    void givenMissingHandler_whenBuild_thenThrows() {
        // GIVEN
        List<ChatFlowHandler> incomplete = new ArrayList<>(allHandlers());
        incomplete.removeIf(handler -> handler.supports() == AgentIntent.DIVISION);

        // WHEN / THEN
        assertThatThrownBy(() -> new ChatFlowDispatcher(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DIVISION");
    }

    @Test
    @DisplayName("Falla al construirse si falta el handler de PROGRAM_PLANNING")
    void givenMissingProgramPlanningHandler_whenBuild_thenThrows() {
        // GIVEN
        List<ChatFlowHandler> incomplete = new ArrayList<>(allHandlers());
        incomplete.removeIf(handler -> handler.supports() == AgentIntent.PROGRAM_PLANNING);

        // WHEN / THEN
        assertThatThrownBy(() -> new ChatFlowDispatcher(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROGRAM_PLANNING");
    }

    @Test
    @DisplayName("Falla al construirse si hay dos handlers para la misma intención")
    void givenDuplicatedHandler_whenBuild_thenThrows() {
        // GIVEN
        List<ChatFlowHandler> duplicated = new ArrayList<>(allHandlers());
        duplicated.add(new FakeFlowHandler(AgentIntent.GENERAL));

        // WHEN / THEN
        assertThatThrownBy(() -> new ChatFlowDispatcher(duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("más de un ChatFlowHandler");
    }

    @Test
    @DisplayName("Falla al construirse si no se registró ningún handler")
    void givenNoHandlers_whenBuild_thenThrows() {
        // WHEN / THEN
        assertThatThrownBy(() -> new ChatFlowDispatcher(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ningún ChatFlowHandler");
    }

    private static List<ChatFlowHandler> allHandlers() {
        return Arrays.stream(AgentIntent.values())
                .map(FakeFlowHandler::new)
                .map(ChatFlowHandler.class::cast)
                .toList();
    }

    private static ChatFlowContext contextOf(AgentIntent intent) {
        return new ChatFlowContext(USER_TEXT, CONTEXT_ID, IntentResolution.of(intent));
    }

    /** Handler de prueba que devuelve el nombre de la intención que atiende. */
    private record FakeFlowHandler(AgentIntent intent) implements ChatFlowHandler {

        @Override
        public AgentIntent supports() {
            return intent;
        }

        @Override
        public Mono<String> handle(ChatFlowContext context) {
            return Mono.just(intent.name());
        }
    }
}

