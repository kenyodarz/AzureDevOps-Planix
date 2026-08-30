package co.com.bancolombia.config.prompt;

import co.com.bancolombia.model.prompt.exceptions.PromptTemplateException;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sirve las plantillas de prompt desde {@code classpath:prompts/{nombre}.txt}.
 *
 * <p>Aquí termina el viaje que empieza en la Fase 04: hasta entonces estos textos eran constantes
 * {@code String} dentro de {@code DevOpsDashboardUseCase}, es decir, la interfaz con un proveedor
 * de IA concreto viviendo en el dominio. Convertirlos en ficheros es además el requisito previo de
 * <b>D-35</b>, que los llevará al MCP y al agente.
 *
 * <p><b>Caché.</b> Las plantillas son inmutables durante la vida del proceso; releer el classpath
 * en cada petición sería E/S repetida sin ganancia. Se cachean por nombre.
 *
 * <p><b>Normalización.</b> Se descartan las líneas de metadato ({@code #~}), se unifican los
 * saltos de línea a {@code \n} —para que el prompt no dependa del sistema que hizo el checkout— y
 * se garantiza exactamente un salto final, igual que producía el bloque de texto original.
 */
@Component
public class ClasspathPromptTemplateAdapter implements PromptTemplatePort {

    private static final String BASE_PATH = "prompts/";
    private static final String EXTENSION = ".txt";
    private static final String METADATA_PREFIX = "#~";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");

    private final ResourceReader resourceReader;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Constructor que usa Spring.
     *
     * <p>Va anotado con {@code @Autowired} porque la clase tiene <b>dos</b> constructores y sin la
     * anotación el contenedor tiene que adivinar cuál es el bueno. El otro es el de pruebas, que
     * recibe un lector falso: si algún día Spring eligiera ése, el adaptador serviría plantillas de
     * mentira en producción sin que nada fallara al arrancar.
     */
    @Autowired
    public ClasspathPromptTemplateAdapter() {
        this(ClasspathPromptTemplateAdapter::openFromClasspath);
    }

    /**
     * Constructor de pruebas: permite contar cuántas veces se lee realmente el recurso y así fijar
     * que la caché hace su trabajo.
     */
    ClasspathPromptTemplateAdapter(ResourceReader resourceReader) {
        this.resourceReader = resourceReader;
    }

    private static InputStream openFromClasspath(String path) {
        return ClasspathPromptTemplateAdapter.class.getClassLoader().getResourceAsStream(path);
    }

    @Override
    public String render(String name, Map<String, String> variables) {
        if (name == null || name.isBlank()) {
            throw new PromptTemplateException("El nombre de la plantilla de prompt es obligatorio");
        }
        String template = cache.computeIfAbsent(name, this::load);
        return substitute(name, template, variables == null ? Map.of() : variables);
    }

    private String load(String name) {
        try (InputStream stream = resourceReader.open(BASE_PATH + name + EXTENSION)) {
            if (stream == null) {
                throw PromptTemplateException.notFound(name, null);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String body = reader.lines()
                        .filter(line -> !line.startsWith(METADATA_PREFIX))
                        .collect(Collectors.joining("\n"));
                return body.stripTrailing() + "\n";
            }
        } catch (IOException e) {
            throw PromptTemplateException.notFound(name, e);
        }
    }

    private String substitute(String name, String template, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String variable = matcher.group(1);
            String value = variables.get(variable);
            if (value == null) {
                throw PromptTemplateException.missingVariable(name, variable);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    /**
     * Origen de bytes de una plantilla. Devuelve {@code null} si el recurso no existe.
     */
    @FunctionalInterface
    interface ResourceReader {

        InputStream open(String path);
    }
}

