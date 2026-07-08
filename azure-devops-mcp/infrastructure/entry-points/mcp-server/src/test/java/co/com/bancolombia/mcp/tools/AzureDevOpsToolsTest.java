package co.com.bancolombia.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AzureDevOpsToolsTest {

    @Mock
    private GetWorkItemUseCase getWorkItemUseCase;
    @Mock
    private CreateWorkItemUseCase createWorkItemUseCase;
    @Mock
    private UpdateWorkItemUseCase updateWorkItemUseCase;
    @Mock
    private QueryByWiqlUseCase queryByWiqlUseCase;
    @Mock
    private GetWorkItemsBatchUseCase getWorkItemsBatchUseCase;
    @Mock
    private GetTeamFieldValuesUseCase getTeamFieldValuesUseCase;

    @InjectMocks
    private AzureDevOpsTools azureDevOpsTools;

    @Test
    void givenParams_whenListWorkItemsByTeamAndSprint_thenReturnsQueryResult() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA")
                        .build()));
        when(queryByWiqlUseCase.queryByWiql(eq(org), eq(proj), any(WiqlQuery.class),
                eq(apiVersion)))
                .thenReturn(Mono.just(mockResult));

        // Act (WHEN)
        Mono<WiqlResult> result = azureDevOpsTools.listWorkItemsByTeamAndSprint(org, proj, team,
                sprint, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenParamsWithDoubleBackslashes_whenListWorkItemsByTeamAndSprint_thenReturnsCorrectQuery() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String teamWithDoubleBackslashes = "Vicepresidencia Servicios de Tecnología\\\\EQU1096 - EXODIA";
        String sprintWithDoubleBackslashes = "Vicepresidencia Servicios de Tecnología\\\\2026\\\\Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, "EQU1096 - EXODIA"))
                .thenReturn(Mono.error(new RuntimeException("API error")));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that double backslashes were successfully replaced with a single backslash
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA"));
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\2026\\Sprint 247"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = azureDevOpsTools.listWorkItemsByTeamAndSprint(org, proj,
                teamWithDoubleBackslashes, sprintWithDoubleBackslashes, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenParamsWithoutProjectPrefix_whenListWorkItemsByTeamAndSprint_thenPrependsProjectName() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String teamWithoutProject = "EQU1096 - EXODIA";
        String sprintWithoutProject = "2026\\Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, teamWithoutProject))
                .thenReturn(Mono.error(new RuntimeException("API error")));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that project prefix was correctly prepended
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA"));
            assertTrue(argQuery.getQuery()
                    .contains("Vicepresidencia Servicios de Tecnología\\2026\\Sprint 247"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = azureDevOpsTools.listWorkItemsByTeamAndSprint(org, proj,
                teamWithoutProject, sprintWithoutProject, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenCustomWorkItemTypes_whenListWorkItemsByTeamAndSprint_thenFormatsTypesCorrectly() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String customTypes = "User Story, Task, Bug";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA")
                        .build()));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that the work item types are formatted properly as SQL literals
            assertTrue(argQuery.getQuery()
                    .contains("[System.WorkItemType] IN ('Historia de Usuario','Task','Bug')"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = azureDevOpsTools.listWorkItemsByTeamAndSprint(org, proj, team,
                sprint, customTypes, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }

    @Test
    void givenDynamicAreaPath_whenListWorkItemsByTeamAndSprint_thenUsesResolvedAreaPath() {
        // Arrange (GIVEN)
        String org = "grupobancolombia";
        String proj = "Vicepresidencia Servicios de Tecnología";
        String team = "EQU1096 - EXODIA";
        String sprint = "Sprint 247";
        String apiVersion = "7.0";

        WiqlResult mockResult = WiqlResult.builder().build();
        when(getTeamFieldValuesUseCase.getTeamFieldValues(org, proj, team))
                .thenReturn(Mono.just(TeamFieldValues.builder()
                        .defaultValue("DynamicAreaPath\\SpecialBranch\\Exodia").build()));
        when(queryByWiqlUseCase.queryByWiql(
                eq(org),
                eq(proj),
                any(WiqlQuery.class),
                eq(apiVersion)
        )).thenAnswer(invocation -> {
            WiqlQuery argQuery = invocation.getArgument(2);
            // Verify that the dynamically resolved AreaPath is used
            assertTrue(argQuery.getQuery()
                    .contains("[System.AreaPath] = 'DynamicAreaPath\\SpecialBranch\\Exodia'"));
            return Mono.just(mockResult);
        });

        // Act (WHEN)
        Mono<WiqlResult> result = azureDevOpsTools.listWorkItemsByTeamAndSprint(org, proj, team,
                sprint, null, apiVersion);

        // Assert (THEN)
        StepVerifier.create(result)
                .expectNext(mockResult)
                .verifyComplete();
    }
}

