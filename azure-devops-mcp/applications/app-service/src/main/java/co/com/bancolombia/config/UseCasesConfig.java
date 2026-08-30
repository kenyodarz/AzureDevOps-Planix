package co.com.bancolombia.config;

import co.com.bancolombia.model.team.gateways.TeamScopePort;
import co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics;
import co.com.bancolombia.model.workitem.gateways.WorkItemCommandPort;
import co.com.bancolombia.model.workitem.gateways.WorkItemQueryPort;
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
    public GetTeamFieldValuesUseCase getTeamFieldValuesUseCase(TeamScopePort teamScopePort) {
        return new GetTeamFieldValuesUseCase(teamScopePort);
    }

    /**
     * Gemelo del anterior para las iteraciones: resuelve el {@code IterationPath} preguntándoselo a
     * Azure DevOps en lugar de fabricarlo con el año del calendario.
     *
     * <p>Desde la Fase 05 ambos reciben el <b>mismo</b> puerto: las dos consultas describen el
     * ámbito de un equipo y ya no viven en dos gateways de dos paquetes distintos (D-14).
     */
    @Bean
    public GetTeamIterationsUseCase getTeamIterationsUseCase(TeamScopePort teamScopePort) {
        return new GetTeamIterationsUseCase(teamScopePort);
    }

    @Bean
    public GetWorkItemUseCase getWorkItemUseCase(WorkItemQueryPort workItemQueryPort) {
        return new GetWorkItemUseCase(workItemQueryPort);

    }

    @Bean
    public CreateWorkItemUseCase createWorkItemUseCase(WorkItemCommandPort workItemCommandPort) {
        return new CreateWorkItemUseCase(workItemCommandPort);
    }

    @Bean
    public UpdateWorkItemUseCase updateWorkItemUseCase(WorkItemCommandPort workItemCommandPort) {
        return new UpdateWorkItemUseCase(workItemCommandPort);
    }

    @Bean
    public QueryByWiqlUseCase queryByWiqlUseCase(WorkItemQueryPort workItemQueryPort) {
        return new QueryByWiqlUseCase(workItemQueryPort);
    }

    @Bean
    public GetWorkItemsBatchUseCase getWorkItemsBatchUseCase(WorkItemQueryPort workItemQueryPort) {
        return new GetWorkItemsBatchUseCase(workItemQueryPort);
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
