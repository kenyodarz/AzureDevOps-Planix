package co.com.bancolombia.model.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Fija los valores serializados de {@link TaskState}.
 *
 * <p><b>Es contrato público, no un detalle.</b> El cliente A2A lee {@code status.state} como
 * cadena en minúsculas: {@code RouterRestTest} verifica {@code "completed"} y {@code "canceled"}
 * sobre el JSON de respuesta. Cambiar cualquiera de estos literales rompe a los consumidores.
 */
@DisplayName("TaskState - Valores serializados del protocolo A2A")
class TaskStateTest {

    @ParameterizedTest(name = "{0} se serializa como \"{1}\"")
    @CsvSource({
            "SUBMITTED,submitted",
            "WORKING,working",
            "INPUT_REQUIRED,input-required",
            "AUTH_REQUIRED,auth-required",
            "COMPLETED,completed",
            "CANCELED,canceled",
            "REJECTED,rejected",
            "FAILED,failed"
    })
    @DisplayName("GIVEN un estado WHEN se lee su valor THEN coincide con el literal del protocolo")
    void givenState_whenGetValue_thenMatchesProtocolLiteral(TaskState state, String expected) {
        assertThat(state.getValue()).isEqualTo(expected);
        assertThat(state).hasToString(expected);
    }

    @ParameterizedTest
    @EnumSource(TaskState.class)
    @DisplayName("GIVEN cualquier estado THEN su valor es minúscula y no está en blanco")
    void givenAnyState_thenValueIsLowercaseAndNotBlank(TaskState state) {
        assertThat(state.getValue())
                .isNotBlank()
                .isLowerCase();
    }

    @Test
    @DisplayName("GIVEN el enum THEN declara los ocho estados de la especificación")
    void givenEnum_thenDeclaresTheEightSpecStates() {
        assertThat(TaskState.values()).hasSize(8);
    }
}

