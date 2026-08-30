package co.com.bancolombia.config;

import co.com.bancolombia.model.agent.gateways.AgentGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.dashboard.gateways.DashboardFallbackPort;
import co.com.bancolombia.model.dashboard.gateways.ReportStoragePort;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.usecase.agent.TrackAgentTaskUseCase;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de los casos de uso.
 *
 * <p>Todos los beans se declaran de forma explícita. La versión anterior combinaba un
 * {@code @ComponentScan} con filtros por expresión regular y una exclusión a mano de
 * {@code AgentChatUseCase}: esa exclusión era la señal de que aquel caso de uso no encajaba aquí
 * —replicaba el cerebro del agente dentro del BFF— y se eliminó en la Fase 02.
 */
@Configuration
public class UseCasesConfig {

    private final String defaultOrg;
    private final String defaultProject;

    /**
     * Repliegue al tablero simulado (B-07).
     *
     * <p><b>Fase 08 (D-32, B-11).</b> Antes, sin valor explícito, esto se deducía de
     * {@code spring.ai.mcp.client.enabled}: si el cliente MCP estaba apagado, se repliega. Esa
     * bandera describía un cliente MCP que el BFF ya no tiene —el módulo se eliminó en esta fase—,
     * así que gobernar con ella el tablero simulado era hacer depender una decisión de negocio de
     * la configuración de un protocolo ausente. Ahora la propiedad es <b>explícita y única</b>.
     *
     * <p>El valor efectivo <b>no cambia</b>: en local, {@code spring.ai.mcp.client.enabled} valía
     * {@code false} y el repliegue quedaba encendido; el {@code application.yaml} declara ahora
     * {@code dashboard.mock-fallback.enabled} con ese mismo {@code true} por defecto y una variable
     * de entorno para apagarlo. El valor por defecto <b>en código</b> es {@code false}, que es el
     * seguro: si nadie declara nada, no se sirve un tablero de mentira.
     */
    private final boolean mockFallbackEnabled;

    /**
     * Inyección por constructor, no por campo (Rule_2.7). Los cuatro {@code @Value} que había aquí
     * eran campos no finales y sumaban cuatro violaciones de la regla; la Fase 05 ya tropezó con
     * esto mismo al añadir dos propiedades al {@code Handler}.
     */
    public UseCasesConfig(
            @Value("${azure-devops.default-org:grupobancolombia}") String defaultOrg,
            @Value("${azure-devops.default-project:Vicepresidencia Servicios de Tecnología}")
            String defaultProject,
            @Value("${dashboard.mock-fallback.enabled:false}") boolean mockFallbackEnabled) {
        this.defaultOrg = defaultOrg;
        this.defaultProject = defaultProject;
        this.mockFallbackEnabled = mockFallbackEnabled;
    }

    @Bean
    public IngestPlanningSpecUseCase ingestPlanningSpecUseCase(
            PlanningVectorStorePort planningVectorStorePort) {
        return new IngestPlanningSpecUseCase(planningVectorStorePort);
    }

    @Bean
    public SearchPlanningSpecUseCase searchPlanningSpecUseCase(
            PlanningVectorStorePort planningVectorStorePort) {
        return new SearchPlanningSpecUseCase(planningVectorStorePort);
    }

    @Bean
    public ManagePlanningUseCase managePlanningUseCase(
            PlanningVectorStorePort planningVectorStorePort) {
        return new ManagePlanningUseCase(planningVectorStorePort);
    }

    /**
     * Desde la Fase 04 el caso de uso ya no lleva los prompts ni el JSON simulado dentro: los pide
     * a {@link PromptTemplatePort} y {@link DashboardFallbackPort}, cuyos adaptadores viven en este
     * mismo módulo y se descubren por componente. Desde la Fase 06 escribe sus reportes a través de
     * {@link ReportStoragePort} (D-10) en lugar de tocar el disco por su cuenta.
     */
    @Bean
    public DevOpsDashboardUseCase devOpsDashboardUseCase(AgentGateway agentGateway,
            PromptTemplatePort promptTemplatePort,
            DashboardFallbackPort dashboardFallbackPort,
            ReportStoragePort reportStoragePort) {
        return new DevOpsDashboardUseCase(agentGateway, promptTemplatePort, dashboardFallbackPort,
                reportStoragePort, defaultOrg, defaultProject, mockFallbackEnabled);
    }

    /**
     * Seguimiento de las tareas que el agente ejecuta, para que el frontend pueda mostrar el estado
     * de cada una.
     */
    @Bean
    public TrackAgentTaskUseCase trackAgentTaskUseCase(AgentGateway agentGateway,
            TaskStoreGateway taskStoreGateway) {
        return new TrackAgentTaskUseCase(agentGateway, taskStoreGateway);
    }
}
