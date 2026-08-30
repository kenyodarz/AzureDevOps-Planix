# CONTRATO MCP PÚBLICO — `azure-devops-mcp`

> **Fase:** [`docs/fases/fase-01.md`](../fases/fase-01.md) · **Fecha:** 2026-08-30
> **Fuente:** `infrastructure/entry-points/mcp-server/.../tools/AzureDevOpsTools.java` (267 líneas) y
> `.../tools/HealthTool.java`
>
> **Para qué sirve este documento.** El agente y el BFF son **clientes reales** de este servidor. Las
> Fases 03 y 04 van a mover DTOs y lógica de sitio, y necesitan una referencia contra la que
> comprobar que **el contrato no cambió**. Todo lo que aparece aquí es **contrato público**: cambiar
> un nombre de tool, un nombre de parámetro o la forma del resultado **rompe a los clientes en
> silencio**, porque MCP no valida esquemas al vuelo.

---

## 1. Configuración del servidor

| Propiedad                | Valor                                     |
|--------------------------|-------------------------------------------|
| Nombre                   | `McpAzureDevOps`                          |
| Versión                  | `1.0.0`                                   |
| Protocolo                | `STATELESS`                               |
| Tipo                     | `ASYNC`                                   |
| Endpoint                 | `/mcp/McpAzureDevOps`                     |
| Puerto                   | `8080`                                    |
| Capacidades              | `tool: true`, `resource: true`, `prompt: true`, `completion: false` |
| Timeout de petición      | `30s`                                     |

> ⚠️ Se declaran las capacidades `resource` y `prompt`, pero **no existe ni un solo `@McpResource` ni
> `@McpPrompt`** en el repositorio. El servidor anuncia capacidades que no puede atender.
> Se anota como observación para la Fase 08 (emparentada con D-19 y D-20).

---

## 2. Tools expuestas — contrato vigente

### 2.1 `getWorkItem`

| Parámetro     | Tipo     | Obligatorio | Descripción                             |
|---------------|----------|-------------|-----------------------------------------|
| `organization`| `String` | ✅          | Organización en Azure DevOps            |
| `project`     | `String` | ✅          | Nombre o UUID del proyecto              |
| `id`          | `int`    | ✅          | ID numérico del Work Item               |
| `apiVersion`  | `String` | ❌          | Por defecto `7.1`                       |

**Retorna:** `Mono<WorkItem>` → `{ id, rev, fields: Map<String,Object>, relations[], url }`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.READ')` *(activa desde la Fase 02)*

### 2.2 `createWorkItem`

| Parámetro     | Tipo                       | Obligatorio | Descripción                    |
|---------------|----------------------------|-------------|--------------------------------|
| `organization`| `String`                   | ✅          |                                |
| `project`     | `String`                   | ✅          |                                |
| `type`        | `String`                   | ✅          | Ej. `User Story`, `Task`       |
| `patch`       | **`List<JsonPatchOperation>`** | ✅      | ⚠️ **modelo de dominio** (§3)  |
| `apiVersion`  | `String`                   | ❌          | Por defecto `7.1`              |

**Retorna:** `Mono<WorkItem>`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.WRITE')`

### 2.3 `updateWorkItem`

| Parámetro     | Tipo                       | Obligatorio | Descripción                    |
|---------------|----------------------------|-------------|--------------------------------|
| `organization`| `String`                   | ✅          |                                |
| `project`     | `String`                   | ✅          |                                |
| `id`          | `int`                      | ✅          |                                |
| `patch`       | **`List<JsonPatchOperation>`** | ✅      | ⚠️ **modelo de dominio** (§3)  |
| `apiVersion`  | `String`                   | ❌          | Por defecto `7.1`              |

**Retorna:** `Mono<WorkItem>`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.WRITE')`

### 2.4 `listWorkItemsByTeamAndSprint` — **la tool crítica**

| Parámetro       | Tipo     | Obligatorio | Descripción                                             |
|-----------------|----------|-------------|---------------------------------------------------------|
| `organization`  | `String` | ✅          | Ej. `grupobancolombia`                                  |
| `project`       | `String` | ✅          | Ej. `Vicepresidencia Servicios de Tecnología`           |
| `teamName`      | `String` | ✅          | Nombre de célula **o ruta completa**. Ej. `EQU1096 - EXODIA` |
| `sprintName`    | `String` | ✅          | Nombre de sprint **o ruta completa**. Ej. `Sprint 247`  |
| `workItemTypes` | `String` | ❌          | CSV. Por defecto `Historia de Usuario, Habilitador`     |
| `apiVersion`    | `String` | ❌          | Por defecto `7.0`                                       |

**Retorna:** `Mono<WiqlResult>` → `{ queryType, queryResultType, asOf, workItems: [{ id, url }] }`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.READ')` *(escrita en la Fase 02; antes no existía ni comentada)*

**Es la tool más usada y la única con lógica compuesta.** Su sentencia WIQL está congelada carácter
a carácter en `WiqlCharacterizationTest` (Fase 01, T-02). Contrato interno observable:

```
SELECT [System.Id] FROM workitems
WHERE [System.TeamProject] = @project
  AND [System.IterationPath] = '<resuelto>'
  AND [System.AreaPath] = '<resuelto>'
  AND [System.WorkItemType] IN (<tipos>)
ORDER BY [System.Id]
```

**Comportamientos tolerantes que los clientes pueden estar explotando** (y que por tanto son
contrato de facto, aunque no estén documentados en la descripción de la tool):

1. `teamName` y `sprintName` admiten **barras dobles** (`\\`), que se colapsan a una.
2. `teamName` admite ruta completa: se toma **el último tramo**.
3. `workItemTypes` traduce `User Story` → `Historia de Usuario`.
4. `workItemTypes` admite valores **ya entrecomillados** sin duplicar comillas.
5. Si Azure DevOps no resuelve el `AreaPath`, se **fabrica por concatenación** sin avisar al cliente.
6. Si Azure DevOps no resuelve el `IterationPath` y el sprint es `Sprint N`, se intercala **el año
   del calendario** — origen del fallo del tablero vacío (D-09).

### 2.5 `getWorkItemsBatch`

| Parámetro     | Tipo            | Obligatorio | Descripción                                  |
|---------------|-----------------|-------------|----------------------------------------------|
| `organization`| `String`        | ✅          |                                              |
| `project`     | `String`        | ✅          |                                              |
| `ids`         | `List<Integer>` | ✅          |                                              |
| `fields`      | `List<String>`  | ❌          |                                              |
| `expand`      | `String`        | ❌          | `None`\|`Relations`\|`Fields`\|`Links`\|`All`. Por defecto **`None`** |
| `errorPolicy` | `String`        | ❌          | `Fail`\|`Omit`. Por defecto **`Omit`**       |
| `apiVersion`  | `String`        | ❌          | Por defecto `7.1`                            |

**Retorna:** `Mono<List<WorkItem>>`
**Autorización:** ✅ `hasRole('MCP.AZURE_DEVOPS.READ')` *(activa desde la Fase 02)*

> El entry-point construye internamente un **`WorkItemsBatchRequest`**, que es un modelo de dominio
> (§3) y la clase que provoca la violación de ArchUnit `Rule_2.2` (D-17).

### 2.6 `checkHealth` / `getServerInfo` (`HealthTool`)

| Tool            | Parámetros | Retorna       | Autorización |
|-----------------|------------|---------------|--------------|
| `checkHealth`   | ninguno    | `Mono<String>` | ✅ `permitAll()` **declarado** *(Fase 02)* |
| `getServerInfo` | ninguno    | `Mono<String>` | ✅ `permitAll()` **declarado** *(Fase 02)* |

`getServerInfo` devuelve la cadena literal `"Server:  v1.0.0 - Package: co.com.bancolombia"`, con la
versión hardcodeada y **un placeholder sin rellenar** (doble espacio tras `Server:`). Ambas duplican
lo que ya ofrece el actuator (D-20).

---

## 3. ⚠️ Parámetros cuyo tipo es un **modelo de dominio**

Éste es el punto que la **Fase 03** debe resolver y sobre el que **DP-03** debe decidir. Los
siguientes tipos de `domain/model` **son el contrato de cable**: Jackson los deserializa
directamente desde el payload MCP.

| Tool                            | Parámetro | Tipo de dominio expuesto  | Forma en el cable                       |
|---------------------------------|-----------|---------------------------|-----------------------------------------|
| `createWorkItem`                | `patch`   | `JsonPatchOperation`      | `{ op, path, value, from }`             |
| `updateWorkItem`                | `patch`   | `JsonPatchOperation`      | `{ op, path, value, from }`             |
| `getWorkItemsBatch` *(interno)* | —         | `WorkItemsBatchRequest`   | `{ ids, fields, expand, errorPolicy }`  |

Y en el **retorno**, la misma fuga en sentido contrario: `WorkItem`, `WorkItemRelation`,
`WorkItemReference`, `WiqlResult` y `TeamFieldValues` se serializan tal cual, sin DTO ni mapper.

> **Restricción para la Fase 03:** el DTO que sustituya a `JsonPatchOperation` **debe conservar
> exactamente los nombres de campo `op`, `path`, `value` y `from`**, y `WorkItemsBatchRequest` los
> suyos. Cambiar el nombre de la **clase** es seguro; cambiar el de un **campo** no lo es.

---

## 4. Superficie declarada pero **no** expuesta

| Elemento     | Estado                                                                                     |
|--------------|--------------------------------------------------------------------------------------------|
| `queryByWiql`| 🟠 **Camino muerto (D-19).** El `@McpTool` sigue **comentado**, pero el método es `public`, y su caso de uso, su gateway y su prueba de adaptador existen y funcionan. **No forma parte del contrato público de hoy.** La Fase 08 debe decidir: exponerla o retirarla. **Cambio de la Fase 02:** su `@PreAuthorize` **ya no está comentado** (`hasRole('MCP.AZURE_DEVOPS.READ')`), de modo que el método público queda protegido sin exponer la tool |
| `@McpResource` | Capacidad `resource: true` anunciada, **cero implementaciones**                            |
| `@McpPrompt`   | Capacidad `prompt: true` anunciada, **cero implementaciones**                              |

---

## 5. Resumen de autorización

> **Actualizado al cierre de la Fase 02 (2026-08-30).**

| Estado                                    | Tools |
|-------------------------------------------|------:|
| ✅ Con autorización **declarada y compilada** | **8 de 8** |
| 🟠 Con `@PreAuthorize` **comentado**      | **0** |
| 🔴 **Sin ninguna**, ni comentada          | **0** |

| Tool                          | Expresión                           |
|-------------------------------|-------------------------------------|
| `getWorkItem`                 | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `getWorkItemsBatch`           | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `listWorkItemsByTeamAndSprint`| `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `queryByWiql` *(no expuesta)* | `hasRole('MCP.AZURE_DEVOPS.READ')`  |
| `createWorkItem`              | `hasRole('MCP.AZURE_DEVOPS.WRITE')` |
| `updateWorkItem`              | `hasRole('MCP.AZURE_DEVOPS.WRITE')` |
| `checkHealth`                 | `permitAll()`                       |
| `getServerInfo`               | `permitAll()`                       |

Los nombres de rol **se conservaron literalmente** (B-03) y se centralizaron en
`mcp-server/.../mcp/security/McpRoles.java`.

> **El contrato público NO cambió.** Conforme a **DP-01** la postura sigue siendo laxa: el modo por
> defecto es `PERMISSIVE` y en él la identidad anónima porta los dos roles, de modo que **una
> llamada sin token se sigue atendiendo exactamente igual que antes**. Lo que cambió es que activar
> la exigencia ya no requiere recompilar, sino exportar `MCP_SECURITY_MODE=ENFORCED`
> (ver [`ACTIVACION-SEGURIDAD.md`](ACTIVACION-SEGURIDAD.md)). Las ocho expresiones se ejecutan en los
> dos modos y están cubiertas por `McpToolsAuthorizationTest`.
