package co.com.bancolombia.agentclient;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.agentclient.config.AgentConnectionProperties;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.exceptions.AgentExecutionException;
import co.com.bancolombia.model.agent.exceptions.AgentUnavailableException;
import co.com.bancolombia.model.agent.exceptions.InvalidAgentRequestException;
import co.com.bancolombia.model.agent.exceptions.TaskNotFoundException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

/**
 * Pruebas del cliente A2A del agente (Fase 02).
 *
 * <p>Invierten las dos pruebas que la Fase 01 dejó documentando deudas:
 * <ul>
 *   <li><b>D-22</b>: ahora se llama a {@code POST /} con JSON-RPC 2.0, no al legacy
 *       {@code /message:send};</li>
 *   <li><b>D-23</b>: ahora la {@code Task} <b>sí</b> llega al dominio.</li>
 * </ul>
 */
class A2AAgentAdapterTest {

    private static final String CONTEXT_ID = "ctx-1";
    private static final String PROMPT = "Analiza el sprint 247";
    private static final String TASK_ID = "task-42";
    private final AtomicReference<String> capturedBody = new AtomicReference<>("");
    private final AtomicReference<String> capturedPath = new AtomicReference<>("");
    private final AtomicReference<String> responseBody = new AtomicReference<>("{}");
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<Duration> responseDelay =
            new AtomicReference<>(Duration.ZERO);
    private DisposableServer server;
    private A2AAgentAdapter adapter;

    @BeforeEach
    void setUp() {
        server = HttpServer.create()
                .port(0)
                .handle((request, response) -> {
                    capturedPath.set(request.uri());
                    return request.receive().aggregate().asString()
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                capturedBody.set(body);
                                return response.status(responseStatus.get())
                                        .addHeader("Content-Type", "application/json")
                                        .sendString(Mono.just(responseBody.get())
                                                .delayElement(responseDelay.get()))
                                        .then();
                            });
                })
                .bindNow();

        adapter = newAdapter(Duration.ofSeconds(10));
    }

    private A2AAgentAdapter newAdapter(Duration timeout) {
        return new A2AAgentAdapter(WebClient.builder(),
                new AgentConnectionProperties("http://localhost:" + server.port(), timeout));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    private void givenAgentAnswers(String json) {
        responseStatus.set(200);
        responseBody.set(json);
    }

    private void givenAgentAnswersError(int code, String message) {
        responseStatus.set(200);
        responseBody.set("""
                {"jsonrpc":"2.0","id":"1","error":{"code":%d,"message":"%s"}}"""
                .formatted(code, message));
    }

    private String successWithTask(String state) {
        return """
                {"jsonrpc":"2.0","id":"1","result":{
                   "task":{"id":"%s","contextId":"%s",
                           "status":{"state":"%s","timestamp":"2026-08-29T00:00:00Z",
                                     "message":{"role":"agent","parts":[{"text":"En marcha"}]}}},
                   "message":{"role":"agent","parts":[{"text":"Recibido"}]}}}"""
                .formatted(TASK_ID, CONTEXT_ID, state);
    }

    // ---------------------------------------------------------------------------------------
    // Contrato de transporte
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN una orden WHEN sendMessage THEN llama a POST / con JSON-RPC 2.0 (D-22 saldada)")
    void givenCommand_whenSendMessage_thenCallsJsonRpcRoot() {
        // GIVEN
        givenAgentAnswers(successWithTask("working"));

        // WHEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectNextCount(1)
                .verifyComplete();

        // THEN
        assertThat(capturedPath.get())
                .as("el endpoint legacy /message:send tiene el sunset vencido en el agente")
                .isEqualTo("/");
        assertThat(capturedBody.get())
                .contains("\"jsonrpc\":\"2.0\"")
                .contains("\"method\":\"message/send\"");
    }

    @Test
    @DisplayName("GIVEN una orden no bloqueante WHEN sendMessage THEN envía blocking=false (DP-08)")
    void givenNonBlockingCommand_whenSendMessage_thenSendsBlockingFalse() {
        // GIVEN
        givenAgentAnswers(successWithTask("submitted"));

        // WHEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectNextCount(1)
                .verifyComplete();

        // THEN
        assertThat(capturedBody.get()).contains("\"blocking\":false");
    }

    @Test
    @DisplayName("GIVEN una orden WHEN sendMessage THEN el mensaje lleva role, contextId y el prompt")
    void givenCommand_whenSendMessage_thenMessageCarriesRoleContextAndPrompt() {
        // GIVEN
        givenAgentAnswers(successWithTask("working"));

        // WHEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectNextCount(1)
                .verifyComplete();

        // THEN
        assertThat(capturedBody.get())
                .contains("\"role\":\"user\"")
                .contains("\"contextId\":\"" + CONTEXT_ID + "\"")
                .contains(PROMPT);
    }

    // ---------------------------------------------------------------------------------------
    // La Task deja de perderse — D-23 saldada
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN el agente devuelve una Task WHEN sendMessage THEN la Task LLEGA al dominio (D-23 saldada)")
    void givenAgentReturnsTask_whenSendMessage_thenTaskReachesTheDomain() {
        // GIVEN
        givenAgentAnswers(successWithTask("working"));

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction -> {
                    assertThat(interaction.hasTask())
                            .as("es lo que el frontend necesita para mostrar el progreso")
                            .isTrue();
                    assertThat(interaction.task().getId()).isEqualTo(TASK_ID);
                    assertThat(interaction.task().getContextId()).isEqualTo(CONTEXT_ID);
                    assertThat(interaction.task().getStatus().getState())
                            .isEqualTo(TaskState.WORKING);
                    assertThat(interaction.reply()).isEqualTo("Recibido");
                })
                .verifyComplete();
    }

    @ParameterizedTest
    @CsvSource({
            "submitted, SUBMITTED",
            "working, WORKING",
            "input-required, INPUT_REQUIRED",
            "completed, COMPLETED",
            "canceled, CANCELED",
            "failed, FAILED"
    })
    @DisplayName("GIVEN cada estado A2A WHEN sendMessage THEN se traduce al enumerado del dominio")
    void givenEachA2AState_whenSendMessage_thenItIsTranslated(String wireValue,
            TaskState expected) {
        // GIVEN
        givenAgentAnswers(successWithTask(wireValue));

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction ->
                        assertThat(interaction.task().getStatus().getState()).isEqualTo(expected))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un estado desconocido WHEN sendMessage THEN no revienta y deja el estado nulo")
    void givenUnknownState_whenSendMessage_thenItDoesNotBlowUp() {
        // GIVEN
        givenAgentAnswers(successWithTask("un-estado-que-no-existe"));

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction ->
                        assertThat(interaction.task().getStatus().getState()).isNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN respuesta sin message WHEN sendMessage THEN toma el texto del estado de la tarea")
    void givenNoTopLevelMessage_whenSendMessage_thenFallsBackToTaskStatusMessage() {
        // GIVEN
        givenAgentAnswers("""
                {"jsonrpc":"2.0","id":"1","result":{
                   "task":{"id":"%s","status":{"state":"working",
                            "message":{"role":"agent","parts":[{"text":"Trabajando"}]}}}}}"""
                .formatted(TASK_ID));

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction -> assertThat(interaction.reply()).isEqualTo("Trabajando"))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN respuesta conversacional sin tarea WHEN sendMessage THEN no hay tarea que seguir")
    void givenReplyWithoutTask_whenSendMessage_thenThereIsNoTask() {
        // GIVEN
        givenAgentAnswers("""
                {"jsonrpc":"2.0","id":"1","result":{
                   "message":{"role":"agent","parts":[{"text":"Hola"}]}}}""");

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction -> {
                    assertThat(interaction.hasTask()).isFalse();
                    assertThat(interaction.reply()).isEqualTo("Hola");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN la respuesta trae un bloque <think> WHEN sendMessage THEN se elimina")
    void givenThinkBlock_whenSendMessage_thenItIsStripped() {
        // GIVEN
        givenAgentAnswers("""
                {"jsonrpc":"2.0","id":"1","result":{
                   "message":{"role":"agent",
                              "parts":[{"text":"<think>razonando</think>Respuesta final"}]}}}""");

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .assertNext(interaction ->
                        assertThat(interaction.reply()).isEqualTo("Respuesta final"))
                .verifyComplete();
    }

    // ---------------------------------------------------------------------------------------
    // tasks/get y tasks/cancel
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN una tarea existente WHEN getTask THEN invoca tasks/get y la devuelve")
    void givenExistingTask_whenGetTask_thenItInvokesTasksGet() {
        // GIVEN
        givenAgentAnswers(successWithTask("working"));

        // WHEN / THEN
        StepVerifier.create(adapter.getTask(TASK_ID))
                .assertNext(task -> assertThat(task.getId()).isEqualTo(TASK_ID))
                .verifyComplete();

        assertThat(capturedBody.get())
                .contains("\"method\":\"tasks/get\"")
                .contains("\"taskId\":\"" + TASK_ID + "\"");
    }

    @Test
    @DisplayName("GIVEN una tarea en curso WHEN cancelTask THEN invoca tasks/cancel")
    void givenRunningTask_whenCancelTask_thenItInvokesTasksCancel() {
        // GIVEN
        givenAgentAnswers(successWithTask("canceled"));

        // WHEN / THEN
        StepVerifier.create(adapter.cancelTask(TASK_ID))
                .assertNext(task ->
                        assertThat(task.getStatus().getState()).isEqualTo(TaskState.CANCELED))
                .verifyComplete();

        assertThat(capturedBody.get()).contains("\"method\":\"tasks/cancel\"");
    }

    @Test
    @DisplayName("GIVEN el result no trae tarea WHEN getTask THEN falla con TaskNotFoundException")
    void givenResultWithoutTask_whenGetTask_thenTaskNotFound() {
        // GIVEN
        givenAgentAnswers("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}");

        // WHEN / THEN
        StepVerifier.create(adapter.getTask(TASK_ID))
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    // ---------------------------------------------------------------------------------------
    // Rutas REST del agente: listado de tareas y agent card (Fase 03)
    // ---------------------------------------------------------------------------------------

    /**
     * En el agente, {@code GET /api/tasks} <b>no</b> es JSON-RPC: es REST plano. Esta prueba fija
     * que el adaptador usa {@code GET} sobre la ruta correcta y no el {@code POST /} del
     * dispatcher.
     */
    @Test
    @DisplayName("GIVEN el agente lista tareas WHEN listTasks THEN hace GET /api/tasks, no POST /")
    void givenAgentTasks_whenListTasks_thenItCallsRestEndpoint() {
        // GIVEN
        givenAgentAnswers("""
                [{"id":"%s","contextId":"%s","status":{"state":"working"}},
                 {"id":"task-43","contextId":"ctx-2","status":{"state":"completed"}}]"""
                .formatted(TASK_ID, CONTEXT_ID));

        // WHEN / THEN
        StepVerifier.create(adapter.listTasks())
                .assertNext(task -> {
                    assertThat(task.getId()).isEqualTo(TASK_ID);
                    assertThat(task.getStatus().getState()).isEqualTo(TaskState.WORKING);
                })
                .assertNext(task ->
                        assertThat(task.getStatus().getState()).isEqualTo(TaskState.COMPLETED))
                .verifyComplete();

        assertThat(capturedPath.get()).isEqualTo("/api/tasks");
        assertThat(capturedBody.get())
                .as("una ruta REST no lleva sobre JSON-RPC")
                .doesNotContain("jsonrpc");
    }

    @Test
    @DisplayName("GIVEN el listado viene envuelto en un objeto WHEN listTasks THEN también se lee")
    void givenWrappedTaskList_whenListTasks_thenItIsStillRead() {
        // GIVEN
        givenAgentAnswers(
                "{\"tasks\":[{\"id\":\"%s\",\"status\":{\"state\":\"submitted\"}}]}"
                        .formatted(TASK_ID));

        // WHEN / THEN
        StepVerifier.create(adapter.listTasks())
                .assertNext(task -> assertThat(task.getId()).isEqualTo(TASK_ID))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN un cuerpo irreconocible WHEN listTasks THEN devuelve vacío sin reventar")
    void givenUnknownBody_whenListTasks_thenItIsEmpty() {
        // GIVEN
        givenAgentAnswers("{\"algo\":\"inesperado\"}");

        // WHEN / THEN
        StepVerifier.create(adapter.listTasks()).verifyComplete();
    }

    @Test
    @DisplayName("GIVEN el agente no responde WHEN listTasks THEN AgentUnavailableException")
    void givenAgentDown_whenListTasks_thenAgentUnavailable() {
        // GIVEN
        responseStatus.set(500);
        responseBody.set("{\"error\":\"boom\"}");

        // WHEN / THEN
        StepVerifier.create(adapter.listTasks())
                .expectError(AgentUnavailableException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN la agent card del agente WHEN getAgentCard THEN la sirve el BFF (D-26)")
    void givenAgentCard_whenGetAgentCard_thenItIsMapped() {
        // GIVEN
        givenAgentAnswers("""
                {"protocolVersion":"1.0","name":"Agente DevOps","description":"Audita backlogs",
                 "version":"2.3.1",
                 "provider":{"organization":"Bancolombia","url":"https://bancolombia.com"},
                 "capabilities":{"streaming":true,"pushNotifications":false},
                 "defaultInputModes":["text"],"defaultOutputModes":["text","data"],
                 "supportedInterfaces":[{"protocol":"jsonrpc","url":"http://localhost:8082/"}],
                 "skills":[{"id":"audit","name":"Auditar","tags":["backlog"]}],
                 "tags":["devops"]}""");

        // WHEN / THEN
        StepVerifier.create(adapter.getAgentCard())
                .assertNext(card -> {
                    assertThat(card.getName()).isEqualTo("Agente DevOps");
                    assertThat(card.getVersion()).isEqualTo("2.3.1");
                    assertThat(card.getProvider().getOrganization()).isEqualTo("Bancolombia");
                    assertThat(card.getCapabilities().isStreaming()).isTrue();
                    assertThat(card.getCapabilities().isPushNotifications()).isFalse();
                    assertThat(card.getDefaultOutputModes()).containsExactly("text", "data");
                    assertThat(card.getSupportedInterfaces()).singleElement()
                            .satisfies(iface ->
                                    assertThat(iface.getProtocol()).isEqualTo("jsonrpc"));
                    assertThat(card.getSkills()).singleElement()
                            .satisfies(skill -> assertThat(skill.getId()).isEqualTo("audit"));
                    assertThat(card.getTags()).containsExactly("devops");
                })
                .verifyComplete();

        assertThat(capturedPath.get()).isEqualTo("/.well-known/agent-card.json");
    }

    @Test
    @DisplayName("GIVEN el agente no responde WHEN getAgentCard THEN AgentUnavailableException")
    void givenAgentDown_whenGetAgentCard_thenAgentUnavailable() {
        // GIVEN
        responseStatus.set(503);
        responseBody.set("{}");

        // WHEN / THEN
        StepVerifier.create(adapter.getAgentCard())
                .expectError(AgentUnavailableException.class)
                .verify();
    }

    // ---------------------------------------------------------------------------------------
    // Traducción de errores
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("GIVEN el agente responde -32004 WHEN getTask THEN TaskNotFoundException")
    void givenTaskNotFoundCode_whenGetTask_thenDomainException() {
        // GIVEN
        givenAgentAnswersError(-32004, "Task not found: task-42");

        // WHEN / THEN
        StepVerifier.create(adapter.getTask(TASK_ID))
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    @ParameterizedTest
    @CsvSource({"-32600", "-32601", "-32602"})
    @DisplayName("GIVEN el agente rechaza la petición WHEN sendMessage THEN InvalidAgentRequestException")
    void givenRejectedRequest_whenSendMessage_thenInvalidAgentRequest(int code) {
        // GIVEN
        givenAgentAnswersError(code, "payload mal formado");

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectError(InvalidAgentRequestException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN el agente responde -32603 WHEN sendMessage THEN AgentExecutionException")
    void givenInternalErrorCode_whenSendMessage_thenAgentExecutionException() {
        // GIVEN
        givenAgentAnswersError(-32603, "el LLM falló");

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectError(AgentExecutionException.class)
                .verify();
    }

    @Test
    @DisplayName("GIVEN el agente responde 500 WHEN sendMessage THEN AgentUnavailableException")
    void givenHttp500_whenSendMessage_thenAgentUnavailable() {
        // GIVEN
        responseStatus.set(500);
        responseBody.set("{\"error\":\"boom\"}");

        // WHEN / THEN
        StepVerifier.create(adapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectError(AgentUnavailableException.class)
                .verify();
    }

    /**
     * Invierte la prueba que la Fase 01 dejó afirmando que <i>no</i> había timeout. Sin él, un
     * agente colgado bloqueaba indefinidamente el stream SSE del dashboard.
     */
    @Test
    @DisplayName("GIVEN el agente no responde a tiempo WHEN sendMessage THEN AgentUnavailableException")
    void givenSlowAgent_whenSendMessage_thenTimeoutBecomesUnavailable() {
        // GIVEN
        givenAgentAnswers(successWithTask("working"));
        responseDelay.set(Duration.ofSeconds(3));
        A2AAgentAdapter impatientAdapter = newAdapter(Duration.ofMillis(200));

        // WHEN / THEN
        StepVerifier.create(
                        impatientAdapter.sendMessage(AgentCommand.nonBlocking(PROMPT, CONTEXT_ID)))
                .expectError(AgentUnavailableException.class)
                .verify(Duration.ofSeconds(10));
    }
}

