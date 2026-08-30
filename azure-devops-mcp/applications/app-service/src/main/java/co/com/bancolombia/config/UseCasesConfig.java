package co.com.bancolombia.config;

import co.com.bancolombia.model.createworkitem.gateways.CreateWorkItemRepository;
import co.com.bancolombia.model.getworkitem.gateways.GetWorkItemRepository;
import co.com.bancolombia.model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository;
import co.com.bancolombia.model.iteration.gateways.GetTeamIterationsRepository;
import co.com.bancolombia.model.querybywiql.gateways.QueryByWiqlRepository;
import co.com.bancolombia.model.team.gateways.GetTeamFieldValuesRepository;
import co.com.bancolombia.model.updateworkitem.gateways.UpdateWorkItemRepository;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.listworkitems.ListWorkItemsByTeamAndSprintUseCase;
import co.com.bancolombia.usecase.listworkitems.ResolveTeamScopeUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.time.Clock;
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

    /**
     * Gemelo del anterior para las iteraciones: resuelve el {@code IterationPath} preguntándoselo a
     * Azure DevOps en lugar de fabricarlo con el año del calendario.
     */
    @Bean
    public GetTeamIterationsUseCase getTeamIterationsUseCase(
            GetTeamIterationsRepository getTeamIterationsRepository) {
        return new GetTeamIterationsUseCase(getTeamIterationsRepository);
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

    /**
     * Reloj del sistema, inyectado en lugar de invocarse estáticamente.
     *
     * <p>El repliegue por concatenación necesita el año del calendario. Calcularlo con
     * {@code LocalDate.now()} dentro del dominio lo ataba al reloj de la máquina y hacía imposible
     * probarlo de forma determinista, que es una de las razones por las que <b>D-09</b> sobrevivió
     * tanto tiempo sin que nadie pudiera reproducir el fallo del tablero vacío.
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Resolución del ámbito (AreaPath + IterationPath) con su repliegue medido.
     */
    @Bean
    public ResolveTeamScopeUseCase resolveTeamScopeUseCase(
            GetTeamFieldValuesUseCase getTeamFieldValuesUseCase,
            GetTeamIterationsUseCase getTeamIterationsUseCase,
            TeamScopeFallbackMetrics teamScopeFallbackMetrics,
            Clock clock) {
        return new ResolveTeamScopeUseCase(getTeamFieldValuesUseCase, getTeamIterationsUseCase,
                teamScopeFallbackMetrics, clock);
    }

    /**
     * El flujo compuesto, que en la Fase 04 bajó del entry-point a la capa de aplicación
     * (D-07, D-12).
     */
    @Bean
    public ListWorkItemsByTeamAndSprintUseCase listWorkItemsByTeamAndSprintUseCase(
            ResolveTeamScopeUseCase resolveTeamScopeUseCase,
            QueryByWiqlUseCase queryByWiqlUseCase) {
        return new ListWorkItemsByTeamAndSprintUseCase(resolveTeamScopeUseCase,
                queryByWiqlUseCase);
    }
}
