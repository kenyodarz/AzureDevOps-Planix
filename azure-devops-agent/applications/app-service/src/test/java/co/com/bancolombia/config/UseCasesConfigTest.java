package co.com.bancolombia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Verifica el comportamiento <b>fail-fast</b> de {@link UseCasesConfig} cuando faltan los puertos
 * de infraestructura.
 *
 * <p><b>Corregido en la Fase 07.</b> La versión anterior de este test, procedente del andamiaje,
 * capturaba {@code UnsatisfiedDependencyException} y respondía con {@code assertTrue(true)}:
 *
 * <pre>{@code
 * } catch (UnsatisfiedDependencyException e) {
 *     assertTrue(true, "Unsatisfied dependencies are expected...");
 * }
 * }</pre>
 *
 * <p>Con ese {@code catch}, <b>cualquier</b> cableado roto pasaba como prueba verde, y de hecho el
 * cuerpo del {@code try} nunca llegaba a evaluarse. Las fases 04 y 05 rehicieron el wiring por
 * completo sin que nada lo verificara.
 *
 * <p>Ahora la expectativa es <b>explícita</b>: sin gateways el contexto <b>debe</b> fallar. Si
 * algún día arrancase a medias —o fallase por una causa distinta— el test lo detecta.
 *
 * @see UseCasesConfigWiringTest prueba complementaria: el contexto arranca de verdad con los
 *      puertos simulados
 */
@DisplayName("UseCasesConfig - Fail-fast ante puertos de infraestructura ausentes")
class UseCasesConfigTest {

    @Test
    @DisplayName("GIVEN la configuración real sin gateways WHEN arranca el contexto "
            + "THEN falla en lugar de publicar un caso de uso a medio cablear")
    void givenMissingGateways_whenContextStarts_thenFailsFast() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(TestConfig.class))
                .isInstanceOf(UnsatisfiedDependencyException.class);
    }

    @Test
    @DisplayName("GIVEN la configuración real sin gateways WHEN arranca el contexto "
            + "THEN el error identifica el puerto del dominio que falta")
    void givenMissingGateways_whenContextStarts_thenErrorNamesTheMissingPort() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(TestConfig.class))
                .isInstanceOf(UnsatisfiedDependencyException.class)
                .satisfies(error -> assertThat(error.getMessage())
                        .contains("co.com.bancolombia.model"));
    }

    /** Importa la configuración real <b>sin</b> proveer ningún puerto de infraestructura. */
    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {
    }
}

