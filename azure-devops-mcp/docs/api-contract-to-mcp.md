# API Contract to MCP Mapping Matrix

## 1. Metadata

- Initiative name: MCP Azure DevOps Automatizador
- Source API: Azure DevOps Work Item Tracking REST API (7.0 & 7.1)
- Contract version: 7.1-preview.3
- Status: IMPLEMENTED

## 2. Source contract summary

- API type: REST
- Base path: `https://dev.azure.com/{organization}/{project}/_apis/wit`
- Authentication: Basic Auth (Personal Access Token)
- Payload format: JSON / JSON Patch (`application/json-patch+json`)

## 3. Assumptions

- Los clientes MCP se autentican con tokens Entra ID (Bearer token) emitidos para este servidor de recursos.
- El servidor MCP actúa como intermediario seguro y utiliza un PAT configurado o proporcionado en variables de entorno para llamar a las APIs de Azure DevOps.
- El protocolo de comunicación expone únicamente `@McpTool`s a través del starter de Spring AI MCP Server WebFlux.

## 4. Operation inventory

| ID | Source operation | Method / verb | Route or identifier | Objective | Notes |
|----|------------------|---------------|---------------------|-----------|-------|
| API-01 | Get Work Item | GET | `/_apis/wit/workItems/{id}` | Obtener detalles de una HU, HA, Task o Issue por su ID | Retorna el elemento y sus campos clave |
| API-02 | Create Work Item | POST | `/_apis/wit/workitems/{type}` | Crear una nueva HU, HA, Task o Issue con JSON Patch | El path `{type}` es parametrizado como `$User Story`, etc. |
| API-03 | Update Work Item | PATCH | `/_apis/wit/workitems/{id}` | Actualizar campos o establecer relaciones parent-child | Utiliza `application/json-patch+json` |
| API-04 | Query by WIQL | POST | `/_apis/wit/wiql` | Buscar elementos en el Sprint para un desarrollador por correo | Utiliza la sintaxis estructurada WIQL |
| API-05 | Batch Get Work Items | POST | `/_apis/wit/workitemsbatch` | Obtener en lote los detalles de los elementos de la consulta WIQL | Optimiza las llamadas de red limitando a 200 IDs |

## 5. MCP mapping rules

### Tool

Se mapearon las 5 operaciones como **Tools** debido a que:
1. Las operaciones de escritura (`create`, `update`) producen efectos colaterales (cambios en el tablero).
2. Las operaciones de lectura (`get`, `query`, `batch`) requieren validación de claims y control de tokens a nivel de autorización.

## 6. API -> MCP matrix

| API ID | MCP Primitive | Proposed MCP name | Associated use case | Main input | MCP output type | Required role | Sensitivity | Timeout | Fallback | Resilience |
|--------|---------------|-------------------|---------------------|------------|-----------------|---------------|-------------|---------|----------|------------|
| API-01 | Tool | `getWorkItem` | `GetWorkItemUseCase` | `id`, `organization`, `project` | JSON (WorkItem) | `ROLE_MCP.AZURE_DEVOPS.READ` | Media | 5000ms | Omitir/Excepción | CircuitBreaker |
| API-02 | Tool | `createWorkItem` | `CreateWorkItemUseCase` | `type`, `patch`, `organization`, `project` | JSON (WorkItem) | `ROLE_MCP.AZURE_DEVOPS.WRITE` | Alta | 5000ms | Excepción | CircuitBreaker |
| API-03 | Tool | `updateWorkItem` | `UpdateWorkItemUseCase` | `id`, `patch`, `organization`, `project` | JSON (WorkItem) | `ROLE_MCP.AZURE_DEVOPS.WRITE` | Alta | 5000ms | Excepción | CircuitBreaker |
| API-04 | Tool | `queryByWiql` | `QueryByWiqlUseCase` | `query`, `organization`, `project` | JSON (WiqlResult) | `ROLE_MCP.AZURE_DEVOPS.READ` | Media | 5000ms | Excepción | CircuitBreaker |
| API-05 | Tool | `getWorkItemsBatch` | `GetWorkItemsBatchUseCase` | `ids`, `fields`, `organization`, `project` | JSON (List<WorkItem>) | `ROLE_MCP.AZURE_DEVOPS.READ` | Media | 5000ms | Lista vacía / Excepción | CircuitBreaker |

## 7. Derived domain design

### Candidate models

- `co.com.bancolombia.model.workitem.WorkItem`
- `co.com.bancolombia.model.workitem.WorkItemRelation`
- `co.com.bancolombia.model.workitem.JsonPatchOperation`
- `co.com.bancolombia.model.workitem.WiqlQuery`
- `co.com.bancolombia.model.workitem.WiqlResult`
- `co.com.bancolombia.model.workitem.WorkItemReference`
- `co.com.bancolombia.model.workitem.WorkItemsBatchRequest`

### Candidate use cases

- `GetWorkItemUseCase`
- `CreateWorkItemUseCase`
- `UpdateWorkItemUseCase`
- `QueryByWiqlUseCase`
- `GetWorkItemsBatchUseCase`

### Candidate gateways or adapters

- `RestConsumer` (Driven Adapter en `infrastructure/driven-adapters/rest-consumer` implementing the domain interfaces).

## 8. Security considerations

- **Autenticación**: A través de tokens emitidos por Azure Entra ID, validados por el servidor de recursos Spring Boot.
- **Autorización**: Roles extraídos de los claims (`jwt.json-exp-roles`) anotados con `@PreAuthorize("hasRole('MCP.AZURE_DEVOPS.READ')")` y `@PreAuthorize("hasRole('MCP.AZURE_DEVOPS.WRITE')")`.
- **Fail-Closed**: El acceso se restringe automáticamente si no se cumple el rol requerido.
