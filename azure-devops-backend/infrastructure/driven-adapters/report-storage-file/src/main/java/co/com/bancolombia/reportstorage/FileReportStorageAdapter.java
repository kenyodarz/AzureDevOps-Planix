package co.com.bancolombia.reportstorage;

import co.com.bancolombia.model.dashboard.gateways.ReportStoragePort;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Escribe los reportes del tablero en el sistema de ficheros (DP-04, opción A).
 *
 * <p>La escritura es bloqueante por naturaleza, así que se confina en
 * {@link Schedulers#boundedElastic()}: el hilo del bucle de eventos no se toca. Es exactamente lo
 * que no hacía el caso de uso antes de la Fase 06.
 *
 * <p>El directorio es configurable ({@code report.storage.directory}) y por defecto vale
 * {@code reports}, que es la ruta relativa que se venía usando: el valor observable no cambia.
 */
@Slf4j
@Component
public class FileReportStorageAdapter implements ReportStoragePort {

    private final Path baseDirectory;

    public FileReportStorageAdapter(
            @Value("${report.storage.directory:reports}") String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory).normalize();
    }

    @Override
    public Mono<Void> save(String reportName, String content) {
        if (reportName == null || reportName.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("El nombre del reporte es obligatorio"));
        }
        return Mono.fromCallable(() -> write(reportName, content == null ? "" : content))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Path write(String reportName, String content) throws java.io.IOException {
        Path target = resolveInside(reportName);
        Files.createDirectories(baseDirectory);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        log.info("Reporte generado en {}", target);
        return target;
    }

    /**
     * Impide que un nombre de reporte manipulado escriba fuera del directorio configurado. El
     * nombre lo compone el dominio a partir de la célula y el sprint que teclea el usuario, así que
     * es entrada externa y hay que tratarla como tal.
     */
    private Path resolveInside(String reportName) {
        Path target = baseDirectory.resolve(reportName).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new IllegalArgumentException(
                    "El nombre del reporte no puede salir del directorio configurado: "
                            + reportName);
        }
        return target;
    }
}

