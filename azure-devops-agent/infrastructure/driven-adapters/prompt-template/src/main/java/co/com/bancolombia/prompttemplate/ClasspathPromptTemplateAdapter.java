package co.com.bancolombia.prompttemplate;

import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.exceptions.PromptTemplateException;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Adaptador que resuelve las plantillas de prompt desde el classpath.
 *
 * <p>Todas las plantillas se cargan y cachean <b>una sola vez durante la construcción del bean</b>.
 * Nunca se lee del classpath durante la atención de una petición.
 *
 * <p>Las variables se sustituyen por nombre mediante marcadores {@code {{nombre}}}. Antes de
 * sustituir se verifica que la plantilla no declare marcadores sin valor, de modo que jamás se
 * envíe al modelo un prompt a medio renderizar.
 */
@Component
public class ClasspathPromptTemplateAdapter implements PromptTemplatePort {

    private static final String TEMPLATES_FOLDER = "prompts/";
    private static final String FRAGMENTS_FOLDER = TEMPLATES_FOLDER + "fragmentos/";

    /**
     * Nombre del fragmento compartido que contiene la tabla oficial de equivalencia Story Points a
     * horas. Se inyecta automáticamente en toda plantilla que lo declare.
     */
    private static final String SHARED_ESTIMATION_TABLE = "tablaEstimacion";

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private static final Map<PromptTemplateId, String> TEMPLATE_FILES = Map.of(
            PromptTemplateId.PLANNING_DRAFT, "fase1-propuesta-inicial.md",
            PromptTemplateId.STRUCTURED_STORY, "fase2-historia-estructurada.md",
            PromptTemplateId.STORY_DIVISION, "division-historias.md",
            PromptTemplateId.STORY_REFINEMENT, "refinamiento-historia.md",
            PromptTemplateId.QUALITY_AUDIT, "auditoria-calidad.md",
            PromptTemplateId.PROGRAM_PLANNING, "program-planning-roadmap.md",
            PromptTemplateId.EVALUATE_PULL_REQUEST, "evaluate-pull-request.st");

    private final Map<PromptTemplateId, String> templates;
    private final Map<String, Object> sharedFragments;

    @Autowired
    public ClasspathPromptTemplateAdapter() {
        this(TEMPLATES_FOLDER, FRAGMENTS_FOLDER);
    }

    /**
     * Constructor para pruebas, que permite apuntar a carpetas alternativas del classpath.
     */
    ClasspathPromptTemplateAdapter(String templatesFolder, String fragmentsFolder) {
        Map<PromptTemplateId, String> loaded = new EnumMap<>(PromptTemplateId.class);
        for (Map.Entry<PromptTemplateId, String> entry : TEMPLATE_FILES.entrySet()) {
            loaded.put(entry.getKey(), readResource(templatesFolder + entry.getValue()));
        }
        this.templates = Map.copyOf(loaded);

        Map<String, Object> fragments = new LinkedHashMap<>();
        fragments.put(SHARED_ESTIMATION_TABLE, readResource(fragmentsFolder + "tabla-estimacion.md"));
        this.sharedFragments = Map.copyOf(fragments);
    }

    @Override
    public String render(PromptTemplateId id, Map<String, Object> variables) {
        Objects.requireNonNull(id, "El identificador de plantilla es obligatorio");
        Objects.requireNonNull(variables, "El mapa de variables es obligatorio");

        String template = templates.get(id);
        if (template == null) {
            throw new PromptTemplateException("No existe la plantilla de prompt: " + id);
        }

        Map<String, Object> effectiveValues = new LinkedHashMap<>(sharedFragments);
        effectiveValues.putAll(variables);

        assertAllPlaceholdersResolvable(id, template, effectiveValues);
        return substitute(template, effectiveValues);
    }

    /**
     * Verifica, <b>antes</b> de sustituir, que la plantilla no declare marcadores sin valor.
     *
     * <p>La comprobación se hace sobre la plantilla original y no sobre el resultado, para no
     * confundir un marcador real con texto inyectado que casualmente contenga llaves dobles.
     */
    private void assertAllPlaceholdersResolvable(PromptTemplateId id, String template,
            Map<String, Object> values) {
        Set<String> missing = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!values.containsKey(placeholder)) {
                missing.add(placeholder);
            }
        }
        if (!missing.isEmpty()) {
            throw new PromptTemplateException(
                    "La plantilla " + id + " declara marcadores sin valor: " + missing);
        }
    }

    private String substitute(String template, Map<String, Object> values) {
        String result = template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String marker = "{{" + entry.getKey() + "}}";
            Object rawValue = entry.getValue();
            String value = rawValue == null ? "" : rawValue.toString();
            result = result.replace(marker, value);
        }
        return result;
    }

    private String readResource(String path) {
        Resource resource = new ClassPathResource(path);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException e) {
            throw new PromptTemplateException(
                    "No se pudo cargar la plantilla de prompt desde el classpath: " + path, e);
        }
    }
}

