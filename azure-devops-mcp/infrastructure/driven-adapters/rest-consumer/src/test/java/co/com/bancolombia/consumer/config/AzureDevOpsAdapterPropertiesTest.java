package co.com.bancolombia.consumer.config;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Los valores por defecto de D-18 y D-24, fijados para que <b>nadie los cambie por descuido</b>.
 *
 * <p>Estas aserciones son la garantía de la regla innegociable nº 2 de la Fase 06: si el
 * {@code application.yaml} no dice nada sobre versiones, <b>las llamadas HTTP son exactamente las de
 * siempre</b>. Los dos literales llevan aquí desde antes de este plan.
 */
class AzureDevOpsAdapterPropertiesTest {

    @Test
    @DisplayName("GIVEN ninguna configuracion WHEN se piden los valores por defecto THEN las versiones son las de siempre (B-09)")
    void givenNoConfiguration_whenDefaults_thenApiVersionsAreUnchanged() {
        // Act (WHEN)
        var properties = AzureDevOpsAdapterProperties.defaults();

        // Assert (THEN)
        assertEquals("7.1", properties.apiVersion().workItem());
        assertEquals("7.0", properties.apiVersion().wiql());
    }

    @Test
    @DisplayName("GIVEN ninguna configuracion WHEN se piden los timeouts THEN son los que eligio DP-06 §0.2 (D-24)")
    void givenNoConfiguration_whenDefaults_thenTimeoutsArePerOperation() {
        // Act (WHEN)
        var timeouts = AzureDevOpsAdapterProperties.defaults().operationTimeout();

        // Assert (THEN) — el del lote es el triple que el de consulta: es el caso critico, la
        // llamada que mas tarda y la que hasta ahora compartia los 5 s de Netty con todas.
        assertEquals(Duration.ofSeconds(10), timeouts.query());
        assertEquals(Duration.ofSeconds(30), timeouts.batch());
        assertEquals(Duration.ofSeconds(10), timeouts.command());
        assertEquals(Duration.ofSeconds(5), timeouts.teamScope());
    }

    @Test
    @DisplayName("GIVEN un YAML sin los bloques nuevos WHEN se enlaza THEN se normaliza a los valores por defecto")
    void givenNullSections_whenBound_thenNormalizedToDefaults() {
        // Act (WHEN) — es lo que ocurre con un application.yaml que no mencione nada de esto.
        var properties = new AzureDevOpsAdapterProperties(null, null);

        // Assert (THEN)
        assertEquals("7.1", properties.apiVersion().workItem());
        assertEquals(Duration.ofSeconds(30), properties.operationTimeout().batch());
    }

    @Test
    @DisplayName("GIVEN valores parciales o en blanco WHEN se enlazan THEN cada hueco cae a su valor por defecto")
    void givenPartialValues_whenBound_thenEachGapFallsBack() {
        // Act (WHEN)
        var apiVersion = new AzureDevOpsAdapterProperties.ApiVersion("7.2", "   ");
        var timeouts = new AzureDevOpsAdapterProperties.OperationTimeout(
                Duration.ofSeconds(3), null, null, null);

        // Assert (THEN)
        assertEquals("7.2", apiVersion.workItem());
        assertEquals("7.0", apiVersion.wiql());
        assertEquals(Duration.ofSeconds(3), timeouts.query());
        assertEquals(Duration.ofSeconds(30), timeouts.batch());
        assertEquals(Duration.ofSeconds(10), timeouts.command());
        assertEquals(Duration.ofSeconds(5), timeouts.teamScope());
    }

    @Test
    @DisplayName("GIVEN una version explicita WHEN se resuelve THEN gana sobre el valor por defecto")
    void givenExplicitVersion_whenResolved_thenOverridesDefault() {
        // Act (WHEN) + Assert (THEN)
        var properties = AzureDevOpsAdapterProperties.defaults();
        assertEquals("7.1", properties.apiVersion().workItem());
        assertEquals("6.0",
                new AzureDevOpsAdapterProperties.ApiVersion("6.0", "6.0").workItem());
    }
}

