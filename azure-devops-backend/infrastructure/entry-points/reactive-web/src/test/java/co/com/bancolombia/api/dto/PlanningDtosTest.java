package co.com.bancolombia.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.a2a.TaskStatus;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.spec.SpecDocument;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlanningDtosTest {

    @Test
    @DisplayName("SpecDocumentDTO: mapeo correcto desde dominio y manejo de nulos")
    void specDocumentDtoMapping() {
        SpecDocument doc = new SpecDocument("ideas.md", "# Content", "docs/ideas.md");
        SpecDocumentDTO dto = SpecDocumentDTO.fromDomain(doc);

        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo("ideas.md");
        assertThat(dto.content()).isEqualTo("# Content");
        assertThat(dto.path()).isEqualTo("docs/ideas.md");

        assertThat(SpecDocumentDTO.fromDomain(null)).isNull();

        SpecDocumentDTO nullContentDto = new SpecDocumentDTO("ideas.md", null, "path");
        assertThat(nullContentDto.content()).isEmpty();
    }

    @Test
    @DisplayName("SpecListDTO: constructores y manejo de listas nulas")
    void specListDtoConstructors() {
        SpecListDTO dto1 = new SpecListDTO(List.of("a.md", "b.md"));
        assertThat(dto1.count()).isEqualTo(2);
        assertThat(dto1.specs()).containsExactly("a.md", "b.md");

        SpecListDTO dto2 = new SpecListDTO(null);
        assertThat(dto2.count()).isEqualTo(0);
        assertThat(dto2.specs()).isEmpty();
    }

    @Test
    @DisplayName("ProgramPlanRequestDTO: conversión correcta a modelo de dominio")
    void programPlanRequestDtoToDomain() {
        ProgramPlanRequestDTO dto = new ProgramPlanRequestDTO(
                "Q3-2026",
                "Objetivo estratégico",
                List.of("Core", "Canales"),
                6,
                40,
                "ctx-123"
        );

        ProgramPlanRequest domain = dto.toDomain();
        assertThat(domain.quarter()).isEqualTo("Q3-2026");
        assertThat(domain.objectives()).isEqualTo("Objetivo estratégico");
        assertThat(domain.targetFronts()).containsExactly("Core", "Canales");
        assertThat(domain.sprintCount()).isEqualTo(6);
        assertThat(domain.maxCapacityPerSprint()).isEqualTo(40);

        // Caso con campos nulos que viola invariantes de dominio
        ProgramPlanRequestDTO nullFields = new ProgramPlanRequestDTO(
                "Q3-2026",
                "Objetivo",
                null,
                null,
                null,
                null
        );
        assertThat(domain.targetFronts()).containsExactly("Core", "Canales");
        org.assertj.core.api.Assertions.assertThatThrownBy(nullFields::toDomain)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sprints");
    }

    @Test
    @DisplayName("ProgramPlanResponseDTO: creación desde AgentInteraction con y sin Task")
    void programPlanResponseDtoCreation() {
        Task task = Task.builder()
                .id("t-1")
                .contextId("ctx-1")
                .status(TaskStatus.builder().state(TaskState.WORKING).build())
                .build();
        AgentInteraction interactionWithTask = new AgentInteraction(task, "Respuesta con tarea");

        ProgramPlanResponseDTO dto = ProgramPlanResponseDTO.from("Q3-2026", interactionWithTask);
        assertThat(dto.quarter()).isEqualTo("Q3-2026");
        assertThat(dto.message()).isEqualTo("Respuesta con tarea");
        assertThat(dto.contextId()).isEqualTo("ctx-1");
        assertThat(dto.task()).isNotNull();
        assertThat(dto.task().id()).isEqualTo("t-1");

        AgentInteraction interactionWithoutTask = AgentInteraction.replyOnly("Solo respuesta");
        ProgramPlanResponseDTO dtoNoTask = ProgramPlanResponseDTO.from("Q3-2026",
                interactionWithoutTask);
        assertThat(dtoNoTask.task()).isNull();
        assertThat(dtoNoTask.contextId()).isNull();

        ProgramPlanResponseDTO nullInteractionDto = ProgramPlanResponseDTO.from("Q3-2026", null);
        assertThat(nullInteractionDto.quarter()).isEqualTo("Q3-2026");
        assertThat(nullInteractionDto.message()).isNull();
        assertThat(nullInteractionDto.task()).isNull();
    }
}
