package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.ProgramPlanRequestDTO;
import co.com.bancolombia.api.dto.ProgramPlanResponseDTO;
import co.com.bancolombia.api.error.ApiErrorTranslator;
import co.com.bancolombia.usecase.planning.TriggerProgramPlanningUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler reactivo funcional para la activación de planeación macro trimestral (Program Planning).
 *
 * <p>Recibe la solicitud HTTP, valida el contrato, delega en {@link TriggerProgramPlanningUseCase}
 * y devuelve la respuesta HTTP con el estado de la tarea iniciada en el agente autónomo
 * (DP-BFF-03).
 */
@Slf4j
@Component
public class ProgramPlanningHandler {

    private final TriggerProgramPlanningUseCase triggerProgramPlanningUseCase;

    public ProgramPlanningHandler(TriggerProgramPlanningUseCase triggerProgramPlanningUseCase) {
        this.triggerProgramPlanningUseCase = triggerProgramPlanningUseCase;
    }

    /**
     * {@code POST /api/planning/program} — activa la planeación macro trimestral de manera no
     * bloqueante.
     *
     * @param request petición HTTP con el payload de planeación
     * @return respuesta HTTP 202 Accepted con {@link ProgramPlanResponseDTO}
     */
    public Mono<ServerResponse> triggerProgramPlanning(ServerRequest request) {
        return request.bodyToMono(ProgramPlanRequestDTO.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "El cuerpo de la solicitud no puede estar vacío")))
                .flatMap(dto -> triggerProgramPlanningUseCase.execute(dto.toDomain(),
                                dto.contextId())
                        .map(interaction -> ProgramPlanResponseDTO.from(dto.quarter(),
                                interaction)))
                .flatMap(response -> ServerResponse.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }
}
