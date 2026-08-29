package co.com.bancolombia.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Pruebas de <b>caracterización</b> del parseo y post-procesado de la respuesta del LLM en
 * {@link AgentChatUseCase}.
 *
 * <p>Los extractores ({@code extractPuntosFromLlmResponse}, {@code extractJustificacion},
 * {@code extractIncertidumbreNivel}) son privados. Deliberadamente <b>no se usa reflexión ni se
 * altera su visibilidad</b>: se verifican de forma indirecta a través del bloque informativo que el
 * flujo de Aprobación añade al texto final de la respuesta.
 *
 * <p>Igual que en {@link AgentChatUseCaseCharacterizationTest}, estas pruebas documentan el
 * comportamiento <i>actual</i>, incluidos sus defectos conocidos.
 *
 * @see docs/plan/DECISIONES_PENDIENTES.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentChatUseCase - Caracterización del parseo de la respuesta del LLM")
class AgentChatUseCaseParsingTest {

    private static final String TEMPLATE_MARKDOWN = "## Plantilla HU/HA corporativa";
    private static final String AGILE_GUIDE = "## Guía de agilidad";
    private static final String QUALITY_AUDIT_GUIDE = "## Estándares de auditoría";
    private static final String DEFAULT_ORG = "grupobancolombia";
    private static final String DEFAULT_PROJECT = "Vicepresidencia Servicios de Tecnología";

    private static final String APPROVAL_COMMAND = "Aprobado";
    private static final String AUDIT_COMMAND = "Audita la historia (ID: 12345)";
    private static final String RENDERED_PROMPT = "Prompt renderizado por la plantilla";

    private static final String COMPLEXITY_ALERT_FRAGMENT = "Alerta de Complejidad";
    private static final String STORY_POINTS_LABEL = "Estimación de Complejidad (Story Points):";
    private static final String UNCERTAINTY_LABEL = "Nivel de Incertidumbre:";


    private static final int POINTS_LOW = 5;
    private static final int POINTS_AT_THRESHOLD = 8;
    private static final int POINTS_ABOVE_THRESHOLD = 9;
    private static final int POINTS_FIBONACCI_NEXT = 13;

    @Mock
    private ChatGateway chatGateway;

    @Mock
    private AgentResponseGateway agentResponseGateway;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    @Mock
    private PlanningVectorStorePort vectorStorePort;

    @Mock
    private PromptTemplatePort promptTemplatePort;

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
                QUALITY_AUDIT_GUIDE,
                promptTemplatePort);
    }

    @Test
    @DisplayName("Un JSON válido del LLM se refleja en el bloque informativo")
    void givenLlmResponseWithValidJson_whenApprovalFlow_thenInfoBlockShowsParsedValues() {
        // GIVEN
        String justification = "el alcance está acotado a un único endpoint";
        givenLlmResponds(estimationResponse(POINTS_LOW, "Baja", justification));

        // WHEN
        String output = executeApprovalFlow();

        // THEN
        assertThat(output).contains(STORY_POINTS_LABEL + "** " + POINTS_LOW)
                .contains(UNCERTAINTY_LABEL + "** Baja")
                .contains(justification);
    }

    /**
     * <b>DP-03 aplicada (Fase 02).</b> Cuando el modelo no devuelve el bloque JSON de estimación,
     * el agente ya no inventa «1 punto, incertidumbre Media»: informa explícitamente del fallo para
     * que el usuario sepa que debe estimar la historia manualmente.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-03
     */
    @Test
    @DisplayName("DP-03: sin JSON se avisa del fallo en lugar de inventar una estimación")
    void givenLlmResponseWithoutJson_whenApprovalFlow_thenShowsMissingEstimationNotice() {
        // GIVEN
        givenLlmResponds("```markdown\n# Historia sin bloque de estimación\n```");

        // WHEN
        String output = executeApprovalFlow();

        // THEN
        assertThat(output).contains("No se pudo obtener la estimación")
                .contains("estima manualmente esta historia")
                .doesNotContain(STORY_POINTS_LABEL);
    }

    @Test
    @DisplayName("13 puntos disparan la alerta de complejidad")
    void givenLlmResponseWithThirteenPoints_whenApprovalFlow_thenAppendsComplexityAlert() {
        // GIVEN
        givenLlmResponds(estimationResponse(POINTS_FIBONACCI_NEXT, "Alta", "alcance muy amplio"));

        // WHEN
        String output = executeApprovalFlow();

        // THEN
        assertThat(output).contains(COMPLEXITY_ALERT_FRAGMENT);
    }

    /**
     * <b>DP-02 aplicada (Fase 02).</b> El umbral pasó de {@code >= 13} a {@code > 8}, según la
     * tabla oficial de {@code HISTORIA_USUARIO.md} («&gt;8 Requiere Dividir») y la equivalencia por
     * horas acordada. Una estimación de 9 puntos —que el modelo puede producir al no respetar
     * siempre la escala Fibonacci— ahora **sí** exige dividir.
     *
     * <p>Esta aserción era la inversa antes de la Fase 02.
     *
     * @see docs/plan/DECISIONES_PENDIENTES.md DP-02
     */
    @Test
    @DisplayName("DP-02: 9 puntos disparan la alerta por superar el estándar de 8")
    void givenLlmResponseWithNinePoints_whenApprovalFlow_thenAppendsComplexityAlert() {
        // GIVEN
        givenLlmResponds(estimationResponse(POINTS_ABOVE_THRESHOLD, "Alta", "varios flujos"));

        // WHEN
        String output = executeApprovalFlow();

        // THEN
        assertThat(output).contains(COMPLEXITY_ALERT_FRAGMENT);
    }

    @Test
    @DisplayName("8 puntos no disparan la alerta (comportamiento correcto, se mantiene)")
    void givenLlmResponseWithEightPoints_whenApprovalFlow_thenDoesNotAppendAlert() {
        // GIVEN
        givenLlmResponds(estimationResponse(POINTS_AT_THRESHOLD, "Media", "historia extensa"));

        // WHEN
        String output = executeApprovalFlow();

        // THEN
        assertThat(output).doesNotContain(COMPLEXITY_ALERT_FRAGMENT)
                .contains("¿Deseas registrar esta Historia");
    }

    @Test
    @DisplayName("El bloque JSON de estimación se elimina del texto mostrado al usuario")
    void givenLlmResponseWithJsonBlock_whenApprovalFlow_thenJsonIsStrippedFromOutput() {
        // GIVEN
        givenLlmResponds(estimationResponse(POINTS_LOW, "Baja", "alcance acotado"));

        // WHEN
        String output = executeApprovalFlow();

        // THEN
        assertThat(output).doesNotContain("\"puntos\"")
                .doesNotContain("incertidumbre_justificacion");
    }

    @Test
    @DisplayName("Los marcadores AUDIT_JSON_START/END se eliminan del reporte de auditoría")
    void givenAuditResponseWithMarkers_whenAuditFlow_thenAuditJsonBlockIsStripped() {
        // GIVEN
        String llmResponse = """
                ## Auditoría de Calidad
                Resultado del análisis.
                AUDIT_JSON_START
                {
                  "workItemId": "12345",
                  "qualityScore": 80,
                  "criticalErrors": [],
                  "canBeWorkedOn": true
                }
                AUDIT_JSON_END""";
        givenLlmResponds(llmResponse);

        // WHEN
        String output = execute(AUDIT_COMMAND);

        // THEN
        assertThat(output).contains("## Auditoría de Calidad")
                .doesNotContain("AUDIT_JSON_START")
                .doesNotContain("AUDIT_JSON_END")
                .doesNotContain("qualityScore");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private static String estimationResponse(int points, String level, String justification) {
        return """
                ```markdown
                # Historia de Usuario propuesta
                ```
                ```json
                {
                  "puntos": %d,
                  "incertidumbre_nivel": "%s",
                  "incertidumbre_justificacion": "%s"
                }
                ```""".formatted(points, level, justification);
    }

    private void givenLlmResponds(String llmResponse) {
        when(promptTemplatePort.render(any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(chatGateway.sendMessage(anyString(), any())).thenReturn(Mono.just(llmResponse));
    }

    private String executeApprovalFlow() {
        return execute(APPROVAL_COMMAND);
    }

    private String execute(String userText) {
        Message message = Message.builder()
                .role("user")
                .messageId("msg-1")
                .parts(List.of(Part.ofText(userText)))
                .build();
        SendMessageRequest request = SendMessageRequest.builder().message(message).build();
        SendMessageResponse response = useCase.chatAndRespond(request).block();
        return response.getMessage().extractText();
    }
}

