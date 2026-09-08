# CONTRATO MCP PÚBLICO — `azure-devops-mcp`

> **Fase:** [`docs/fases/fase-01.md`](../fases/fase-01.md) · **Fecha:** 2026-08-30
> **Fuente:** `infrastructure/entry-points/mcp-server/.../tools/AzureDevOpsTools.java` (267 líneas)
> y
> `.../tools/HealthTool.java`
>
> **Para qué sirve este documento.** El agente y el BFF son **clientes reales** de este servidor.
> Las
> Fases 03 y 04 van a mover DTOs y lógica de sitio, y necesitan una referencia contra la que
> comprobar que **el contrato no cambió**. Todo lo que aparece aquí es **contrato público**: cambiar
> un nombre de tool, un nombre de parámetro o la forma del resultado **rompe a los clientes en
> silencio**, porque MCP no valida esquemas al vuelo.

---

## 1. Configuración del servidor

| Propiedad           | Valor                                                                         |
|---------------------|-------------------------------------------------------------------------------|
| Nombre              | `McpAzureDevOps`                                                              |
| Versión             | `1.0.0`                                                                       |
| Protocolo           | `STATELESS`                                                                   |
| Tipo                | `ASYNC`                                                                       |
| Endpoint            | `/mcp/McpAzureDevOps`                                                         |
| Puerto              | `8080`                                                                        |
| Capacidades         | `tool: true`, **`resource: false`**, **`prompt: false`**, `completion: false` |
| Timeout de petición | `30s`                                                                         |

> ✅ **Actualizado al cierre de la Fase 08 (DP-08 §0.2 (c)).** Hasta ahora el servidor anunciaba
> `resource: true` y `prompt: true` **sin una sola implementación**: cero `@McpResource` y cero
> `@McpPrompt` en todo el repositorio.
>
> **Es un cambio observable, y se hizo con el criterio invertido respecto a la intuición inicial.**
> La especificación MCP dice: *«Servers that **declare** the `resources` capability **MUST respond**
> to `resources/list` requests»*. Declarar una capacidad **no es un permiso: es una promesa de
> responder**. Anunciar `true` sin handler es el antipatrón que hace que un cliente llame y reciba
> un **error genérico**; con `false`, un cliente conforme **ni siquiera llama**.
>
> El día que exista un `@McpResource` o un `@McpPrompt`, se vuelven a poner en `true`.

---

## 2. Tools expuestas — contrato vigente

### 2.1 `getWorkItem`

| Parámetro      | Tipo     | Obligatorio | Descripción                  |
|----------------|----------|-------------|------------------------------|
| `organization` | `String` | ✅          | Organización en Azure DevOps |
| `project`      | `String` | ✅          | Nombre o UUID del proyecto   |
| `id`           | `int`    | ✅          | ID numérico del Work Item    |
| `apiVersion`   | `String` | ❌          | Por defecto `7.1`            |

**Retorna:** `Mono<WorkItem>` → `{ id, rev, fields: Map<String,Object>, relations[], url }`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.READ')` *(activa desde la Fase 02)*

### 2.2 `createWorkItem`

| Parámetro      | Tipo                                | Obligatorio | Descripción                                                 |
|----------------|-------------------------------------|-------------|-------------------------------------------------------------|
| `organization` | `String`                            | ✅          |                                                             |
| `project`      | `String`                            | ✅          |                                                             |
| `type`         | `String`                            | ✅          | Ej. `User Story`, `Task`                                    |
| `patch`        | **`List<JsonPatchOperation­Input>`** | ✅          | DTO del entry-point; cable `{ op, path, value, from }` (§3) |
| `apiVersion`   | `String`                            | ❌          | Por defecto `7.1`                                           |

**Retorna:** `Mono<WorkItem>`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.WRITE')`

### 2.3 `updateWorkItem`

| Parámetro      | Tipo                                | Obligatorio | Descripción                                                 |
|----------------|-------------------------------------|-------------|-------------------------------------------------------------|
| `organization` | `String`                            | ✅          |                                                             |
| `project`      | `String`                            | ✅          |                                                             |
| `id`           | `int`                               | ✅          |                                                             |
| `patch`        | **`List<JsonPatchOperation­Input>`** | ✅          | DTO del entry-point; cable `{ op, path, value, from }` (§3) |
| `apiVersion`   | `String`                            | ❌          | Por defecto `7.1`                                           |

**Retorna:** `Mono<WorkItem>`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.WRITE')`

### 2.4 `listWorkItemsByTeamAndSprint` — **la tool crítica**

| Parámetro       | Tipo     | Obligatorio | Descripción                                                  |
|-----------------|----------|-------------|--------------------------------------------------------------|
| `organization`  | `String` | ✅          | Ej. `grupobancolombia`                                       |
| `project`       | `String` | ✅          | Ej. `Vicepresidencia Servicios de Tecnología`                |
| `teamName`      | `String` | ✅          | Nombre de célula **o ruta completa**. Ej. `EQU1096 - EXODIA` |
| `sprintName`    | `String` | ✅          | Nombre de sprint **o ruta completa**. Ej. `Sprint 247`       |
| `workItemTypes` | `String` | ❌          | CSV. Por defecto `Historia de Usuario, Habilitador`          |
| `apiVersion`    | `String` | ❌          | Por defecto `7.0`                                            |

**Retorna:** `Mono<WiqlResult>` → `{ queryType, queryResultType, asOf, workItems: [{ id, url }] }`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.READ')` *(escrita en la Fase 02; antes no existía ni
comentada)*

**Es la tool más usada y la única con lógica compuesta.** Su sentencia WIQL está congelada carácter
a carácter en `WiqlCharacterizationTest` (Fase 01, T-02; **reubicada a `domain/usecase` en la Fase
04**, con las ocho ramas y sus aserciones intactas). Contrato interno observable:

```
SELECT [System.Id] FROM workitems
WHERE [System.TeamProject] = @project
  AND [System.IterationPath] = '<resuelto>'
  AND [System.AreaPath] = '<resuelto>'
  AND [System.WorkItemType] IN (<tipos>)
ORDER BY [System.Id]
```

> ✅ **Actualizado al cierre de la Fase 04 (2026-08-30). Decisión: DP-04.**
> **Ninguno de los seis comportamientos tolerantes ha cambiado.** Todos siguen produciendo
> exactamente el mismo resultado observable; lo que cambió es **dónde vive cada regla** y, en los
> dos repliegues, que **ahora se miden**. La sentencia emitida es idéntica carácter a carácter en
> las ocho ramas.

**Comportamientos tolerantes que los clientes pueden estar explotando** (y que por tanto son
contrato de facto, aunque no estén documentados en la descripción de la tool):

| # | Comportamiento                                                                                                                                               | Estado tras la Fase 04                                                                                                                                                                                                                                                                                                                                                                                                              | Dónde vive ahora                                                             |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| 1 | `teamName` y `sprintName` admiten **barras dobles** (`\\`), que se colapsan a una                                                                            | ✅ **Sin cambios**                                                                                                                                                                                                                                                                                                                                                                                                                  | `TeamName.of(...)` y `SprintName.of(...)` *(dominio)*                        |
| 2 | `teamName` admite ruta completa: se toma **el último tramo** para consultar a Azure DevOps, pero la ruta entera alimenta el repliegue                        | ✅ **Sin cambios**                                                                                                                                                                                                                                                                                                                                                                                                                  | `TeamName.shortName()` vs `TeamName.value()` *(dominio)*                     |
| 3 | `workItemTypes` traduce `User Story` → `Historia de Usuario`                                                                                                 | ✅ **Sin cambios.** **DP-04 §0.2 lo declaró regla de dominio**, no configuración                                                                                                                                                                                                                                                                                                                                                    | `WorkItemTypes.parse(...)` *(dominio)*                                       |
| 4 | `workItemTypes` admite valores **ya entrecomillados** sin duplicar comillas                                                                                  | ✅ **Sin cambios**                                                                                                                                                                                                                                                                                                                                                                                                                  | `WorkItemTypes.parse(...)` *(dominio)*                                       |
| 5 | Si Azure DevOps no resuelve el `AreaPath`, se **fabrica por concatenación** sin avisar al cliente                                                            | 🔸 **Se conserva** (DP-04 §0.1, opción **a**). Sigue **sin avisar al cliente** —el contrato no cambia—, pero ya **no es invisible para el operador**: incrementa el contador `azuredevops.teamscope.fallback{path=area}` y emite un `WARN` con la causa                                                                                                                                                                             | `TeamPathFallback.areaPath(...)` *(dominio)* · métrica en `app-service`      |
| 6 | Si Azure DevOps no resuelve el `IterationPath` y el sprint es `Sprint N`, se intercala **el año del calendario** — origen del fallo del tablero vacío (D-09) | 🔸 **Se conserva, año incluido** (DP-04 §0.1, opción **a**). Ahora incrementa `azuredevops.teamscope.fallback{path=iteration,calendarYearInterleaved=true}` —**etiqueta propia, separada del repliegue inocuo**— y emite un `WARN` que dice explícitamente que la consulta puede devolver cero elementos. Además el año **ya no lo calcula el dominio**: se inyecta un `Clock`, lo que hace el repliegue determinista y comprobable | `TeamPathFallback.iterationPath(...)` *(dominio)* · métrica en `app-service` |

> **Por qué 5 y 6 se conservaron.** Eliminarlos —opciones (b) y (c) de DP-04 §0.1— habría cambiado
> el comportamiento observable ante un fallo de Azure DevOps: donde hoy sale un tablero vacío,
> saldría un error. Al ser contrato de facto, ese cambio le corresponde al propietario y no a un
> refactor. Lo que la Fase 04 sí resolvió es el motivo por el que la decisión llevaba años
> aplazada: **hasta ahora nadie sabía cuántas veces se disparaban**. Con la métrica, la retirada
> podrá decidirse con cifras.

### 2.5 `getWorkItemsBatch`

| Parámetro      | Tipo            | Obligatorio | Descripción                                                           |
|----------------|-----------------|-------------|-----------------------------------------------------------------------|
| `organization` | `String`        | ✅          |                                                                       |
| `project`      | `String`        | ✅          |                                                                       |
| `ids`          | `List<Integer>` | ✅          |                                                                       |
| `fields`       | `List<String>`  | ❌          |                                                                       |
| `expand`       | `String`        | ❌          | `None`\|`Relations`\|`Fields`\|`Links`\|`All`. Por defecto **`None`** |
| `errorPolicy`  | `String`        | ❌          | `Fail`\|`Omit`. Por defecto **`Omit`**                                |
| `apiVersion`   | `String`        | ❌          | Por defecto `7.1`                                                     |

**Retorna:** `Mono<List<WorkItem>>`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.READ')` *(activa desde la Fase 02)*

> El entry-point construye internamente un **`WorkItemsBatchInput`** (DTO del entry-point, §3) que
> el
> `McpToolDtoMapper` traduce a `WorkItemBatchCriteria`. Hasta la Fase 03 construía directamente el
> modelo de dominio `WorkItemsBatchRequest`, que era la clase responsable de la violación de
> ArchUnit
> `Rule_2.2` (D-17, **saldada**). **Los siete parámetros de la tool no cambiaron.**

### 2.6 ❌ `checkHealth` / `getServerInfo` — **RETIRADAS en la Fase 08**

> 🔴 **Cambio observable. DP-08 §0.2 (b).** Las dos tools de `HealthTool` **ya no existen**.

| Tool            | Estado          |
|-----------------|-----------------|
| `checkHealth`   | ❌ **Retirada** |
| `getServerInfo` | ❌ **Retirada** |

**Por qué.** Duplicaban lo que ya ofrece el actuator (`/actuator/health`, `/actuator/info`) y
`getServerInfo` devolvía la cadena `"Server:  v1.0.0 - Package: co.com.bancolombia"`, con la
versión **codificada a fuego** y un **placeholder sin rellenar** —el doble espacio tras `Server:`—
que llevaba ahí desde el scaffold original (D-20).

⚠️ **Si el agente o el BFF las estaban llamando, dejarán de funcionar.** El sustituto es el
actuator,
que da la misma información y se mantiene solo. Es el cambio observable más grande de todo el plan
después del contrato de errores de la Fase 06, y por eso lo decidió el propietario expresamente.

**Tools públicas tras este cambio: 6.**

---

## 3. ✅ Parámetros cuyo tipo era un **modelo de dominio** — resuelto en la Fase 03

> **Actualizado al cierre de la Fase 03 (2026-08-30). Decisión: DP-03.**
> **El contrato público NO cambió: ni un nombre de tool, ni un nombre de parámetro, ni un nombre de
> campo, ni la forma del resultado.** Lo único que cambió son **nombres de clase Java**, que no
> viajan por el cable.

Hasta la Fase 03 los tipos de `domain/model` **eran el contrato de cable**: Jackson los
deserializaba
directamente desde el payload MCP y el `WebClient` los serializaba tal cual hacia Azure DevOps
(D-11).
Ahora cada frontera tiene sus propios DTOs y su mapper.

### 3.1 Frontera de entrada (payload MCP → dominio)

| Tool                            | Parámetro | Antes *(modelo de dominio)*            | Ahora *(DTO del entry-point)*     | Forma en el cable **(sin cambios)**    |
|---------------------------------|-----------|----------------------------------------|-----------------------------------|----------------------------------------|
| `createWorkItem`                | `patch`   | `model.workitem.JsonPatchOperation`    | `mcp.dto.JsonPatchOperationInput` | `{ op, path, value, from }`            |
| `updateWorkItem`                | `patch`   | `model.workitem.JsonPatchOperation`    | `mcp.dto.JsonPatchOperationInput` | `{ op, path, value, from }`            |
| `getWorkItemsBatch` *(interno)* | —         | `model.workitem.WorkItemsBatchRequest` | `mcp.dto.WorkItemsBatchInput`     | `{ ids, fields, expand, errorPolicy }` |

Traduce `mcp-server/.../mcp/dto/McpToolDtoMapper.java` (estático, sin Spring, sin lógica de
negocio).

### 3.2 Frontera de salida (dominio → Azure DevOps)

| Cuerpo enviado | Antes *(modelo de dominio serializado por el `WebClient`)* | Ahora *(DTO del adaptador)*                 | Mapper                |
|----------------|------------------------------------------------------------|---------------------------------------------|-----------------------|
| JSON Patch     | `.bodyValue(patch)` → `JsonPatchOperation`                 | `consumer.dto.JsonPatchOperationRequestDTO` | `JsonPatchMapper`     |
| Consulta WIQL  | `.bodyValue(query)` → `WiqlQuery`                          | `consumer.dto.WiqlQueryRequestDTO`          | `WiqlQueryMapper`     |
| Lote           | `.bodyValue(request)` → `WorkItemsBatchRequest`            | `consumer.dto.WorkItemsBatchRequestDTO`     | `WorkItemBatchMapper` |

Las respuestas ya tenían DTOs propios, pero se mapeaban con métodos privados dentro de
`RestConsumer`; ahora viven en `consumer/mapper/` (`WorkItemMapper`, `TeamMapper`).

### 3.3 Renombrado de dominio (DP-03)

`model.workitem.WorkItemsBatchRequest` → **`model.workitem.WorkItemBatchCriteria`**. Era la única
violación real de ArchUnit `Rule_2.2` (D-17): un *request* HTTP dentro de `domain/model`. El sufijo
`Request` vive ahora donde le corresponde, en el DTO del adaptador. **`Rule_2.2` queda a 0
violaciones**, verificado en el log del `ArchitectureTest` (por D-25 el `issues.json` no sirve como
prueba).

### 3.3.1 Reagrupación de puertos y adaptadores (DP-05, Fase 05)

> **El contrato público NO cambió.** Lo que cambió son **nombres de interfaz y de clase Java** y
> **el
> paquete en que viven**. Nada de esto viaja por el cable: ni un nombre de tool, ni de parámetro, ni
> de campo, ni la forma de un resultado.

Los **7 puertos por operación CRUD** se funden en **3 por agregado y responsabilidad**, y los **7
paquetes** en **2**. **Ninguna firma de método cambió**:

| Antes *(7 puertos, 7 paquetes)*                                  | Ahora *(3 puertos, 2 paquetes)*               |
|------------------------------------------------------------------|-----------------------------------------------|
| `model.getworkitem.gateways.GetWorkItemRepository`               | `model.workitem.gateways.WorkItemQueryPort`   |
| `model.getworkitemsbatch.gateways.GetWorkItemsBatchRepository`   | `model.workitem.gateways.WorkItemQueryPort`   |
| `model.querybywiql.gateways.QueryByWiqlRepository`               | `model.workitem.gateways.WorkItemQueryPort`   |
| `model.createworkitem.gateways.CreateWorkItemRepository`         | `model.workitem.gateways.WorkItemCommandPort` |
| `model.updateworkitem.gateways.UpdateWorkItemRepository`         | `model.workitem.gateways.WorkItemCommandPort` |
| `model.team.gateways.GetTeamFieldValuesRepository`               | `model.team.gateways.TeamScopePort`           |
| `model.iteration.gateways.GetTeamIterationsRepository`           | `model.team.gateways.TeamScopePort`           |
| `model.workitem.gateways.WorkItemRepository` *(huérfano, vacío)* | **borrado** (B-06)                            |
| `model.iteration.TeamIteration`                                  | `model.team.TeamIteration`                    |

Del lado del adaptador, `RestConsumer` —una clase de 191 líneas que implementaba los siete— se parte
en **`WorkItemQueryAdapter`**, **`WorkItemCommandAdapter`** y **`TeamScopeAdapter`**. **Las 7 URLs,
los 7 verbos HTTP, los 2 `contentType`, los 4 cuerpos y las 2 versiones por defecto son literalmente
los mismos**, verificado con diff contra `HEAD` y congelado por
`OutboundPayloadCharacterizationTest`.

Los `@CircuitBreaker` pasan de 7 nombres por operación a **3 por adaptador** —`workItemQuery`,
`workItemCommand`, `teamScope`— (B-05). **No es un cambio de comportamiento**: ninguna instancia,
ni la vieja ni la nueva, está declarada en `application.yaml` (D-06, **Fase 06**).

### 3.4 Lo que sigue siendo intocable

> **Los nombres de campo son contrato público.** `op`, `path`, `value`, `from`, `ids`, `fields`,
> `expand` y `errorPolicy` se conservan **literalmente**, y ahora hay una prueba que lo verifica por
> reflexión (`McpToolDtoMapperTest`). El cuerpo HTTP emitido se compara **carácter a carácter**
> contra el que producía el modelo de dominio en `OutboundPayloadCharacterizationTest`.
> Cambiar el nombre de una **clase** es seguro; cambiar el de un **campo** no lo es nunca.

### 3.5 ✅ Frontera de **salida hacia el cliente MCP** — cerrada en la Fase 07 (DP-07)

> **Actualizado al cierre de la Fase 07 (2026-08-30). Decisión: DP-07 §0.2 (a), opción (a1).**
> **El contrato público NO cambió: ni un nombre de campo, ni el orden, ni la emisión de nulos.**

Hasta la Fase 07, esta sección decía que en el **retorno** `WorkItem`, `WorkItemRelation`,
`WorkItemReference`, `WiqlResult` y `TeamFieldValues` «siguen serializándose desde el dominio hacia
el
cliente MCP» y que quedaba «anotada para cuando el dominio se convierta en Value Objects (D-16,
**Fase 07**)». **Ese momento llegó y la frontera está cerrada.**

**Corrección de inventario medida en la Fase 07:** las clases realmente serializadas hacia el
cliente
eran **cuatro**, no cinco. `TeamFieldValues` **nunca** se devolvió a ninguna tool —es interna a
`ResolveTeamScopeUseCase`—, de modo que la lista original la incluía por error.

| Retorno de                                          | Antes *(modelo de dominio serializado)* | Ahora *(DTO de respuesta)*          | Forma en el cable **(sin cambios)**               |
|-----------------------------------------------------|-----------------------------------------|-------------------------------------|---------------------------------------------------|
| `getWorkItem` · `createWorkItem` · `updateWorkItem` | `model.workitem.WorkItem`               | `mcp.dto.WorkItemResponse`          | `{ id, rev, fields, relations, url }`             |
| *(anidado en el anterior)*                          | `model.workitem.WorkItemRelation`       | `mcp.dto.WorkItemRelationResponse`  | `{ rel, url, attributes }`                        |
| `listWorkItemsByTeamAndSprint` · `queryByWiql`      | `model.workitem.WiqlResult`             | `mcp.dto.WiqlResultResponse`        | `{ queryType, queryResultType, asOf, workItems }` |
| *(anidado en el anterior)*                          | `model.workitem.WorkItemReference`      | `mcp.dto.WorkItemReferenceResponse` | `{ id, url }`                                     |
| `getWorkItemsBatch`                                 | `List<WorkItem>`                        | `List<WorkItemResponse>`            | array de la forma de arriba                       |

Traduce `mcp-server/.../mcp/dto/McpResponseMapper.java` (estático, sin Spring, sin lógica de
negocio),
simétrico al `McpToolDtoMapper` de entrada.

**Por qué había que cerrarla antes de tocar el modelo.** Convertir `WiqlResult` en `record` renombra
sus accesores (`getQueryType()` → `queryType()`) y, mientras el dominio *fuera* el contrato, eso
habría renombrado **los campos del JSON en silencio** — en el retorno de la tool más usada del
sistema. Con la frontera en medio, la forma del dominio y la del cable son independientes.

**Cómo se garantizó que no cambió nada.** `McpResponsePayloadCharacterizationTest` se escribió
**antes de tocar una sola clase**, contra el modelo de dominio de entonces, y compara **cadenas
literales**, no el dominio consigo mismo. Los literales **no se han modificado**; lo único que
cambió
es qué objeto se serializa. Se congelan cuatro cosas:

1. los **nombres** de los campos;
2. su **orden** de aparición;
3. la **emisión de nulos** — un `fields` nulo se sigue serializando como `null` y **no** como `{}`;
4. el **orden de las claves** dentro del mapa `fields`.

> ⚠️ **Por qué importa el matiz de los nulos.** Normalizar los nulos a colecciones vacías habría
> sido
> más limpio, y por eso **DP-07 §0.2 (c) tuvo que decidirlo expresamente**: es un cambio observable
> para el agente y el BFF. Se aplicó el mismo criterio que el javadoc de `TeamScope` fijó para las
> cadenas en blanco — no se endurece nada que convierta un resultado vacío en otra cosa.

### 3.6 El modelo de dominio ya no es mutable (D-16, Fase 07)

Las **9 clases mutables** de `domain/model` —7 con `@Setter` y **2 con `@Data`**, que además
generaba
`equals`/`hashCode` sobre campos mutables— son ahora **`record` inmutables** (DP-07 §0.2 (b)).
**Nada de esto viaja por el cable**, gracias a §3.5.

> 📌 **Corrección de una cifra del plan.** El plan maestro contabilizaba «21 clases con `@Setter`».
> La medición de la Fase 07 demostró que **21 era el número de tipos del módulo**, no el de clases
> mutables: las mutables eran **9**, y **ningún setter se invocaba en todo el repositorio** —los
> cinco mappers construían ya con `builder()`—.

**Invariantes declaradas (DP-07 §0.2 (c)):** copia defensiva e inmodificable de todas las
colecciones,
preservando orden y nulos; `WiqlQuery` exige sentencia no vacía; `JsonPatchOperation` exige `op` y
`path`. **Nada más se endureció**, y las tolerancias deliberadas están fijadas por prueba en
`DomainInvariantsTest` para que un refactor futuro no las elimine por parecer más limpio.


---

## 4. Superficie declarada pero **no** expuesta

| Elemento       | Estado                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `queryByWiql`  | ✅ **RESUELTA en la Fase 08 (D-19, DP-08 §0.2(a)): la tool se retira.** El `@McpTool` comentado **ya no existe**; el método sigue siendo `public` y con `@PreAuthorize`, pero es **una operación interna**, no una tool. **No es código muerto**: lo invoca `ListWorkItemsByTeamAndSprintUseCase` a través de `QueryByWiqlUseCase`, y es el corazón del flujo compuesto. **Por qué no se expuso:** una tool que acepta **WIQL crudo del cliente** convertiría al llamante en el dueño de *cómo* se consulta Azure DevOps — justo lo contrario de la misión de §1 del plan maestro. Quien necesite listar usa `listWorkItemsByTeamAndSprint`, que expresa la **intención** |
| `@McpResource` | ✅ Capacidad `resource` puesta a **`false`** en la Fase 08 (§1). Ya no se anuncia lo que no se implementa                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `@McpPrompt`   | ✅ Capacidad `prompt` puesta a **`false`** en la Fase 08 (§1)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |

---

## 5. Resumen de autorización

> **Actualizado al cierre de la Fase 02 (2026-08-30).**

| Estado                                        |      Tools |
|-----------------------------------------------|-----------:|
| ✅ Con autorización **declarada y compilada** | **8 de 8** |
| 🟠 Con `@PreAuthorize` **comentado**          |      **0** |
| 🔴 **Sin ninguna**, ni comentada              |      **0** |

| Tool                           | Expresión                           |
|--------------------------------|-------------------------------------|
| `getWorkItem`                  | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `getWorkItemsBatch`            | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `listWorkItemsByTeamAndSprint` | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `queryByWiql` *(no expuesta)*  | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `createWorkItem`               | `hasRole('MCP.AZURE_DEVOPS.WRITE')` |
| `updateWorkItem`               | `hasRole('MCP.AZURE_DEVOPS.WRITE')` |
| `checkHealth`                  | `permitAll()`                       |
| `getServerInfo`                | `permitAll()`                       |

Los nombres de rol **se conservaron literalmente** (B-03) y se centralizaron en
`mcp-server/.../mcp/security/McpRoles.java`.

> **El contrato público NO cambió.** Conforme a **DP-01** la postura sigue siendo laxa: el modo por
> defecto es `PERMISSIVE` y en él la identidad anónima porta los dos roles, de modo que **una
> llamada sin token se sigue atendiendo exactamente igual que antes**. Lo que cambió es que activar
> la exigencia ya no requiere recompilar, sino exportar `MCP_SECURITY_MODE=ENFORCED`
> (ver [`ACTIVACION-SEGURIDAD.md`](ACTIVACION-SEGURIDAD.md)). Las ocho expresiones se ejecutan en
> los
> dos modos y están cubiertas por `McpToolsAuthorizationTest`.

---

## 6. 🆕 Contrato de errores — **nuevo en la Fase 06 (DP-06)**

> **Esta es la primera sección de este documento que describe un cambio de comportamiento
> observable.** Las Fases 01 a 05 no cambiaron nada de lo que ve un cliente. La Fase 06 sí, y por
> eso
> **lo decidió el propietario (DP-06) antes de escribirse una sola línea**.
>
> **Nada de §2 a §5 ha cambiado:** ni un nombre de tool, ni de parámetro, ni de campo, ni la forma
> de
> un resultado **de éxito**. Lo único nuevo es **qué se ve cuando la cosa falla**.

### 6.1 Qué se veía antes y qué se ve ahora

Hasta la Fase 06 el repositorio tenía **cero excepciones de dominio** (D-13). Un `401`, un `404` o
un
`TF401232` de Azure DevOps viajaba **crudo** hasta el cliente MCP como `WebClientResponseException`
—una excepción de **Spring**—, arrastrando consigo la nomenclatura y los códigos internos del
proveedor, justo lo que §1 del plan maestro prohíbe filtrar aguas arriba.

```
ANTES                                          AHORA
WebClientResponseException: 401 Unauthorized   AZDO_UNAUTHORIZED: La credencial configurada no
  from GET https://dev.azure.com/... ,           permite ejecutar la operacion solicitada
  body: {"message":"TF400813: ..."}              (getWorkItem). Verifique el token de servicio.
```

### 6.2 Forma del error — **DP-06 §0.1 (a)**

El mensaje que recibe el cliente tiene **siempre** esta forma, sin excepciones:

```
CODIGO_ESTABLE: mensaje neutro en español
```

- El **código** existe para que un cliente pueda ramificar **sin hacer `catch` por texto**, que es
  lo
  frágil. **Es contrato público: no cambia.**
- El **mensaje** es legible, no cita nomenclatura de Azure DevOps y nombra la operación afectada
  —que es el nombre de la tool, ya público, así que no filtra nada nuevo—.

### 6.3 Los cuatro códigos — **DP-06 §0.1 (c)**

| Código               | Cuándo                                                                                               | Excepción de dominio                        | Qué significa para quien lo recibe                                                                                                                                                                                                        |
|----------------------|------------------------------------------------------------------------------------------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AZDO_NOT_FOUND`     | Azure DevOps responde **404**                                                                        | `WorkItemNotFoundException`                 | El recurso **no existe o no es accesible**. Casi siempre es un error del llamante. ⚠️ Azure DevOps devuelve el mismo 404 para «no existe» y para «existe pero está fuera del alcance del PAT», por eso el mensaje no afirma que no exista |
| `AZDO_UNAUTHORIZED`  | **401** o **403**                                                                                    | `AzureDevOpsUnauthorizedException`          | La credencial **de este servidor** no vale o no alcanza. No es culpa del llamante ni del proveedor: hay que **rotar o revisar el token**                                                                                                  |
| `AZDO_UNAVAILABLE`   | **5xx**, cualquier otro **4xx**, **timeout por operación**, fallo de red o **cortacircuito abierto** | `AzureDevOpsUnavailableException`           | «Ahora no puedo, reintenta». Todo esto comparte código a propósito: desde el punto de vista del cliente **hay una sola decisión que tomar**                                                                                               |
| `MCP_INTERNAL_ERROR` | Cualquier fallo que **no** sea de dominio (un defecto del propio servidor)                           | — *(no llega a haber excepción de dominio)* | Red de seguridad: nada escapa crudo, ni siquiera lo que nadie previó                                                                                                                                                                      |

Las tres excepciones extienden de **`AzureDevOpsException`** (`domain/model/.../model/exception/`),
así que un cliente que **no** quiera distinguir puede capturar una sola cosa. Son **dominio puro**:
sin Spring, sin HTTP, sin Jackson — no conocen siquiera el concepto de «código de estado».

### 6.4 El cuerpo original **no se propaga** — DP-06 §0.1 (b)

El cuerpo de respuesta de Azure DevOps —con sus `TFxxxxxx`— se registra **íntegro en `ERROR` en el
log del MCP**, que es donde le sirve a quien opera, y **se oculta al cliente**, que es donde
filtraría nomenclatura ajena. Hay pruebas que lo verifican explícitamente
(`AzureDevOpsErrorTranslatorTest`, `TeamScopeAdapterTest`).

### 6.5 ⚠️ Lo que **NO** cambió: el repliegue de rutas sigue absorbiendo sus fallos

**DP-06 §0.1 (d) ratificó DP-04 y no lo contradijo.** Los comportamientos **5** y **6** de §2.4
—fabricar el `AreaPath` y el `IterationPath` por concatenación cuando Azure DevOps no los resuelve—
**siguen exactamente igual**: `ResolveTeamScopeUseCase` captura el error *ya traducido* y cae al
repliegue, con su `WARN` y su contador `azuredevops.teamscope.fallback`.

> **Donde hoy sale un tablero vacío, sigue saliendo un tablero vacío.** Convertirlo en un error es
> una decisión de producto que el propietario aún no ha tomado; esta fase **no la ha tomado por
él**.
> Lo único que gana es que ahora el log dice **por qué** se disparó el repliegue, y no solo cuántas
> veces. Fijado por prueba en `ResolveTeamScopeUseCaseTest`.

### 6.6 Dónde se traduce — **B-08**

Dos piezas, dos responsabilidades, **cero duplicación**:

| Capa                                                    | Sabe                                                | De                       | A                              |
|---------------------------------------------------------|-----------------------------------------------------|--------------------------|--------------------------------|
| `rest-consumer/.../consumer/AzureDevOpsErrorTranslator` | qué es un 404, un timeout, un cortacircuito abierto | excepción **técnica**    | excepción de **dominio**       |
| `mcp-server/.../mcp/error/McpErrorTranslator`           | qué forma tiene un error en el protocolo MCP        | excepción de **dominio** | mensaje con **código estable** |

Hay **tres** adaptadores y **siete** puntos de salida, pero **un solo traductor**, compartido — el
mismo criterio con el que la Fase 05 repartió los cinco mappers de la Fase 03.

### 6.7 Parámetros operativos que dejaron de ser fantasmas

| Qué                                             | Antes                                                  | Ahora                                                                                                                       |
|-------------------------------------------------|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Cortacircuitos declarados en `application.yaml` | `testGet` y `testPost`, **que no usaba nadie**         | `workItemQuery`, `workItemCommand`, `teamScope` — **los tres reales**, con umbrales elegidos (50 % / 10 / 10 s)             |
| Timeout                                         | **1** de Netty, **5 s compartidos** por las 7 llamadas | Netty intacto **+ 4 timeouts reactivos por operación**: consulta 10 s · **lote 30 s** · comando 10 s · ámbito de equipo 5 s |
| Versiones de API                                | 2 literales dentro del código                          | `adapter.restconsumer.api-version.*` — **mismos valores**, ahora configurables (B-09)                                       |

> **B-10:** las dos consultas de ámbito de equipo conservan `api-version=7.0` **escrito literalmente
> en la ruta**. Se decidió **no** parametrizarlas porque habría cambiado dos llamadas HTTP.

