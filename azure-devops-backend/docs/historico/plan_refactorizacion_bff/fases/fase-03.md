# FASE 03 — API de tareas del BFF hacia el frontend

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** ✅ COMPLETADA — 2026-08-29 · 153/153 en
> verde
> **Deudas objetivo:** D-21, D-24, D-25, D-26, D-28, D-30, D-33 — **todas saldadas**
> **Decisiones resueltas (2026-08-29):** **DP-07 → Opción B** · **DP-07-bis → B2** · **D-33 → A**
> **Fase siguiente:** `fase-04.md` (generada)

---

## 1. Contexto

### 1.1 Dependencias resueltas de la Fase 02

La Fase 02 está cerrada (`docs/resultados/RESULTADO-FASE-02.md`). Al arrancar esta fase existe:

- **Build en verde: 128 pruebas, 0 fallos.**
- `AgentGateway` (`domain/model/agent/gateways/`) operativo sobre JSON-RPC 2.0 con `sendMessage`,
  `getTask` y `cancelTask`; timeout configurable; traducción de los 6 códigos de error a excepciones
  de dominio.
- `A2AAgentAdapter` (`infrastructure/driven-adapters/agent-client/`), 24 pruebas con servidor HTTP
  embebido.
- `TrackAgentTaskUseCase` (`domain/usecase/agent/`) con `sendAndTrack`, `refreshTask`, `cancelTask`
  y `listTasks`.
- `AgentCommand` / `AgentInteraction`: **la `Task` del agente ya llega al dominio**.
- El rol de servidor A2A retirado por completo (−708 líneas productivas).

### 1.2 Lo que aún no funciona

Todo lo anterior es **interno**: ningún cliente lo aprovecha todavía. El frontend sigue igual que al
empezar el plan.

```json
// azure-devops-frontend/proxy.conf.json — estado actual
{
  "/api": {
    "target": "http://localhost:8081"
  },
  // ← BFF
  "/message:send": {
    "target": "http://localhost:8082"
  }
  // ← AGENTE, directo
}
```

| Llamada del front (`devops-agent-api.service.ts`) | Destino real                  | Estado                    |
|---------------------------------------------------|-------------------------------|---------------------------|
| `POST /message:send`                              | Agente `:8082`                | ⚠️ **Salta el BFF**       |
| `GET /.well-known/agent-card.json`                | `:4200`                       | 🔴 rota                   |
| `POST /` (`tasks/cancel`)                         | `:4200` devuelve `index.html` | 🔴 rota                   |
| `GET /api/tasks`                                  | BFF, lee la tabla compartida  | 🟡 funciona por accidente |

Consecuencia: **cuando el usuario escribe en el chat, el BFF no se entera.** No valida, no registra
y no puede seguir la tarea.

### 1.3 Alcance específico

Que el frontend hable **con un solo servicio** y pueda ver el estado de cada tarea que el agente
ejecuta.

**No entra:** modificar el repositorio del frontend. Los cambios que necesita se **documentan** en
`docs/resultados/CAMBIOS-FRONTEND.md` para que los aplique su equipo.

---

## 2. Decisiones aplicadas

### DP-07 → **Opción B: rutas nuevas bajo `/api/**`**

El BFF **no** adopta las URLs heredadas. Motivo: la Opción A obligaba a exponer `POST /`, que choca
con el fallback de SPA de `RouterRest`, y a perpetuar `/message:send`, ya deprecado en el agente.

**Contrato definitivo del BFF hacia el frontend:**

| Verbo  | Ruta del BFF             | Delega en                                   | Sustituye a                              |
|--------|--------------------------|---------------------------------------------|------------------------------------------|
| `POST` | `/api/chat/messages`     | `AgentGateway.sendMessage` (`message/send`) | `POST /message:send` a `:8082`           |
| `GET`  | `/api/tasks`             | ver DP-07-bis                               | lectura de la tabla compartida           |
| `GET`  | `/api/tasks/{id}`        | `AgentGateway.getTask` (`tasks/get`)        | — (no existía)                           |
| `POST` | `/api/tasks/{id}/cancel` | `AgentGateway.cancelTask` (`tasks/cancel`)  | `POST /`, hoy rota                       |
| `GET`  | `/api/agent/card`        | agent card del agente                       | `/.well-known/agent-card.json`, hoy rota |

`/api/planning/**` y `/api/devops/**` **no cambian de contrato**.

### DP-07-bis → **B2: combinar ambas fuentes con un campo `origin`**

Hay **dos productores de tareas** y el frontend debe ver la foto completa:

| Origen  | Quién la crea                                   | Ejemplo                 |
|---------|-------------------------------------------------|-------------------------|
| `AGENT` | El agente, al procesar `message/send`           | «refina esta historia»  |
| `BFF`   | El propio `Handler`, en el stream del dashboard | «analiza el sprint 247» |

`GET /api/tasks` devuelve la **unión** de ambas, cada una etiquetada con su origen. El BFF **deja de
escribir** en una tabla que no le pertenece.

### D-33 → **A: eliminar `AgentResponseGateway` y `NoOpAgentResponseAdapter`**

Es un puerto de **salida** que publicaba respuestas propias por Kafka, propio del rol de servidor
A2A ya eliminado. No sirve para el modo no bloqueante: eso exigiría un canal de **entrada**.

Se anota como **mejora futura** (no se implementa aquí): un `TaskUpdateNotifierPort` de salida hacia
el frontend por SSE/WebSocket, que eliminaría el sondeo de 5 s del front. Encaja con DP-08.

---

## 3. Instrucciones

### T-01 · Ampliar `AgentGateway` con listado y agent card

**Archivo:** `domain/model/src/main/java/co/com/bancolombia/model/agent/gateways/AgentGateway.java`

El puerto actual no cubre dos operaciones que esta fase necesita. Añadir:

```java
/** Lista las tareas que el agente conoce. Corresponde a GET /api/tasks del agente. */
Flux<Task> listTasks();

/** Publica las capacidades del agente. Corresponde a GET /.well-known/agent-card.json. */
Mono<AgentCard> getAgentCard();
```

> ⚠️ **Ojo:** estas dos operaciones del agente **no son JSON-RPC**, son REST plano. El adaptador
> debe usar `GET`, no el `POST /` del dispatcher.

**Archivo:** `infrastructure/driven-adapters/agent-client/.../A2AAgentAdapter.java`

Implementarlas con `webClient.get()`, aplicando el mismo `timeout` y la misma traducción de errores
(`AgentUnavailableException` ante fallo de transporte). El modelo `AgentCard` ya existe en
`domain/model/a2a/AgentCard.java`; mapearlo en `A2APayloadMapper`.

### T-02 · Modelar el origen de las tareas (DP-07-bis)

**Archivos nuevos en `domain/model/src/main/java/co/com/bancolombia/model/agent/`:**

```java
/** Quién originó la tarea. El frontend lo usa para agrupar el panel de progreso. */
public enum TaskOrigin {AGENT, BFF}

/** Una tarea junto con su procedencia. */
public record TrackedTask(Task task, TaskOrigin origin) {

}
```

No tocar `Task`: es el modelo A2A compartido con el agente y debe seguir siendo fiel al protocolo.

### T-03 · Reescribir `TrackAgentTaskUseCase.listTasks()`

**Archivo:**
`domain/usecase/src/main/java/co/com/bancolombia/usecase/agent/TrackAgentTaskUseCase.java`

```java
public Flux<TrackedTask> listTasks() {
    Flux<TrackedTask> fromAgent = agentGateway.listTasks()
            .map(task -> new TrackedTask(task, TaskOrigin.AGENT));
    Flux<TrackedTask> fromBff = taskStoreGateway.findAll()
            .map(task -> new TrackedTask(task, TaskOrigin.BFF));
    return Flux.merge(fromAgent, fromBff);   // reactivo, sin block()
}
```

**Reglas:**

- Si el agente no responde, el listado **no debe romperse**: aplicar `onErrorResume` sobre
  `fromAgent` para degradar a las tareas del BFF y registrar un `warn`. El panel del front es
  informativo; que se caiga entero por un agente caído sería peor que mostrar datos parciales.
- Deduplicar por `Task.getId()`: mientras la tabla siga compartida puede llegar la misma tarea por
  ambas vías. Prevalece la del agente, que es la fuente de verdad.

### T-04 · Cortar la integración por base de datos compartida (D-24)

El BFF deja de escribir tareas ajenas:

- En `TrackAgentTaskUseCase`, **eliminar** la persistencia local de las tareas que vienen del
  agente:
  `sendAndTrack`, `refreshTask` y `cancelTask` ya no llaman a `taskStoreGateway.save(...)`. La
  fuente de verdad es el agente.
- `taskStoreGateway` queda **solo** para las tareas propias del BFF (las del dashboard, que crea
  `Handler`).
- Actualizar `TrackAgentTaskUseCaseTest`: las pruebas que hoy verifican `save(...)` tras
  `refreshTask` y `cancelTask` deben invertirse a `verify(taskStoreGateway, never()).save(...)`.

> Esto **no** cambia el esquema de base de datos: solo deja de escribirse desde este lado. La
> separación física de tablas queda fuera de alcance (DP-05, Fase 07).

### T-05 · DTOs de respuesta propios (D-15)

**Paquete nuevo:** `infrastructure/entry-points/reactive-web/.../api/dto/task/`

El entry-point **no** devuelve el modelo de dominio. Definir DTOs con exactamente los campos que el
frontend consume, según sus interfaces TypeScript `AgentTask` y `AgentTaskStatus`:

```java
public record TaskResponse(String id, String contextId, String origin, TaskStatusResponse status) {

}

public record TaskStatusResponse(String state, String message, String timestamp) {

}

public record ChatMessageResponse(String reply, TaskResponse task) {

}
```

- `state` se serializa con el **valor A2A en minúsculas** (`working`, `input-required`…), que es lo
  que el front ya espera: usar `TaskState.getValue()`, **no** `name()`.
- `message` es el texto plano extraído con `Message.extractText()`.
- Mapper estático `TaskDtoMapper` en el mismo paquete.

### T-06 · `TaskHandler` y registro de rutas

**Archivo nuevo:** `infrastructure/entry-points/reactive-web/.../api/TaskHandler.java`

> ⛔ **No ampliar `Handler`.** Ya tiene 415 líneas y cuatro responsabilidades; es objetivo de la
> Fase 06. Las rutas de tareas y chat van en una clase propia.

Operaciones:

| Método              | Ruta                          | Caso de uso    | Respuesta             |
|---------------------|-------------------------------|----------------|-----------------------|
| `handleSendMessage` | `POST /api/chat/messages`     | `sendAndTrack` | `ChatMessageResponse` |
| `handleListTasks`   | `GET /api/tasks`              | `listTasks`    | `List<TaskResponse>`  |
| `handleGetTask`     | `GET /api/tasks/{id}`         | `refreshTask`  | `TaskResponse`        |
| `handleCancelTask`  | `POST /api/tasks/{id}/cancel` | `cancelTask`   | `TaskResponse`        |
| `handleAgentCard`   | `GET /api/agent/card`         | `getAgentCard` | agent card            |

Cuerpo de `POST /api/chat/messages` (lo que hoy manda el front a `/message:send`, simplificado):

```json
{
  "message": {
    "contextId": "...",
    "parts": [
      {
        "text": "..."
      }
    ]
  }
}
```

**Archivo:** `RouterRest.java` — registrar las cinco rutas **antes** del fallback de SPA.
`handleListTasks` deja de apuntar a `Handler` y pasa a `TaskHandler`.

### T-07 · Validación de entrada (D-28)

Es la misión declarada del BFF y hoy no la cumple.

- Crear un componente de validación en `reactive-web` que traduzca parámetros inválidos a **400**
  con mensaje útil, **dentro** de la cadena reactiva.
- Casos mínimos: `maxResults` no numérico o negativo; `query` vacía; `id` de tarea vacío; cuerpo de
  chat sin `parts` o sin texto; cuerpo de `ingest` sin `initiativeId`.
- **Invertir** la prueba `givenNonNumericMaxResults_whenGet_then500NotBadRequest` de
  `PlanningRoutesCharacterizationTest` → pasa a esperar **400** y se renombra a
  `givenNonNumericMaxResults_whenGet_thenBadRequest`.

### T-08 · Mapeo de errores del agente a HTTP

Aunque el mapa completo es DP-06 (Fase 07), estas rutas nacen con el suyo:

| Excepción de dominio                    |    HTTP |
|-----------------------------------------|--------:|
| `TaskNotFoundException`                 | **404** |
| `InvalidAgentRequestException`          | **400** |
| `AgentUnavailableException`             | **503** |
| `AgentExecutionException`               | **502** |
| `IllegalArgumentException` (validación) | **400** |

### T-09 · Propagación de contexto (D-30)

- `TaskHandler` propaga el `contextId` recibido hacia `AgentCommand`.
- Si el front no envía `contextId`, generar uno y **devolverlo** en la respuesta para que el front
  lo reutilice en los mensajes siguientes de la misma conversación.
- Registrar el `contextId` en los logs de `TaskHandler` y `A2AAgentAdapter` para poder rastrear una
  tarea a través de los dos servicios.

### T-10 · Eliminar `AgentResponseGateway` (D-33 → A)

Verificar con `grep` antes y después, **no con el compilador**:

| Archivo                                                                       | Acción                                          |
|-------------------------------------------------------------------------------|-------------------------------------------------|
| `domain/model/.../model/chat/gateways/AgentResponseGateway.java`              | Eliminar                                        |
| `infrastructure/driven-adapters/mcp-client/.../NoOpAgentResponseAdapter.java` | Eliminar                                        |
| `domain/model/.../model/chat/ClientRequest.java`                              | Comprobar si queda sin uso; si es así, eliminar |

Si tras esto el paquete `model/chat` queda vacío, eliminarlo también.

### T-11 · Pruebas

- **`TaskRoutesTest`** (`@WebFluxTest` con `RouterRest` + `TaskHandler`): las cinco rutas, caso
  feliz y de error, incluido el mapeo de T-08. Referencia de estilo:
  `PlanningRoutesCharacterizationTest`.
- **Invertir** en `PlanningRoutesCharacterizationTest` la prueba
  `givenRoutesTheFrontendCalls_whenInvoked_thenBffDoesNotServeThemYet`: ahora el BFF **sí** sirve el
  equivalente. Renombrarla y apuntarla a `/api/agent/card` y `/api/tasks/{id}/cancel`.
- **Ajustar** en `PlanningRoutesCharacterizationTest` las dos pruebas de `GET /api/tasks`
  (`givenTasks_whenGet_then200WithList` y `givenTaskStoreFails_whenGet_then400`): esa ruta pasa a
  `TaskHandler` y a `TrackAgentTaskUseCase`, ya no usa `taskStoreGateway` directamente.
- **Ampliar** `TrackAgentTaskUseCaseTest` con el merge de fuentes, la deduplicación y la degradación
  cuando el agente no responde.
- **Ampliar** `A2AAgentAdapterTest` con `listTasks()` y `getAgentCard()`.
- Cobertura objetivo: `domain/usecase` ≥ 90 % (hoy 96,5 %), `reactive-web` ≥ 85 % (hoy 87,1 %).

### T-12 · Documentar los cambios del frontend

**Archivo nuevo:** `docs/resultados/CAMBIOS-FRONTEND.md`

```jsonc
// proxy.conf.json — DESPUÉS
{ "/api": { "target": "http://localhost:8081", "secure": false } }
```

```typescript
// devops-agent-api.service.ts — DESPUÉS
getAgentCard()
{
  return this.http.get<AgentCard>('/api/agent/card');
}
sendMessage(p)
{
  return this.http.post<SendMessageResponse>('/api/chat/messages', p);
}
cancelTask(id)
{
  return this.http.post(`/api/tasks/${id}/cancel`, {});
}
```

Incluir además: el `AgentTask` de TypeScript gana el campo `origin: 'AGENT' | 'BFF'`, y
`cancelTask` deja de construir un sobre JSON-RPC a mano.

**No se toca el repositorio del frontend en esta fase.**

### T-13 · Validaciones de cierre

- `gradlew test` en **verde**.
- `grep -r "AgentResponseGateway"` en `src/main` → **cero**.
- Las 27 pruebas de caracterización de la Fase 01 siguen verdes, **salvo las cuatro que se invierten
  o ajustan de forma deliberada** (T-07 y T-11), cada una documentada en el informe de resultado.
- El BFF no escribe tareas del agente en la base de datos.
- Ninguna cadena reactiva contiene `block()` ni `subscribe()` manual (DP-08).

---

## 4. Orden de ejecución

- [x] **P-01** — Confirmar que la Fase 02 está cerrada y `gradlew test` da **128/128** en verde.
- [x] **P-02** — `TaskOrigin` y `TrackedTask` en `domain/model/agent` (T-02).
- [x] **P-03** — Ampliar `AgentGateway` con `listTasks()` y `getAgentCard()` (T-01).
- [x] **P-04** — Implementarlas en `A2AAgentAdapter` + mapeo de `AgentCard` (T-01).
- [x] **P-05** — Ampliar `A2AAgentAdapterTest` con las dos operaciones nuevas (T-11).
- [x] **P-06** — Reescribir `listTasks()` en `TrackAgentTaskUseCase` con merge, dedup y degradación
  (T-03).
- [x] **P-07** — Cortar la persistencia de tareas del agente (T-04) y ajustar
  `TrackAgentTaskUseCaseTest`.
- [x] **P-08** — DTOs de tarea y `TaskDtoMapper` (T-05).
- [x] **P-09** — `TaskHandler` con las cinco operaciones (T-06).
- [x] **P-10** — Registrar las rutas en `RouterRest`, antes del fallback de SPA (T-06).
- [x] **P-11** — Mapeo de excepciones a códigos HTTP (T-08).
- [x] **P-12** — Propagación de `contextId` y correlación en logs (T-09).
- [x] **P-13** — `TaskRoutesTest` (T-11).
- [x] **P-14** — Invertir y ajustar las cuatro pruebas de la Fase 01 afectadas (T-07, T-11).
- [x] **P-15** — Validación de entrada (T-07).
- [x] **P-16** — Eliminar `AgentResponseGateway` y `NoOpAgentResponseAdapter`, con `grep` de
  verificación (T-10).
- [x] **P-17** — `gradlew test` en **verde** (T-13).
- [x] **P-18** — `gradlew jacocoMergedReport --no-configuration-cache` y anotar cobertura.
- [x] **P-19** — Redactar `docs/resultados/CAMBIOS-FRONTEND.md` (T-12).
- [x] **P-20** — Redactar `docs/resultados/RESULTADO-FASE-03.md` con la plantilla de
  `docs/resultados/_PLANTILLA_RESULTADO.md`.
- [x] **P-21** — Rellenar el bloque **Resultado** de abajo y actualizar §9 del plan maestro.
- [x] **P-22** — **Generar `docs/fases/fase-04.md`**: prompts y datos fuera del dominio (D-08,
  D-19), bloqueada por **DP-02**.

---

## 5. Referencia rápida del contrato del agente

> Verificado en código el 2026-08-29. El agente escucha en **`:8082`**.

| Verbo  | Ruta                                                               | Nota                                                          |
|--------|--------------------------------------------------------------------|---------------------------------------------------------------|
| `POST` | `/`                                                                | **JSON-RPC 2.0**: `message/send`, `tasks/get`, `tasks/cancel` |
| `GET`  | `/.well-known/agent-card.json`, `/.well-known/agent.json`, `/card` | Agent Card (REST)                                             |
| `GET`  | `/api/tasks`                                                       | Listado de tareas (REST)                                      |
| `POST` | `/message:send`                                                    | **Legacy, sunset vencido. No usar.**                          |

**Petición JSON-RPC:**

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
      ]
    },
    "configuration": {
      "blocking": false
    }
  }
}
```

**Respuesta:** `result` contiene `task` y/o `message`.

**Códigos de error** (intocables en el agente): `-32700` parse · `-32600` invalid request ·
`-32601` method not found · `-32602` invalid params · `-32603` internal · **`-32004` task not
found**.

**`TaskState`** serializado en minúsculas con guion: `submitted`, `working`, `input-required`,
`auth-required`, `completed`, `canceled`, `rejected`, `failed`.

---

## 6. Resultado

- **Fecha de cierre:** 2026-08-29
- **Tests totales / fallidos:** **153** / **0**
- **Rutas nuevas expuestas:** **5** — `POST /api/chat/messages`, `GET /api/tasks`,
  `GET /api/tasks/{id}`, `POST /api/tasks/{id}/cancel`, `GET /api/agent/card`
- **Servicios con los que habla el frontend:** **1** (era 2) — pendiente de aplicar
  `docs/resultados/CAMBIOS-FRONTEND.md` en el repositorio del front
- **Pruebas de la Fase 01 invertidas o ajustadas:** 3 invertidas + 2 sustituidas por 1
    1. `givenNonNumericMaxResults_whenGet_then500NotBadRequest` → `..._thenBadRequest` (500 →
       **400**)
    2. `givenRoutesTheFrontendCalls_whenInvoked_thenBffDoesNotServeThemYet` →
       `givenTheNewContractRoutes_whenInvoked_thenBffServesThem`
    3. En `TrackAgentTaskUseCaseTest`: `refreshTask` y `cancelTask` pasan a
       `verify(taskStoreGateway, never()).save(...)`
    4. Las dos de `GET /api/tasks` → `givenTasksRoute_whenGet_thenItIsServedByTaskHandler`
- **Cobertura `domain/usecase` / `reactive-web`:** **96,4 %** / **89,8 %** (objetivos ≥ 90 % y ≥
  85 %)
- **Hallazgos no previstos:**
    - **H-1** el agente no declara la forma del cuerpo de `GET /api/tasks`; el mapper acepta ambas
    - **H-2** `Flux.merge` habría hecho la deduplicación no determinista; se usó `Flux.concat`
    - **H-3** `ClientRequest` violaba una regla de ArchUnit en modo advertencia
    - **H-4** el mapa de errores HTTP queda partido en dos hasta DP-06
    - **H-5** la tabla `agent_tasks` sigue físicamente compartida; la dedup es un parche hasta DP-05
- **Deudas nuevas detectadas:** **D-34** — el agente no declara el contrato de respuesta de
  `GET /api/tasks`

> Informe completo: [`docs/resultados/RESULTADO-FASE-03.md`](../resultados/RESULTADO-FASE-03.md)

