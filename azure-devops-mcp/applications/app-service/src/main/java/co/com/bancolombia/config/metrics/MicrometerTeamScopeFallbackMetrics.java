package co.com.bancolombia.config.metrics;

import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementación con Micrometer del puerto {@link TeamScopeFallbackMetrics}.
 *
 * <p>Cierra la mitad que faltaba de la deuda <b>D-09</b>: hasta la Fase 04, el repliegue por
 * concatenación se disparaba <b>sin dejar ningún dato</b>. Ahora cada disparo queda contado y
 * expuesto por el actuator, de modo que la decisión de retirarlo —que DP-04 §0.1 aplazó al elegir
 * la opción (a)— podrá tomarse con cifras en lugar de con intuiciones.
 *
 * <p>Aquí vive también el {@code WARN}, y no en el caso de uso, porque el módulo
 * {@code domain/usecase} no admite ninguna dependencia más allá de {@code :model} —SLF4J
 * incluido—; el puerto transporta la excepción original para que el aviso no pierda información de
 * diagnóstico.
 *
 * <h2>Por qué el equipo y el sprint NO son etiquetas</h2>
 *
 * <p>Etiquetar por nombre de célula o de sprint produciría una serie temporal nueva por cada valor
 * distinto —cardinalidad ilimitada, porque el nombre lo escribe una persona en un formulario—, que
 * es la forma más habitual de tumbar un sistema de métricas. El nombre concreto va al {@code WARN},
 * que es donde se necesita para diagnosticar un caso puntual; la métrica solo responde «cuántas
 * veces» y «de qué tipo».
 */
@Slf4j
@Component
public class MicrometerTeamScopeFallbackMetrics implements TeamScopeFallbackMetrics {

    private static final String COUNTER_NAME = "azuredevops.teamscope.fallback";
    private static final String TAG_PATH = "path";
    private static final String TAG_CALENDAR_YEAR = "calendarYearInterleaved";

    private final Counter areaPathFallbacks;
    private final Counter iterationPathFallbacksWithYear;
    private final Counter iterationPathFallbacksWithoutYear;

    public MicrometerTeamScopeFallbackMetrics(MeterRegistry meterRegistry) {
        this.areaPathFallbacks = Counter.builder(COUNTER_NAME)
                .description("Veces que el AreaPath se fabricó por concatenación porque Azure DevOps no lo resolvió")
                .tag(TAG_PATH, "area")
                .tag(TAG_CALENDAR_YEAR, "false")
                .register(meterRegistry);
        this.iterationPathFallbacksWithYear = Counter.builder(COUNTER_NAME)
                .description("Veces que el IterationPath se fabricó por concatenación intercalando el año del calendario")
                .tag(TAG_PATH, "iteration")
                .tag(TAG_CALENDAR_YEAR, "true")
                .register(meterRegistry);
        this.iterationPathFallbacksWithoutYear = Counter.builder(COUNTER_NAME)
                .description("Veces que el IterationPath se fabricó por concatenación sin intercalar año")
                .tag(TAG_PATH, "iteration")
                .tag(TAG_CALENDAR_YEAR, "false")
                .register(meterRegistry);
    }

    @Override
    public void areaPathFallbackUsed(String team, Throwable cause) {
        areaPathFallbacks.increment();
        log.warn(
                "No se pudo obtener el AreaPath dinámico para la célula: {}. Se usará la normalización por defecto.",
                team, cause);
    }

    @Override
    public void iterationPathFallbackUsed(String sprint, boolean calendarYearInterleaved,
            Throwable cause) {
        if (calendarYearInterleaved) {
            iterationPathFallbacksWithYear.increment();
            log.warn(
                    "No se pudo obtener el IterationPath dinámico para el sprint: {}. Se fabricará por concatenación intercalando el año en curso, lo que devolverá cero elementos si el sprint cruza el cambio de ejercicio.",
                    sprint, cause);
        } else {
            iterationPathFallbacksWithoutYear.increment();
            log.warn(
                    "No se pudo obtener el IterationPath dinámico para el sprint: {}. Se usará la normalización por defecto.",
                    sprint, cause);
        }
    }
}

