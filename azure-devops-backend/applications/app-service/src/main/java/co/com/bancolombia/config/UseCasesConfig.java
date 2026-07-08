package co.com.bancolombia.config;

import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import co.com.bancolombia.usecase.dashboard.DevOpsDashboardUseCase;
import co.com.bancolombia.usecase.ingestplanning.IngestPlanningSpecUseCase;
import co.com.bancolombia.usecase.manageplanning.ManagePlanningUseCase;
import co.com.bancolombia.usecase.searchplanning.SearchPlanningSpecUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "co.com.bancolombia.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = co.com.bancolombia.usecase.chat.AgentChatUseCase.class)
        },
        useDefaultFilters = false)
public class UseCasesConfig {

    @Value("${azure-devops.default-org:grupobancolombia}")
    private String defaultOrg;

    @Value("${azure-devops.default-project:Vicepresidencia Servicios de Tecnología}")
    private String defaultProject;

    @Value("${spring.ai.mcp.client.enabled:true}")
    private boolean isMcpEnabled;

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

    @Bean
    public DevOpsDashboardUseCase devOpsDashboardUseCase(co.com.bancolombia.model.chat.gateways.ChatGateway chatGateway) {
        return new DevOpsDashboardUseCase(chatGateway, defaultOrg, defaultProject, isMcpEnabled);
    }
}
