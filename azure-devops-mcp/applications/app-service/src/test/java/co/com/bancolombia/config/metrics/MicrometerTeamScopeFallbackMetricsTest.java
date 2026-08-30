package co.com.bancolombia.config.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>La prueba que demuestra que D-09 dejó de ser invisible.</b>
 *
 * <p>Hasta la Fase 04 no existía ninguna forma de saber cuántas veces se disparaba el repliegue por
 * concatenación. Aquí se comprueba, contra un registro de métricas real
 * ({@link SimpleMeterRegistry}), que cada disparo se cuenta y que el caso peligroso —el que
 * intercala el año del calendario— queda separado del inocuo por su etiqueta.
 */
class MicrometerTeamScopeFallbackMetricsTest {

    private static final String COUNTER = "azuredevops.teamscope.fallback";

    private MeterRegistry registry;
    private MicrometerTeamScopeFallbackMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MicrometerTeamScopeFallbackMetrics(registry);
    }

    private double count(String path, String calendarYearInterleaved) {
        return registry.get(COUNTER)
                .tag("path", path)
                .tag("calendarYearInterleaved", calendarYearInterleaved)
                .counter()
                .count();
    }

    @Test
    @DisplayName("GIVEN un registro recien creado WHEN no hay repliegues THEN los contadores estan a cero")
    void givenFreshRegistry_whenNoFallbacks_thenCountersAreZero() {
        assertEquals(0.0, count("area", "false"));
        assertEquals(0.0, count("iteration", "true"));
        assertEquals(0.0, count("iteration", "false"));
    }

    @Test
    @DisplayName("GIVEN un repliegue del AreaPath WHEN se notifica THEN se incrementa su contador")
    void givenAreaFallback_whenReported_thenItsCounterIncreases() {
        // Act (WHEN)
        metrics.areaPathFallbackUsed("EQU1096 - EXODIA", new IllegalStateException("sin respuesta"));

        // Assert (THEN)
        assertEquals(1.0, count("area", "false"));
        assertEquals(0.0, count("iteration", "true"));
    }

    /**
     * El caso caro: el que produce el tablero vacío cuando el sprint cruza el cambio de ejercicio.
     * Debe quedar contado <b>aparte</b>, o el dato no sirve para decidir nada.
     */
    @Test
    @DisplayName("GIVEN un repliegue con anio intercalado WHEN se notifica THEN se cuenta aparte del inocuo")
    void givenIterationFallbackWithYear_whenReported_thenItIsCountedSeparately() {
        // Act (WHEN)
        metrics.iterationPathFallbackUsed("Sprint 247", true, new RuntimeException("sin respuesta"));
        metrics.iterationPathFallbackUsed("Cierre de Trimestre", false,
                new RuntimeException("sin respuesta"));

        // Assert (THEN)
        assertEquals(1.0, count("iteration", "true"));
        assertEquals(1.0, count("iteration", "false"));
        assertEquals(0.0, count("area", "false"));
    }

    @Test
    @DisplayName("GIVEN varios repliegues del mismo tipo WHEN se notifican THEN el contador acumula")
    void givenRepeatedFallbacks_whenReported_thenTheCounterAccumulates() {
        // Act (WHEN)
        for (int i = 0; i < 3; i++) {
            metrics.areaPathFallbackUsed("EQU1096 - EXODIA", new RuntimeException("sin respuesta"));
        }

        // Assert (THEN)
        assertEquals(3.0, count("area", "false"));
    }
}

