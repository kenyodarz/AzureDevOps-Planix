# FASE 02 — Cliente A2A del agente y control de tareas

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** 🟡 EN EJECUCIÓN
> **Deudas objetivo:** D-01, D-02, D-03, D-22, D-23, D-29
> **Decisión bloqueante:** **DP-08 → RESUELTA (2026-08-29)**: *no bloqueante y reactivo de extremo a
> extremo, en Angular y en Spring*. El BFF envía `configuration.blocking = false`, recibe la `Task`
> en `submitted`/`working` y el front sigue su progreso. **Prohibido `block()`, `subscribe()` manual
> o cualquier salto a imperativo en las cadenas reactivas.**
> **Fase siguiente:** `fase-03.md` (se genera al cerrar esta)

---

## 1. Contexto

### 1.1 Dependencias resueltas de la Fase 01

La Fase 01 está **cerrada** (`docs/resultados/RESULTADO-FASE-01.md`). Al arrancar esta fase existe:

- **117 pruebas**, 15 fallos —todos heredados, todos del rol de servidor A2A que aquí se retira—.
- `ChatGatewayAdapterTest` (10 pruebas) con servidor HTTP embebido, incluida la que documenta que
  **la `Task` se descarta** y la que afirma que **no hay timeout**. Ambas deben invertirse en esta
  fase.
- `PlanningRoutesCharacterizationTest` (15) y `DashboardRoutesCharacterizationTest` (12): la red que
  detecta cualquier daño colateral. **No se tocan.**
- `UseCasesConfigTest` (5) reparado: ahora un cableado roto pone el test en rojo de verdad.
- `DevOpsDashboardUseCaseTest` (18) con los prompts capturados: detecta cualquier cambio no
  intencionado al migrar de puerto.

### 1.2 El problema que resuelve esta fase

El BFF ya llama al agente, pero lo hace mal en tres sentidos:

1. **Por el endpoint equivocado.** Usa `POST /message:send`, que en el agente es *legacy*, está tras
   un feature flag y su **sunset (`2026-06-30`) ya venció**. Un solo cambio de variable de entorno
   en el agente (`A2A_DISABLE_LEGACY_AFTER_SUNSET=true`) deja al BFF sin servicio. El sustituto ya
   existe: `POST /` con JSON-RPC 2.0.
2. **Perdiendo la información que el front necesita.** `ChatGateway.sendMessage` devuelve
   `Mono<String>`, así que `ChatGatewayAdapter` se queda con el texto y **tira la `Task`**. Sin esa
   `Task` el front no puede mostrar el estado de lo que el agente está haciendo, que es precisamente
   la funcionalidad pedida.
3. **Sin poder consultar ni cancelar.** El agente expone `tasks/get` y `tasks/cancel`; el BFF no los
   consume. Hoy el estado se obtiene leyendo la tabla `agent_tasks` que ambos comparten.

Además, el BFF conserva el rol contrario: `AgentChatUseCase` (537 líneas con prompts, intenciones y
división de historias) y los DTOs `JsonRpc*` lo convierten en un **servidor** A2A que replica el
cerebro del agente. Las 15 pruebas en rojo defienden ese rol.

### 1.3 Contrato del agente (verificado en código, 2026-08-29)

`azure-devops-agent` escucha en **`:8082`** y expone:

| Verbo  | Ruta                                                               | Nota                                  |
|--------|--------------------------------------------------------------------|---------------------------------------|
| `POST` | `/`                                                                | **JSON-RPC 2.0** — el canal a usar    |
| `GET`  | `/.well-known/agent-card.json`, `/.well-known/agent.json`, `/card` | Agent Card                            |
| `GET`  | `/api/tasks`                                                       | Listado de tareas                     |
| `POST` | `/message:send`                                                    | **Legacy**, con flag y sunset vencido |

**Métodos JSON-RPC** (`JsonRpcDispatcher`): `message/send`, `tasks/get`, `tasks/cancel`.

**Códigos de error** (`JsonRpcResponseFactory`, marcados como intocables en el agente):

|   Código | Significado                         |
|---------:|-------------------------------------|
| `-32700` | Parse error                         |
| `-32600` | Invalid Request                     |
| `-32601` | Method not found                    |
| `-32602` | Invalid params                      |
| `-32603` | Internal error                      |
| `-32004` | **Task not found** (específico A2A) |

**Petición** — `message/send`:

```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "message/send",
  "params": {
    "message": {
      "role": "user",
      "messageId": "...",
      "contextId": "...",
      "parts": [
        {
          "text": "..."
        }
      ],
      "referenceTaskIds": []
    },
    "configuration": {
      "acceptedOutputModes": [],
      "historyLength": null,
      "blocking": false
    }
  }
}
```

**Respuesta** — `result` contiene `task` y/o `message`:

```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "result": {
    "task": {
      "id": "...",
      "contextId": "...",
      "status": {
        "state": "working",
        "message": {
          ...
        },
        "timestamp": "..."
      },
      "artifacts": [],
      "history": [],
      "metadata": {}
    },
    "message": {
      "role": "agent",
      "parts": [
        {
          "text": "..."
        }
      ]
    }
  }
}
```

`tasks/get` y `tasks/cancel` reciben `params: { "taskId": "..." }` y devuelven `result.task`.

**`TaskState`** (idéntico en ambos proyectos, serializado en minúsculas con guion):
`submitted`, `working`, `input-required`, `auth-required`, `completed`, `canceled`, `rejected`,
`failed`.

### 1.4 Alcance específico

**Entra:** el canal BFF → agente y el modelo de tareas. **No entra:** exponer rutas nuevas al front
(eso es la Fase 03), ni tocar el flujo del dashboard más allá de adaptarlo al nuevo puerto.

---

## 2. Instrucciones

> ⛔ **Antes de empezar:** resolver **DP-08**. Si el BFF llama en modo bloqueante, la `Task` llega ya
> en `completed` y el panel de progreso del front no tiene nada que mostrar. Si llama en modo no
> bloqueante, la `Task` llega en `submitted`/`working` y el front puede seguirla — que es la
> funcionalidad pedida. La respuesta determina el valor de `configuration.blocking` y si `T-04`
> necesita o no un mecanismo de sondeo.

### T-01 · Definir el puerto `AgentGateway` en el dominio

**Archivo nuevo:**
`domain/model/src/main/java/co/com/bancolombia/model/agent/gateways/AgentGateway.java`

```java
public interface AgentGateway {

    Mono<AgentInteraction> sendMessage(AgentCommand command);

    Mono<Task> getTask(String taskId);

    Mono<Task> cancelTask(String taskId);
}
```

- `AgentCommand` (nuevo, `domain/model/agent/`): `prompt`, `contextId`, `messageId`, `blocking`.
  Value Object inmutable con validación en construcción (prompt no vacío, `contextId` obligatorio).
- `AgentInteraction` (nuevo): `Task task` + `String reply`. **Es la clave de toda la fase**: el
  puerto deja de devolver `String` y pasa a devolver ambas cosas (D-23).
- Cero anotaciones de Spring, cero Jackson (`rules/spring-rules.md` §1).

**`ChatGateway` queda obsoleto.** No borrarlo todavía: en T-05 se reescriben sus consumidores y en
T-06 se elimina.

### T-02 · Implementar el adaptador JSON-RPC

**Archivo nuevo:** `infrastructure/driven-adapters/agent-client/.../A2AAgentAdapter.java`

Módulo nuevo `agent-client` (registrarlo en `settings.gradle` y `main.gradle`). No reutilizar
`mcp-client`: su nombre miente, porque el BFF no habla MCP con nadie —habla A2A con el agente, y es
el agente quien habla MCP.

- `POST /` contra `${agent.url}` con el sobre JSON-RPC 2.0 de §1.3.
- Un `JsonRpcEnvelope`/`JsonRpcResult` **propios del adaptador**, no del dominio ni del entry-point
  (los `JsonRpcRequest/Response/Error` actuales de `reactive-web` se eliminan en T-06).
- Mapper dedicado `A2APayloadMapper` en el propio adaptador: JSON ⇄ modelo de dominio
  (`rules/spring-rules.md` §1, *mappers obligatorios*).
- **Traducción de errores** a excepciones tipadas del dominio, en lugar de propagar el crudo:
  | Origen | Excepción de dominio | |---|---| | `error.code = -32004` | `TaskNotFoundException` | |
  `error.code = -32602` | `InvalidAgentRequestException` | | Resto de `error.code` |
  `AgentExecutionException` | | Fallo de transporte / timeout | `AgentUnavailableException` |
- Conservar `filterReasoning()` (elimina bloques `<think>…</think>`): es comportamiento observable
  que la Fase 01 dejó congelado.
- Configurar **timeout** explícito y política de reintentos. Un `POST /` sin timeout deja colgado un
  stream SSE del dashboard indefinidamente.

### T-03 · Cablear la configuración del agente

- **`application.yaml`**: añadir el bloque que hoy **no existe** (D-27):
  ```yaml
  agent:
    url: "${AGENT_URL:http://localhost:8082}"
  ```
  Hoy `agent.url` solo vive como valor por defecto dentro de una anotación `@Value`: nadie que lea
  la configuración sabe que el BFF depende de otro servicio.
- Sustituir el `@Value` suelto por un `@ConfigurationProperties` tipado
  (`AgentConnectionProperties`)
  con `url`, `timeout` y `blocking`.
- Registrar el bean en `UseCasesConfig` / configuración del nuevo módulo.

### T-04 · Consolidar el ciclo de vida de las tareas

Objetivo: que exista **un solo sitio** que sepa cómo nace, avanza y muere una tarea.

- **Caso de uso nuevo** `domain/usecase/agent/TrackAgentTaskUseCase`:
    - `sendAndTrack(AgentCommand)` → delega en `AgentGateway.sendMessage` y persiste la `Task`
      devuelta en `TaskStoreGateway`.
    - `getTask(String id)`, `cancelTask(String id)`, `listTasks()`.
- **Regla:** las tareas del **agente** son propiedad del agente. El BFF **no las inventa**: las
  recibe y las proyecta. Las del **dashboard** sí son propias del BFF. El corte definitivo de la BD
  compartida (D-24) es de la Fase 03; aquí solo se deja el caso de uso preparado para que la fuente
  de verdad sea intercambiable.
- Si **DP-08** resuelve *no bloqueante*, añadir aquí la operación de refresco (`refreshTask(id)` →
  `AgentGateway.getTask`) que la Fase 03 expondrá al front.

### T-05 · Migrar el flujo del dashboard al nuevo puerto

`DevOpsDashboardUseCase` usa `ChatGateway.sendMessage(prompt, sessionKey)` en tres puntos
(`getDashboardData`, `getDashboardInitialData`, `getBatchAudit`).

- Sustituir por `AgentGateway.sendMessage(AgentCommand)` y consumir `AgentInteraction.reply()`.
- **No cambiar nada más** de ese caso de uso: sus prompts, su resolución de rutas y su fallback se
  atacan en las Fases 04, 05 y 06. Aquí solo se cambia el cable.
- Las pruebas de caracterización de la Fase 01 deben seguir en verde **sin tocarlas**. Si alguna
  exige cambios, es que el cambio no fue neutral: revisarlo.
- Oportunidad inmediata: ahora que `sendMessage` devuelve también la `Task`, el dashboard puede
  **enlazar su `Task` propia con la del agente** vía `contextId`. Dejarlo preparado, exponerlo en la
  Fase 03.

### T-06 · Retirar el rol de servidor A2A

Ésta es la parte que pone el build en verde. **Verificar cada eliminación con `grep`, no con el
compilador** (`CIERRE-DEL-PLAN.md` §7.3: el código muerto no lo detecta el compilador).

| Elemento                                                                                                               | Acción                                                                                                                                                                                                                      |
|------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/usecase/chat/AgentChatUseCase.java` (537 líneas)                                                               | **Eliminar**: es el cerebro del agente duplicado                                                                                                                                                                            |
| `domain/usecase/.../AgentChatUseCaseTest.java` (7 tests)                                                               | Eliminar: prueba código que se va                                                                                                                                                                                           |
| `UseCasesConfig` línea 20 (`excludeFilters`)                                                                           | Eliminar la exclusión, que deja de tener sentido                                                                                                                                                                            |
| `api/JsonRpcRequest`, `JsonRpcResponse`, `JsonRpcError`                                                                | Eliminar del entry-point (el sobre JSON-RPC pasa a ser detalle del adaptador cliente)                                                                                                                                       |
| `RouterRestTest`, `JsonRpcErrorContractTest`, `RouterRestLegacyToggleTest`, `RouterRestLegacySunsetTest`               | Eliminar: prueban un rol de servidor que el BFF no tiene                                                                                                                                                                    |
| `application.yaml`: `a2a.*` y `agent.system-prompt`                                                                    | Eliminar: el *system prompt* es del agente y allí ya vive                                                                                                                                                                   |
| `domain/model/a2a/*` (`Task`, `TaskStatus`, `TaskState`, `Message`, `Part`, `Artifact`, `SendMessageRequest/Response`) | **CONSERVAR**: es el modelo que el BFF necesita como cliente                                                                                                                                                                |
| `AgentCard`, `AgentCapabilities`, `AgentSkill`, `AgentInterface`, `AgentProvider`                                      | **CONSERVAR**: el front pide la agent card (Fase 03)                                                                                                                                                                        |
| `model/chat/ClientRequest`, `AgentResponseGateway`, `NoOpAgentResponseAdapter`                                         | Comprobar con `grep` si queda algún consumidor tras eliminar `AgentChatUseCase`. Si no queda: **preguntar antes de borrar**; en el agente esto resultó ser un punto de extensión A2A/Kafka deliberado (DP-05 de aquel plan) |

> Antes de eliminar `AgentChatUseCase`, revisar si contiene alguna regla que sea **del BFF y no del
> agente** (validaciones, normalización de entrada). Si aparece alguna, extraerla en lugar de
> perderla, y documentarlo en el bloque **Resultado**.

### T-07 · Pruebas

- **`A2AAgentAdapterTest`** con `MockWebServer`: sobre JSON-RPC correcto para los tres métodos;
  parseo de `result.task` y `result.message`; traducción de cada código de error a su excepción de
  dominio; timeout; `filterReasoning`.
- **`TrackAgentTaskUseCaseTest`** con Mockito puro: delegación, persistencia de la `Task` devuelta,
  y `TaskNotFoundException` cuando el agente responde `-32004`.
- **Tests de los Value Objects** `AgentCommand` y `AgentInteraction`: construcción válida y rechazo
  de entradas inválidas.
- **Prueba de contrato**: un test que fije el sobre JSON-RPC exacto que el BFF emite. Es lo único
  que detectará una divergencia futura con el agente.
- Cobertura mínima de la fase: `domain/model/agent` y `domain/usecase/agent` ≥ 90 %.

### T-08 · Validaciones de cierre

- `gradlew test` en **verde**, sin excepciones. Es el criterio principal de la fase.
- `grep -r "message:send"` en `src/main` → **cero** resultados.
- `grep -r "AgentChatUseCase\|JsonRpcRequest\|JsonRpcResponse"` en `src/main` → **cero**.
- Las pruebas de caracterización de la Fase 01 siguen en verde **sin haber sido modificadas**.
- Ninguna clase productiva supera las 300 líneas salvo `Handler` y `DevOpsDashboardUseCase`, que se
  atacan más adelante.

---

## 3. Orden de ejecución

> ✅ **DP-08 resuelta (2026-08-29): no bloqueante y reactivo de extremo a extremo.** El BFF envía
> `configuration.blocking = false`, el agente devuelve la `Task` en `submitted`/`working` y el front
> sigue su progreso con el panel que ya tiene construido. En consecuencia:
> - `AgentCommand` lleva `blocking = false` por defecto;
> - `TrackAgentTaskUseCase` **debe** incluir la operación de refresco (`refreshTask`) que la Fase 03
>   expondrá al front;
> - ninguna cadena reactiva puede contener `block()`, `subscribe()` manual ni `toFuture()`.

- [x] **P-00** — ⛔ Resolver **DP-08** con el usuario. → *no bloqueante y reactivo de extremo a
  extremo*.
- [x] **P-01** — Confirmar que la Fase 01 está cerrada y `gradlew test` da los 15 fallos conocidos.
- [x] **P-02** — Crear `AgentCommand`, `AgentInteraction` y las excepciones de dominio (T-01).
- [x] **P-03** — Definir el puerto `AgentGateway` (T-01).
- [x] **P-04** — Tests de los Value Objects (T-07, primera parte). Rojo → verde.
- [x] **P-05** — Crear el módulo `agent-client` y registrarlo en Gradle (T-02).
- [x] **P-06** — Implementar `A2AAgentAdapter` + `A2APayloadMapper` (T-02).
- [x] **P-07** — `A2AAgentAdapterTest` con `MockWebServer`, incluida la prueba de contrato (T-07).
- [x] **P-08** — `AgentConnectionProperties` y bloque `agent:` en `application.yaml` (T-03).
- [x] **P-09** — `TrackAgentTaskUseCase` + su test (T-04, T-07).
- [x] **P-10** — Cablear los beans nuevos en `UseCasesConfig` y **verificar con
  `UseCasesConfigTest`**, ya reparado en la Fase 01.
- [x] **P-11** — Migrar `DevOpsDashboardUseCase` al nuevo puerto (T-05). Las caracterizaciones deben
  seguir verdes sin tocarlas.
- [x] **P-12** — Revisar `AgentChatUseCase` en busca de reglas propias del BFF antes de eliminarlo
  (T-06). → No había ninguna: todo su contenido era lógica del agente.
- [x] **P-13** — Eliminar el rol de servidor: clases, tests y propiedades (T-06).
- [x] **P-14** — `grep` de verificación de las eliminaciones (T-08).
- [ ] **P-15** — Decidir con el usuario el destino de `AgentResponseGateway` /
  `NoOpAgentResponseAdapter`, que quedaron sin consumidor. → **Registrado como D-33; no bloquea.**
- [x] **P-16** — `gradlew test` en **verde**. → **128 pruebas, 0 fallos.**
- [x] **P-17** — Rellenar el bloque **Resultado** y actualizar §9 del plan maestro.
- [x] **P-18** — **Generar `docs/fases/fase-03.md`**: exposición del API de tareas al front y corte
  de la base de datos compartida.

---

## 4. Resultado

> Informe completo: **`docs/resultados/RESULTADO-FASE-02.md`**

- **Fecha de cierre:** 2026-08-29
- **DP-08 resuelta como:** no bloqueante y reactivo de extremo a extremo
- **Tests totales / fallidos:** **128 / 0** — el build pasa de rojo a verde
- **Líneas eliminadas (rol de servidor):** ~1 930, de las cuales **708 de código productivo**
- **Reglas del BFF rescatadas de `AgentChatUseCase`:** ninguna; todo su contenido era del agente
- **Destino de `AgentResponseGateway`:** pendiente de decisión del usuario (D-33)
- **Hallazgos no previstos:**
    - **H-1**: el `@ComponentScan` de `UseCasesConfig` duplicaba los beans ya declarados con `@Bean`
      y funcionaba por accidente. Sustituido por cinco `@Bean` explícitos.
    - **H-2** → **D-32**: el módulo `mcp-client` queda casi vacío y su nombre siempre fue engañoso.
    - **H-3** → **D-33**: `AgentResponseGateway` sin consumidor.
- **Deudas nuevas detectadas:** D-32, D-33

