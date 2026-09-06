package co.com.bancolombia.usecase.chat.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.agent.AgentIntent;
import co.com.bancolombia.model.agent.IntentResolution;
import co.com.bancolombia.model.planning.ActivityType;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.planning.ProgramPlanResult;
import co.com.bancolombia.model.planning.SprintAllocation;
import co.com.bancolombia.usecase.planning.ProgramPlanningUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgramPlanningFlowHandler - Flujo conversacional de planeación")
class ProgramPlanningFlowHandlerTest {

    private static final String CONTEXT_ID = "ctx-planning-1";

    @Mock
    private ProgramPlanningUseCase programPlanningUseCase;

    private ProgramPlanningFlowHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProgramPlanningFlowHandler(programPlanningUseCase);
    }

    @Test
    @DisplayName("Declara soporte para la intención PROGRAM_PLANNING")
    void givenHandler_whenSupports_thenProgramPlanning() {
        assertThat(handler.supports()).isEqualTo(AgentIntent.PROGRAM_PLANNING);
    }

    @Test
    @DisplayName("GIVEN comando con parámetros explícitos WHEN handle THEN parsea solicitud y delega al caso de uso")
    void givenCommandWithParameters_whenHandle_thenParsesAndDelegates() {
        // GIVEN
        String userText = "/plan Q3-2026 6 sprints capacidad 34 Frentes: Canales, Core. Objetivos: Modernización Cloud";
        ChatFlowContext context = new ChatFlowContext(
                userText,
                CONTEXT_ID,
                IntentResolution.of(AgentIntent.PROGRAM_PLANNING));

        ProgramPlanResult mockResult = ProgramPlanResult.builder()
                .quarter("Q3-2026")
                .executiveSummary("Resumen de planeación para Q3-2026.")
                .allocations(List.of(
                        SprintAllocation.builder()
                                .sprintNumber(1)
                                .type(ActivityType.USER_STORY)
                                .title("Canales | Pantalla Login")
                                .description("Pruebas en QA")
                                .storyPoints(5)
                                .front("Canales")
                                .dependencies(List.of())
                                .build(),
                        SprintAllocation.builder()
                                .sprintNumber(1)
                                .type(ActivityType.ENABLER)
                                .title("DevOps | Pipeline HyMS")
                                .description("Paso a producción HyMS")
                                .storyPoints(3)
                                .front("DevOps")
                                .dependencies(List.of())
                                .build()
                ))
                .generatedSpecNames(List.of("ideas_planning_q3_2026.md", "frente_canales.md"))
                .build();

        when(programPlanningUseCase.plan(any(ProgramPlanRequest.class), anyString()))
                .thenReturn(Mono.just(mockResult));

        // WHEN & THEN
        StepVerifier.create(handler.handle(context))
                .assertNext(response -> {
                    assertThat(response)
                            .contains("# Roadmap de Planeación — Q3-2026")
                            .contains("## Resumen Ejecutivo")
                            .contains("Resumen de planeación para Q3-2026.")
                            .contains("## Métricas del Plan")
                            .contains("Total Story Points:** 8 pts")
                            .contains("Total Actividades:** 2 (1 HUs hasta QA, 1 HAs HyMS/Setups)")
                            .contains("ideas_planning_q3_2026.md")
                            .contains("## Tabla de Asignaciones por Sprint")
                            .contains("Canales | Pantalla Login")
                            .contains("DevOps | Pipeline HyMS");
                })
                .verifyComplete();

        // Validar argumentos capturados hacia el caso de uso
        ArgumentCaptor<ProgramPlanRequest> captor = ArgumentCaptor.forClass(
                ProgramPlanRequest.class);
        verify(programPlanningUseCase).plan(captor.capture(), anyString());
        ProgramPlanRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.quarter()).isEqualTo("Q3-2026");
        assertThat(capturedRequest.sprintCount()).isEqualTo(6);
        assertThat(capturedRequest.maxCapacityPerSprint()).isEqualTo(34);
        assertThat(capturedRequest.targetFronts()).contains("Canales", "Core");
        assertThat(capturedRequest.objectives()).contains("Modernización Cloud");
    }

    @Test
    @DisplayName("GIVEN texto con frente entre corchetes y defaults WHEN handle THEN extrae frente y defaults")
    void givenBracketedFront_whenHandle_thenExtractsFrontAndDefaults() {
        String userText = "Planificar trimestre Q4-2026 para el frente [Aegis Security] con foco en auditoría.";
        ChatFlowContext context = new ChatFlowContext(
                userText,
                CONTEXT_ID,
                IntentResolution.of(AgentIntent.PROGRAM_PLANNING));

        ProgramPlanResult mockResult = ProgramPlanResult.builder()
                .quarter("Q4-2026")
                .executiveSummary("Resumen Q4.")
                .allocations(List.of())
                .generatedSpecNames(List.of("ideas_planning_q4_2026.md"))
                .build();

        when(programPlanningUseCase.plan(any(ProgramPlanRequest.class), anyString()))
                .thenReturn(Mono.just(mockResult));

        StepVerifier.create(handler.handle(context))
                .assertNext(response -> {
                    assertThat(response)
                            .contains("# Roadmap de Planeación — Q4-2026")
                            .contains("Resumen Q4.")
                            .doesNotContain("## Tabla de Asignaciones por Sprint");
                })
                .verifyComplete();

        ArgumentCaptor<ProgramPlanRequest> captor = ArgumentCaptor.forClass(
                ProgramPlanRequest.class);
        verify(programPlanningUseCase).plan(captor.capture(), anyString());
        ProgramPlanRequest captured = captor.getValue();
        assertThat(captured.quarter()).isEqualTo("Q4-2026");
        assertThat(captured.sprintCount()).isEqualTo(6); // Default
        assertThat(captured.maxCapacityPerSprint()).isEqualTo(34); // Default
        assertThat(captured.targetFronts()).containsExactly("Aegis Security");
    }

    @Test
    @DisplayName("GIVEN texto sin parámetros explícitos WHEN handle THEN aplica defaults robustos")
    void givenMinimalText_whenHandle_thenAppliesAllDefaults() {
        String userText = "generar roadmap de planeacion";
        ChatFlowContext context = new ChatFlowContext(
                userText,
                CONTEXT_ID,
                IntentResolution.of(AgentIntent.PROGRAM_PLANNING));

        ProgramPlanResult mockResult = ProgramPlanResult.builder()
                .quarter("Q3-2026")
                .executiveSummary("Resumen default.")
                .allocations(List.of())
                .generatedSpecNames(List.of("ideas_planning_q3_2026.md"))
                .build();

        when(programPlanningUseCase.plan(any(ProgramPlanRequest.class), anyString()))
                .thenReturn(Mono.just(mockResult));

        StepVerifier.create(handler.handle(context))
                .assertNext(response -> assertThat(response).contains("Q3-2026"))
                .verifyComplete();

        ArgumentCaptor<ProgramPlanRequest> captor = ArgumentCaptor.forClass(
                ProgramPlanRequest.class);
        verify(programPlanningUseCase).plan(captor.capture(), anyString());
        ProgramPlanRequest captured = captor.getValue();
        assertThat(captured.quarter()).isEqualTo("Q3-2026");
        assertThat(captured.sprintCount()).isEqualTo(6);
        assertThat(captured.maxCapacityPerSprint()).isEqualTo(34);
        assertThat(captured.targetFronts()).isEmpty();
        assertThat(captured.objectives()).contains("generar roadmap de planeacion");
    }
}
