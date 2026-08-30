package co.com.bancolombia.model.prompt.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El contrato que importa de esta excepción es que <b>nombre el problema en lenguaje de
 * dominio</b>: quien la lea en un log debe saber qué plantilla y qué variable fallaron sin abrir el
 * adaptador.
 */
class PromptTemplateExceptionTest {

    @Test
    @DisplayName("GIVEN una plantilla ausente WHEN notFound THEN nombra la plantilla y conserva la causa")
    void givenMissingTemplate_whenNotFound_thenNamesItAndKeepsCause() {
        // GIVEN
        IOException cause = new IOException("classpath");

        // WHEN
        PromptTemplateException exception = PromptTemplateException.notFound("dashboard", cause);

        // THEN
        assertThat(exception).hasMessageContaining("dashboard").hasCause(cause);
    }

    @Test
    @DisplayName("GIVEN una variable sin valor WHEN missingVariable THEN nombra plantilla y variable")
    void givenMissingVariable_whenMissingVariable_thenNamesBoth() {
        // GIVEN / WHEN
        PromptTemplateException exception =
                PromptTemplateException.missingVariable("batch-audit", "idsCsv");

        // THEN
        assertThat(exception.getMessage()).contains("batch-audit", "idsCsv");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("GIVEN un mensaje libre WHEN se construye THEN lo conserva tal cual")
    void givenFreeMessage_whenBuilt_thenItIsKept() {
        // GIVEN / WHEN / THEN
        assertThat(new PromptTemplateException("sin nombre"))
                .hasMessage("sin nombre")
                .isInstanceOf(RuntimeException.class);
    }
}

