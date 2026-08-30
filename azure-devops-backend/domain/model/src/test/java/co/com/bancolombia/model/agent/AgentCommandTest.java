package co.com.bancolombia.model.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de los Value Objects de la interacción con el agente (Fase 02).
 */
class AgentCommandTest {

    private static final String PROMPT = "Analiza el sprint 247";
    private static final String CONTEXT_ID = "ctx-1";

    @Test
    @DisplayName("GIVEN prompt y contexto WHEN nonBlocking THEN crea un comando no bloqueante (DP-08)")
    void givenPromptAndContext_whenNonBlocking_thenCommandIsNonBlocking() {
        // GIVEN / WHEN
        AgentCommand command = AgentCommand.nonBlocking(PROMPT, CONTEXT_ID);

        // THEN
        assertThat(command.blocking())
                .as("el sistema es reactivo de extremo a extremo: el modo estándar es no bloqueante")
                .isFalse();
        assertThat(command.prompt()).isEqualTo(PROMPT);
        assertThat(command.contextId()).isEqualTo(CONTEXT_ID);
    }

    @Test
    @DisplayName("GIVEN prompt y contexto WHEN blocking THEN crea un comando bloqueante")
    void givenPromptAndContext_whenBlocking_thenCommandIsBlocking() {
        // GIVEN / WHEN
        AgentCommand command = AgentCommand.blocking(PROMPT, CONTEXT_ID);

        // THEN
        assertThat(command.blocking()).isTrue();
    }

    @Test
    @DisplayName("GIVEN sin messageId WHEN se construye THEN genera uno único")
    void givenNoMessageId_whenBuilt_thenOneIsGenerated() {
        // GIVEN / WHEN
        AgentCommand first = AgentCommand.nonBlocking(PROMPT, CONTEXT_ID);
        AgentCommand second = AgentCommand.nonBlocking(PROMPT, CONTEXT_ID);

        // THEN
        assertThat(first.messageId()).isNotBlank();
        assertThat(first.messageId()).isNotEqualTo(second.messageId());
    }

    @Test
    @DisplayName("GIVEN un messageId explícito WHEN se construye THEN se respeta")
    void givenExplicitMessageId_whenBuilt_thenItIsHonoured() {
        // GIVEN / WHEN
        AgentCommand command = new AgentCommand(PROMPT, CONTEXT_ID, "msg-7", false);

        // THEN
        assertThat(command.messageId()).isEqualTo("msg-7");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    @DisplayName("GIVEN prompt vacío WHEN se construye THEN falla")
    void givenBlankPrompt_whenBuilt_thenItFails(String blankPrompt) {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> AgentCommand.nonBlocking(blankPrompt, CONTEXT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prompt");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("GIVEN contextId vacío WHEN se construye THEN falla")
    void givenBlankContextId_whenBuilt_thenItFails(String blankContextId) {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> AgentCommand.nonBlocking(PROMPT, blankContextId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextId");
    }

    @Test
    @DisplayName("GIVEN una tarea y una respuesta WHEN se crea la interacción THEN ambas se conservan")
    void givenTaskAndReply_whenInteractionCreated_thenBothAreKept() {
        // GIVEN
        Task task = Task.builder()
                .id("task-42")
                .status(TaskStatus.builder().state(TaskState.WORKING).build())
                .build();

        // WHEN
        AgentInteraction interaction = new AgentInteraction(task, "Trabajando en ello");

        // THEN
        assertThat(interaction.hasTask()).isTrue();
        assertThat(interaction.taskAsOptional()).containsSame(task);
        assertThat(interaction.reply()).isEqualTo("Trabajando en ello");
    }

    @Test
    @DisplayName("GIVEN solo una respuesta WHEN replyOnly THEN no hay tarea que seguir")
    void givenOnlyReply_whenReplyOnly_thenThereIsNoTask() {
        // GIVEN / WHEN
        AgentInteraction interaction = AgentInteraction.replyOnly("Hola");

        // THEN
        assertThat(interaction.hasTask()).isFalse();
        assertThat(interaction.taskAsOptional()).isEmpty();
        assertThat(interaction.reply()).isEqualTo("Hola");
    }

    @Test
    @DisplayName("GIVEN una tarea sin id WHEN se crea la interacción THEN no se considera seguible")
    void givenTaskWithoutId_whenInteractionCreated_thenItIsNotTrackable() {
        // GIVEN / WHEN
        AgentInteraction interaction = new AgentInteraction(Task.builder().build(), "texto");

        // THEN
        assertThat(interaction.hasTask()).isFalse();
        assertThat(interaction.taskAsOptional()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN respuesta nula WHEN se crea la interacción THEN se normaliza a cadena vacía")
    void givenNullReply_whenInteractionCreated_thenItBecomesEmptyString() {
        // GIVEN / WHEN
        AgentInteraction interaction = new AgentInteraction(null, null);

        // THEN
        assertThat(interaction.reply()).isEmpty();
        assertThat(interaction.hasTask()).isFalse();
    }
}

