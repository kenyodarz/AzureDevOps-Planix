package co.com.bancolombia.usecase.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.planning.ActivityType;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.planning.SprintAllocation;
import co.com.bancolombia.model.prompt.PromptTemplateId;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.SpecNotFoundException;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgramPlanningUseCase - Orquestación de Program Planning")
class ProgramPlanningUseCaseTest {

    private static final String QUARTER = "Q3-2026";
    private static final String OBJECTIVES = "Modernizar la autenticación e integrar canales móviles con el Core.";
    private static final List<String> FRONTS = List.of("Canales", "Core");
    private static final int SPRINTS = 6;
    private static final int CAPACITY = 34;

    private static final String SAMPLE_LLM_RESPONSE = """
            # Roadmap de Planeación — Q3-2026
            
            ## 1. Resumen Ejecutivo
            
            Plan estratégico para el trimestre Q3-2026 enfocado en migración a la nube y autenticación unificada.
            
            ## 2. Tabla de Roadmap por Sprints
            
            | Sprint | Tipo | Título | Story Points | Frente | Dependencias | Descripción / Criterio de Entrega |
            |:------:|:----:|:-------|:------------:|:-------|:-------------|:----------------------------------|
            | 1 | HU | Canales | Implementar pantalla Login | 5 | Canales | Ninguna | Construcción y pruebas funcionales en ambiente QA |
            | 1 | HA | Core | Setup pipeline CI/CD HyMS | 3 | Core | Ninguna | Habilitación de infraestructura y pipeline de despliegue HyMS |
            | 2 | HU | Canales | API de Cuentas Móviles | 8 | Canales | Canales | Implementar pantalla Login | Integración y pruebas QA |
            | 2 | HA | Canales | Paso a Producción HyMS Login | 2 | Canales | Implementar pantalla Login | Runbook de operación y paso a producción formal HyMS |
            
            ## 3. Especificaciones y Entregables por Frente
            
            - `frente_canales.md`
            - `frente_core.md`
            """;

    @Mock
    private PromptTemplatePort promptTemplatePort;

    @Mock
    private SpecStoragePort specStoragePort;

    @Mock
    private ChatGateway chatGateway;

    private ProgramPlanningUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProgramPlanningUseCase(promptTemplatePort, specStoragePort, chatGateway);
    }

    @Test
    @DisplayName("GIVEN solicitud válida WHEN planifica THEN orquesta specs, prompt, LLM y persiste roadmap maestro")
    void givenValidRequest_whenPlan_thenOrchestratesAndPersistsMasterSpec() {
        // GIVEN
        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .targetFronts(FRONTS)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        SpecDocument docCanales = new SpecDocument("frente_canales.md", "Specs de canales",
                "/specs/frente_canales.md");
        SpecDocument docCore = new SpecDocument("frente_core.md", "Specs de core",
                "/specs/frente_core.md");

        when(specStoragePort.getSpec("frente_canales.md")).thenReturn(Mono.just(docCanales));
        when(specStoragePort.getSpec("frente_core.md")).thenReturn(Mono.just(docCore));
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt Renderizado Q3-2026");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(
                Mono.just(SAMPLE_LLM_RESPONSE));
        when(specStoragePort.saveSpec(eq("ideas_planning_q3_2026.md"), anyString())).thenReturn(
                Mono.empty());

        // WHEN
        StepVerifier.create(useCase.plan(request))
                .assertNext(result -> {
                    // THEN
                    assertThat(result.quarter()).isEqualTo(QUARTER);
                    assertThat(result.executiveSummary())
                            .contains("Plan estratégico para el trimestre Q3-2026");
                    assertThat(result.allocations()).hasSize(4);
                    assertThat(result.totalStoryPoints()).isEqualTo(18);

                    // Validar allocations por sprint
                    List<SprintAllocation> sprint1Allocs = result.allocationsForSprint(1);
                    assertThat(sprint1Allocs).hasSize(2);

                    // Validar DP-PL-02: tipos HU (hasta QA) y HA (HyMS)
                    List<SprintAllocation> hus = result.allocationsForType(ActivityType.USER_STORY);
                    List<SprintAllocation> has = result.allocationsForType(ActivityType.ENABLER);
                    assertThat(hus).hasSize(2);
                    assertThat(has).hasSize(2);

                    assertThat(result.generatedSpecNames())
                            .contains("ideas_planning_q3_2026.md", "frente_canales.md",
                                    "frente_core.md");
                })
                .verifyComplete();

        // THEN: verificar variables enviadas a la plantilla
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(promptTemplatePort).render(eq(PromptTemplateId.PROGRAM_PLANNING), captor.capture());
        Map<String, Object> vars = captor.getValue();
        assertThat(vars)
                .containsEntry("trimestre", QUARTER)
                .containsEntry("capacidadSprint", CAPACITY)
                .containsEntry("frentes", "Canales, Core")
                .containsEntry("objetivos", OBJECTIVES);
        assertThat((String) vars.get("specsContexto"))
                .contains("### Frente: Canales", "### Frente: Core");

        // THEN: verificar persistencia del roadmap maestro (DP-PL-04)
        verify(specStoragePort).saveSpec("ideas_planning_q3_2026.md", SAMPLE_LLM_RESPONSE);
    }

    @Test
    @DisplayName("GIVEN actividad etiquetada como HU pero con alcance HyMS WHEN planifica THEN reclasifica a HA (DP-PL-02)")
    void givenHuWithHyMSScope_whenPlan_thenReclassifiesToHA() {
        // GIVEN: El LLM marcó erróneamente 'HU' en una actividad de paso a producción HyMS
        String responseWithHyMSMisclassification = """
                ## 1. Resumen Ejecutivo
                Resumen de prueba.
                
                ## 2. Tabla de Roadmap por Sprints
                | Sprint | Tipo | Título | Story Points | Frente | Dependencias | Descripción / Criterio de Entrega |
                |:------:|:----:|:-------|:------------:|:-------|:-------------|:----------------------------------|
                | 1 | HU | Canales | Despliegue a Producción HyMS | 5 | Canales | Ninguna | Ejecución de runbook y paso a producción formal |
                """;

        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter("Q4-2026")
                .objectives("Objetivos")
                .sprintCount(4)
                .maxCapacityPerSprint(30)
                .build();

        when(specStoragePort.getSpec(anyString())).thenReturn(Mono.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(
                Mono.just(responseWithHyMSMisclassification));
        when(specStoragePort.saveSpec(anyString(), anyString())).thenReturn(Mono.empty());

        // WHEN & THEN
        StepVerifier.create(useCase.plan(request))
                .assertNext(result -> {
                    assertThat(result.allocations()).hasSize(1);
                    SprintAllocation allocation = result.allocations().get(0);
                    // Por DP-PL-02 debe ser reclasificada a ENABLER (HA)
                    assertThat(allocation.type()).isEqualTo(ActivityType.ENABLER);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN respuesta nula o en blanco del LLM WHEN planifica THEN retorna resultado defensivo sin fallar")
    void givenNullOrBlankLlmResponse_whenPlan_thenReturnsDefensiveFallback() {
        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        when(specStoragePort.getSpec(anyString())).thenReturn(Mono.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(Mono.just("   "));

        StepVerifier.create(useCase.plan(request))
                .assertNext(result -> {
                    assertThat(result.quarter()).isEqualTo(QUARTER);
                    assertThat(result.allocations()).isEmpty();
                    assertThat(result.executiveSummary()).contains("No se generó planeación");
                    assertThat(result.generatedSpecNames()).contains("ideas_planning_q3_2026.md");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN respuesta sin tabla Markdown WHEN planifica THEN extrae resumen y retorna lista vacía de asignaciones")
    void givenResponseWithoutTable_whenPlan_thenExtractsSummaryWithoutFailing() {
        String responseWithoutTable = """
                ## 1. Resumen Ejecutivo
                Este es un resumen sin tabla de actividades adjunta.
                """;

        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        when(specStoragePort.getSpec(anyString())).thenReturn(Mono.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(
                Mono.just(responseWithoutTable));
        when(specStoragePort.saveSpec(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.plan(request))
                .assertNext(result -> {
                    assertThat(result.executiveSummary())
                            .isEqualTo("Este es un resumen sin tabla de actividades adjunta.");
                    assertThat(result.allocations()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN falla en specStorage al leer specs WHEN planifica THEN continúa con spec por defecto")
    void givenSpecStorageFailure_whenPlan_thenFallsBackResiliently() {
        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .targetFronts(List.of("FrontInexistente"))
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        when(specStoragePort.getSpec("frente_frontinexistente.md"))
                .thenReturn(Mono.error(new SpecNotFoundException("No encontrado")));
        when(specStoragePort.getSpec("ideas_planning_q3_2026.md"))
                .thenReturn(Mono.error(new SpecNotFoundException("No encontrado")));
        when(specStoragePort.getSpec("ideas_planning_q3.md"))
                .thenReturn(Mono.error(new SpecNotFoundException("No encontrado")));

        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(
                Mono.just(SAMPLE_LLM_RESPONSE));
        when(specStoragePort.saveSpec(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.plan(request))
                .assertNext(result -> assertThat(result.allocations()).isNotEmpty())
                .verifyComplete();

        // Verificar que specsContexto recibió el mensaje de fallback
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(promptTemplatePort).render(eq(PromptTemplateId.PROGRAM_PLANNING), captor.capture());
        assertThat((String) captor.getValue().get("specsContexto"))
                .contains("No hay especificaciones técnicas previas disponibles.");
    }

    @Test
    @DisplayName("GIVEN falla al guardar en specStorage WHEN planifica THEN completa retornando el resultado (resiliencia)")
    void givenSaveSpecFailure_whenPlan_thenReturnsResultResiliently() {
        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        when(specStoragePort.getSpec(anyString())).thenReturn(Mono.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(
                Mono.just(SAMPLE_LLM_RESPONSE));
        when(specStoragePort.saveSpec(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Disk full")));

        StepVerifier.create(useCase.plan(request))
                .assertNext(result -> assertThat(result.allocations()).hasSize(4))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN método alias execute WHEN invocado THEN delega a plan")
    void givenExecuteMethod_whenCalled_thenDelegatesToPlan() {
        ProgramPlanRequest request = ProgramPlanRequest.builder()
                .quarter(QUARTER)
                .objectives(OBJECTIVES)
                .sprintCount(SPRINTS)
                .maxCapacityPerSprint(CAPACITY)
                .build();

        when(specStoragePort.getSpec(anyString())).thenReturn(Mono.empty());
        when(promptTemplatePort.render(eq(PromptTemplateId.PROGRAM_PLANNING), anyMap()))
                .thenReturn("Prompt");
        when(chatGateway.sendMessage(anyString(), anyString())).thenReturn(
                Mono.just(SAMPLE_LLM_RESPONSE));
        when(specStoragePort.saveSpec(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(request))
                .assertNext(result -> assertThat(result.quarter()).isEqualTo(QUARTER))
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN request nula WHEN planifica THEN lanza NullPointerException")
    void givenNullRequest_whenPlan_thenThrowsNpe() {
        assertThatThrownBy(() -> useCase.plan(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("obligatoria");
    }
}
