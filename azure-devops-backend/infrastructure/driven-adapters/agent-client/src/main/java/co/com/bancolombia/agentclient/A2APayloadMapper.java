package co.com.bancolombia.agentclient;

import co.com.bancolombia.model.a2a.AgentCapabilities;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.AgentInterface;
import co.com.bancolombia.model.a2a.AgentProvider;
import co.com.bancolombia.model.a2a.AgentSkill;
import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentCommand;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce entre el modelo de dominio A2A y el JSON crudo que viaja hacia y desde el agente.
 *
 * <p>Cumple la exigencia de <b>mapper obligatorio</b> de {@code rules/spring-rules.md} §1: el JSON
 * del agente nunca se deserializa directamente sobre las clases del dominio, de modo que el dominio
 * permanece libre de anotaciones de serialización y un cambio en el cable no lo arrastra.
 *
 * <p>Es deliberadamente tolerante: los campos ausentes se resuelven a {@code null} o a colecciones
 * vacías, nunca a una excepción. Un agente que añada campos nuevos no debe romper al BFF.
 */
final class A2APayloadMapper {

    private static final String MESSAGE_KEY = "message";
    private static final String TASK_KEY = "task";
    private static final String PARTS_KEY = "parts";
    private static final String TASKS_KEY = "tasks";
    private static final String NAME_KEY = "name";
    private static final String URL_KEY = "url";
    private static final String DESCRIPTION_KEY = "description";
    private static final String TAGS_KEY = "tags";
    private static final String CONTEXT_ID_KEY = "contextId";
    private static final String METADATA_KEY = "metadata";
    private static final String SECURITY_SCHEMES_KEY = "securitySchemes";

    private A2APayloadMapper() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * Construye el {@code params} de {@code message/send} a partir de una orden del dominio.
     *
     * @param command orden ya validada
     * @return el mapa listo para serializar dentro del sobre JSON-RPC
     */
    static Map<String, Object> toSendMessageParams(AgentCommand command) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("messageId", command.messageId());
        message.put(CONTEXT_ID_KEY, command.contextId());
        message.put(PARTS_KEY, List.of(Map.of("text", command.prompt())));

        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("blocking", command.blocking());

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(MESSAGE_KEY, message);
        params.put("configuration", configuration);
        return params;
    }

    /**
     * Construye el {@code params} de {@code tasks/get} y {@code tasks/cancel}.
     *
     * @param taskId identificador de la tarea
     * @return el mapa listo para serializar
     */
    static Map<String, Object> toTaskParams(String taskId) {
        return Map.of("taskId", taskId);
    }

    /**
     * Extrae la tarea del {@code result} de la respuesta del agente.
     *
     * @param result cuerpo de {@code result}, puede ser nulo
     * @return la tarea, o {@code null} si el agente no abrió ninguna
     */
    static Task toTask(Map<String, Object> result) {
        if (result == null) {
            return null;
        }
        return buildTask(result.get(TASK_KEY));
    }

    /**
     * Extrae el texto de la respuesta del agente del {@code result}.
     *
     * <p>Busca primero en {@code result.message} y, si no hay, en el mensaje embebido dentro del
     * estado de la tarea, que es donde el agente deja el aviso cuando responde de forma no
     * bloqueante.
     *
     * @param result cuerpo de {@code result}, puede ser nulo
     * @return el texto encontrado, o cadena vacía
     */
    static String toReply(Map<String, Object> result) {
        if (result == null) {
            return "";
        }
        Message message = buildMessage(result.get(MESSAGE_KEY));
        if (message != null && !message.extractText().isEmpty()) {
            return message.extractText();
        }
        Task task = toTask(result);
        if (task != null && task.getStatus() != null && task.getStatus().getMessage() != null) {
            return task.getStatus().getMessage().extractText();
        }
        return "";
    }

    /**
     * Extrae el listado de tareas de la respuesta REST de {@code GET /api/tasks} del agente.
     *
     * <p>Es tolerante con la forma del cuerpo porque el agente no la declara en un contrato
     * versionado: acepta tanto un array JSON en la raíz como un objeto envolvente con una clave
     * {@code tasks}. Cualquier otra forma se resuelve a lista vacía, nunca a excepción: el panel de
     * progreso del frontend es informativo y no debe caerse porque el agente cambie el sobre.
     *
     * @param rawBody cuerpo de la respuesta, ya deserializado a estructuras genéricas
     * @return las tareas encontradas; lista vacía si no hay ninguna reconocible
     */
    static List<Task> toTasks(Object rawBody) {
        Object rawList = rawBody;
        if (rawBody instanceof Map<?, ?>) {
            rawList = asMap(rawBody).get(TASKS_KEY);
        }
        if (!(rawList instanceof List<?> items)) {
            return List.of();
        }
        List<Task> tasks = new ArrayList<>();
        for (Object item : items) {
            Task task = buildTask(item);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    /**
     * Construye la tarjeta de presentación del agente a partir del JSON de
     * {@code GET /.well-known/agent-card.json}.
     *
     * @param raw cuerpo de la respuesta; puede ser nulo
     * @return la tarjeta, o {@code null} si no hay cuerpo
     */
    static AgentCard toAgentCard(Map<String, Object> raw) {
        if (raw == null) {
            return null;
        }
        return AgentCard.builder()
                .protocolVersion(asString(raw.get("protocolVersion")))
                .name(asString(raw.get(NAME_KEY)))
                .description(asString(raw.get(DESCRIPTION_KEY)))
                .version(asString(raw.get("version")))
                .provider(buildProvider(raw.get("provider")))
                .supportedInterfaces(buildInterfaces(raw.get("supportedInterfaces")))
                .capabilities(buildCapabilities(raw.get("capabilities")))
                .defaultInputModes(asStringList(raw.get("defaultInputModes")))
                .defaultOutputModes(asStringList(raw.get("defaultOutputModes")))
                .skills(buildSkills(raw.get("skills")))
                .securitySchemes(raw.get(SECURITY_SCHEMES_KEY) instanceof Map<?, ?> schemes
                        ? asMap(schemes)
                        : null)
                .tags(asStringList(raw.get(TAGS_KEY)))
                .build();
    }

    private static AgentProvider buildProvider(Object rawProvider) {
        if (!(rawProvider instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, Object> raw = asMap(rawProvider);
        return AgentProvider.builder()
                .organization(asString(raw.get("organization")))
                .url(asString(raw.get(URL_KEY)))
                .build();
    }

    private static AgentCapabilities buildCapabilities(Object rawCapabilities) {
        if (!(rawCapabilities instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, Object> raw = asMap(rawCapabilities);
        return AgentCapabilities.builder()
                .streaming(Boolean.TRUE.equals(raw.get("streaming")))
                .pushNotifications(Boolean.TRUE.equals(raw.get("pushNotifications")))
                .build();
    }

    private static List<AgentInterface> buildInterfaces(Object rawInterfaces) {
        if (!(rawInterfaces instanceof List<?> items)) {
            return List.of();
        }
        List<AgentInterface> interfaces = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?>) {
                Map<String, Object> raw = asMap(item);
                interfaces.add(AgentInterface.builder()
                        .protocol(asString(raw.get("protocol")))
                        .url(asString(raw.get(URL_KEY)))
                        .build());
            }
        }
        return interfaces;
    }

    private static List<AgentSkill> buildSkills(Object rawSkills) {
        if (!(rawSkills instanceof List<?> items)) {
            return List.of();
        }
        List<AgentSkill> skills = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?>) {
                skills.add(buildSkill(asMap(item)));
            }
        }
        return skills;
    }

    private static AgentSkill buildSkill(Map<String, Object> raw) {
        return AgentSkill.builder()
                .id(asString(raw.get("id")))
                .name(asString(raw.get(NAME_KEY)))
                .description(asString(raw.get(DESCRIPTION_KEY)))
                .tags(asStringList(raw.get(TAGS_KEY)))
                .inputModes(asStringList(raw.get("inputModes")))
                .outputModes(asStringList(raw.get("outputModes")))
                .examples(asStringList(raw.get("examples")))
                .schema(raw.get("schema") instanceof Map<?, ?> schema ? asMap(schema) : null)
                .build();
    }

    private static Task buildTask(Object rawTaskValue) {
        if (!(rawTaskValue instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, Object> rawTask = asMap(rawTaskValue);
        return Task.builder()
                .id(asString(rawTask.get("id")))
                .contextId(asString(rawTask.get(CONTEXT_ID_KEY)))
                .status(buildStatus(rawTask.get("status")))
                .history(buildMessages(rawTask.get("history")))
                .metadata(rawTask.get(METADATA_KEY) instanceof Map<?, ?> metadata
                        ? asMap(metadata)
                        : null)
                .build();
    }

    private static TaskStatus buildStatus(Object rawStatusValue) {
        if (!(rawStatusValue instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, Object> rawStatus = asMap(rawStatusValue);
        return TaskStatus.builder()
                .state(toState(asString(rawStatus.get("state"))))
                .message(buildMessage(rawStatus.get(MESSAGE_KEY)))
                .timestamp(asString(rawStatus.get("timestamp")))
                .build();
    }

    /**
     * Traduce el valor textual del estado, que el agente serializa en minúsculas y con guion
     * ({@code input-required}), al enumerado del dominio. Un estado desconocido devuelve
     * {@code null} en lugar de reventar: el BFF no debe caerse porque el agente añada un estado.
     */
    private static TaskState toState(String rawState) {
        if (rawState == null) {
            return null;
        }
        for (TaskState state : TaskState.values()) {
            if (state.getValue().equalsIgnoreCase(rawState) || state.name()
                    .equalsIgnoreCase(rawState)) {
                return state;
            }
        }
        return null;
    }

    private static Message buildMessage(Object rawMessageValue) {
        if (!(rawMessageValue instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, Object> rawMessage = asMap(rawMessageValue);
        return Message.builder()
                .role(asString(rawMessage.get("role")))
                .messageId(asString(rawMessage.get("messageId")))
                .contextId(asString(rawMessage.get(CONTEXT_ID_KEY)))
                .parts(buildParts(rawMessage.get(PARTS_KEY)))
                .referenceTaskIds(asStringList(rawMessage.get("referenceTaskIds")))
                .build();
    }

    private static List<Message> buildMessages(Object rawHistory) {
        if (!(rawHistory instanceof List<?> rawList)) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>();
        for (Object item : rawList) {
            Message message = buildMessage(item);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    private static List<Part> buildParts(Object rawParts) {
        if (!(rawParts instanceof List<?> rawList)) {
            return List.of();
        }
        List<Part> parts = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?>) {
                Map<String, Object> rawPart = asMap(item);
                parts.add(Part.builder()
                        .text(asString(rawPart.get("text")))
                        .data(rawPart.get("data") instanceof Map<?, ?> data ? asMap(data) : null)
                        .url(asString(rawPart.get("url")))
                        .mediaType(asString(rawPart.get("mediaType")))
                        .filename(asString(rawPart.get("filename")))
                        .metadata(rawPart.get(METADATA_KEY) instanceof Map<?, ?> metadata
                                ? asMap(metadata)
                                : null)
                        .build());
            }
        }
        return parts;
    }

    private static String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(asString(item));
        }
        return result;
    }

    /**
     * Convierte a {@code Map<String, Object>} cualquier estructura genérica, descartando claves que
     * no sean texto.
     *
     * <p><b>Nunca devuelve {@code null}</b>: un valor que no es un mapa se resuelve a mapa vacío.
     * Así ningún llamador tiene que comprobar nulos antes de un {@code get(...)}. La contrapartida
     * es que «no venía» y «venía vacío» aquí son indistinguibles; donde esa diferencia importa
     * —{@code metadata} y {@code securitySchemes}, que se guardan y se publican tal cual— el
     * llamador hace la comprobación en línea, para no reintroducir un método que devuelva
     * colecciones nulas.
     */
    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}

