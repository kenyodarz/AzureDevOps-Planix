package co.com.bancolombia.mcp.tools;

import co.com.bancolombia.model.team.TeamFieldValues;
import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WorkItem;
import co.com.bancolombia.model.workitem.WorkItemsBatchRequest;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import co.com.bancolombia.usecase.team.GetTeamFieldValuesUseCase;
import co.com.bancolombia.usecase.updateworkitem.UpdateWorkItemUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Herramientas MCP expuestas como beans de Spring AI para interactuar con Azure DevOps (WIT & WIQL).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureDevOpsTools {

    private final GetWorkItemUseCase getWorkItemUseCase;
    private final CreateWorkItemUseCase createWorkItemUseCase;
    private final UpdateWorkItemUseCase updateWorkItemUseCase;
    private final QueryByWiqlUseCase queryByWiqlUseCase;
    private final GetWorkItemsBatchUseCase getWorkItemsBatchUseCase;
    private final GetTeamFieldValuesUseCase getTeamFieldValuesUseCase;

    /**
     * Obtener un Work Item por ID.
     */
    @McpTool(
            name = "getWorkItem",
            description = "Recupera los detalles de un elemento de trabajo específico (User Story, Task, Issue) en Azure DevOps utilizando su identificador numérico."
    )
//    @PreAuthorize("hasRole('MCP.AZURE_DEVOPS.READ')")
    public Mono<WorkItem> getWorkItem(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "ID único numérico del Work Item", required = true) int id,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getWorkItem] ejecutada para ID: {}", id);
        return getWorkItemUseCase.getWorkItem(organization, project, id, apiVersion);
    }

    /**
     * Crear un nuevo Work Item.
     */
    @McpTool(
            name = "createWorkItem",
            description = "Crea un nuevo elemento de trabajo (como User Story, Task, Issue) en Azure DevOps utilizando una lista de operaciones JSON Patch."
    )
    @PreAuthorize("hasRole('MCP.AZURE_DEVOPS.WRITE')")
    public Mono<WorkItem> createWorkItem(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Tipo de Work Item a crear (ej. User Story, Task, Issue)", required = true) String type,
            @McpToolParam(description = "Lista de operaciones JSON Patch para inicializar los campos (ej. [{op: 'add', path: '/fields/System.Title', value: '...' }])", required = true) List<JsonPatchOperation> patch,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [createWorkItem] ejecutada para tipo: {}", type);
        return createWorkItemUseCase.createWorkItem(organization, project, type, patch, apiVersion);
    }

    /**
     * Actualizar un Work Item.
     */
    @McpTool(
            name = "updateWorkItem",
            description = "Actualiza los campos o relaciones (vínculos jerárquicos de padre-hijo) de un Work Item existente usando JSON Patch."
    )
    @PreAuthorize("hasRole('MCP.AZURE_DEVOPS.WRITE')")
    public Mono<WorkItem> updateWorkItem(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "ID único numérico del Work Item a actualizar", required = true) int id,
            @McpToolParam(description = "Lista de operaciones JSON Patch para aplicar cambios (ej. [{op: 'add', path: '/relations/-', value: {...} }])", required = true) List<JsonPatchOperation> patch,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [updateWorkItem] ejecutada para ID: {}", id);
        return updateWorkItemUseCase.updateWorkItem(organization, project, id, patch, apiVersion);
    }

    /**
     * Consultar Work Items usando WIQL.
     */
    // @McpTool(
    //         name = "queryByWiql",
    //         description = "Realiza una consulta estructurada en lenguaje WIQL (Work Item Query Language) para buscar y listar elementos de trabajo."
    // )
//    @PreAuthorize("hasRole('MCP.AZURE_DEVOPS.READ')")
    public Mono<WiqlResult> queryByWiql(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Query estructurado en lenguaje WIQL", required = true) String query,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.0)", required = false) String apiVersion) {
        log.info("MCP Tool [queryByWiql] ejecutada");
        WiqlQuery wiqlQuery = WiqlQuery.builder().query(query).build();
        return queryByWiqlUseCase.queryByWiql(organization, project, wiqlQuery, apiVersion);
    }

    /**
     * Busca y lista los elementos de trabajo (User Stories y Habilitadores) asignados a una
     * célula/equipo y sprint específicos en Azure DevOps.
     */
    @McpTool(
            name = "listWorkItemsByTeamAndSprint",
            description = "Busca y lista los elementos de trabajo (User Stories y Habilitadores) asignados a una célula/equipo y sprint específicos en Azure DevOps."
    )
    public Mono<WiqlResult> listWorkItemsByTeamAndSprint(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. grupobancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps (ej. Vicepresidencia Servicios de Tecnología)", required = true) String project,
            @McpToolParam(description = "Ruta completa de área de la célula (ej. Vicepresidencia Servicios de Tecnología\\EQU1096 - EXODIA)", required = true) String teamName,
            @McpToolParam(description = "Ruta completa de iteración del sprint (ej. Vicepresidencia Servicios de Tecnología\\2026\\Sprint 247)", required = true) String sprintName,
            @McpToolParam(description = "Tipos de elementos de trabajo separados por coma (por defecto: Historia de Usuario, Habilitador)", required = false) String workItemTypes,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.0)", required = false) String apiVersion
    ) {
        log.info("MCP Tool [listWorkItemsByTeamAndSprint] ejecutada");
        String cleanTeam = teamName.replace("\\\\", "\\");
        String cleanSprint = sprintName.replace("\\\\", "\\");

        String teamNameParam = cleanTeam;
        if (cleanTeam.contains("\\")) {
            teamNameParam = cleanTeam.substring(cleanTeam.lastIndexOf("\\") + 1).trim();
        }

        return getTeamFieldValuesUseCase.getTeamFieldValues(organization, project, teamNameParam)
                .map(TeamFieldValues::getDefaultValue)
                .onErrorResume(e -> {
                    log.warn(
                            "No se pudo obtener el AreaPath dinámico para la célula: {}. Se usará la normalización por defecto.",
                            cleanTeam, e);
                    return Mono.just(resolveAreaPath(project, cleanTeam));
                })
                .flatMap(finalAreaPath -> {
                    log.info("Resolución de AreaPath final para célula {}: {}", cleanTeam,
                            finalAreaPath);
                    String finalIterationPath = resolveIterationPath(project, cleanSprint);
                    String typesStr = parseWorkItemTypes(workItemTypes);

                    String query = String.format(
                            "SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project AND [System.IterationPath] = '%s' AND [System.AreaPath] = '%s' AND [System.WorkItemType] IN (%s) ORDER BY [System.Id]",
                            finalIterationPath, finalAreaPath, typesStr
                    );
                    log.info("WIQL Query construida: {}", query);

                    WiqlQuery wiqlQuery = WiqlQuery.builder().query(query).build();
                    return queryByWiqlUseCase.queryByWiql(organization, project, wiqlQuery,
                            apiVersion);
                });
    }

    private String resolveAreaPath(String project, String cleanTeam) {
        if (!cleanTeam.startsWith(project)) {
            return project + '\\' + cleanTeam;
        }
        return cleanTeam;
    }

    private String resolveIterationPath(String project, String cleanSprint) {
        if (!cleanSprint.startsWith(project)) {
            if (cleanSprint.matches("Sprint \\d+")) {
                int currentYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                        .getYear();
                return project + '\\' + currentYear + '\\' + cleanSprint;
            } else {
                return project + '\\' + cleanSprint;
            }
        }
        return cleanSprint;
    }

    private String parseWorkItemTypes(String workItemTypes) {
        if (workItemTypes == null || workItemTypes.isBlank()) {
            return "'Historia de Usuario','Habilitador'";
        }
        return java.util.Arrays.stream(workItemTypes.split(","))
                .map(String::trim)
                .map(this::normalizeWorkItemType)
                .map(this::quoteWorkItemType)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String normalizeWorkItemType(String type) {
        return "User Story".equalsIgnoreCase(type) ? "Historia de Usuario" : type;
    }

    private String quoteWorkItemType(String type) {
        if (type.startsWith("'") && type.endsWith("'")) {
            return type;
        }
        return "'" + type + "'";
    }

    /**
     * Obtener Work Items en lote.
     */
    @McpTool(
            name = "getWorkItemsBatch",
            description = "Obtiene de manera masiva los detalles de múltiples elementos de trabajo a partir de sus IDs en una sola llamada."
    )
//    @PreAuthorize("hasRole('MCP.AZURE_DEVOPS.READ')")
    public Mono<List<WorkItem>> getWorkItemsBatch(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Lista de IDs únicos numéricos de Work Items a consultar", required = true) List<Integer> ids,
            @McpToolParam(description = "Lista de campos específicos a retornar en la respuesta", required = false) List<String> fields,
            @McpToolParam(description = "Expansión de relaciones o enlaces asociados (None, Relations, Fields, Links, All)", required = false) String expand,
            @McpToolParam(description = "Política de error si algún elemento no existe (Fail, Omit)", required = false) String errorPolicy,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getWorkItemsBatch] ejecutada para ids: {}", ids);
        WorkItemsBatchRequest request = WorkItemsBatchRequest.builder()
                .ids(ids)
                .fields(fields)
                .expand(expand != null && !expand.isBlank() ? expand : "None")
                .errorPolicy(errorPolicy != null && !errorPolicy.isBlank() ? errorPolicy : "Omit")
                .build();
        return getWorkItemsBatchUseCase.getWorkItemsBatch(organization, project, request, apiVersion);
    }
}
