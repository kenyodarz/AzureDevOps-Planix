package co.com.bancolombia.config;

import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Wiring de los casos de uso.
 *
 * <p><b>Cambio de la Fase 08 (D-05, B-13): un solo mecanismo de wiring.</b> Hasta ahora esta clase
 * combinaba <b>dos</b>: el {@code @ComponentScan} por expresión regular y <b>nueve {@code @Bean}
 * manuales del mismo tipo</b>. La Fase 01 midió que el escaneo era inerte —cada caso de uso
 * resolvía a exactamente un bean—, de modo que no era un riesgo de arranque, pero sí <b>código
 * muerto que induce a error</b>: quien leía la clase no podía saber cuál de los dos mecanismos
 * estaba realmente registrando los beans.
 *
 * <p>B-13 decidió conservar el <b>escaneo</b> y retirar los {@code @Bean}. Los casos de uso se
 * registran por el filtro {@code ^.+UseCase$} y Spring resuelve sus dependencias <b>por
 * constructor</b>, que es justo lo que {@code spring-rules.md} pide para
 * {@code domain/usecase}: inyección por constructor, sin anotaciones de Spring en el dominio.
 *
 * <p>⚠️ <b>El {@code Clock} sigue siendo un {@code @Bean} explícito, y no es una excepción
 * arbitraria:</b> no es un caso de uso, así que el filtro no lo alcanza, y sin él
 * {@code ResolveTeamScopeUseCase} no puede construirse. Es la única dependencia del wiring que no
 * es un {@code *UseCase}.
 */
@Configuration
@ComponentScan(basePackages = "co.com.bancolombia.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

    /**
     * Reloj del sistema, inyectado en lugar de invocarse estáticamente.
     *
     * <p>El repliegue por concatenación necesita el año del calendario. Calcularlo con
     * {@code LocalDate.now()} dentro del dominio lo ataba al reloj de la máquina y hacía imposible
     * probarlo de forma determinista, que es una de las razones por las que <b>D-09</b> sobrevivió
     * tanto tiempo sin que nadie pudiera reproducir el fallo del tablero vacío.
     *
     * <p><b>Es el único {@code @Bean} que sobrevive a B-13</b>: el {@code @ComponentScan} solo
     * registra clases cuyo nombre acaba en {@code UseCase}, y {@link Clock} no lo es. Sin esta
     * declaración, {@code ResolveTeamScopeUseCase} no tendría cómo resolver su cuarto argumento.
     * {@link TeamScopeFallbackMetrics} sí lo aporta el adaptador de métricas.
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
