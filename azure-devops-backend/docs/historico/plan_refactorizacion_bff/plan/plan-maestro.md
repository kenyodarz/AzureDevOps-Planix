# PLAN MAESTRO — Refactorización `azure-devops-backend` (BFF)

> **Versión:** 2.0 · **Fecha:** 2026-08-29 · **Estado:** 🟡 EN CURSO
> **Cambio v1.0 → v2.0:** se incorpora la misión declarada del proyecto (**BFF**, no agente) y el
> hallazgo de que el front habla con dos backends. **DP-01 queda resuelta**: el flujo A2A **se
> reactiva**, pero como *cliente*, no como servidor.
> **Reglas:** `rules/spring-rules.md` · **Precedente:**
> `azure-devops-agent/docs/resultados/CIERRE-DEL-PLAN.md`
> **Fases:** 8 (01 → 08) · **Fase activa:** `docs/fases/fase-08.md` · **Completadas:** 01, 02, 03,
> 04, 05, 06, 07

---

## 1. Misión del proyecto

`azure-devops-backend` es el **BFF (Backend For Frontend)**. El sistema nació como un único agente
con front; al crecer se separó en dos servicios. Su misión, por definición del propietario:

1. Ser el **único interlocutor del frontend**.
2. Hacer las **validaciones** necesarias.
3. **Llamar al agente** para que ejecute las tareas.
4. **Controlar y exponer el estado de cada tarea** que el agente ejecuta, para que el front pueda
   mostrarlo (ej.: «analiza este sprint» → el front ve el progreso de cada tarea).

La inteligencia —prompts, intenciones, estimación, división de historias— **vive en el agente**. El
BFF no la replica.

### 1.1 Arquitectura objetivo

```
Front (4200) ── /api/** ──► BFF (8081) ── JSON-RPC 2.0 POST / ──► Agente (8082) ──► MCP (8080)
                              │                                      │
                              └──── estado de tareas ◄────────────────┘
```

---

## 2. Diagnóstico: distancia respecto a esa misión

### 2.1 El front habla con dos backends

`azure-devops-frontend/proxy.conf.json` lo demuestra:

```json
{ "/api":          { "target": "http://localhost:8081" },   ← BFF
  "/message:send": { "target": "http://localhost:8082" } }  ← AGENTE, directo
```

**El chat se salta el BFF por completo.** Y hay llamadas del front que no encajan en ninguno de los
destinos:

| Llamada del front (`devops-agent-api.service.ts`) | Destino real                                | Estado                                                                |
|---------------------------------------------------|---------------------------------------------|-----------------------------------------------------------------------|
| `POST /message:send`                              | Agente `:8082`                              | ⚠️ Salta el BFF y usa el endpoint **legacy** del agente               |
| `GET /.well-known/agent-card.json`                | No proxiado → `:4200`                       | 🔴 **ROTA**                                                           |
| `POST /` con `{"method":"tasks/cancel"}`          | No proxiado → `:4200` devuelve `index.html` | 🔴 **ROTA**                                                           |
| `GET /api/tasks`                                  | BFF `:8081`                                 | 🟡 Funciona **por accidente**: ambos comparten la tabla `agent_tasks` |

### 2.2 El endpoint que usa el BFF ya está vencido

`ChatGatewayAdapter` llama a `POST {agent.url}/message:send`. En el agente esa ruta:

- está detrás del flag `a2a.legacy-message-send-enabled`;
- tiene **fecha de sunset `2026-06-30`**, ya pasada (hoy es 2026-08-29);
- sobrevive solo porque `a2a.legacy.disable-after-sunset` viene en `false`.

Basta con exportar `A2A_DISABLE_LEGACY_AFTER_SUNSET=true` para que **el BFF entero deje de
funcionar**. El agente ya expone el sustituto: `POST /` con JSON-RPC 2.0.

### 2.3 El BFF descarta la información que el front necesita

```java
// ChatGatewayAdapter — el puerto obliga a devolver String
public Mono<String> sendMessage(String message, String contextId) { ...
    .map(response -> response.getMessage().extractText())   // ← la Task se pierde aquí
```

El agente **sí** devuelve la `Task` dentro de `SendMessageResponse`. El BFF la tira porque
`ChatGateway` devuelve `Mono<String>`. **Ésta es la causa raíz de que el front no pueda seguir el
estado de las tareas**, y por eso hoy se suple leyendo la base de datos compartida.

### 2.4 Integración por base de datos compartida

BFF y agente tienen **el mismo adaptador duplicado** (`PostgresTaskStoreAdapter`, 106 líneas
idénticas) escribiendo en **la misma tabla `agent_tasks`** de **la misma base**
(`agent_azure_devops`). `GET /api/tasks` del BFF devuelve una mezcla indistinguible de tareas
propias del dashboard y tareas del agente. Es integración por BD compartida: justo el antipatrón que
la separación en dos servicios buscaba evitar.

### 2.5 El front ya está construido, esperando datos buenos

El panel de tareas **ya existe**: `AgentTask`, `AgentTaskStatus` con los 7 estados A2A, contador
`workingTasksCount`, filtro por `working|submitted` y **polling adaptativo** (5 s con tareas
activas, 30 s sin ellas). No hay que construirlo: hay que **alimentarlo bien**.

### 2.6 El residuo del BFF corresponde al rol equivocado

`AgentChatUseCase` (537 líneas: prompts, intenciones, división de historias, RAG) y los DTOs
`JsonRpc*` convierten al BFF en un **servidor** A2A que replica el cerebro del agente. Las 4 suites
en rojo prueban exactamente eso: `/message:send`, `/card`, `/.well-known/agent.json` y JSON-RPC
entrante. Ese rol ya lo cumple el agente.

> **Resolución de DP-01:** el flujo A2A **se reactiva como cliente**. Se conservan y potencian el
> modelo `a2a` (`Task`, `TaskStatus`, `TaskState`, `Message`, `Part`) y el `TaskStoreGateway`; se
> retiran la lógica de agente duplicada (`AgentChatUseCase`) y el rol de servidor JSON-RPC.

### 2.7 Estado del build

**Rojo: 41 tests, 15 fallidos**, todos en `reactive-web`, todos por golpear rutas de servidor A2A
que `RouterRest` ya no expone.

### 2.8 Clases más largas

|  Líneas | Clase                     | Veredicto                                         |
|--------:|---------------------------|---------------------------------------------------|
| **537** | `AgentChatUseCase`        | Rol equivocado: cerebro del agente dentro del BFF |
| **415** | `Handler`                 | 4 responsabilidades mezcladas                     |
| **411** | `DevOpsDashboardUseCase`  | 179 de sus líneas son prompts y datos mock        |
|     240 | `PgVectorPlanningAdapter` | Serializa el dominio sin mapper                   |

---

## 3. Deudas técnicas

> 🟢 **ESTADO FINAL — plan cerrado el 2026-08-29.** De las **48** deudas registradas (45 del
> diagnóstico inicial + **D-46, D-47 y D-48** detectadas al cerrar): **33 saldadas** y **15 vivas**.
> El estado de cada una, con la fase en que se cerró o el **dueño** de la que sigue viva, está en
> [`CIERRE-DEL-PLAN.md`](../resultados/CIERRE-DEL-PLAN.md) §3. Las secciones que siguen conservan la
> redacción original del diagnóstico: sirven de registro de por dónde se empezó.

### 🔴 Bloqueantes — misión del BFF

**Todas saldadas.**

| ID       | Deuda                                                                                                                                                                                                                                                                                                                                                                                             |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-21** | El front va **directo al agente** para el chat: el BFF no valida, no registra y no puede seguir la tarea                                                                                                                                                                                                                                                                                          |
| **D-22** | El BFF consume el endpoint **legacy vencido** `/message:send` en vez de JSON-RPC `POST /`                                                                                                                                                                                                                                                                                                         |
| **D-23** | `ChatGateway.sendMessage` devuelve `Mono<String>` y **descarta la `Task`** del agente                                                                                                                                                                                                                                                                                                             |
| **D-24** | Integración por **base de datos compartida**: misma tabla `agent_tasks`, adaptador duplicado                                                                                                                                                                                                                                                                                                      |
| **D-25** | El BFF **no expone** `tasks/get` ni `tasks/cancel`; el front los llama a `POST /` y recibe `index.html`                                                                                                                                                                                                                                                                                           |
| **D-26** | `GET /.well-known/agent-card.json` que pide el front no lo sirve nadie                                                                                                                                                                                                                                                                                                                            |
| **D-27** | ~~`agent.url` **no existe en `application.yaml`**: solo vive como valor por defecto de una anotación~~ · ✅ **SALDADA — verificado en la Fase 08.** `agent.url` está declarado (`application.yaml`, línea 4, con `agent.timeout` debajo) y lo lee `AgentConnectionProperties`, un `record` con `@ConfigurationProperties(prefix = "agent")`. Alguna fase anterior lo resolvió sin marcar la deuda |
| **D-01** | Build rojo: 15 de 41 tests fallan                                                                                                                                                                                                                                                                                                                                                                 |
| **D-02** | `AgentChatUseCase` (537 líneas) replica el cerebro del agente, sin bean y sin ruta                                                                                                                                                                                                                                                                                                                |
| **D-17** | `UseCasesConfigTest` **miente**: captura `UnsatisfiedDependencyException` y da verde                                                                                                                                                                                                                                                                                                              |

### 🟠 Altas — separación de flujos

| ID       | Deuda                                                                                                                                                                                                                                                                                                                                    |
|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-05** | ~~`Handler` mezcla HTTP + orquestación SSE + parseo + métricas de negocio + ciclo de vida de `Task`~~ · ✅ **SALDADA (Fase 06)** — el ciclo de vida de la `Task` sale a `DashboardTaskTracker` y la orquestación a `DashboardStreamOrchestrator`; `handleDevOpsDashboardStream` pasa de ~90 a **15** líneas y `Handler` de 382 a **192** |
| **D-06** | ~~Mutación in-place de modelos dentro de la cadena reactiva y array compartido `sharedData[1]`~~ · ✅ **SALDADA (Fase 05)** — `BacklogAudit`, `BacklogMetrics`, `StoryQuality` y `StoryUpdate` son `record` inmutables; `sharedData[1]` pasa a `AtomicReference` y nunca se modifica una auditoría, se reemplaza la referencia           |
| **D-07** | ~~Reglas de negocio en el entry-point: partición en lotes y recálculo de métricas~~ · ✅ **SALDADA (Fase 05)** — `partition`, `withUpdates`, `recalculatedFrom` y `unaudited` viven en `domain/model`; reglas de negocio en `entry-points`: 3 → **0**                                                                                    |
| **D-08** | ~~3 prompts + 193 líneas de datos mock embebidos en el dominio~~ · ✅ **SALDADA (Fase 04)** — los 3 prompts a `resources/prompts/` tras `PromptTemplatePort` y el mock a `resources/mock/dashboard.json` tras `DashboardFallbackPort`; `grep` en `domain/` → cero                                                                        |
| **D-09** | ~~Resolución de `AreaPath`/`IterationPath` duplicada literalmente en dos métodos~~ · ✅ **SALDADA (Fase 05)** — el BFF **dejó de construir rutas**: envía el nombre y quien resuelve es el MCP, preguntando a Azure DevOps. `TeamName` y `SprintName` son Value Objects autovalidados (B-01 = C)                                         |
| **D-10** | ~~I/O de ficheros dentro del caso de uso, en un método `void` síncrono~~ · ✅ **SALDADA (Fase 06)** — `saveDashboardReport` devuelve `Mono<Void>` y delega en `ReportStoragePort`; el adaptador de disco (`report-storage-file`) confina la escritura en `boundedElastic`. I/O síncrona en `domain/usecase`: **cero**                    |
| **D-11** | ~~Fallback a mock duplicado en 4 puntos; `isMcpEnabled()` filtrado al entry-point~~ · ✅ **SALDADA (Fase 06)** — un único punto (`askWithFallback`) gobernado por `dashboard.mock-fallback.enabled`, apagado por defecto (B-07); `isMcpEnabled()` y `getMockDashboardData()` salen de la API pública del caso de uso                     |
| **D-16** | 6 bloques `onErrorResume` idénticos; **todo** error se traduce a HTTP 400                                                                                                                                                                                                                                                                |
| **D-28** | El BFF **no valida nada**, pese a ser su misión declarada: `maxResults` acepta cualquier `Integer.parseInt`, `initiativeId` no se valida, el cuerpo de `ingest` tampoco                                                                                                                                                                  |

### 🟡 Medias

| ID       | Deuda                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-03** | ~~DTOs `JsonRpcRequest/Response/Error` sin uso (rol de servidor)~~ · ✅ **SALDADA — verificado en la Fase 08.** Esas tres clases ya no existen: el sobre JSON-RPC vive hoy en `JsonRpcEnvelope`, dentro de `agent-client`, y lo usa `A2AAgentAdapter` en su **rol de cliente**, que es el que le corresponde al BFF. Alguna fase anterior lo resolvió sin marcar la deuda                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **D-04** | ~~`application.yaml` con residuos: `a2a.*`, `agent.system-prompt`, `adapters.aws.s3` **y** `adapter.aws.s3`, consola H2~~ · ✅ **SALDADA (Fase 08)** — `a2a.*` y `agent.system-prompt` ya habían desaparecido. Se retiraron la **consola H2** (no hay dependencia de `com.h2database` en ningún `build.gradle`, y el datasource es PostgreSQL) y el bloque **`adapter.aws.s3`** en singular, copia byte a byte del bueno con el prefijo mal escrito: el único lector es `S3ConnectionProperties`, con `prefix = "adapters.aws.s3"` **en plural**. Fichero 114 → **93** líneas                                                                                                                                                                                                                                                                                                                                                                                                         |
| **D-12** | ~~Tres fachadas de logging conviviendo~~ · ✅ **SALDADA (Fase 08, B-12)** — **una por capa y ninguna a mano**: `@Slf4j` en toda la infraestructura (los 4 `@Log4j2` de `reactive-web` convertidos) y `@Log` de Lombok en el único log del dominio, que **conserva `java.util.logging` a propósito**: es la fachada del JDK, y meter SLF4J en `domain/usecase` sería introducir una dependencia técnica en el dominio                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| **D-13** | ~~Dominio anémico y mutable, con sufijos técnicos (`...Response` en `domain/model`)~~ · ✅ **SALDADA (Fase 05)** — los tres `...Response` se mudaron a `reactive-web/api/dto/dashboard/` con `DashboardDtoMapper` como frontera; ArchUnit *Rule_2.2* 3 → **0**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **D-14** | ~~El dominio se serializa con Jackson en el adaptador, sin entidad ni mapper~~ · ✅ **SALDADA (Fase 07)** — `PlanningChunkEntity` + `PlanningChunkEntityMapper` en `pgvector-store`. El JSON almacenado es idéntico: sin migración ni columnas nuevas (DP-05b). Queda **D-44** para el modelo `a2a`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **D-15** | ~~El entry-point devuelve modelos de dominio como respuesta HTTP~~ · ✅ **SALDADA (Fase 07)** — `PlanningChunkResponse` + `PlanningDtoMapper` en las dos rutas que devolvían `PlanningChunk`. Queda **D-45** para `GET /api/agent/card`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **D-18** | Cobertura ~0 % en `Handler`, adaptadores, `domain/model` y 3 de 5 casos de uso                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **D-19** | ~~Números mágicos: lote `10`, `maxResults 3`, `800 ms`, año actual~~ · ✅ **SALDADA (Fase 05)** — el **año actual** desapareció del BFF **y** del MCP (B-01 = C); el lote pasa a `audit.batch-size` (10 por defecto) más `audit.concurrency` (tope 2), que antes no existía                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **D-20** | ~~`PostgresTaskStoreAdapter` vive en el módulo `task-store-inmemory`~~ · ✅ **SALDADA (Fase 07)** — módulo renombrado a **`task-store-postgres`** con `git mv`; actualizados `settings.gradle`, `app-service/build.gradle` y la ruta codificada de `ArchitectureTest` (DP-05a)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **D-29** | Modelo `a2a` duplicado íntegro entre BFF y agente, sin contrato compartido ni versión                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **D-30** | El BFF no propaga `contextId` ni correlación de trazas hacia el agente                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **D-31** | ~~Un fallo del agente en el stream SSE devuelve **500 en vez de un evento de error**~~ · ✅ **SALDADA (Fase 06)** — el stream **ya nunca termina en error**: el fallo viaja como evento SSE `ERROR` con su mensaje y la respuesta es 200 con cabecera `text/event-stream`. Queda **D-40**: el frontend todavía no lo escucha                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **D-32** | ~~El módulo `mcp-client` queda casi vacío tras retirar `ChatGatewayAdapter`~~ · ✅ **SALDADA (Fase 08, B-11)** — **el módulo se eliminó**: el BFF no habla MCP con nadie. `NoOpAgentResponseAdapter` ya se había ido en la Fase 03; las tres clases restantes eran configuración de un cliente MCP y de su OAuth2. Antes de borrarlo hubo que **rescatar tres cosas que sostenía sin que se viera**: el starter `spring-ai-starter-model-openai` del que depende `pgvector-store` (movido allí), el `maxInMemorySize` de **10 MB** del `WebClient.Builder` que inyectaba `A2AAgentAdapter` (movido al adaptador) y la bandera `spring.ai.mcp.client.enabled`, de la que se **deducía** el repliegue al tablero simulado (sustituida por `dashboard.mock-fallback.enabled`, explícita)                                                                                                                                                                                                 |
| **D-33** | ~~`AgentResponseGateway` y `NoOpAgentResponseAdapter` sin consumidor~~ · ✅ **SALDADA (Fase 03)** — eliminados junto con `ClientRequest`, verificado con `grep`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **D-34** | El agente **no declara el contrato de respuesta** de `GET /api/tasks`: no se sabe si devuelve un array en la raíz o un objeto envolvente. `A2APayloadMapper.toTasks(...)` acepta ambas formas por tolerancia deliberada, no por suposición *(hallazgo de la Fase 03 → se fija junto con D-29 en la Fase 07)*                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **D-35** | **Los prompts deben acabar en el MCP y en el agente, no en el BFF.** La Fase 04 los sacó del dominio a `resources/prompts/`, que era el paso previo obligado; falta el segundo tiempo. Reparto por dueño: las **reglas WIQL** van al MCP —quien define la herramienta define cómo se usa—, la **definición de calidad y `qualityScore`** va al agente (DP-01), y el **esquema JSON de salida se queda en el BFF** porque es su contrato de deserialización: moverlo lo rompería en silencio y en producción. El estado objetivo es que el BFF envíe **intenciones** («audita célula X, sprint Y») en vez de prompts. **Precondiciones:** contrato versionado BFF↔agente (D-29), decidir dónde se declara el esquema JSON, y levantar la restricción de §10 para `azure-devops-agent` y `azure-devops-mcp`. **Coste:** toca los tres repositorios y cambia `POST /api/chat/messages`; **requiere plan propio, no un `T-nn` dentro de una fase** *(decisión del usuario en la Fase 04)* |

| **D-36** | `RestConsumerTest` del MCP no cubre `getTeamFieldValues` **ni** el nuevo
`getTeamIterations`: las **dos** consultas que resuelven `AreaPath` e `IterationPath` carecen de
prueba de integración, justo las que sostienen el arreglo del fallo del año *(hallazgo de la Fase
05 → repositorio `azure-devops-mcp`)* | | **D-37** | El repliegue por concatenación del MCP **sigue
produciendo una ruta con el año del calendario**. Es correcto como red de seguridad —conserva el
comportamiento previo si Azure DevOps no responde— pero hoy nadie sabe con qué frecuencia se
dispara. Hace falta **medirlo** antes de darlo por bueno indefinidamente *(hallazgo de la Fase
05)* | | **D-38** | El `10` del lote de auditoría y el `2` de concurrencia **siguen sin respaldo
empírico**. La Fase 05 los hizo configurables, que era el paso previo, pero falta **medir el coste
real de un lote** para elegirlos con datos en vez de por intuición *(hallazgo de la Fase 05 →
requiere entorno con carga real)* | | **D-39** | El MCP arrastra una violación **preexistente** de
ArchUnit *Rule_2.2* (1 vez), ajena a la Fase 05 y aún sin atender *(hallazgo de la Fase 05 →
repositorio `azure-devops-mcp`)* | | **D-40** | ~~El **frontend no maneja el evento SSE
`ERROR`**~~ · ✅ **SALDADA (Fase 07, B-08)** — el servicio de API escucha `ERROR`, extrae `message` y
lo propaga; el estado ya lo pinta en la UI. **D-31 queda cerrada por completo** | | **D-41** | Los
reportes `.md` viven **solo en disco** (DP-04 = A). Con varias réplicas o sistema de ficheros
efímero **se pierden al reiniciar**. `ReportStoragePort` deja la puerta abierta a S3 sin tocar el
dominio; falta decidir si merece la pena *(consecuencia conocida de DP-04)* | | **D-42** | ~~
`DashboardStreamOrchestrator` y `DashboardTaskTracker` se instancian con `new` dentro del
`Handler`~~ · ✅ **SALDADA (Fase 08, B-13)** — los dos son `@Component` y el `Handler` los
**inyecta**. El javadoc que justificaba el `new` diciendo que no tenían configuración propia no era
exacto: el orquestador leía `audit.batch-size` y `audit.concurrency`, sólo que los declaraba el
`Handler` en su nombre. Su constructor baja de **8 parámetros a 5**. Obligó a la excepción **S-6**
en el `@ContextConfiguration` de **tres** pruebas —también `TaskRoutesTest`—, sin tocar ninguna
aserción | | **D-43** | `GET /api/planning/initiatives` devuelve el `Map<String,Object>` crudo que
sale de las columnas JSONB (`initiative_id`, `initiative_title`, `cell`): **el esquema de
persistencia se publica tal cual por HTTP**. No es un modelo de dominio —por eso no entraba en
D-15—, pero es la misma fuga por otro camino. No se tocó por no conocer todos los consumidores de
esas claves *(hallazgo de la Fase 07)* | | **D-44** | `PostgresTaskStoreAdapter` y el cliente del
agente **serializan el modelo `a2a` con Jackson dentro del adaptador**, exactamente lo que D-14
acaba de arreglar en pgvector. Atada al contrato A2A → se resuelve con **D-29/D-34** *(hallazgo de
la Fase 07)* | | **D-45** | `GET /api/agent/card` devuelve `AgentCard`, un modelo de dominio, sin
DTO intermedio. Misma causa y mismo destino que D-44: **B-10** dejó el contrato `a2a` fuera de la
Fase 07 *(hallazgo de la Fase 07)* | | **D-46** | `application.yaml` declara
`spring.ai.openai.embedding.model`, pero el único lector identificado —`PgVectorConfig`— lee
`spring.ai.openai.embedding.options.model`, **con `options.` en medio**. Si la clave declarada no
alimenta nada, el BFF estaría embebiendo con el valor por defecto del código
(`text-embedding-ada-002`) **sin que nadie lo haya elegido**. No es cosmético: ver **D-48**. **No se
retiró en la Fase 08**: confirmarlo exige saber cómo enlaza esa propiedad la autoconfiguración de
Spring AI 2.0.0 *(hallazgo de la Fase 08)* | | **D-47** | `pgvector-store` arrastra
`spring-ai-starter-model-openai` —heredado de `mcp-client` al eliminarlo— mientras `PgVectorConfig`
publica su **propio** `EmbeddingModel` anotado `@Primary`, construido a mano con `RestClient`.
Probablemente el **starter** sobre; el **`EmbeddingModel` no**, porque el BFF sí vectoriza (ver
D-48). **Se mantuvo para preservar el classpath exacto**: quitarlo sin arrancar contra un pgvector
real sería una suposición *(hallazgo de la Fase 08)* | | **D-48** | **El agente y el BFF escriben
vectores en la misma tabla `planning_chunks`, cada uno con su propio modelo de embeddings y sin nada
que garantice que coinciden.** El BFF no se limita a leer planificaciones ya vectorizadas:
`POST /api/planning/ingest` llega a `vectorStore.add(documents)` y la búsqueda a
`similaritySearch(...)`, y **ambas operaciones calculan embeddings**. Si el agente y el BFF usan
modelos distintos —cosa que D-46 hace plausible por accidente—, en la tabla conviven vectores de
espacios distintos: la búsqueda por similitud **no falla, devuelve resultados malos en silencio**,
que es la peor forma de fallar. Es la misma clase de problema que D-24 (integración por base de
datos compartida), que se cortó para `agent_tasks` pero **sigue viva para el almacén vectorial**.
**Decisión del usuario al cerrar el plan: no se toca ahora; se diseñará una solución aparte** *(
hallazgo de la Fase 08)* |

### ✅ Saldadas

| ID       | Deuda                                                                        | Fase |
|----------|------------------------------------------------------------------------------|------|
| **D-21** | El front iba directo al agente para el chat                                  | 03   |
| **D-24** | Integración por base de datos compartida: el BFF ya no escribe tareas ajenas | 03   |
| **D-25** | El BFF no exponía `tasks/get` ni `tasks/cancel`                              | 03   |
| **D-26** | Nadie servía la agent card que pedía el front                                | 03   |
| **D-28** | El BFF no validaba nada                                                      | 03   |
| **D-30** | El BFF no propagaba `contextId` hacia el agente                              | 03   |
| **D-33** | `AgentResponseGateway` sin consumidor                                        | 03   |
| **D-08** | Prompts y mock embebidos en el dominio                                       | 04   |
| **D-06** | Mutación in-place de modelos y array `sharedData[1]`                         | 05   |
| **D-07** | Reglas de negocio en el entry-point                                          | 05   |
| **D-09** | Resolución de rutas de Azure DevOps duplicada y por concatenación            | 05   |
| **D-13** | Dominio anémico y mutable, con sufijos técnicos                              | 05   |
| **D-19** | Números mágicos, incluido **el año actual**                                  | 05   |
| **D-05** | `Handler` mezclaba HTTP, orquestación SSE, parseo y ciclo de vida de `Task`  | 06   |
| **D-10** | I/O de ficheros síncrona dentro del caso de uso                              | 06   |
| **D-11** | Fallback a mock duplicado en 4 puntos                                        | 06   |
| **D-31** | El stream SSE devolvía un 500 mudo en vez de un evento de error              | 06   |

---

## 4. Contrato objetivo del BFF hacia el front

Propuesta derivada de lo que el front **ya hace hoy**, para que deje de hablar con dos servicios.
Los nombres se ratifican en **DP-07** antes de exponerlos.

| Verbo  | Ruta del BFF             | Delega en el agente                                         | Sustituye a                              |
|--------|--------------------------|-------------------------------------------------------------|------------------------------------------|
| `POST` | `/api/chat/messages`     | `message/send`                                              | `POST /message:send` a `:8082`           |
| `GET`  | `/api/tasks`             | listado del agente **+** tareas propias, con campo `origin` | lectura de la BD compartida              |
| `GET`  | `/api/tasks/{id}`        | `tasks/get`                                                 | — (hoy no existe)                        |
| `POST` | `/api/tasks/{id}/cancel` | `tasks/cancel`                                              | `POST /`, hoy roto                       |
| `GET`  | `/api/agent/card`        | agent card                                                  | `/.well-known/agent-card.json`, hoy roto |

Los flujos `/api/planning/**` y `/api/devops/**` **no cambian de contrato**. Contrato ratificado por
el usuario el 2026-08-29 (DP-07 opción B, DP-07-bis opción B2).

---

## 5. Desglose secuencial de fases

| #      | Fase                                     | Objetivo                                                                                                                                                                                           | Deudas                       | Decisión                   |
|--------|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|----------------------------|
| **01** | **Baseline y red de seguridad**          | Medir y congelar con pruebas de caracterización los flujos vivos, incluida la delegación al agente. Reparar `UseCasesConfigTest`. Cero cambios en `src/main`.                                      | D-17, D-18                   | —                          |
| **02** | **Cliente A2A del agente**               | `AgentGateway` con JSON-RPC 2.0 sobre `POST /`: `message/send`, `tasks/get`, `tasks/cancel`. La `Task` deja de perderse. Retirar el rol de servidor A2A y el cerebro duplicado. Build a **verde**. | D-01…D-03, D-22, D-23, D-29  | **DP-08**                  |
| **03** | **API de tareas del BFF**                | Exponer al front el contrato de §4 y **cortar la BD compartida**: las tareas del agente se piden al agente. Validación de entrada.                                                                 | D-21, D-24…D-28, D-30        | **DP-07**                  |
| **04** | **Prompts y datos fuera del dominio**    | `PromptTemplatePort` + plantillas en `resources/`; mock tras un puerto.                                                                                                                            | D-08, D-19                   | **DP-02**                  |
| **05** | **Dominio del dashboard**                | Value Objects, `BacklogAudit` inmutable, partición en lotes como regla de dominio. **El año del `IterationPath` deja de calcularse: se le pregunta a Azure DevOps.**                               | D-06, D-07, D-09, D-13, D-19 | **DP-03**                  |
| **06** | **Desacople del flujo SSE** ✅           | La orquestación y la `Task` salen del `Handler`. `ReportStoragePort`. Repliegue único. **El 500 mudo pasa a evento SSE `ERROR`.**                                                                  | D-05, D-10, D-11, D-31       | **DP-04** ✅               |
| **07** | **Adaptadores y contrato HTTP** ✅       | Entidad + mapper para `PlanningChunk`, traducción de excepciones, DTOs de respuesta, errores centralizados. **Un solo criterio de traducción de errores.**                                         | D-14, D-15, D-16, D-20, D-40 | **DP-05** ✅, **DP-06** ✅ |
| **08** | **Configuración, cierre y verificación** | Configuración tipada, `application.yaml` limpio, logging unificado, wiring verificado, ArchUnit a cero, informe de cierre.                                                                         | D-04, D-12, D-27, D-32, D-42 | —                          |

---

## 6. Criterios de aceptación

| Métrica                                             |          Baseline |             Objetivo |        Actual (Fase 06) |           **Actual (Fase 07)** |
|-----------------------------------------------------|------------------:|---------------------:|------------------------:|-------------------------------:|
| Tests fallando                                      |            **15** |                **0** |                **0** ✅ |                       **0** ✅ |
| Tests totales                                       |                41 |                ≥ 150 |              **227** ✅ |                     **238** ✅ |
| Servicios con los que habla el front                |             **2** |                **1** |                **1** ✅ |                       **1** ✅ |
| Llamadas del front a rutas inexistentes             |             **2** |                **0** |                **0** ✅ |                       **0** ✅ |
| Endpoints deprecados consumidos por el BFF          |             **1** |                **0** |                **0** ✅ |                       **0** ✅ |
| Tablas compartidas **escritas** por ambos           |             **1** |                **0** |                **0** ✅ |                       **0** ✅ |
| `Task` del agente visible para el front             | por BD compartida | **por contrato A2A** | **por contrato A2A** ✅ |        **por contrato A2A** ✅ |
| **Criterios de traducción de errores**              |                 2 |                **1** |                       2 |                       **1** ✅ |
| Rutas que devuelven modelos de dominio              |                 3 |                **0** |                       3 | **1** *(contrato `a2a`, D-45)* |
| Adaptadores que serializan el dominio con Jackson   |                 3 |                **0** |                       3 | **2** *(contrato `a2a`, D-44)* |
| Módulos con nombre que contradice su contenido      |                 1 |                **0** |                       1 |                       **0** ✅ |
| Líneas de `Handler`                                 |               415 |                ≤ 150 |                 **192** |                        **186** |
| Líneas de `DevOpsDashboardUseCase`                  |               411 |                ≤ 120 |                     208 |                            208 |
| Clases productivas > 300 líneas                     |                 3 |                **0** |                       1 |                          **1** |
| Código con rol equivocado (líneas)                  |             ≥ 590 |                **0** |                **0** ✅ |                       **0** ✅ |
| Cobertura `domain/model` y `domain/usecase`         |    ~0 % / parcial |               ≥ 90 % |  **94,8 % / 98,9 %** ✅ |         **94,8 % / 98,9 %** ✅ |
| Complejidad cognitiva máx. por método               |              > 15 |                 ≤ 15 |                 ≤ 15 ✅ |                        ≤ 15 ✅ |
| Reglas de negocio en `entry-points`                 |                 3 |                **0** |                **0** ✅ |                       **0** ✅ |
| Mutaciones in-place en cadenas reactivas            |                 3 |                **0** |                **0** ✅ |                       **0** ✅ |
| Puntos de repliegue a mock                          |                 4 |                **1** |                **1** ✅ |                       **1** ✅ |
| I/O síncrona en `domain/usecase`                    |                 1 |                **0** |                **0** ✅ |                       **0** ✅ |
| Fachadas de logging                                 |                 3 |                    1 |                       3 |                              3 |
| ArchUnit *Rule_2.2* (sufijos en el dominio)         |                 3 |                **0** |                **0** ✅ |                       **0** ✅ |
| ArchUnit *Rule_2.7* (campos no finales en beans)    |                 6 |                **0** |                   **3** |                          **3** |
| Cálculos del año por calendario en la ruta caliente |                 2 |                **0** |                **0** ✅ |                       **0** ✅ |

> **Sobre las cifras de la Fase 04.** `Handler` sube 17 líneas: son las dos constantes de D-19 con
> su
> javadoc; bajarlo a ≤ 150 es de la Fase 06. `DevOpsDashboardUseCase` baja de 430 a 225 y solo queda
> **una** clase productiva por encima de 300 líneas (`Handler`). La cobertura de `domain/model` cae
> de 88,3 % a 82,2 % porque entran tres tipos nuevos —dos puertos y una excepción— y solo la
> excepción tiene prueba propia; las interfaces no aportan líneas ejecutables.

> **Sobre las cifras de la Fase 05.** Las dos métricas que llevaban cuatro fases clavadas en 3
> —reglas de negocio en `entry-points` y mutaciones in-place— llegan a **0**. La cobertura de
> `domain/model` recupera y supera su máximo histórico (94,8 %) porque los tipos nuevos entran con
> pruebas propias. `Handler` baja a 382 líneas: sigue por encima del objetivo de 150, y es
> precisamente lo que la Fase 06 viene a resolver (D-05).

> **Sobre las cifras de la Fase 07.** Las tres métricas nuevas —criterios de traducción de errores,
> rutas que devuelven dominio y adaptadores que lo serializan— nacen aquí porque hasta ahora no
> había
> forma de contarlas. Las dos que no llegan a cero se quedan a un paso, y en el **mismo** sitio: el
> modelo `a2a`, cuya frontera es D-29/D-34 y que **B-10 dejó fuera** de esta fase. `Handler` baja de
> 192 a 186 líneas sin proponérselo: seis `onErrorResume` de cuatro líneas se convierten en seis de
> una. *Rule_2.7* no sube porque el traductor de errores es **estático y sin inyección**. La única
> clase productiva por encima de 300 líneas sigue siendo `A2APayloadMapper` (351), que traduce
> precisamente el contrato `a2a`: es material de la Fase 08 o de D-29/D-34.

**Criterio transversal:** al cerrar cada fase `gradlew test` termina en verde y el contrato de
`/api/planning/**` y `/api/devops/**` sigue idéntico, salvo donde una `DP-nn` diga lo contrario.

---

## 7. Decisiones

### Resueltas

| ID            | Decisión                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Fuente  | Fecha      |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|------------|
| **DP-01**     | **Reactivar el flujo A2A**, pero como **cliente** del agente y con control del estado de tareas para el front. El cerebro (prompts, intenciones) no se replica en el BFF.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Usuario | 2026-08-29 |
| **DP-08**     | **No bloqueante y reactivo de extremo a extremo**: Angular y Spring, todo. El BFF envía `configuration.blocking = false`, recibe la `Task` en `submitted`/`working` y el front sigue su progreso. Prohibido `block()`, `subscribe()` manual o cualquier salto a imperativo en las cadenas reactivas.                                                                                                                                                                                                                                                                                                                                                                           | Usuario | 2026-08-29 |
| **DP-07**     | **Opción B — rutas nuevas bajo `/api/**`**: `POST /api/chat/messages`, `GET /api/tasks/{id}`, `POST /api/tasks/{id}/cancel`, `GET /api/agent/card`. El BFF no adopta las URLs heredadas porque obligarían a exponer `POST /` —que choca con el fallback de SPA— y a perpetuar `/message:send`, ya deprecado en el agente. `proxy.conf.json` queda con una sola entrada.                                                                                                                                                                                                                                                                                                        | Usuario | 2026-08-29 |
| **DP-07-bis** | **B2 — combinar ambas fuentes con un campo `origin`**: `GET /api/tasks` devuelve la unión de las tareas del agente y las propias del BFF, cada una etiquetada (`AGENT` / `BFF`). El BFF **deja de escribir** tareas ajenas.                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Usuario | 2026-08-29 |
| **DP-02**     | **a → los prompts van al MCP/agente, en dos tiempos**: la Fase 04 los saca del dominio a `resources/prompts/` tras `PromptTemplatePort`; la migración al MCP queda como **D-35**, con plan propio. El **esquema JSON de salida se queda en el BFF**: es su contrato de deserialización y moverlo lo rompería en silencio. · **b → opción B**: el mock se mueve a `resources/mock/dashboard.json` tras `DashboardFallbackPort`; **no se elimina**, es el modo de trabajo local de la POC. · **c → aplazada a la Fase 05** (el año del `IterationPath`).                                                                                                                         | Usuario | 2026-08-29 |
| **D-33**      | **A — eliminar** `AgentResponseGateway` y `NoOpAgentResponseAdapter`: puerto de **salida** propio del rol de servidor A2A ya retirado; para recibir avisos del agente haría falta un canal de **entrada**. Se anota como mejora futura un `TaskUpdateNotifierPort` hacia el front (SSE/WebSocket) que elimine el sondeo de 5 s.                                                                                                                                                                                                                                                                                                                                                | Usuario | 2026-08-29 |
| **DP-03**     | **B-04 → B (excepción nominal al MCP)**, texto literal en §10. · **B-01 → C**: el año **se le pregunta a Azure DevOps**; el BFF deja de construir rutas y envía el nombre. · **B-02 → B**: `audit.batch-size` configurable (10 por defecto) más `audit.concurrency` con tope **2**, que antes no existía. · **B-03 → A por pasos**: dominio inmutable (`BacklogAudit`, `BacklogMetrics`, `StoryQuality`, `StoryUpdate`) y los `...Response` a `reactive-web` como DTO.                                                                                                                                                                                                         | Usuario | 2026-08-29 |
| **S-2**       | **Autorizado modificar `DashboardRoutesCharacterizationTest` en dos líneas de *tipos*** —un `import` y un `any(...)`— sin tocar ninguna aserción, dato ni comportamiento esperado. Surgió al ejecutar la Fase 05: ese test anclaba el nombre y el paquete de los tipos de dominio, y sin liberarlos **D-13 no podía cerrarse en ninguna fase**. Queda registrada también en el javadoc del propio test.                                                                                                                                                                                                                                                                        | Usuario | 2026-08-29 |
| **DP-04**     | **A — solo disco, detrás de un `ReportStoragePort`.** No se comprobó ni se asumió que el bucket S3 configurado sea accesible ni con qué credenciales (Política de No-Asunción); el puerto permite añadirlo después sin tocar el dominio. Consecuencia asumida: con varias réplicas o FS efímero los reportes se pierden (**D-41**). · **B-05 → solo registrar**: perder el reporte no invalida una consulta ya entregada. · **B-06 → sí**: el stream emite un evento SSE `ERROR` con `message`; cambio **aditivo**, el front lo consumirá en **D-40**. · **B-07 → no**: repliegue unificado en un punto del dominio y apagado por defecto (`dashboard.mock-fallback.enabled`). | Usuario | 2026-08-29 |
| **S-3**       | **Autorizado añadir un *stub* de `saveDashboardReport` en `DashboardRoutesCharacterizationTest`.** Al devolver `Mono<Void>` (D-10), Mockito retorna `null` y el stream moría con un NPE **contra un doble de prueba**. Una línea en `setUp`; ninguna aserción tocada.                                                                                                                                                                                                                                                                                                                                                                                                          | Usuario | 2026-08-29 |
| **S-4**       | **Autorizado reescribir dos pruebas de `DashboardRoutesCharacterizationTest`** cuyo comportamiento **deja de existir**: la que fijaba el 500 mudo (B-06) y la que stubeaba `isMcpEnabled()` (B-07), que ni siquiera compilaba. Las diez restantes siguen intactas.                                                                                                                                                                                                                                                                                                                                                                                                             | Usuario | 2026-08-29 |
| **DP-05**     | **a → `task-store-postgres`.** Se autoriza actualizar la ruta codificada del módulo en `ArchitectureTest`, pese a su aviso de «no modificar»: sin ello `issues.json` se escribiría en un directorio inexistente. · **b → `planning_chunks` no admite columnas nuevas y no hay más consumidores previstos**, así que D-14 se cierra con entidad de persistencia + mapper **sobre el esquema actual**, sin DDL ni migración.                                                                                                                                                                                                                                                     | Usuario | 2026-08-29 |
| **DP-06**     | **El mapa de `TaskHandler`, extendido a todo el entry-point**, más `TimeoutException → 504`: `TaskNotFoundException` → **404**; `InvalidAgentRequestException` e `IllegalArgumentException` → **400**; `AgentUnavailableException` → **503**; `AgentExecutionException` → **502**; `TimeoutException` → **504**; el resto → **500**. El cuerpo `{"error": "<mensaje>"}` **no cambia**: lo único que cambia es el código, que es lo que D-16 pedía. · **B-08 → sí**, se puede tocar el frontend, en tarea propia. · **B-09 → sí**, se autorizan las reescrituras de caracterización que provoque D-16. · **B-10 → no**, D-29/D-34 esperan a D-35.                               | Usuario | 2026-08-29 |
| **S-5**       | **Autorizado reescribir dos pruebas de caracterización** —una en `DashboardRoutesCharacterizationTest` y otra en `PlanningRoutesCharacterizationTest`— que fijaban el **400 indiscriminado** que D-16 existe para eliminar. Ambas pasan a esperar **500** y **ninguna otra aserción se tocó**; el cuerpo de la respuesta sigue igual. Ambas llevaban desde la Fase 01 un javadoc que anunciaba este cambio.                                                                                                                                                                                                                                                                    | Usuario | 2026-08-29 |

### Pendientes

| ID        | Fase | Decisión requerida                                                                                                                                                                                                       |
|-----------|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-02** | 04   | ✅ **RESUELTA (2026-08-29)** — ver §«Resueltas».                                                                                                                                                                         |
| **DP-03** | 05   | ✅ **RESUELTA (2026-08-29)** — B-01 a B-04, más **S-2** surgido en ejecución. Ver §«Resueltas» y [`BLOQUEANTES-FASE-05.md`](BLOQUEANTES-FASE-05.md).                                                                     |
| **DP-04** | 06   | ✅ **RESUELTA (2026-08-29)** — opción **A**, más B-05, B-06, B-07 y las excepciones **S-3** y **S-4** surgidas en ejecución. Ver §«Resueltas» y [`RESULTADO-FASE-06.md`](../resultados/RESULTADO-FASE-06.md).            |
| **DP-05** | 07   | ✅ **RESUELTA (2026-08-29)** — módulo a **`task-store-postgres`**; `planning_chunks` **sin columnas nuevas ni consumidores adicionales**, así que D-14 se cerró sin migración. Ver §«Resueltas».                         |
| **DP-06** | 07   | ✅ **RESUELTA (2026-08-29)** — mapa único de excepción → código HTTP, más B-08, B-09, B-10 y la excepción **S-5** surgida en ejecución. Ver §«Resueltas» y [`RESULTADO-FASE-07.md`](../resultados/RESULTADO-FASE-07.md). |

> **Ninguna decisión pendiente para la Fase 08.**

---

## 8. Protocolo de continuidad

1. Cada fase vive en `docs/fases/fase-NN.md` con **Contexto**, **Instrucciones** y **Orden de
   Ejecución**.
2. Al cerrar una fase: marcar su checklist, rellenar su bloque **Resultado**, actualizar §9 aquí y
   **generar `fase-NN+1.md`** con el estado real alcanzado.
3. Punto de reanudación ante reinicio: leer §9, abrir el último `fase-NN.md` sin cerrar y continuar
   por el primer paso sin marcar.
4. Ninguna fase arranca con el build en rojo heredado (la 01 es la única excepción, declarada).
5. Ninguna `DP-nn` se resuelve por cuenta propia (`rules/spring-rules.md` §6).

---

## 9. Bitácora

| Fase | Estado            | Fecha      |                     Tests | Resultado                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|------|-------------------|------------|--------------------------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 01   | 🟢 **COMPLETADA** | 2026-08-29 | **117** (15 ❌ heredados) | [`RESULTADO-FASE-01.md`](../resultados/RESULTADO-FASE-01.md) · +51 pruebas · 4 tests falsos eliminados · D-31 detectada                                                                                                                                                                                                                                                                                                                                                                              |
| 02   | 🟢 **COMPLETADA** | 2026-08-29 |            **128** (0 ❌) | [`RESULTADO-FASE-02.md`](../resultados/RESULTADO-FASE-02.md) · **build a verde** · cliente A2A operativo · −708 líneas productivas · D-32 y D-33 detectadas                                                                                                                                                                                                                                                                                                                                          |
| 03   | 🟢 **COMPLETADA** | 2026-08-29 |            **153** (0 ❌) | [`RESULTADO-FASE-03.md`](../resultados/RESULTADO-FASE-03.md) · **el front pasa a hablar con 1 solo servicio** · 5 rutas nuevas · BD compartida cortada · D-34 detectada                                                                                                                                                                                                                                                                                                                              |
| 04   | 🟢 **COMPLETADA** | 2026-08-29 |            **170** (0 ❌) | [`RESULTADO-FASE-04.md`](../resultados/RESULTADO-FASE-04.md) · **D-08 saldada** · 184 líneas de prompts y mock fuera del dominio · `DevOpsDashboardUseCase` 430 → **225** · D-19 parcial · D-35 detectada                                                                                                                                                                                                                                                                                            |
| 05   | 🟢 **COMPLETADA** | 2026-08-29 |            **219** (0 ❌) | [`RESULTADO-FASE-05.md`](../resultados/RESULTADO-FASE-05.md) · **D-06, D-07, D-09, D-13 y D-19 saldadas** · reglas de negocio en `entry-points` 3 → **0** · mutaciones in-place 3 → **0** · **el fallo del año cerrado en el BFF y en el MCP** · ArchUnit *Rule_2.2* 3 → **0** · D-36 a D-39 detectadas                                                                                                                                                                                              |
| 06   | 🟢 **COMPLETADA** | 2026-08-29 |            **227** (0 ❌) | [`RESULTADO-FASE-06.md`](../resultados/RESULTADO-FASE-06.md) · **D-05, D-10, D-11 y D-31 saldadas** · **el 500 mudo pasa a evento SSE `ERROR`** · `Handler` 382 → **192** y su método del stream ~90 → **15** · repliegue a mock 4 → **1** · I/O síncrona en el dominio → **0** · *Rule_2.7* 6 → **3** · D-40 a D-42 detectadas                                                                                                                                                                      |
| 07   | 🟢 **COMPLETADA** | 2026-08-29 |            **238** (0 ❌) | [`RESULTADO-FASE-07.md`](../resultados/RESULTADO-FASE-07.md) · **D-14, D-15, D-16, D-20 y D-40 saldadas** · criterios de traducción de errores 2 → **1** · módulo `task-store-inmemory` → **`task-store-postgres`** · entidad de persistencia para `PlanningChunk` **sin migración** · el frontend por fin escucha el evento `ERROR` (29 pruebas, 0 ❌) · D-43 a D-45 detectadas                                                                                                                     |
| 08   | 🟢 **COMPLETADA** | 2026-08-29 |            **238** (0 ❌) | [`RESULTADO-FASE-08.md`](../resultados/RESULTADO-FASE-08.md) · **D-04, D-12, D-32 y D-42 saldadas**; **D-27 y D-03 verificadas como ya saldadas** · módulo **`mcp-client` eliminado** (el BFF no habla MCP) · `application.yaml` 114 → **93** líneas, cada borrado con su `grep` · fachadas de logging 3 → **2**, una por capa, y declaraciones a mano 1 → **0** · colaboradores con `new` en el entry-point 2 → **0** (excepción **S-6**) · *Rule_2.7* 3 → **0** · D-46, D-47 y **D-48** detectadas |

> 🟢 **PLAN CERRADO** el 2026-08-29 con las 8 fases ejecutadas.
> Informe de cierre: [`CIERRE-DEL-PLAN.md`](../resultados/CIERRE-DEL-PLAN.md) — recorrido de las 8
> fases, estado final de las deudas (**48 registradas · 33 saldadas · 15 vivas**) y dueño de cada
> una
> de las que siguen vivas.

---

## 10. Fuera de alcance

- Modificar `azure-devops-agent` y `azure-devops-mcp`.
  > **Excepción vigente, autorizada el 2026-08-29 (B-04, Fase 05):** *se autoriza modificar
  > `azure-devops-mcp` **exclusivamente** para añadir la resolución de `IterationPath` por consulta
  a
  > Azure DevOps, simétrica a la que ya existe para `AreaPath`, conservando la concatenación actual
  > como fallback. Cualquier otro cambio en ese repositorio sigue fuera de alcance.* Esta excepción
  > **ya se consumió** en la Fase 05 y **no cubre D-35** (migrar los prompts).
- Cambios en el **frontend**: se documentan los ajustes necesarios (`proxy.conf.json` y
  `devops-agent-api.service.ts`), pero se ejecutan fuera de este plan (DP-07).
  > **Excepción vigente, autorizada el 2026-08-29 (B-08, Fase 07):** *se autoriza modificar
  > `azure-devops-frontend` **exclusivamente** para consumir el evento SSE `ERROR` (D-40) y para
  > absorber los códigos HTTP nuevos de D-16.* Se consumió en la Fase 07 con el alcance declarado:
  > tres ficheros y sus pruebas.
- Sustituir `NoOpAgentResponseAdapter` por un productor Kafka real.
- Cambiar el proveedor de LLM o el protocolo MCP.
- Migrar el esquema de base de datos, salvo lo que autorice DP-05.





