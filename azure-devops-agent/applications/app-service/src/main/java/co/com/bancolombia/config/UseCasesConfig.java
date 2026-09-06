package co.com.bancolombia.config;

import co.com.bancolombia.model.agent.AzureDevOpsScope;
import co.com.bancolombia.model.agent.CorporateKnowledge;
import co.com.bancolombia.model.agent.IntentResolver;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.prompt.gateways.PromptTemplatePort;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import co.com.bancolombia.usecase.chat.handler.ApprovalFlowHandler;
import co.com.bancolombia.usecase.chat.handler.ChatFlowDispatcher;
import co.com.bancolombia.usecase.chat.handler.ChatFlowHandler;
import co.com.bancolombia.usecase.chat.handler.DivisionFlowHandler;
import co.com.bancolombia.usecase.chat.handler.GeneralFlowHandler;
import co.com.bancolombia.usecase.chat.handler.PlanningDraftFlowHandler;
import co.com.bancolombia.usecase.chat.handler.ProgramPlanningFlowHandler;
import co.com.bancolombia.usecase.chat.handler.QualityAuditFlowHandler;
import co.com.bancolombia.usecase.chat.handler.RefinementFlowHandler;
import co.com.bancolombia.usecase.planning.ProgramPlanningUseCase;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.Resource;

@Configuration
@ComponentScan(basePackages = "co.com.bancolombia.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

    /**
     * Resolutor de intención: servicio de dominio sin estado ni dependencias, seguro como
     * singleton compartido.
     */
    @Bean
    public IntentResolver intentResolver() {
        return new IntentResolver();
    }

    // ─── Configuración tipada ──────────────────────────────────────────────────

    /**
     * Organización y proyecto sobre los que trabaja el agente.
     * <p>
     * Los valores se inyectan como <b>parámetros del método</b> y no como campos anotados: un
     * bean de configuración no debe tener estado mutable (ArchUnit {@code Rule_2.7}).
     */
    @Bean
    public AzureDevOpsScope azureDevOpsScope(
            @Value("${azure-devops.default-org:grupobancolombia}") String defaultOrg,
            @Value("${azure-devops.default-project:Vicepresidencia Servicios de Tecnología}")
            String defaultProject) {
        return new AzureDevOpsScope(defaultOrg, defaultProject);
    }

    /**
     * Documentación corporativa que alimenta los prompts. Cada recurso del classpath se lee
     * <b>una sola vez</b> al arrancar y se comparte con todos los handlers que lo necesitan.
     */
    @Bean
    public CorporateKnowledge corporateKnowledge(
            @Value("classpath:Plantilla_HU_HA.md") Resource templateResource,
            @Value("classpath:HISTORIA_USUARIO.md") Resource agileGuideResource,
            @Value("classpath:AUDITORIA_CALIDAD_HU.md") Resource qualityAuditResource) {
        return new CorporateKnowledge(
                readResource(templateResource),
                readResource(agileGuideResource),
                readResource(qualityAuditResource));
    }

    // ─── Handlers de flujo, uno por AgentIntent ────────────────────────────────

    @Bean
    public GeneralFlowHandler generalFlowHandler(ChatGateway chatGateway) {
        return new GeneralFlowHandler(chatGateway);
    }

    @Bean
    public QualityAuditFlowHandler qualityAuditFlowHandler(ChatGateway chatGateway,
            PromptTemplatePort promptTemplatePort, AzureDevOpsScope scope,
            CorporateKnowledge knowledge) {
        return new QualityAuditFlowHandler(chatGateway, promptTemplatePort, scope, knowledge);
    }

    @Bean
    public RefinementFlowHandler refinementFlowHandler(ChatGateway chatGateway,
            PromptTemplatePort promptTemplatePort, AzureDevOpsScope scope,
            CorporateKnowledge knowledge) {
        return new RefinementFlowHandler(chatGateway, promptTemplatePort, scope, knowledge);
    }

    @Bean
    public ApprovalFlowHandler approvalFlowHandler(ChatGateway chatGateway,
            PromptTemplatePort promptTemplatePort, CorporateKnowledge knowledge) {
        return new ApprovalFlowHandler(chatGateway, promptTemplatePort, knowledge);
    }

    @Bean
    public DivisionFlowHandler divisionFlowHandler(ChatGateway chatGateway,
            PromptTemplatePort promptTemplatePort, CorporateKnowledge knowledge) {
        return new DivisionFlowHandler(chatGateway, promptTemplatePort, knowledge);
    }

    @Bean
    public PlanningDraftFlowHandler planningDraftFlowHandler(ChatGateway chatGateway,
            PromptTemplatePort promptTemplatePort, SpecStoragePort specStoragePort) {
        return new PlanningDraftFlowHandler(chatGateway, promptTemplatePort,
                specStoragePort);
    }

    @Bean
    public ProgramPlanningFlowHandler programPlanningFlowHandler(
            ProgramPlanningUseCase programPlanningUseCase) {
        return new ProgramPlanningFlowHandler(programPlanningUseCase);
    }

    /**
     * Indexa los handlers por intención. Falla al arrancar si falta alguna o si hay duplicados.
     */
    @Bean
    public ChatFlowDispatcher chatFlowDispatcher(List<ChatFlowHandler> chatFlowHandlers) {
        return new ChatFlowDispatcher(chatFlowHandlers);
    }

    @Bean
    public AgentChatUseCase agentChatUseCase(AgentResponseGateway agentResponseGateway,
            TaskStoreGateway taskStoreGateway,
            IntentResolver intentResolver,
            ChatFlowDispatcher chatFlowDispatcher) {
        return new AgentChatUseCase(agentResponseGateway, taskStoreGateway, intentResolver,
                chatFlowDispatcher);
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo cargar la plantilla HU/HA desde los recursos", e);
        }
    }
}

