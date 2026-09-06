package co.com.bancolombia.spec.storage;

import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adaptador de infraestructura que gestiona el almacenamiento y lectura de documentos de
 * especificación directamente en el sistema de archivos local o workspace.
 *
 * <p>Todas las operaciones I/O en disco se ejecutan de manera no bloqueante derivando
 * su ejecución a {@link Schedulers#boundedElastic()}.
 */
@Component
public class FileSystemSpecAdapter implements SpecStoragePort {

    private static final String MARKDOWN_EXTENSION = ".md";
    private static final String PATH_TRAVERSAL_INDICATOR = "..";

    private final Path basePath;

    @Autowired
    public FileSystemSpecAdapter(@Value("${agent.specs.base-path:specs}") String basePath) {
        this(Path.of(Objects.requireNonNull(basePath, "La ruta base no puede ser nula")));
    }

    public FileSystemSpecAdapter(Path basePath) {
        this.basePath = Objects.requireNonNull(basePath, "La ruta base no puede ser nula")
                .normalize();
    }

    @Override
    public Mono<SpecDocument> getSpec(String specName) {
        return Mono.fromCallable(() -> {
            Path targetPath = resolveAndValidatePath(specName);
            if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
                throw new SpecNotFoundException(specName);
            }
            try {
                String content = Files.readString(targetPath, StandardCharsets.UTF_8);
                return new SpecDocument(specName, content, targetPath.toString());
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Error al leer el archivo de especificación: " + specName, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> listAvailableSpecs() {
        return Mono.fromCallable(() -> {
                    if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
                        return List.<String>of();
                    }
                    try (Stream<Path> stream = Files.list(basePath)) {
                        return stream
                                .filter(Files::isRegularFile)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(fileName -> fileName.endsWith(MARKDOWN_EXTENSION))
                                .sorted()
                                .toList();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Error al listar las especificaciones en: " + basePath, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Void> saveSpec(String specName, String markdownContent) {
        return Mono.fromRunnable(() -> {
                    Path targetPath = resolveAndValidatePath(specName);
                    try {
                        Path parentDirectory = targetPath.getParent();
                        if (parentDirectory != null && !Files.exists(parentDirectory)) {
                            Files.createDirectories(parentDirectory);
                        }
                        String contentToWrite = markdownContent != null ? markdownContent : "";
                        Files.writeString(targetPath, contentToWrite, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Error al guardar el archivo de especificación: " + specName, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Valida y resuelve la ruta de un archivo de especificación previniendo ataques de Path
     * Traversal.
     *
     * @param specName Nombre o identificador del archivo.
     * @return {@link Path} absoluto o relativo normalizado dentro de {@code basePath}.
     * @throws IllegalArgumentException si el nombre es nulo, vacío o intenta salirse de la ruta
     *                                  base.
     */
    private Path resolveAndValidatePath(String specName) {
        if (specName == null || specName.isBlank()) {
            throw new IllegalArgumentException("El nombre del spec no puede ser nulo ni vacío");
        }
        if (specName.contains(PATH_TRAVERSAL_INDICATOR)) {
            throw new IllegalArgumentException(
                    "Ruta no permitida (Path Traversal detectado): " + specName);
        }
        Path resolved = basePath.resolve(specName).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException(
                    "Ruta no permitida (fuera del directorio base): " + specName);
        }
        return resolved;
    }
}
