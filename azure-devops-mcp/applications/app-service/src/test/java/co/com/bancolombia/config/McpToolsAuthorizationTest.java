package co.com.bancolombia.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.bancolombia.mcp.security.McpRoles;
import co.com.bancolombia.mcp.tools.AzureDevOpsTools;
import co.com.bancolombia.mcp.tools.HealthTool;
import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.iteration.GetTeamIterationsUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

/**
 * Prueba que las ocho tools tienen autorización <b>declarada, compilada y ejecutada</b>.
 *
 * <p>Antes de la Fase 02 tres {@code @PreAuthorize} estaban comentados y
 * {@code listWorkItemsByTeamAndSprint} —la tool más usada— no tenía ninguno: el día del Service
 * Principal se habrían estrenado en producción sin haberse ejecutado jamás. Aquí se ejecutan los
 * dos caminos, con rol y sin rol, sobre el proxy real de seguridad de métodos.
 */
class McpToolsAuthorizationTest {

    private static AnnotationConfigApplicationContext context;
    private static AzureDevOpsTools tools;
    private static HealthTool healthTool;

    @BeforeAll
    static void setUp() {
        context = new AnnotationConfigApplicationContext(SecuredToolsConfig.class);
        tools = context.getBean(AzureDevOpsTools.class);
        healthTool = context.getBean(HealthTool.class);
    }

    @AfterAll
    static void tearDown() {
        context.close();
    }

    // ---------------------------------------------------------------- lectura

    @Test
    @DisplayName("GIVEN identidad con rol de lectura WHEN se invocan las tools de consulta THEN se permiten")
    void givenReadRole_whenQueryTools_thenAllowed() {
        stubReadUseCases();

        verifyAllowed(tools.getWorkItem("org", "proj", 1, null), readerIdentity());
        verifyAllowed(tools.getWorkItemsBatch("org", "proj", List.of(1), null, null, null, null),
                readerIdentity());
        verifyAllowed(tools.listWorkItemsByTeamAndSprint("org", "proj", "EQU1096 - EXODIA",
                "Sprint 247", null, null), readerIdentity());
        verifyAllowed(tools.queryByWiql("org", "proj", "SELECT 1", null), readerIdentity());
    }

    @Test
    @DisplayName("GIVEN identidad sin roles WHEN se invocan las tools de consulta THEN se deniegan")
    void givenNoRoles_whenQueryTools_thenDenied() {
        verifyDenied(tools.getWorkItem("org", "proj", 1, null));
        verifyDenied(tools.getWorkItemsBatch("org", "proj", List.of(1), null, null, null, null));
        verifyDenied(tools.queryByWiql("org", "proj", "SELECT 1", null));
    }

    @Test
    @DisplayName("GIVEN identidad sin roles WHEN listWorkItemsByTeamAndSprint THEN se deniega (la tool que nunca tuvo autorizacion)")
    void givenNoRoles_whenListWorkItemsByTeamAndSprint_thenDenied() {
        verifyDenied(tools.listWorkItemsByTeamAndSprint("org", "proj", "EQU1096 - EXODIA",
                "Sprint 247", null, null));
    }

    @Test
    @DisplayName("GIVEN identidad con rol de escritura WHEN se invoca una tool de lectura THEN se deniega")
    void givenOnlyWriteRole_whenQueryTool_thenDenied() {
        StepVerifier.create(tools.getWorkItem("org", "proj", 1, null)
                        .contextWrite(identity(McpRoles.ROLE_WRITE)))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    // -------------------------------------------------------------- escritura

    @Test
    @DisplayName("GIVEN identidad con rol de escritura WHEN se invocan las tools de mutacion THEN se permiten")
    void givenWriteRole_whenCommandTools_thenAllowed() {
        stubWriteUseCases();

        verifyAllowed(tools.createWorkItem("org", "proj", "Task", List.of(), null),
                writerIdentity());
        verifyAllowed(tools.updateWorkItem("org", "proj", 1, List.of(), null), writerIdentity());
    }

    @Test
    @DisplayName("GIVEN identidad con solo rol de lectura WHEN se invocan las tools de mutacion THEN se deniegan")
    void givenOnlyReadRole_whenCommandTools_thenDenied() {
        StepVerifier.create(tools.createWorkItem("org", "proj", "Task", List.of(), null)
                        .contextWrite(readerIdentity()))
                .expectError(AccessDeniedException.class)
                .verify();

        StepVerifier.create(tools.updateWorkItem("org", "proj", 1, List.of(), null)
                        .contextWrite(readerIdentity()))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    // ------------------------------------------------------------ sondas vida

    @Test
    @DisplayName("GIVEN ninguna identidad WHEN se invocan las sondas de vida THEN se permiten por politica declarada")
    void givenNoIdentity_whenHealthTools_thenAllowedByExplicitPolicy() {
        StepVerifier.create(healthTool.checkHealth())
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(healthTool.getServerInfo())
                .expectNextCount(1)
                .verifyComplete();
    }

    // ------------------------------------------------------------- utilidades

    private void verifyAllowed(Mono<?> invocation, Context identity) {
        StepVerifier.create(invocation.contextWrite(identity))
                .expectNextCount(1)
                .verifyComplete();
    }

    private void verifyDenied(Mono<?> invocation) {
        StepVerifier.create(invocation.contextWrite(identity()))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    private Context readerIdentity() {
        return identity(McpRoles.ROLE_READ);
    }

    private Context writerIdentity() {
        return identity(McpRoles.ROLE_WRITE);
    }

    private Context identity(String... authorities) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "agente-mcp", "n/a", AuthorityUtils.createAuthorityList(authorities));
        return ReactiveSecurityContextHolder.withAuthentication(authentication);
    }

    private void stubReadUseCases() {
        when(context.getBean(GetWorkItemUseCase.class)
                .getWorkItem(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.just(WorkItem.builder().build()));
        when(context.getBean(GetWorkItemsBatchUseCase.class)
                .getWorkItemsBatch(anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(List.of(WorkItem.builder().build())));
        when(context.getBean(GetTeamFieldValuesUseCase.class)
                .getTeamFieldValues(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(TeamFieldValues.builder().defaultValue("area").build()));
        when(context.getBean(GetTeamIterationsUseCase.class)
                .resolveIterationPath(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just("iteracion"));
        when(context.getBean(QueryByWiqlUseCase.class)
                .queryByWiql(anyString(), anyString(), any(WiqlQuery.class), any()))
                .thenReturn(Mono.just(WiqlResult.builder().build()));
    }

    private void stubWriteUseCases() {
        when(context.getBean(CreateWorkItemUseCase.class)
                .createWorkItem(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(WorkItem.builder().build()));
        when(context.getBean(UpdateWorkItemUseCase.class)
                .updateWorkItem(anyString(), anyString(), anyInt(), any(), any()))
                .thenReturn(Mono.just(WorkItem.builder().build()));
    }

    /**
     * Contexto mínimo con seguridad de métodos activa: es el mismo mecanismo que aplica en
     * producción, sin levantar la aplicación completa.
     */
    @Configuration
    @EnableReactiveMethodSecurity
    static class SecuredToolsConfig {

        @Bean
        GetWorkItemUseCase getWorkItemUseCase() {
            return mock(GetWorkItemUseCase.class);
        }

        @Bean
        CreateWorkItemUseCase createWorkItemUseCase() {
            return mock(CreateWorkItemUseCase.class);
        }

        @Bean
        UpdateWorkItemUseCase updateWorkItemUseCase() {
            return mock(UpdateWorkItemUseCase.class);
        }

        @Bean
        QueryByWiqlUseCase queryByWiqlUseCase() {
            return mock(QueryByWiqlUseCase.class);
        }

        @Bean
        GetWorkItemsBatchUseCase getWorkItemsBatchUseCase() {
            return mock(GetWorkItemsBatchUseCase.class);
        }

        @Bean
        GetTeamFieldValuesUseCase getTeamFieldValuesUseCase() {
            return mock(GetTeamFieldValuesUseCase.class);
        }

        @Bean
        GetTeamIterationsUseCase getTeamIterationsUseCase() {
            return mock(GetTeamIterationsUseCase.class);
        }

        @Bean
        AzureDevOpsTools azureDevOpsTools(GetWorkItemUseCase getWorkItemUseCase,
                CreateWorkItemUseCase createWorkItemUseCase,
                UpdateWorkItemUseCase updateWorkItemUseCase,
                QueryByWiqlUseCase queryByWiqlUseCase,
                GetWorkItemsBatchUseCase getWorkItemsBatchUseCase,
                GetTeamFieldValuesUseCase getTeamFieldValuesUseCase,
                GetTeamIterationsUseCase getTeamIterationsUseCase) {
            return new AzureDevOpsTools(getWorkItemUseCase, createWorkItemUseCase,
                    updateWorkItemUseCase, queryByWiqlUseCase, getWorkItemsBatchUseCase,
                    getTeamFieldValuesUseCase, getTeamIterationsUseCase);
        }

        @Bean
        HealthTool healthTool() {
            return new HealthTool();
        }
    }
}
