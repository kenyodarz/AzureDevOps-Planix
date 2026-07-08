package co.com.bancolombia.config;

import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.ChatGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.usecase.chat.AgentChatUseCase;
import java.nio.charset.StandardCharsets;
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

    @Value("${azure-devops.default-org:grupobancolombia}")
    private String defaultOrg;

    @Value("${azure-devops.default-project:Vicepresidencia Servicios de Tecnología}")
    private String defaultProject;

    @Bean
    public AgentChatUseCase agentChatUseCase(ChatGateway chatGateway,
            AgentResponseGateway agentResponseGateway, TaskStoreGateway taskStoreGateway,
            PlanningVectorStorePort planningVectorStorePort,
            @Value("classpath:Plantilla_HU_HA.md") Resource templateResource,
            @Value("classpath:HISTORIA_USUARIO.md") Resource agileGuideResource,
            @Value("classpath:AUDITORIA_CALIDAD_HU.md") Resource qualityAuditResource) {
        String templateContent = readResource(templateResource);
        String agileGuideContent = readResource(agileGuideResource);
        String qualityAuditContent = readResource(qualityAuditResource);
        return new AgentChatUseCase(chatGateway, agentResponseGateway, taskStoreGateway,
                templateContent, planningVectorStorePort, agileGuideContent, defaultOrg,
                defaultProject, qualityAuditContent);
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
