package co.com.bancolombia.config;

import co.com.bancolombia.model.createworkitem.gateways.CreateWorkItemRepository;
import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.querybywiql.gateways.QueryByWiqlRepository;
import co.com.bancolombia.model.team.gateways.GetTeamFieldValuesRepository;
import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "co.com.bancolombia.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

    @Bean
    public GetTeamFieldValuesUseCase getTeamFieldValuesUseCase(
            GetTeamFieldValuesRepository getTeamFieldValuesRepository) {
        return new GetTeamFieldValuesUseCase(getTeamFieldValuesRepository);
    }

    @Bean
    public GetWorkItemUseCase getWorkItemUseCase(GetWorkItemRepository getWorkItemRepository) {
        return new GetWorkItemUseCase(getWorkItemRepository);

    }

    @Bean
    public CreateWorkItemUseCase createWorkItemUseCase(
            CreateWorkItemRepository createWorkItemRepository) {
        return new CreateWorkItemUseCase(createWorkItemRepository);
    }

    @Bean
    public UpdateWorkItemUseCase updateWorkItemUseCase(
            UpdateWorkItemRepository updateWorkItemRepository) {
        return new UpdateWorkItemUseCase(updateWorkItemRepository);
    }

    @Bean
    public QueryByWiqlUseCase queryByWiqlUseCase(QueryByWiqlRepository queryByWiqlRepository) {
        return new QueryByWiqlUseCase(queryByWiqlRepository);
    }

    @Bean
    public GetWorkItemsBatchUseCase getWorkItemsBatchUseCase(
            GetWorkItemsBatchRepository getWorkItemsBatchRepository) {
        return new GetWorkItemsBatchUseCase(getWorkItemsBatchRepository);
    }
}
