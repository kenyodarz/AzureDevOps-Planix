package co.com.bancolombia.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.agent.AzureDevOpsScope;
import co.com.bancolombia.model.agent.CorporateKnowledge;
import co.com.bancolombia.model.agent.IntentResolver;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import co.com.bancolombia.usecase.chat.handler.ApprovalFlowHandler;
import co.com.bancolombia.usecase.chat.handler.ChatFlowDispatcher;
import co.com.bancolombia.usecase.chat.handler.DivisionFlowHandler;
import co.com.bancolombia.usecase.chat.handler.GeneralFlowHandler;
import co.com.bancolombia.usecase.chat.handler.PlanningDraftFlowHandler;
import co.com.bancolombia.usecase.chat.handler.ProgramPlanningFlowHandler;
import co.com.bancolombia.usecase.chat.handler.QualityAuditFlowHandler;
import co.com.bancolombia.usecase.chat.handler.RefinementFlowHandler;
import co.com.bancolombia.usecase.planning.ProgramPlanningUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas de <b>caracterización</b> del enrutamiento de flujos de {@link AgentChatUseCase}.
 *
 * <p><b>Propósito:</b> estas pruebas NO validan que el comportamiento sea correcto. Documentan y
 * congelan el comportamiento <i>actual</i> antes de la refactorización planificada en
 * {@code docs/plan/PLAN_MAESTRO.md}, de forma que cualquier desvío no intencionado durante las
 * fases 02 a 07 rompa el build de inmediato.
 *
 * <p><b>Identificación del flujo:</b> desde la Fase 01 cada flujo se reconoce por el
 * {@link PromptTemplateId} que solicita al {@link PromptTemplatePort}. Antes se identificaba por un
 * fragmento del texto del prompt; el criterio actual es equivalente pero más preciso, porque
 * compara un valor enumerado en lugar de una subcadena. El flujo General es el único que no usa
 * plantilla: envía el texto del usuario tal cual.
 *
 * <p><b>Nota sobre {@code @InjectMocks}:</b> sigue sin usarse, pero por una razón distinta a la
 * original. Hasta la Fase 04 el problema eran los 4 {@code String} consecutivos del constructor,
 * que hacían ambigua la inyección automática. Desde la Fase 05 el constructor tiene 4 argumentos
 * tipados y sin ambigüedad, pero uno de ellos —{@link ChatFlowDispatcher}— <b>no es un mock</b>:
 * es un objeto real que debe ensamblarse con los seis handlers apuntando a los gateways simulados,
 * porque es justamente el enrutamiento lo que estas pruebas caracterizan. Mockearlo vaciaría la
 * suite de sentido. La construcción manual es aquí una decisión, no una limitación.
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentChatUseCase - Caracterización del enrutamiento de flujos")
class AgentChatUseCaseCharacterizationTest {

    private static final String TEMPLATE_MARKDOWN = "## Plantilla HU/HA corporativa";
    private static final String AGILE_GUIDE = "## Guía de agilidad";
    private static final String QUALITY_AUDIT_GUIDE = "## Estándares de auditoría";
    private static final String DEFAULT_ORG = "grupobancolombia";
    private static final String DEFAULT_PROJECT = "Vicepresidencia Servicios de Tecnología";
    private static final String RENDERED_PROMPT = "Prompt renderizado por la plantilla";
    private static final String LLM_RESPONSE = "Respuesta simulada del modelo";
    private static final String NO_CONTENT = "No content provided";
    private static final String WORK_ITEM_ID = "12345";

    private static final String VAR_WORK_ITEM_ID = "workItemId";
    private static final String VAR_ORGANIZATION = "organizacion";
    private static final String VAR_PROJECT = "proyecto";
    private static final String VAR_AGILE_GUIDE = "guiaAgilidad";
    private static final String VAR_STANDARDS = "estandares";
    private static final String VAR_ORIGINAL_IDEA = "ideaOriginal";
    private static final String VAR_RAG_CONTEXT = "contextoRag";
    private static final String VAR_CORPORATE_TEMPLATE = "plantillaCorporativa";

    private static final String LONG_IDEA_TEXT =
            "Necesito construir un microservicio de notificaciones push para la app móvil";

    @Mock
    private ChatGateway chatGateway;

    @Mock
    private AgentResponseGateway agentResponseGateway;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    @Mock
    private SpecStoragePort specStoragePort;

    @Mock
    private PromptTemplatePort promptTemplatePort;

    private AgentChatUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AgentChatUseCase(
                agentResponseGateway,
                taskStoreGateway,
                new IntentResolver(),
                buildDispatcher());
    }

    /**
     * Construye el dispatcher con los siete handlers reales apuntando a los mocks. Desde la Fase 04
     * el caso de uso ya no conoce los puertos de cada flujo, solo el despachador.
     */
    private ChatFlowDispatcher buildDispatcher() {
        AzureDevOpsScope scope = new AzureDevOpsScope(DEFAULT_ORG, DEFAULT_PROJECT);
        CorporateKnowledge knowledge =
                new CorporateKnowledge(TEMPLATE_MARKDOWN, AGILE_GUIDE, QUALITY_AUDIT_GUIDE);
        return new ChatFlowDispatcher(List.of(
                new GeneralFlowHandler(chatGateway),
                new QualityAuditFlowHandler(chatGateway, promptTemplatePort, scope, knowledge),
                new RefinementFlowHandler(chatGateway, promptTemplatePort, scope, knowledge),
                new ApprovalFlowHandler(chatGateway, promptTemplatePort, knowledge),
                new DivisionFlowHandler(chatGateway, promptTemplatePort, knowledge),
                new PlanningDraftFlowHandler(chatGateway, promptTemplatePort, specStoragePort),
                new ProgramPlanningFlowHandler(
                        new ProgramPlanningUseCase(promptTemplatePort, specStoragePort,
                                chatGateway))));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo General — único flujo sin plantilla
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("contextId 'general-*' enruta al flujo General")
    void givenContextIdGeneral_whenChatAndRespond_thenExecutesGeneralFlow() {
        // GIVEN
        String userText = "Cualquier texto suficientemente largo para no ser un comando";
        givenChatGatewayReturnsResponse();

        // WHEN
        SendMessageResponse response = executeAndGet(userText, "general-123");

        // THEN: el flujo General envía el texto del usuario sin plantilla
        assertThat(capturePrompt()).isEqualTo(userText);
        verifyNoTemplateWasRendered();
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
        verifyNoTemplateWasRendered();
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
        verifyNoTemplateWasRendered();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Auditoría de Calidad
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Verbo de auditoría + (ID: n) enruta al flujo de Auditoría de Calidad")
    void givenAuditRequestWithWorkItemId_whenChatAndRespond_thenExecutesQualityAuditFlow() {
        // GIVEN
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet("Audita la historia (ID: " + WORK_ITEM_ID + ")", null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.QUALITY_AUDIT);
        Map<String, Object> variables = captureTemplateVariables();
        assertThat(variables).containsEntry(VAR_WORK_ITEM_ID, WORK_ITEM_ID)
                .containsEntry(VAR_ORGANIZATION, DEFAULT_ORG)
                .containsEntry(VAR_PROJECT, DEFAULT_PROJECT);
        assertThat(variables.get(VAR_STANDARDS).toString())
                .contains(AGILE_GUIDE)
                .contains(QUALITY_AUDIT_GUIDE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Refinamiento
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("'Analicemos y refinemos' + (ID: n) enruta al flujo de Refinamiento")
    void givenRefinementRequestWithWorkItemId_whenChatAndRespond_thenExecutesRefinementFlow() {
        // GIVEN
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet("Analicemos y refinemos la historia (ID: 999)", null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STORY_REFINEMENT);
        assertThat(captureTemplateVariables()).containsEntry(VAR_WORK_ITEM_ID, "999")
                .containsEntry(VAR_PROJECT, DEFAULT_PROJECT)
                .containsEntry(VAR_AGILE_GUIDE, AGILE_GUIDE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Planificación (Spec Storage)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Texto libre largo enruta al flujo de Planificación con contexto de spec")
    void givenLongIdeaText_whenChatAndRespond_thenExecutesPlanningFlowWithSpec() {
        // GIVEN
        SpecDocument spec = new SpecDocument("ideas_planning_q3.md",
                "Contenido de planeación relevante",
                "/specs/ideas_planning_q3.md");
        when(specStoragePort.getSpec("ideas_planning_q3.md")).thenReturn(Mono.just(spec));
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet(LONG_IDEA_TEXT, null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PLANNING_DRAFT);
        Map<String, Object> variables = captureTemplateVariables();
        assertThat(variables).containsEntry(VAR_ORIGINAL_IDEA, LONG_IDEA_TEXT);
        assertThat(variables.get(VAR_RAG_CONTEXT).toString())
                .contains("Contenido de planeación relevante");
        verify(specStoragePort).getSpec("ideas_planning_q3.md");
    }

    @Test
    @DisplayName("Un fallo del spec storage no interrumpe el flujo de Planificación")
    void givenSpecStorageError_whenPlanningFlow_thenContinuesWithoutContext() {
        // GIVEN
        when(specStoragePort.getSpec(anyString()))
                .thenReturn(Mono.error(new SpecNotFoundException("spec no encontrado")));
        givenTemplateAndGatewayRespond();

        // WHEN
        SendMessageResponse response = executeAndGet(LONG_IDEA_TEXT, null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PLANNING_DRAFT);
        assertThat(captureTemplateVariables())
                .containsEntry(VAR_RAG_CONTEXT, "No hay contexto de planeación adicional.");
        assertThat(response.getTask().getStatus().getState()).isEqualTo(TaskState.COMPLETED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Comandos especiales: Aprobación y División
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("El comando 'Aprobado' enruta al flujo de Aprobación (Fase 2)")
    void givenApprovalCommand_whenChatAndRespond_thenExecutesApprovalFlow() {
        // GIVEN
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet("Aprobado", null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STRUCTURED_STORY);
        assertThat(captureTemplateVariables())
                .containsEntry(VAR_CORPORATE_TEMPLATE, TEMPLATE_MARKDOWN)
                .containsEntry(VAR_AGILE_GUIDE, AGILE_GUIDE);
    }

    @Test
    @DisplayName("El comando 'Sí' (con tilde) se normaliza y enruta al flujo de Aprobación")
    void givenApprovalCommandWithAccent_whenChatAndRespond_thenExecutesApprovalFlow() {
        // GIVEN
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet("Sí", null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STRUCTURED_STORY);
    }

    @Test
    @DisplayName("El comando 'Dividir' enruta al flujo de División")
    void givenDivisionCommand_whenChatAndRespond_thenExecutesDivisionFlow() {
        // GIVEN
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet("Dividir", null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STORY_DIVISION);
        assertThat(captureTemplateVariables()).containsEntry(VAR_AGILE_GUIDE, AGILE_GUIDE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Casos borde
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sustituye a la antigua rama por defecto de {@code handleSpecialCommands()}, eliminada en la
     * Fase 03: un texto corto que no es comando ni contiene palabra clave se atiende como consulta
     * general, enviando el texto del usuario sin plantilla.
     */
    @Test
    @DisplayName("Un texto corto que no es comando se envía al modelo sin plantilla")
    void givenShortNonCommandText_whenChatAndRespond_thenExecutesGeneralFlow() {
        // GIVEN
        String userText = "Hola";
        givenChatGatewayReturnsResponse();

        // WHEN
        SendMessageResponse response = executeAndGet(userText, null);

        // THEN
        assertThat(capturePrompt()).isEqualTo(userText);
        verifyNoTemplateWasRendered();
        assertThat(response.getTask().getStatus().getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    @DisplayName("Texto vacío no invoca al LLM y retorna 'No content provided'")
    void givenEmptyText_whenChatAndRespond_thenReturnsNoContentWithoutCallingGateway() {
        // WHEN
        SendMessageResponse response = executeAndGet("", null);

        // THEN
        assertThat(response.getMessage().extractText()).isEqualTo(NO_CONTENT);
        verify(chatGateway, never()).sendMessage(anyString(), any());
        verifyNoTemplateWasRendered();
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
    // Defectos corregidos — DP-01 aplicada en la Fase 03
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>DP-01 aplicada.</b> Hasta la Fase 02 la evaluación de {@code isGeneralFlow} ocurría antes
     * que la detección de auditoría, por lo que la palabra genérica «reporte» secuestraba una
     * petición que incluye explícitamente un identificador de Work Item.
     *
     * <p>Desde la Fase 03 la decisión vive en
     * {@link co.com.bancolombia.model.agent.IntentResolver}, que aplica la tabla de precedencia: un
     * {@code (ID: n)} junto a un verbo de auditoría es una señal fuerte y gana sobre cualquier
     * palabra clave genérica.
     *
     * <p><b>Aserción invertida respecto a la Fase 00:</b> antes se exigía flujo General; ahora se
     * exige flujo de Auditoría de Calidad.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
     */
    @Test
    @DisplayName("DP-01: 'reporte' ya no secuestra una petición de auditoría con (ID: n)")
    void givenAuditRequestContainingKeywordReporte_whenChatAndRespond_thenExecutesQualityAuditFlow() {
        // GIVEN
        String userText = "Genera un reporte de calidad de la historia (ID: " + WORK_ITEM_ID + ")";
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet(userText, null);

        // THEN: gana la auditoría y el identificador viaja a la plantilla
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.QUALITY_AUDIT);
        assertThat(captureTemplateVariables()).containsEntry(VAR_WORK_ITEM_ID, WORK_ITEM_ID);
    }

    /**
     * <b>DP-01 aplicada.</b> Hasta la Fase 02 la palabra genérica «consulta» dentro de una idea de
     * desarrollo secuestraba el flujo de Planificación con RAG, porque se buscaba como subcadena
     * sobre la frase completa.
     *
     * <p>Desde la Fase 03 las palabras clave se evalúan como palabra completa y se descartan las
     * que van gobernadas por un pronombre relativo: en «un servicio <i>que consulta</i> el saldo»,
     * «consulta» describe el sistema a construir, no una acción pedida al agente.
     *
     * <p><b>Aserción invertida respecto a la Fase 00:</b> antes se exigía flujo General sin
     * búsqueda vectorial; ahora se exige flujo de Planificación consultando el vector store.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-01
     */
    @Test
    @DisplayName("DP-01: 'consulta' ya no secuestra el flujo de Planificación")
    void givenLongIdeaContainingKeywordConsulta_whenChatAndRespond_thenExecutesPlanningSimilarityFlow() {
        // GIVEN
        String userText = "Necesito un servicio que consulta el saldo del cliente desde el core";
        when(specStoragePort.getSpec("ideas_planning_q3.md"))
                .thenReturn(Mono.just(new SpecDocument("ideas_planning_q3.md", "",
                        "/specs/ideas_planning_q3.md")));
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet(userText, null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PLANNING_DRAFT);
        assertThat(captureTemplateVariables()).containsEntry(VAR_ORIGINAL_IDEA, userText);
        verify(specStoragePort).getSpec("ideas_planning_q3.md");
    }

    /**
     * Regresión de <b>DP-07</b>, resuelta en la Fase 01 mediante eliminación.
     *
     * <p>El marcador {@code CREATE_STRUCTURED_USER_STORY} tiene 28 caracteres, por lo que
     * {@code shouldSearchPlanning()} lo considera texto libre y lo enruta al flujo de
     * Planificación. La plantilla de creación estructurada era, por tanto, código inalcanzable y
     * fue eliminada por decisión del usuario.
     *
     * <p>Este test permanece como prueba de regresión: el marcador se trata como texto libre.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-07
     */
    @Test
    @DisplayName("DP-07: el marcador de creación estructurada se trata como texto libre")
    void givenStructuredCreationMarker_whenChatAndRespond_thenPlanningFlowWins() {
        // GIVEN
        when(specStoragePort.getSpec(anyString()))
                .thenReturn(Mono.just(new SpecDocument("ideas_planning_q3.md", "",
                        "/specs/ideas_planning_q3.md")));
        givenTemplateAndGatewayRespond();

        // WHEN
        executeAndGet("CREATE_STRUCTURED_USER_STORY", null);

        // THEN
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PLANNING_DRAFT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Flujo Program Planning
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Comando /plan o /roadmap enruta a ProgramPlanningFlowHandler")
    void givenPlanningCommand_whenChatAndRespond_thenExecutesProgramPlanningFlow() {
        // GIVEN
        String planningMarkdown = "# Roadmap de Planeación — Q3-2026\n\n"
                + "## Resumen Ejecutivo\n\nPlaneación ejecutiva.\n\n"
                + "## Métricas del Plan\n\n- **Total Story Points:** 13 pts\n\n"
                + "## Tabla de Asignaciones por Sprint\n\n"
                + "| Sprint | Tipo | Título | Story Points | Frente | Dependencias | Criterio de Entrega |\n"
                + "|:------:|:----:|:-------|:------------:|:-------|:-------------|:--------------------|\n"
                + "| 1 | [HU] | Setup Inicial | 5 | Core | Ninguna | QA funcional |\n";
        when(specStoragePort.getSpec(anyString()))
                .thenReturn(Mono.just(new SpecDocument("ideas_planning_q3.md", "Specs previas",
                        "/specs/ideas_planning_q3.md")));
        when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(planningMarkdown));
        when(specStoragePort.saveSpec(any(), any())).thenReturn(Mono.empty());

        // WHEN
        SendMessageResponse response = executeAndGet("/plan Q3-2026 6 sprints 34 sp", "plan-1");

        // THEN
        assertThat(response.getTask().getStatus().getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PROGRAM_PLANNING);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private void givenChatGatewayReturnsResponse() {
        when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(LLM_RESPONSE));
    }

    private void givenTemplateAndGatewayRespond() {
        when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
        givenChatGatewayReturnsResponse();
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

    private PromptTemplateId captureTemplateId() {
        ArgumentCaptor<PromptTemplateId> idCaptor =
                ArgumentCaptor.forClass(PromptTemplateId.class);
        verify(promptTemplatePort).render(idCaptor.capture(), anyMap());
        return idCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureTemplateVariables() {
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(promptTemplatePort).render(any(), varsCaptor.capture());
        return varsCaptor.getValue();
    }

    private void verifyNoTemplateWasRendered() {
        verify(promptTemplatePort, never()).render(any(), anyMap());
    }
}

