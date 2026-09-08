# ACTIVACIÓN DE LA SEGURIDAD — `azure-devops-mcp`

> **Fase:** [`docs/fases/fase-02.md`](../fases/fase-02.md) · **Fecha:** 2026-08-30
> **Decisiones que lo sustentan:** DP-01, DP-02, B-01, B-02, B-03
>
> **Para qué sirve este documento.** La Fase 02 dejó el modo `ENFORCED` **construido, compilado y
> probado, pero apagado**. Éste es el procedimiento exacto para encenderlo el día que exista el
> Service Principal. Si este documento no existiera, el trabajo de la fase sería inservible: nadie
> sabría que basta una variable de entorno.

---

## 1. Lo que hay que hacer — y lo que no

| Paso | Acción                                                                               | Dueño                |
|------|--------------------------------------------------------------------------------------|----------------------|
| a    | Registrar la App en Entra ID y conceder los roles `MCP.AZURE_DEVOPS.READ` y `.WRITE` | Plataforma / IAM     |
| b    | Dotar al **agente** de la obtención de su token **M2M** hacia el MCP                 | `azure-devops-agent` |
| c    | Exportar `MCP_SECURITY_MODE=ENFORCED` y `AZURE_DEVOPS_TOKEN`                         | Despliegue           |

> ❗ **Lo que NO hace falta: recompilar.** Ése es el entregable de la Fase 02. El `.jar` de la POC y
> el de producción son **el mismo artefacto**; lo único que cambia es el entorno. Los pasos (a) y
> (b)
> no pertenecen a este repositorio (§12 del plan maestro); este plan solo garantiza que (c) sea
> suficiente **por parte del MCP**.

### El flujo de tokens (DP-01) — son dos tokens distintos

```
Front ──(token de USUARIO, válido solo para el BFF)──► BFF ──► Agente ──(token M2M)──► MCP
```

El token que el front obtiene del IDP **no sirve** para hablar con el MCP: su audiencia es el BFF.
El
agente debe obtener un **segundo token**, de máquina a máquina, con la audiencia del MCP. Confundir
ambos es el error más probable el día de la activación.

---

## 2. Variables de entorno

| Variable             | Valores                   | Por defecto    | Efecto                                          |
|----------------------|---------------------------|----------------|-------------------------------------------------|
| `MCP_SECURITY_MODE`  | `PERMISSIVE` / `ENFORCED` | `PERMISSIVE`   | Postura de acceso. Ver §3                       |
| `AZURE_DEVOPS_TOKEN` | `Base64(":" + PAT)`       | *(ninguno)*    | Credencial de Azure DevOps. Ver §4              |
| `TENANT_ID`          | GUID del tenant           | `dummy_tenant` | Compone el `issuer-uri` de Entra ID             |
| `CLIENT_ID`          | GUID de la App            | `dummy_client` | Audiencia (`aud`/`appid`) que se exige al token |

> En `ENFORCED`, `TENANT_ID` y `CLIENT_ID` **deben** tener valores reales: con los de relleno ningún
> token supera la validación y todas las llamadas responden `401`.

---

## 3. Qué cambia exactamente entre los dos modos

| Aspecto                               | `PERMISSIVE`                                            | `ENFORCED`                                  |
|---------------------------------------|---------------------------------------------------------|---------------------------------------------|
| Petición sin token al endpoint MCP    | **200** *(comportamiento histórico)*                    | **401**                                     |
| `/actuator/health` y `/actuator/info` | Abiertos                                                | **Abiertos igualmente**                     |
| Identidad de quien no presenta token  | Anónima **con** `ROLE_MCP.AZURE_DEVOPS.READ` y `.WRITE` | Anónima, sin roles                          |
| `@PreAuthorize` de las tools          | **Activos y evaluados** (los satisface el anónimo)      | **Activos y evaluados**                     |
| Token ausente o malformado            | `WARN` al arranque, la aplicación arranca               | **Falla el arranque** con mensaje explícito |

La clave del diseño está en la tercera fila: en `PERMISSIVE` **no se desactivan** las anotaciones,
se
concede una identidad que las satisface. Por eso las ocho tools se ejecutan por el mismo camino de
autorización en los dos modos, y el comportamiento observable de la POC no cambia.

El modo se deja escrito en el log de arranque, de forma inequívoca:

```
🔓 MCP Security en modo PERMISSIVE (mcp.security.mode=PERMISSIVE): NO se exige token y la identidad
   anónima recibe los roles MCP.AZURE_DEVOPS.READ y MCP.AZURE_DEVOPS.WRITE. [...]
```

---

## 4. El formato del token de Azure DevOps (B-01)

**El formato no cambió, y no debe cambiar.** La propiedad `adapter.restconsumer.token` recibe el
valor **ya codificado**; la aplicación solo antepone el prefijo `Basic `.

```
AZURE_DEVOPS_TOKEN = Base64( ":" + PAT )
                              ↑
                       usuario VACÍO, dos puntos, y el PAT
```

**El PAT no es la fusión de dos claves.** El usuario se ignora por completo, y por eso
`cualquiercosa:PAT` también funciona. Poner el **PAT crudo** en esta variable produce un `401` mudo
de Azure DevOps, indistinguible de un problema de permisos: por eso desde la Fase 02 el valor se
**valida al arranque** (que sea Base64 decodificable y que el texto decodificado contenga `:`).

Generación (ejemplo, PowerShell):

```powershell
$pat = Read-Host -AsSecureString "PAT"   # nunca en el historial ni en un fichero del repositorio
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(":$([Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($pat)))"))
```

> El valor resultante es un **secreto**: va a la bóveda o a las variables de entorno del despliegue,
> nunca al repositorio (DP-02, S2068).

---

## 5. Roles y tools

Los nombres de rol **se conservan literalmente** (B-03) y viven centralizados en
`mcp-server/.../mcp/security/McpRoles.java`.

| Tool                           | Autorización declarada                     |
|--------------------------------|--------------------------------------------|
| `getWorkItem`                  | `hasRole('MCP.AZURE_DEVOPS.READ')`         |
| `getWorkItemsBatch`            | `hasRole('MCP.AZURE_DEVOPS.READ')`         |
| `listWorkItemsByTeamAndSprint` | `hasRole('MCP.AZURE_DEVOPS.READ')`         |
| `queryByWiql` *(no expuesta)*  | `hasRole('MCP.AZURE_DEVOPS.READ')`         |
| `createWorkItem`               | `hasRole('MCP.AZURE_DEVOPS.WRITE')`        |
| `updateWorkItem`               | `hasRole('MCP.AZURE_DEVOPS.WRITE')`        |
| `checkHealth`                  | `permitAll()` *(sonda de vida, declarado)* |
| `getServerInfo`                | `permitAll()` *(sonda de vida, declarado)* |

> `permitAll()` en las dos sondas significa que **no se exige rol**, no que la ruta esté abierta: en
> `ENFORCED` el endpoint MCP sigue exigiendo un token válido a nivel de transporte antes de llegar a
> la tool. Quien necesite una sonda sin token debe usar `/actuator/health`.

---

## 6. Verificación tras activar

1. `GET /actuator/health` sin token → **200**.
2. Llamada MCP sin token → **401**.
3. Llamada MCP con token **sin** el rol correspondiente → **403** (`Acceso Denegado` en el log).
4. Llamada MCP con token y rol → respuesta normal de la tool.
5. Arrancar sin `AZURE_DEVOPS_TOKEN` → **el arranque falla** con un mensaje que nombra la propiedad.

Los cinco escenarios están cubiertos por pruebas automáticas: `McpSecurityModeTest` (transporte),
`McpToolsAuthorizationTest` (roles por tool) y `RestConsumerConfigTokenTest` (credencial).

---

## 7. Vuelta atrás

Retirar `MCP_SECURITY_MODE` —o ponerlo en `PERMISSIVE`— y reiniciar. **Sin recompilar y sin
redesplegar un artefacto distinto.** Ésa era, exactamente, la razón de ser de la Fase 02.

