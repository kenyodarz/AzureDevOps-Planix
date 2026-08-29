package co.com.bancolombia.usecase.chat.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.IntentResolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Contrato del {@link ChatFlowContext}: qué garantiza a los handlers que lo reciben.
 */
@DisplayName("ChatFlowContext - Contrato de entrada de los flujos")
class ChatFlowContextTest {

    private static final String USER_TEXT = "Texto de la conversación";
    private static final String CONTEXT_ID = "ctx-1";
    private static final String WORK_ITEM_ID = "12345";

    @Test
    @DisplayName("Expone la intención de la resolución")
    void givenResolution_whenIntent_thenReturnsIt() {
        // GIVEN
        ChatFlowContext context = new ChatFlowContext(USER_TEXT, CONTEXT_ID,
                IntentResolution.of(AgentIntent.GENERAL));

        // THEN
        assertThat(context.intent()).isEqualTo(AgentIntent.GENERAL);
        assertThat(context.contextId()).isEqualTo(CONTEXT_ID);
    }

    @Test
    @DisplayName("Devuelve el identificador de Work Item cuando la intención lo lleva")
    void givenWorkItem_whenRequireWorkItemId_thenReturnsIt() {
        // GIVEN
        ChatFlowContext context = new ChatFlowContext(USER_TEXT, CONTEXT_ID,
                IntentResolution.withWorkItem(AgentIntent.REFINEMENT, WORK_ITEM_ID));

        // THEN
        assertThat(context.requireWorkItemId()).isEqualTo(WORK_ITEM_ID);
    }

    @Test
    @DisplayName("Falla si se exige un Work Item que la resolución no trae")
    void givenNoWorkItem_whenRequireWorkItemId_thenThrows() {
        // GIVEN
        ChatFlowContext context = new ChatFlowContext(USER_TEXT, CONTEXT_ID,
                IntentResolution.of(AgentIntent.GENERAL));

        // WHEN / THEN
        assertThatThrownBy(context::requireWorkItemId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GENERAL");
    }

    @ParameterizedTest(name = "texto \"{0}\" es inválido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("El texto del usuario es obligatorio")
    void givenBlankUserText_whenBuild_thenThrows(String userText) {
        // WHEN / THEN
        assertThatThrownBy(() -> new ChatFlowContext(userText, CONTEXT_ID,
                IntentResolution.of(AgentIntent.GENERAL)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("texto del usuario");
    }

    @Test
    @DisplayName("La resolución de intención es obligatoria")
    void givenNullResolution_whenBuild_thenThrows() {
        // WHEN / THEN
        assertThatThrownBy(() -> new ChatFlowContext(USER_TEXT, CONTEXT_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolución de intención");
    }

    @Test
    @DisplayName("El contextId admite nulo: no todos los clientes lo envían")
    void givenNullContextId_whenBuild_thenIsAccepted() {
        // WHEN
        ChatFlowContext context = new ChatFlowContext(USER_TEXT, null,
                IntentResolution.of(AgentIntent.GENERAL));

        // THEN
        assertThat(context.contextId()).isNull();
    }
}

