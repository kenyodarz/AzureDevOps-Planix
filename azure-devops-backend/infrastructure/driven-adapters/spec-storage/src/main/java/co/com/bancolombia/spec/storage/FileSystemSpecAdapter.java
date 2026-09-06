package co.com.bancolombia.spec.storage;

import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adaptador de infraestructura que gestiona la persistencia y lectura de documentos de
 * especificación funcional y técnica en el sistema de archivos local.
 *
 * <p>Todas las operaciones de I/O se delegan estrictamente a {@link Schedulers#boundedElastic()}
 * para garantizar la compatibilidad con el modelo reactivo no bloqueante de Netty y BlockHound.
 * Incorpora defensas en profundidad contra ataques de Path Traversal.
 */
@Slf4j
@Repository
public class FileSystemSpecAdapter implements SpecStoragePort {

    private static final String MARKDOWN_EXTENSION = ".md";
    private final Path baseDirectory;

    @Autowired
    public FileSystemSpecAdapter(@Value("${specs.storage.path:specs}") String basePath) {
        this(Path.of(basePath));
    }

    public FileSystemSpecAdapter(Path basePath) {
        if (basePath == null) {
            throw new IllegalArgumentException("La ruta base de almacenamiento no puede ser nula");
        }
        this.baseDirectory = basePath.normalize().toAbsolutePath();
    }

    @Override
    public Mono<SpecDocument> getSpec(String specName) {
        return Mono.fromCallable(() -> {
            Path target = resolveInside(specName);
            if (!Files.exists(target) || !Files.isRegularFile(target)) {
                throw new SpecNotFoundException(specName);
            }
            String content = Files.readString(target, StandardCharsets.UTF_8);
            log.debug("Documento de especificación leído exitosamente desde {}", target);
            return new SpecDocument(target.getFileName().toString(), content, target.toString());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> listAvailableSpecs() {
        return Mono.fromCallable(() -> {
                    if (!Files.exists(baseDirectory) || !Files.isDirectory(baseDirectory)) {
                        log.debug("El directorio de especificaciones no existe o no es directorio: {}",
                                baseDirectory);
                        return Collections.<String>emptyList();
                    }
                    try (Stream<Path> stream = Files.list(baseDirectory)) {
                        List<String> specs = stream
                                .filter(Files::isRegularFile)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(MARKDOWN_EXTENSION))
                                .sorted()
                                .toList();
                        log.debug("Listadas {} especificaciones disponibles en {}", specs.size(),
                                baseDirectory);
                        return specs;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Void> saveSpec(String specName, String markdownContent) {
        return Mono.fromCallable(() -> {
                    Path target = resolveInside(specName);
                    Files.createDirectories(baseDirectory);
                    String safeContent = (markdownContent == null) ? "" : markdownContent;
                    Files.writeString(target, safeContent, StandardCharsets.UTF_8);
                    log.info("Documento de especificación guardado en {}", target);
                    return target;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Resuelve de forma segura la ruta relativa dentro del directorio base configurado, previniendo
     * ataques de Path Traversal.
     *
     * @param specName Nombre del archivo del spec.
     * @return {@link Path} absoluto normalizado dentro del directorio base.
     * @throws IllegalArgumentException si el nombre es nulo, en blanco, contiene caracteres
     *                                  maliciosos o intenta escapar del directorio base.
     */
    private Path resolveInside(String specName) {
        if (specName == null || specName.isBlank()) {
            throw new IllegalArgumentException("El nombre del spec no puede ser nulo ni vacío");
        }
        if (specName.contains("..") || specName.contains("/") || specName.contains("\\")) {
            throw new IllegalArgumentException(
                    "Nombre de especificación no válido o potencial path traversal: " + specName);
        }
        Path target = baseDirectory.resolve(specName).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new IllegalArgumentException(
                    "El spec no puede residir fuera del directorio configurado: " + specName);
        }
        return target;
    }
}
