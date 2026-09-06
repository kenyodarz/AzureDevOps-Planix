package co.com.bancolombia.usecase.planning;

import co.com.bancolombia.model.agent.AgentCommand;
import co.com.bancolombia.model.agent.AgentInteraction;
import co.com.bancolombia.model.planning.ProgramPlanRequest;
import co.com.bancolombia.model.planning.ProgramPlanningCommand;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso responsable de activar la planeación macro trimestral (Program Planning).
 *
 * <p>Materializa la decisión arquitectural <b>DP-BFF-03</b>:
 * Valida la solicitud de planeación ({@link ProgramPlanRequest}), construye el comando canónico A2A
 * ({@link ProgramPlanningCommand}) y lo despacha de forma asíncrona y no bloqueante mediante
 * {@link TrackAgentTaskUseCase#sendAndTrack(AgentCommand)}. De este modo, el frontend recibe
 * inmediatamente la interacción con la tarea trazable para monitorear el progreso del agente
 * autónomo.
 */
@RequiredArgsConstructor
public class TriggerProgramPlanningUseCase {

    private final TrackAgentTaskUseCase trackAgentTaskUseCase;

    /**
     * Activa la planeación macro generando un identificador de contexto único para la sesión.
     *
     * @param request parámetros de la solicitud de planeación
     * @return Mono con la interacción del agente conteniendo la tarea abierta
     */
    public Mono<AgentInteraction> execute(ProgramPlanRequest request) {
        return execute(request, UUID.randomUUID().toString());
    }

    /**
     * Activa la planeación macro asociándola a un contexto de sesión específico.
     *
     * @param request   parámetros de la solicitud de planeación
     * @param contextId identificador de sesión o conversación para agrupar las tareas
     * @return Mono con la interacción del agente conteniendo la tarea abierta
     */
    public Mono<AgentInteraction> execute(ProgramPlanRequest request, String contextId) {
        if (request == null) {
            return Mono.error(
                    new IllegalArgumentException("El request de planeación no puede ser nulo"));
        }

        String resolvedContextId = (contextId == null || contextId.isBlank())
                ? UUID.randomUUID().toString()
                : contextId.trim();

        try {
            ProgramPlanningCommand command = ProgramPlanningCommand.fromRequest(request,
                    resolvedContextId);
            AgentCommand agentCommand = AgentCommand.nonBlocking(command.toCanonicalCommand(),
                    resolvedContextId);
            return trackAgentTaskUseCase.sendAndTrack(agentCommand);
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        }
    }
}
