package co.com.bancolombia.usecase.chat.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.AzureDevOpsScope;
import co.com.bancolombia.model.agent.CorporateKnowledge;
import co.com.bancolombia.model.agent.IntentResolution;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Pruebas unitarias de los seis {@link ChatFlowHandler}.
 *
 * <p>Cada bloque comprueba dos cosas: qué plantilla solicita el handler y con qué variables, y qué
 * texto compone a partir de la respuesta del modelo. El manejo genérico de error <b>no</b> se
 * prueba aquí: desde la Fase 04 vive una sola vez, en el caso de uso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatFlowHandler - Flujos de conversación")
class ChatFlowHandlerTest {

    private static final String CONTEXT_ID = "ctx-1";
    private static final String USER_TEXT = "Necesito un microservicio de notificaciones push";
    private static final String RENDERED_PROMPT = "Prompt renderizado por la plantilla";
    private static final String LLM_RESPONSE = "Respuesta simulada del modelo";
    private static final String WORK_ITEM_ID = "12345";

    private static final String TEMPLATE_MARKDOWN = "## Plantilla HU/HA corporativa";
    private static final String AGILE_GUIDE = "## Guía de agilidad";
    private static final String QUALITY_AUDIT_GUIDE = "## Estándares de auditoría";
    private static final String DEFAULT_ORG = "grupobancolombia";
    private static final String DEFAULT_PROJECT = "Vicepresidencia Servicios de Tecnología";

    private static final AzureDevOpsScope SCOPE =
            new AzureDevOpsScope(DEFAULT_ORG, DEFAULT_PROJECT);
    private static final CorporateKnowledge KNOWLEDGE =
            new CorporateKnowledge(TEMPLATE_MARKDOWN, AGILE_GUIDE, QUALITY_AUDIT_GUIDE);

    private static final String VAR_WORK_ITEM_ID = "workItemId";
    private static final String VAR_ORGANIZATION = "organizacion";
    private static final String VAR_PROJECT = "proyecto";
    private static final String VAR_AGILE_GUIDE = "guiaAgilidad";
    private static final String VAR_STANDARDS = "estandares";
    private static final String VAR_ORIGINAL_IDEA = "ideaOriginal";
    private static final String VAR_RAG_CONTEXT = "contextoRag";
    private static final String VAR_CORPORATE_TEMPLATE = "plantillaCorporativa";

    @Mock
    private ChatGateway chatGateway;

    @Mock
    private PromptTemplatePort promptTemplatePort;

    @Mock
    private SpecStoragePort specStoragePort;

    // ═══════════════════════════════════════════════════════════════════════════
    // General
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GeneralFlowHandler")
    class General {

        private GeneralFlowHandler handler;

        @BeforeEach
        void setUp() {
            handler = new GeneralFlowHandler(chatGateway);
        }

        @Test
        @DisplayName("Declara la intención GENERAL")
        void givenHandler_whenSupports_thenGeneral() {
            assertThat(handler.supports()).isEqualTo(AgentIntent.GENERAL);
        }

        @Test
        @DisplayName("Envía el texto del usuario sin plantilla")
        void givenUserText_whenHandle_thenSendsRawText() {
            // GIVEN
            when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(LLM_RESPONSE));

            // WHEN
            String result = handler.handle(contextOf(AgentIntent.GENERAL)).block();

            // THEN
            assertThat(result).isEqualTo(LLM_RESPONSE);
            verify(chatGateway).sendMessage(USER_TEXT, CONTEXT_ID);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Auditoría de calidad
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("QualityAuditFlowHandler")
    class QualityAudit {

        private QualityAuditFlowHandler handler;

        @BeforeEach
        void setUp() {
            handler = new QualityAuditFlowHandler(chatGateway, promptTemplatePort, SCOPE,
                    KNOWLEDGE);
        }

        @Test
        @DisplayName("Declara la intención QUALITY_AUDIT")
        void givenHandler_whenSupports_thenQualityAudit() {
            assertThat(handler.supports()).isEqualTo(AgentIntent.QUALITY_AUDIT);
        }

        @Test
        @DisplayName("Solicita la plantilla de auditoría con el Work Item y los estándares")
        void givenWorkItem_whenHandle_thenRendersAuditTemplate() {
            // GIVEN
            givenTemplateAndGatewayRespond();

            // WHEN
            handler.handle(contextWithWorkItem(AgentIntent.QUALITY_AUDIT)).block();

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

        @Test
        @DisplayName("Retira el bloque JSON técnico de la respuesta")
        void givenResponseWithAuditJson_whenHandle_thenStripsTechnicalBlock() {
            // GIVEN
            when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
            when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(
                    "Informe de auditoría\nAUDIT_JSON_START{\"score\":80}AUDIT_JSON_END"));

            // WHEN
            String result = handler.handle(contextWithWorkItem(AgentIntent.QUALITY_AUDIT)).block();

            // THEN
            assertThat(result).isEqualTo("Informe de auditoría")
                    .doesNotContain("AUDIT_JSON_START", "AUDIT_JSON_END");
        }

        @Test
        @DisplayName("Falla si la resolución no trae identificador de Work Item")
        void givenResolutionWithoutWorkItem_whenHandle_thenThrows() {
            // GIVEN
            ChatFlowContext context = contextOf(AgentIntent.QUALITY_AUDIT);

            // WHEN / THEN
            assertThatThrownBy(() -> handler.handle(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("identificador de Work Item");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Refinamiento
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RefinementFlowHandler")
    class Refinement {

        private RefinementFlowHandler handler;

        @BeforeEach
        void setUp() {
            handler = new RefinementFlowHandler(chatGateway, promptTemplatePort, SCOPE, KNOWLEDGE);
        }

        @Test
        @DisplayName("Declara la intención REFINEMENT")
        void givenHandler_whenSupports_thenRefinement() {
            assertThat(handler.supports()).isEqualTo(AgentIntent.REFINEMENT);
        }

        @Test
        @DisplayName("Solicita la plantilla de refinamiento con el Work Item")
        void givenWorkItem_whenHandle_thenRendersRefinementTemplate() {
            // GIVEN
            givenTemplateAndGatewayRespond();

            // WHEN
            handler.handle(contextWithWorkItem(AgentIntent.REFINEMENT)).block();

            // THEN
            assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STORY_REFINEMENT);
            assertThat(captureTemplateVariables()).containsEntry(VAR_WORK_ITEM_ID, WORK_ITEM_ID)
                    .containsEntry(VAR_PROJECT, DEFAULT_PROJECT)
                    .containsEntry(VAR_AGILE_GUIDE, AGILE_GUIDE);
        }

        @Test
        @DisplayName("Cierra con la confirmación de actualización del Work Item")
        void givenEstimatedResponse_whenHandle_thenAppendsUpdateConfirmation() {
            // GIVEN
            when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
            when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(
                    "Historia refinada {\"puntos\": 3, \"incertidumbre_nivel\": \"Baja\"}"));

            // WHEN
            String result = handler.handle(contextWithWorkItem(AgentIntent.REFINEMENT)).block();

            // THEN
            assertThat(result).contains("Estimación de Complejidad (Story Points):** 3")
                    .contains("¿Deseas actualizar el Work Item");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Aprobación
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ApprovalFlowHandler")
    class Approval {

        private ApprovalFlowHandler handler;

        @BeforeEach
        void setUp() {
            handler = new ApprovalFlowHandler(chatGateway, promptTemplatePort, KNOWLEDGE);
        }

        @Test
        @DisplayName("Declara la intención APPROVAL")
        void givenHandler_whenSupports_thenApproval() {
            assertThat(handler.supports()).isEqualTo(AgentIntent.APPROVAL);
        }

        @Test
        @DisplayName("Solicita la plantilla estructurada con la plantilla corporativa")
        void givenApproval_whenHandle_thenRendersStructuredTemplate() {
            // GIVEN
            givenTemplateAndGatewayRespond();

            // WHEN
            handler.handle(contextOf(AgentIntent.APPROVAL)).block();

            // THEN
            assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STRUCTURED_STORY);
            assertThat(captureTemplateVariables())
                    .containsEntry(VAR_CORPORATE_TEMPLATE, TEMPLATE_MARKDOWN)
                    .containsEntry(VAR_AGILE_GUIDE, AGILE_GUIDE);
        }

        @Test
        @DisplayName("Avisa cuando el modelo no entrega estimación (DP-03)")
        void givenResponseWithoutEstimation_whenHandle_thenWarnsExplicitly() {
            // GIVEN
            when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
            when(chatGateway.sendMessage(anyString(), any()))
                    .thenReturn(Mono.just("Historia sin bloque JSON"));

            // WHEN
            String result = handler.handle(contextOf(AgentIntent.APPROVAL)).block();

            // THEN
            assertThat(result).contains("No se pudo obtener la estimación")
                    .contains("¿Deseas registrar esta Historia");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // División
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DivisionFlowHandler")
    class Division {

        private DivisionFlowHandler handler;

        @BeforeEach
        void setUp() {
            handler = new DivisionFlowHandler(chatGateway, promptTemplatePort, KNOWLEDGE);
        }

        @Test
        @DisplayName("Declara la intención DIVISION")
        void givenHandler_whenSupports_thenDivision() {
            assertThat(handler.supports()).isEqualTo(AgentIntent.DIVISION);
        }

        @Test
        @DisplayName("Solicita la plantilla de división y cierra con la confirmación")
        void givenDivision_whenHandle_thenRendersDivisionTemplate() {
            // GIVEN
            givenTemplateAndGatewayRespond();

            // WHEN
            String result = handler.handle(contextOf(AgentIntent.DIVISION)).block();

            // THEN
            assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.STORY_DIVISION);
            assertThat(captureTemplateVariables()).containsEntry(VAR_AGILE_GUIDE, AGILE_GUIDE);
            assertThat(result).startsWith(LLM_RESPONSE)
                    .contains("¿Deseas proceder con el registro de estas historias divididas");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Planificación
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PlanningDraftFlowHandler")
    class PlanningDraft {

        private PlanningDraftFlowHandler handler;

        @BeforeEach
        void setUp() {
            handler = new PlanningDraftFlowHandler(chatGateway, promptTemplatePort,
                    specStoragePort);
        }

        @Test
        @DisplayName("Declara la intención PLANNING_DRAFT")
        void givenHandler_whenSupports_thenPlanningDraft() {
            assertThat(handler.supports()).isEqualTo(AgentIntent.PLANNING_DRAFT);
        }

        @Test
        @DisplayName("Carga el spec correspondiente al prefijo de frente y lo inyecta íntegro")
        void givenIdeaWithFrontPrefix_whenHandle_thenLoadsMatchingSpecAndInjectsContent() {
            // GIVEN
            String ideaWithPrefix = "[Aegis Engine] Desarrollar nuevo motor de reglas reactivo";
            ChatFlowContext context = new ChatFlowContext(ideaWithPrefix, CONTEXT_ID,
                    IntentResolution.of(AgentIntent.PLANNING_DRAFT));
            SpecDocument spec = new SpecDocument("aegis_engine.md",
                    "## Documento Completo de Aegis Engine",
                    "/specs/aegis_engine.md");
            when(specStoragePort.getSpec("aegis_engine.md")).thenReturn(Mono.just(spec));
            givenTemplateAndGatewayRespond();

            // WHEN
            handler.handle(context).block();

            // THEN
            assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PLANNING_DRAFT);
            Map<String, Object> variables = captureTemplateVariables();
            assertThat(variables).containsEntry(VAR_ORIGINAL_IDEA, ideaWithPrefix)
                    .containsEntry(VAR_RAG_CONTEXT, "## Documento Completo de Aegis Engine");
            verify(specStoragePort).getSpec("aegis_engine.md");
        }

        @Test
        @DisplayName("Carga el spec por defecto cuando la idea no contiene prefijo")
        void givenIdeaWithoutPrefix_whenHandle_thenLoadsDefaultSpec() {
            // GIVEN
            SpecDocument spec = new SpecDocument("ideas_planning_q3.md", "## Plan Maestro Q3",
                    "/specs/ideas_planning_q3.md");
            when(specStoragePort.getSpec("ideas_planning_q3.md")).thenReturn(Mono.just(spec));
            givenTemplateAndGatewayRespond();

            // WHEN
            handler.handle(contextOf(AgentIntent.PLANNING_DRAFT)).block();

            // THEN
            assertThat(captureTemplateId()).isEqualTo(PromptTemplateId.PLANNING_DRAFT);
            Map<String, Object> variables = captureTemplateVariables();
            assertThat(variables).containsEntry(VAR_ORIGINAL_IDEA, USER_TEXT)
                    .containsEntry(VAR_RAG_CONTEXT, "## Plan Maestro Q3");
            verify(specStoragePort).getSpec("ideas_planning_q3.md");
        }

        @Test
        @DisplayName("Un spec no encontrado no interrumpe el flujo y aplica contingencia")
        void givenSpecNotFound_whenHandle_thenContinuesWithoutContext() {
            // GIVEN
            when(specStoragePort.getSpec(anyString()))
                    .thenReturn(Mono.error(new SpecNotFoundException("Spec no encontrado")));
            givenTemplateAndGatewayRespond();

            // WHEN
            String result = handler.handle(contextOf(AgentIntent.PLANNING_DRAFT)).block();

            // THEN
            assertThat(captureTemplateVariables())
                    .containsEntry(VAR_RAG_CONTEXT, "No hay contexto de planeación adicional.");
            assertThat(result).isEqualTo(LLM_RESPONSE);
        }

        @Test
        @DisplayName("Un spec con contenido en blanco usa el texto de contingencia")
        void givenSpecWithBlankContent_whenHandle_thenUsesFallbackContext() {
            // GIVEN
            SpecDocument spec = new SpecDocument("ideas_planning_q3.md", "   ",
                    "/specs/ideas_planning_q3.md");
            when(specStoragePort.getSpec("ideas_planning_q3.md")).thenReturn(Mono.just(spec));
            givenTemplateAndGatewayRespond();

            // WHEN
            handler.handle(contextOf(AgentIntent.PLANNING_DRAFT)).block();

            // THEN
            assertThat(captureTemplateVariables())
                    .containsEntry(VAR_RAG_CONTEXT, "No hay contexto de planeación adicional.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private ChatFlowContext contextOf(AgentIntent intent) {
        return new ChatFlowContext(USER_TEXT, CONTEXT_ID, IntentResolution.of(intent));
    }

    private ChatFlowContext contextWithWorkItem(AgentIntent intent) {
        return new ChatFlowContext(USER_TEXT, CONTEXT_ID,
                IntentResolution.withWorkItem(intent, WORK_ITEM_ID));
    }

    private void givenTemplateAndGatewayRespond() {
        when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(LLM_RESPONSE));
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
}

