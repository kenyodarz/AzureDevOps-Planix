package co.com.bancolombia.mcp.tools;

import co.com.bancolombia.mcp.dto.JsonPatchOperationInput;
import co.com.bancolombia.mcp.dto.McpResponseMapper;
import co.com.bancolombia.mcp.dto.McpToolDtoMapper;
import co.com.bancolombia.mcp.dto.WiqlResultResponse;
import co.com.bancolombia.mcp.dto.WorkItemResponse;
import co.com.bancolombia.mcp.dto.WorkItemsBatchInput;
import co.com.bancolombia.mcp.error.McpErrorTranslator;
import co.com.bancolombia.mcp.security.McpRoles;
import co.com.bancolombia.model.workitem.ListWorkItemsCommand;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.usecase.createworkitem.CreateWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitem.GetWorkItemUseCase;
import co.com.bancolombia.usecase.getworkitemsbatch.GetWorkItemsBatchUseCase;
import co.com.bancolombia.usecase.listworkitems.ListWorkItemsByTeamAndSprintUseCase;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
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
 *
 * <p><b>Cero lógica de negocio</b> ({@code spring-rules.md}, {@code entry-points}). Cada método
 * traduce protocolo MCP y delega en un caso de uso. Desde la Fase 04, la construcción de la
 * sentencia WIQL, la resolución de rutas y la normalización de los tipos de elemento de trabajo
 * viven en {@code domain}, no aquí.
 *
 * <p><b>Fase 06.</b> Cada operación termina en {@link McpErrorTranslator}, que da al error la forma
 * {@code CODIGO: mensaje neutro} que fijó DP-06 §0.1(a). <b>No es lógica de negocio</b>: es
 * traducción de protocolo, exactamente igual que el {@code McpToolDtoMapper} de la Fase 03. La
 * decisión de <b>qué</b> fallo es cada cosa se tomó una capa más abajo, en el adaptador.
 *
 * <p>⚠️ {@code listWorkItemsByTeamAndSprint} no es una excepción a esto, pero sí un caso a tener
 * presente: el repliegue de rutas de <b>DP-04</b> absorbe los fallos de resolución <b>dentro</b> del
 * caso de uso, así que nunca llegan hasta aquí. Es lo que ratificó <b>DP-06 §0.1(d)</b>.
 *
 * <p><b>Fase 07 (DP-07 §0.2(a)).</b> Las seis tools ya <b>no devuelven modelos de dominio</b>: cada
 * una termina en {@link McpResponseMapper}, que traduce al DTO de respuesta correspondiente. Era la
 * última frontera abierta —la Fase 03 cerró la de entrada y la de salida hacia Azure DevOps, pero el
 * retorno seguía serializando {@code domain/model} tal cual ({@code CONTRATO-MCP.md} §3.4)—, y es lo
 * que ha permitido convertir el modelo en objetos de valor inmutables sin tocar el JSON público.
 * <b>Los nombres de campo emitidos son idénticos</b>, congelados por
 * {@code McpResponsePayloadCharacterizationTest}.
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
    private final ListWorkItemsByTeamAndSprintUseCase listWorkItemsByTeamAndSprintUseCase;

    @McpTool(
            name = "getWorkItem",
            description = "Recupera los detalles de un elemento de trabajo específico (User Story, Task, Issue) en Azure DevOps utilizando su identificador numérico."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<WorkItemResponse> getWorkItem(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "ID único numérico del Work Item", required = true) int id,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getWorkItem] ejecutada para ID: {}", id);
        return getWorkItemUseCase.getWorkItem(organization, project, id, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("getWorkItem"));
    }

    @McpTool(
            name = "createWorkItem",
            description = "Crea un nuevo elemento de trabajo (como User Story, Task, Issue) en Azure DevOps utilizando una lista de operaciones JSON Patch."
    )
    @PreAuthorize(McpRoles.HAS_WRITE)
    public Mono<WorkItemResponse> createWorkItem(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Tipo de Work Item a crear (ej. User Story, Task, Issue)", required = true) String type,
            @McpToolParam(description = "Lista de operaciones JSON Patch para inicializar los campos (ej. [{op: 'add', path: '/fields/System.Title', value: '...' }])", required = true) List<JsonPatchOperationInput> patch,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [createWorkItem] ejecutada para tipo: {}", type);
        return createWorkItemUseCase.createWorkItem(organization, project, type,
                        McpToolDtoMapper.toDomain(patch), apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("createWorkItem"));
    }

    @McpTool(
            name = "updateWorkItem",
            description = "Actualiza los campos o relaciones (vínculos jerárquicos de padre-hijo) de un Work Item existente usando JSON Patch."
    )
    @PreAuthorize(McpRoles.HAS_WRITE)
    public Mono<WorkItemResponse> updateWorkItem(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "ID único numérico del Work Item a actualizar", required = true) int id,
            @McpToolParam(description = "Lista de operaciones JSON Patch para aplicar cambios (ej. [{op: 'add', path: '/relations/-', value: {...} }])", required = true) List<JsonPatchOperationInput> patch,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [updateWorkItem] ejecutada para ID: {}", id);
        return updateWorkItemUseCase.updateWorkItem(organization, project, id,
                        McpToolDtoMapper.toDomain(patch), apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("updateWorkItem"));
    }

    /**
     * Consulta Work Items mediante WIQL. <b>Operación interna: NO es una tool MCP.</b>
     *
     * <p><b>D-19, saldada en la Fase 08 (DP-08 §0.2(a)): se retira la tool.</b> Hasta ahora este
     * método arrastraba un {@code @McpTool} <b>comentado</b>, un camino muerto que nadie sabía si
     * estaba a medio construir o a medio retirar. Se decidió <b>no exponerla</b>, y el motivo es de
     * diseño, no de limpieza: una tool que acepta <b>WIQL crudo del cliente</b> convertiría al
     * llamante en el dueño de <i>cómo</i> se consulta Azure DevOps, que es exactamente lo contrario
     * de la misión declarada en §1 del plan maestro — <i>«el MCP es el dueño del cómo»</i>. Quien
     * necesite listar elementos de trabajo usa {@code listWorkItemsByTeamAndSprint}, que expresa la
     * <b>intención</b> y deja la sentencia en manos del dominio.
     *
     * <p><b>El método sigue vivo y no es código muerto:</b> lo invoca
     * {@code ListWorkItemsByTeamAndSprintUseCase} a través de {@code QueryByWiqlUseCase}, que es el
     * corazón del flujo compuesto. Conserva su {@code @PreAuthorize} para que quede protegido en
     * modo {@code ENFORCED} sin depender de que alguien recuerde añadirlo.
     */
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<WiqlResultResponse> queryByWiql(
            String organization,
            String project,
            String query,
            String apiVersion) {
        log.info("Consulta WIQL interna ejecutada");
        WiqlQuery wiqlQuery = WiqlQuery.builder().query(query).build();
        return queryByWiqlUseCase.queryByWiql(organization, project, wiqlQuery, apiVersion)
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("queryByWiql"));
    }

    @McpTool(
            name = "listWorkItemsByTeamAndSprint",
            description = "Busca y lista los elementos de trabajo (User Stories y Habilitadores) asignados a una célula/equipo y sprint específicos en Azure DevOps."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<WiqlResultResponse> listWorkItemsByTeamAndSprint(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. grupobancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps (ej. Vicepresidencia Servicios de Tecnología)", required = true) String project,
            @McpToolParam(description = "Nombre de la célula o su ruta de área completa (ej. EQU1096 - EXODIA)", required = true) String teamName,
            @McpToolParam(description = "Nombre del sprint o su ruta de iteración completa (ej. Sprint 247). Se resuelve consultando las iteraciones del equipo en Azure DevOps.", required = true) String sprintName,
            @McpToolParam(description = "Tipos de elementos de trabajo separados por coma (por defecto: Historia de Usuario, Habilitador)", required = false) String workItemTypes,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.0)", required = false) String apiVersion
    ) {
        log.info("MCP Tool [listWorkItemsByTeamAndSprint] ejecutada");
        return listWorkItemsByTeamAndSprintUseCase.execute(
                        ListWorkItemsCommand.of(organization, project, teamName, sprintName,
                                workItemTypes, apiVersion))
                .map(McpResponseMapper::toResponse)
                .onErrorMap(McpErrorTranslator.forTool("listWorkItemsByTeamAndSprint"));
    }

    @McpTool(
            name = "getWorkItemsBatch",
            description = "Obtiene de manera masiva los detalles de múltiples elementos de trabajo a partir de sus IDs en una sola llamada."
    )
    @PreAuthorize(McpRoles.HAS_READ)
    public Mono<List<WorkItemResponse>> getWorkItemsBatch(
            @McpToolParam(description = "Nombre de la organización en Azure DevOps (ej. GrupoBancolombia)", required = true) String organization,
            @McpToolParam(description = "Nombre o UUID del proyecto en Azure DevOps", required = true) String project,
            @McpToolParam(description = "Lista de IDs únicos numéricos de Work Items a consultar", required = true) List<Integer> ids,
            @McpToolParam(description = "Lista de campos específicos a retornar en la respuesta", required = false) List<String> fields,
            @McpToolParam(description = "Expansión de relaciones o enlaces asociados (None, Relations, Fields, Links, All)", required = false) String expand,
            @McpToolParam(description = "Política de error si algún elemento no existe (Fail, Omit)", required = false) String errorPolicy,
            @McpToolParam(description = "Versión de la API de Azure DevOps (por defecto 7.1)", required = false) String apiVersion) {
        log.info("MCP Tool [getWorkItemsBatch] ejecutada para ids: {}", ids);
        WorkItemsBatchInput input = WorkItemsBatchInput.builder()
                .ids(ids)
                .fields(fields)
                .expand(expand != null && !expand.isBlank() ? expand : "None")
                .errorPolicy(errorPolicy != null && !errorPolicy.isBlank() ? errorPolicy : "Omit")
                .build();
        return getWorkItemsBatchUseCase.getWorkItemsBatch(organization, project,
                        McpToolDtoMapper.toDomain(input), apiVersion)
                .map(McpResponseMapper::toResponses)
                .onErrorMap(McpErrorTranslator.forTool("getWorkItemsBatch"));
    }
}
