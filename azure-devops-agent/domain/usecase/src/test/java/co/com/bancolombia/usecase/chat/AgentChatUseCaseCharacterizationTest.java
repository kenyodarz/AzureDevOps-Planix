package co.com.bancolombia.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas de <b>caracterización</b> del enrutamiento de flujos de {@link AgentChatUseCase}.
 *
 * <p><b>Propósito:</b> estas pruebas NO validan que el comportamiento sea correcto. Documentan y
 * congelan el comportamiento <i>actual</i> antes de la refactorización planificada en
 * {@code docs/plan/PLAN_MAESTRO.md}, de forma que cualquier desvío no intencionado durante las
 * fases 01 a 07 rompa el build de inmediato.
 *
 * <p><b>Identificación del flujo:</b> cada flujo se reconoce por un fragmento único e inequívoco
 * del prompt que recibe el {@link ChatGateway}, capturado con {@link ArgumentCaptor}.
 *
 * <p><b>Nota sobre {@code @InjectMocks}:</b> no se usa deliberadamente. El constructor de
 * {@link AgentChatUseCase} recibe 9 argumentos, 4 de ellos {@code String} consecutivos, lo que hace
 * que la inyección automática de Mockito sea ambigua y frágil. Se instancia manualmente en
 * {@link #setUp()}. Esta excepción a {@code rules/spring-rules.md} §5 desaparece en la Fase 05,
 * cuando los {@code String} se agrupen en Value Objects.
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentChatUseCase - Caracterización del enrutamiento de flujos")
class AgentChatUseCaseCharacterizationTest {

    // ─── Fragmentos identificadores de cada flujo ──────────────────────────────
    private static final String FRAGMENT_AUDIT = "Eres un Agile Coach y Quality Analyst de Bancolombia";
    private static final String FRAGMENT_REFINEMENT = "Tu objetivo es analizar y proponer mejoras de refinamiento";
    private static final String FRAGMENT_PLANNING = "Analiza la siguiente idea de desarrollo";
    private static final String FRAGMENT_APPROVAL = "Genera el borrador estructurado en Markdown";
    private static final String FRAGMENT_DIVISION = "Divide esta idea de desarrollo en múltiples Historias";
    private static final String FRAGMENT_STRUCTURED_CREATION = "PASO 1 - Crear Historia de Usuario";
    private static final String FRAGMENT_NO_RAG_CONTEXT = "No hay contexto de planeación adicional.";

    // ─── Datos de prueba ───────────────────────────────────────────────────────
    private static final String TEMPLATE_MARKDOWN = "## Plantilla HU/HA corporativa";
    private static final String AGILE_GUIDE = "## Guía de agilidad";
    private static final String QUALITY_AUDIT_GUIDE = "## Estándares de auditoría";
    private static final String DEFAULT_ORG = "grupobancolombia";
    private static final String DEFAULT_PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String LLM_RESPONSE = "Respuesta simulada del modelo";
    private static final String NO_CONTENT = "No content provided";
    private static final String WORK_ITEM_ID = "12345";
    private static final int EXPECTED_RAG_RESULTS = 3;

    private static final String LONG_IDEA_TEXT =
            "Necesito construir un microservicio de notificaciones push para la app móvil";

    @Mock
    private ChatGateway chatGateway;

    @Mock
    private AgentResponseGateway agentResponseGateway;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    @Mock
    private PlanningVectorStorePort vectorStorePort;

    private AgentChatUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AgentChatUseCase(
                chatGateway,
                agentResponseGateway,
                taskStoreGateway,
                TEMPLATE_MARKDOWN,
                vectorStorePort,
                AGILE_GUIDE,
                DEFAULT_ORG,
                DEFAULT_PROJECT,
                QUALITY_AUDIT_GUIDE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo General
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("contextId 'general-*' enruta al flujo General")
    void givenContextIdGeneral_whenChatAndRespond_thenExecutesGeneralFlow() {
        // GIVEN
        String userText = "Cualquier texto suficientemente largo para no ser un comando";
        givenChatGatewayReturnsResponse();

        // WHEN
        SendMessageResponse response = executeAndGet(userText, "general-123");

        // THEN: en el flujo General el prompt es exactamente el texto del usuario
        assertThat(capturePrompt()).isEqualTo(userText);
        assertThat(response.getTask().getStatus().getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    @DisplayName("contextId 'dashboard-*' enruta al flujo General")
    void givenContextIdDashboard_whenChatAndRespond_thenExecutesGeneralFlow() {
        // GIVEN
        String userText = "Cualquier texto suficientemente largo para no ser un comando";
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet(userText, "dashboard-1");

        // THEN
        assertThat(capturePrompt()).isEqualTo(userText);
    }

    @Test
    @DisplayName("La palabra clave 'lista' enruta al flujo General")
    void givenTextWithKeywordLista_whenChatAndRespond_thenExecutesGeneralFlow() {
        // GIVEN
        String userText = "Dame la lista de historias del sprint actual";
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet(userText, null);

        // THEN
        assertThat(capturePrompt()).isEqualTo(userText);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Auditoría de Calidad
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Verbo de auditoría + (ID: n) enruta al flujo de Auditoría de Calidad")
    void givenAuditRequestWithWorkItemId_whenChatAndRespond_thenExecutesQualityAuditFlow() {
        // GIVEN
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet("Audita la historia (ID: " + WORK_ITEM_ID + ")", null);

        // THEN
        String prompt = capturePrompt();
        assertThat(prompt).contains(FRAGMENT_AUDIT)
                .contains(WORK_ITEM_ID)
                .contains(DEFAULT_ORG)
                .contains(QUALITY_AUDIT_GUIDE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Refinamiento
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("'Analicemos y refinemos' + (ID: n) enruta al flujo de Refinamiento")
    void givenRefinementRequestWithWorkItemId_whenChatAndRespond_thenExecutesRefinementFlow() {
        // GIVEN
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet("Analicemos y refinemos la historia (ID: 999)", null);

        // THEN
        String prompt = capturePrompt();
        assertThat(prompt).contains(FRAGMENT_REFINEMENT)
                .contains("999")
                .contains(DEFAULT_PROJECT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Planificación (RAG)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Texto libre largo enruta al flujo de Planificación con contexto RAG")
    void givenLongIdeaText_whenChatAndRespond_thenExecutesPlanningSimilarityFlow() {
        // GIVEN
        PlanningChunk chunk = PlanningChunk.builder()
                .id("chunk-1")
                .initiativeId("init-1")
                .sectionName("Objetivo")
                .content("Contenido de planeación relevante")
                .metadata(Map.of())
                .build();
        when(vectorStorePort.searchSimilarity(anyString(), any(), anyInt()))
                .thenReturn(Flux.just(chunk));
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet(LONG_IDEA_TEXT, null);

        // THEN
        String prompt = capturePrompt();
        assertThat(prompt).contains(FRAGMENT_PLANNING)
                .contains(LONG_IDEA_TEXT)
                .contains("Contenido de planeación relevante");
        verify(vectorStorePort).searchSimilarity(LONG_IDEA_TEXT, null, EXPECTED_RAG_RESULTS);
    }

    @Test
    @DisplayName("Un fallo del vector store no interrumpe el flujo de Planificación")
    void givenVectorStoreError_whenPlanningFlow_thenContinuesWithoutContext() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(anyString(), any(), anyInt()))
                .thenReturn(Flux.error(new IllegalStateException("vector store caído")));
        givenChatGatewayReturnsResponse();

        // WHEN
        SendMessageResponse response = executeAndGet(LONG_IDEA_TEXT, null);

        // THEN
        String prompt = capturePrompt();
        assertThat(prompt).contains(FRAGMENT_PLANNING).contains(FRAGMENT_NO_RAG_CONTEXT);
        assertThat(response.getTask().getStatus().getState()).isEqualTo(TaskState.COMPLETED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Comandos especiales: Aprobación y División
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("El comando 'Aprobado' enruta al flujo de Aprobación (Fase 2)")
    void givenApprovalCommand_whenChatAndRespond_thenExecutesApprovalFlow() {
        // GIVEN
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet("Aprobado", null);

        // THEN
        String prompt = capturePrompt();
        assertThat(prompt).contains(FRAGMENT_APPROVAL)
                .contains(TEMPLATE_MARKDOWN)
                .contains(AGILE_GUIDE);
    }

    @Test
    @DisplayName("El comando 'Sí' (con tilde) se normaliza y enruta al flujo de Aprobación")
    void givenApprovalCommandWithAccent_whenChatAndRespond_thenExecutesApprovalFlow() {
        // GIVEN
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet("Sí", null);

        // THEN
        assertThat(capturePrompt()).contains(FRAGMENT_APPROVAL);
    }

    @Test
    @DisplayName("El comando 'Dividir' enruta al flujo de División")
    void givenDivisionCommand_whenChatAndRespond_thenExecutesDivisionFlow() {
        // GIVEN
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet("Dividir", null);

        // THEN
        assertThat(capturePrompt()).contains(FRAGMENT_DIVISION).contains(AGILE_GUIDE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Casos borde
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Texto vacío no invoca al LLM y retorna 'No content provided'")
    void givenEmptyText_whenChatAndRespond_thenReturnsNoContentWithoutCallingGateway() {
        // WHEN
        SendMessageResponse response = executeAndGet("", null);

        // THEN
        assertThat(response.getMessage().extractText()).isEqualTo(NO_CONTENT);
        verify(chatGateway, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("Request sin mensaje no invoca al LLM y retorna 'No content provided'")
    void givenNullMessage_whenChatAndRespond_thenReturnsNoContent() {
        // GIVEN
        SendMessageRequest request = SendMessageRequest.builder().build();

        // WHEN / THEN
        StepVerifier.create(useCase.chatAndRespond(request))
                .assertNext(response ->
                        assertThat(response.getMessage().extractText()).isEqualTo(NO_CONTENT))
                .verifyComplete();
        verify(chatGateway, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("Un error del LLM produce una Task en estado FAILED")
    void givenGatewayError_whenChatAndRespond_thenReturnsFailedTask() {
        // GIVEN
        when(chatGateway.sendMessage(anyString(), any()))
                .thenReturn(Mono.error(new IllegalStateException("modelo no disponible")));

        // WHEN
        SendMessageResponse response = executeAndGet("Texto para el flujo general", "general-1");

        // THEN
        assertThat(response.getTask().getStatus().getState()).isEqualTo(TaskState.FAILED);
        assertThat(response.getMessage().extractText()).contains("No pude completar la operación");
        assertThat(response.getTask().getMetadata()).containsEntry("errorCode", "INTERNAL_ERROR");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Defectos conocidos — el comportamiento aquí capturado se CORREGIRÁ
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Documenta el defecto <b>DP-01</b>: la evaluación de {@code isGeneralFlow} ocurre antes que la
     * detección de auditoría, por lo que la palabra genérica «reporte» secuestra una petición que
     * incluye explícitamente un identificador de Work Item.
     *
     * <p><b>Comportamiento esperado hoy:</b> flujo General.
     * <p><b>Comportamiento tras la Fase 03:</b> flujo de Auditoría de Calidad.
     * Esta aserción <b>debe invertirse</b> en esa fase.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
     */
    @Test
    @DisplayName("DEFECTO DP-01: 'reporte' secuestra una petición de auditoría con (ID: n)")
    void givenAuditRequestContainingKeywordReporte_whenChatAndRespond_thenGeneralFlowWinsByPrecedence() {
        // GIVEN
        String userText = "Genera un reporte de calidad de la historia (ID: " + WORK_ITEM_ID + ")";
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet(userText, null);

        // THEN: hoy gana el flujo General y el prompt de auditoría nunca se construye
        String prompt = capturePrompt();
        assertThat(prompt).isEqualTo(userText);
        assertThat(prompt).doesNotContain(FRAGMENT_AUDIT);
    }

    /**
     * Documenta el defecto <b>DP-01</b>: la palabra genérica «consulta» dentro de una idea de
     * desarrollo larga secuestra el flujo de Planificación con RAG.
     *
     * <p><b>Comportamiento esperado hoy:</b> flujo General, sin búsqueda vectorial.
     * <p><b>Comportamiento tras la Fase 03:</b> flujo de Planificación.
     * Esta aserción <b>debe invertirse</b> en esa fase.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
     */
    @Test
    @DisplayName("DEFECTO DP-01: 'consulta' secuestra el flujo de Planificación")
    void givenLongIdeaContainingKeywordConsulta_whenChatAndRespond_thenGeneralFlowWinsByPrecedence() {
        // GIVEN
        String userText = "Necesito un servicio que consulta el saldo del cliente desde el core";
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet(userText, null);

        // THEN: hoy gana el flujo General y nunca se consulta el vector store
        assertThat(capturePrompt()).isEqualTo(userText);
        verify(vectorStorePort, never()).searchSimilarity(anyString(), any(), anyInt());
    }

    /**
     * Documenta el defecto <b>DP-07</b>: el marcador {@code CREATE_STRUCTURED_USER_STORY} tiene 28
     * caracteres, por lo que {@code shouldSearchPlanning()} lo considera texto libre y lo enruta al
     * flujo de Planificación. En consecuencia {@code buildPrompt()} y la plantilla de creación
     * estructurada con tareas hijas son <b>código inalcanzable</b>.
     *
     * <p><b>Comportamiento esperado hoy:</b> flujo de Planificación.
     * <p><b>Comportamiento tras la Fase 03/04:</b> depende de la resolución de DP-07.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-07
     */
    @Test
    @DisplayName("DEFECTO DP-07: el marcador de creación estructurada es código inalcanzable")
    void givenStructuredCreationMarker_whenChatAndRespond_thenPlanningFlowWinsAndTemplateIsNeverUsed() {
        // GIVEN
        when(vectorStorePort.searchSimilarity(anyString(), any(), anyInt()))
                .thenReturn(Flux.empty());
        givenChatGatewayReturnsResponse();

        // WHEN
        executeAndGet("CREATE_STRUCTURED_USER_STORY", null);

        // THEN: gana Planificación; el prompt de creación estructurada nunca se construye
        String prompt = capturePrompt();
        assertThat(prompt).contains(FRAGMENT_PLANNING);
        assertThat(prompt).doesNotContain(FRAGMENT_STRUCTURED_CREATION);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private void givenChatGatewayReturnsResponse() {
        when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(LLM_RESPONSE));
    }

    private SendMessageRequest buildRequest(String text, String contextId) {
        Message message = Message.builder()
                .role("user")
                .contextId(contextId)
                .messageId("msg-1")
                .parts(List.of(Part.ofText(text)))
                .build();
        return SendMessageRequest.builder().message(message).build();
    }

    private SendMessageResponse executeAndGet(String text, String contextId) {
        return useCase.chatAndRespond(buildRequest(text, contextId)).block();
    }

    private String capturePrompt() {
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatGateway).sendMessage(promptCaptor.capture(), any());
        return promptCaptor.getValue();
    }
}

