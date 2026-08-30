package co.com.bancolombia.config.dashboard;

import co.com.bancolombia.model.dashboard.gateways.DashboardFallbackPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Sirve el tablero simulado desde {@code classpath:mock/dashboard.json}.
 *
 * <p>Hasta la Fase 04 estas 66 líneas de JSON eran una constante dentro de
 * {@code DevOpsDashboardUseCase}: un artefacto de pruebas incrustado en el dominio.
 *
 * <p><b>Se lee una sola vez, al construir el bean.</b> Por eso el puerto puede ser síncrono sin
 * violar DP-08: en tiempo de petición no queda entrada/salida que bloquear. Si el recurso falta, el
 * contexto de Spring no arranca —es preferible a descubrirlo en la primera petición.
 */
@Component
public class ClasspathDashboardFallbackAdapter implements DashboardFallbackPort {

    private static final String RESOURCE = "mock/dashboard.json";

    private final String payload;

    public ClasspathDashboardFallbackAdapter() {
        this.payload = readResource();
    }

    private static String readResource() {
        try (InputStream stream = ClasspathDashboardFallbackAdapter.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "No se encontró el recurso de respaldo del tablero: " + RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n")).stripTrailing() + "\n";
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo leer el recurso de respaldo del tablero: " + RESOURCE, e);
        }
    }

    @Override
    public String mockDashboard() {
        return payload;
    }
}

