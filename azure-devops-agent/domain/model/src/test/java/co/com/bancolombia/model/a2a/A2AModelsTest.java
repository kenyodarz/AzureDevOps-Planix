package co.com.bancolombia.model.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Cubre los modelos del protocolo A2A que son contenedores de datos: construcción por builder,
 * reconstrucción con {@code toBuilder()} e igualdad por valor.
 *
 * <p>No prueban «que Lombok funcione»: prueban que **el contrato de datos publicado hacia los
 * consumidores A2A sigue siendo el que es**. Estos quince modelos viajaban sin una sola prueba
 * desde el inicio del plan y lastraban la cobertura de {@code domain/model}.
 */
@DisplayName("Modelos del protocolo A2A - Contrato de datos")
class A2AModelsTest {

    @Nested
    @DisplayName("Task y TaskStatus")
    class TaskModels {

        @Test
        @DisplayName("GIVEN una tarea completa WHEN se construye THEN expone todos sus campos")
        void givenFullTask_whenBuilt_thenExposesAllFields() {
            Task task = Task.builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(TaskStatus.builder().state(TaskState.COMPLETED).build())
                    .artifacts(List.of(Artifact.builder().artifactId("a-1").build()))
                    .history(List.of(Message.userText("hola")))
                    .metadata(Map.of("k", "v"))
                    .build();

            assertThat(task.getId()).isEqualTo("task-1");
            assertThat(task.getContextId()).isEqualTo("ctx-1");
            assertThat(task.getStatus().getState()).isEqualTo(TaskState.COMPLETED);
            assertThat(task.getArtifacts()).hasSize(1);
            assertThat(task.getHistory()).hasSize(1);
            assertThat(task.getMetadata()).containsEntry("k", "v");
        }

        @Test
        @DisplayName("GIVEN una tarea WHEN se cancela con toBuilder THEN solo cambia el estado")
        void givenTask_whenToBuilderChangesStatus_thenRestIsPreserved() {
            Task original = Task.builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(TaskStatus.builder().state(TaskState.WORKING).build())
                    .build();

            Task canceled = original.toBuilder()
                    .status(TaskStatus.builder()
                            .state(TaskState.CANCELED)
                            .timestamp("2026-08-29T00:00:00Z")
                            .build())
                    .build();

            assertThat(canceled.getId()).isEqualTo("task-1");
            assertThat(canceled.getContextId()).isEqualTo("ctx-1");
            assertThat(canceled.getStatus().getState()).isEqualTo(TaskState.CANCELED);
            assertThat(canceled.getStatus().getTimestamp()).isEqualTo("2026-08-29T00:00:00Z");
            assertThat(original.getStatus().getState()).isEqualTo(TaskState.WORKING);
        }

        @Test
        @DisplayName("GIVEN un estado con mensaje WHEN se construye THEN lo conserva")
        void givenStatusWithMessage_whenBuilt_thenKeepsIt() {
            TaskStatus status = TaskStatus.builder()
                    .state(TaskState.FAILED)
                    .message(Message.agentText("falló"))
                    .timestamp("2026-08-29T00:00:00Z")
                    .build();

            assertThat(status.getMessage().extractText()).isEqualTo("falló");
            assertThat(status.getState()).isEqualTo(TaskState.FAILED);
        }

        @Test
        @DisplayName("GIVEN una tarea sin datos WHEN se crea vacía THEN sus campos son nulos")
        void givenNoArgsConstructor_whenCreated_thenFieldsAreNull() {
            Task task = new Task();

            assertThat(task.getId()).isNull();
            assertThat(task.getStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("Mensajería: SendMessageRequest, SendMessageResponse y MessageSendConfiguration")
    class MessagingModels {

        @Test
        @DisplayName("GIVEN una petición con configuración WHEN se construye THEN expone ambas partes")
        void givenRequestWithConfiguration_whenBuilt_thenExposesBoth() {
            SendMessageRequest request = SendMessageRequest.builder()
                    .message(Message.userText("hola"))
                    .configuration(MessageSendConfiguration.builder()
                            .acceptedOutputModes(List.of("text"))
                            .historyLength(5)
                            .blocking(true)
                            .build())
                    .build();

            assertThat(request.getMessage().extractText()).isEqualTo("hola");
            assertThat(request.getConfiguration().getAcceptedOutputModes()).containsExactly("text");
            assertThat(request.getConfiguration().getHistoryLength()).isEqualTo(5);
            assertThat(request.getConfiguration().getBlocking()).isTrue();
        }

        @Test
        @DisplayName("GIVEN una respuesta con tarea y mensaje WHEN se construye THEN expone ambos")
        void givenResponse_whenBuilt_thenExposesTaskAndMessage() {
            SendMessageResponse response = SendMessageResponse.builder()
                    .task(Task.builder().id("task-1").build())
                    .message(Message.agentText("listo"))
                    .build();

            assertThat(response.getTask().getId()).isEqualTo("task-1");
            assertThat(response.getMessage().extractText()).isEqualTo("listo");
        }

        @Test
        @DisplayName("GIVEN una configuración vacía WHEN se construye THEN los opcionales son nulos")
        void givenEmptyConfiguration_whenBuilt_thenOptionalsAreNull() {
            MessageSendConfiguration configuration = MessageSendConfiguration.builder().build();

            assertThat(configuration.getAcceptedOutputModes()).isNull();
            assertThat(configuration.getHistoryLength()).isNull();
            assertThat(configuration.getBlocking()).isNull();
        }

        @Test
        @DisplayName("GIVEN dos respuestas con el mismo contenido THEN son iguales")
        void givenSameContent_whenCompared_thenAreEqual() {
            SendMessageResponse first = SendMessageResponse.builder()
                    .message(Message.agentText("ok")).build();
            SendMessageResponse second = SendMessageResponse.builder()
                    .message(Message.agentText("ok")).build();

            assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        }
    }

    @Nested
    @DisplayName("Descubrimiento: AgentCard y sus componentes")
    class DiscoveryModels {

        @Test
        @DisplayName("GIVEN una tarjeta completa WHEN se construye THEN expone todos sus campos")
        void givenFullCard_whenBuilt_thenExposesAllFields() {
            AgentCard card = AgentCard.builder()
                    .protocolVersion("1.0")
                    .name("Agente")
                    .description("Descripción")
                    .version("1.0.0")
                    .provider(AgentProvider.builder()
                            .organization("Bancolombia").url("https://x").build())
                    .supportedInterfaces(List.of(AgentInterface.builder()
                            .protocol("jsonrpc").url("http://localhost:8082").build()))
                    .capabilities(AgentCapabilities.builder()
                            .streaming(true).pushNotifications(true).build())
                    .defaultInputModes(List.of("text"))
                    .defaultOutputModes(List.of("data"))
                    .skills(List.of(AgentSkill.builder().id("s-1").name("Skill").build()))
                    .securitySchemes(Map.of("oauth2", Map.of("type", "oauth2")))
                    .tags(List.of("a2a"))
                    .build();

            assertThat(card.getProtocolVersion()).isEqualTo("1.0");
            assertThat(card.getName()).isEqualTo("Agente");
            assertThat(card.getDescription()).isEqualTo("Descripción");
            assertThat(card.getVersion()).isEqualTo("1.0.0");
            assertThat(card.getProvider().getOrganization()).isEqualTo("Bancolombia");
            assertThat(card.getProvider().getUrl()).isEqualTo("https://x");
            assertThat(card.getSupportedInterfaces().getFirst().getProtocol())
                    .isEqualTo("jsonrpc");
            assertThat(card.getSupportedInterfaces().getFirst().getUrl())
                    .isEqualTo("http://localhost:8082");
            assertThat(card.getCapabilities().isStreaming()).isTrue();
            assertThat(card.getCapabilities().isPushNotifications()).isTrue();
            assertThat(card.getDefaultInputModes()).containsExactly("text");
            assertThat(card.getDefaultOutputModes()).containsExactly("data");
            assertThat(card.getSkills()).hasSize(1);
            assertThat(card.getSecuritySchemes()).containsKey("oauth2");
            assertThat(card.getTags()).containsExactly("a2a");
        }

        @Test
        @DisplayName("GIVEN capacidades por defecto WHEN se construyen THEN todo está desactivado")
        void givenDefaultCapabilities_whenBuilt_thenEverythingIsDisabled() {
            AgentCapabilities capabilities = AgentCapabilities.builder().build();

            assertThat(capabilities.isStreaming()).isFalse();
            assertThat(capabilities.isPushNotifications()).isFalse();
        }

        @Test
        @DisplayName("GIVEN una habilidad completa WHEN se construye THEN expone todos sus campos")
        void givenFullSkill_whenBuilt_thenExposesAllFields() {
            AgentSkill skill = AgentSkill.builder()
                    .id("s-1")
                    .name("Búsqueda")
                    .description("Busca cosas")
                    .tags(List.of("finance"))
                    .inputModes(List.of("text"))
                    .outputModes(List.of("data"))
                    .examples(List.of("un ejemplo"))
                    .schema(Map.of("type", "object"))
                    .build();

            assertThat(skill.getId()).isEqualTo("s-1");
            assertThat(skill.getName()).isEqualTo("Búsqueda");
            assertThat(skill.getDescription()).isEqualTo("Busca cosas");
            assertThat(skill.getTags()).containsExactly("finance");
            assertThat(skill.getInputModes()).containsExactly("text");
            assertThat(skill.getOutputModes()).containsExactly("data");
            assertThat(skill.getExamples()).containsExactly("un ejemplo");
            assertThat(skill.getSchema()).containsEntry("type", "object");
        }
    }

    @Nested
    @DisplayName("Contenido y errores: Artifact y Error")
    class ContentModels {

        @Test
        @DisplayName("GIVEN un artefacto WHEN se construye THEN expone sus partes")
        void givenArtifact_whenBuilt_thenExposesParts() {
            Artifact artifact = Artifact.builder()
                    .artifactId("a-1")
                    .name("Informe")
                    .description("Un informe")
                    .parts(List.of(Part.ofText("contenido")))
                    .build();

            assertThat(artifact.getArtifactId()).isEqualTo("a-1");
            assertThat(artifact.getName()).isEqualTo("Informe");
            assertThat(artifact.getDescription()).isEqualTo("Un informe");
            assertThat(artifact.getParts().getFirst().getText()).isEqualTo("contenido");
        }

        @Test
        @DisplayName("GIVEN un error del protocolo WHEN se construye THEN expone código y mensaje")
        void givenProtocolError_whenBuilt_thenExposesCodeAndMessage() {
            Error error = Error.builder()
                    .code("TASK_NOT_FOUND")
                    .message("Task not found: x")
                    .build();

            assertThat(error.getCode()).isEqualTo("TASK_NOT_FOUND");
            assertThat(error.getMessage()).isEqualTo("Task not found: x");
        }
    }
}

