# RESULTADO — FASE 02: Cliente A2A del agente y control de tareas

> **Ejecutada:** 2026-08-29 · **Estado final:** 🟢 COMPLETADA
> **Instrucciones:** `docs/fases/fase-02.md` · **Plan:** `docs/plan/plan-maestro.md` v2.0
> **Hito:** **el build pasa de rojo a verde** — 15 fallos → **0**

---

## 1. Objetivo de la fase

Convertir al BFF en un **cliente A2A** del agente en condiciones: hablar JSON-RPC 2.0 en lugar del
endpoint legacy vencido, dejar de descartar la `Task` que el agente devuelve, y retirar del BFF el
rol de servidor A2A que replicaba el cerebro del agente.

---

## 2. Qué se construyó

### `domain/model` — el dominio del agente

| Archivo                                                    | Propósito                                                                        |
|------------------------------------------------------------|----------------------------------------------------------------------------------|
| `model/agent/AgentCommand.java`                            | Value Object de la orden. Valida en construcción; `blocking = false` por defecto |
| `model/agent/AgentInteraction.java`                        | **La pieza clave**: `Task` + `reply`. Salda D-23                                 |
| `model/agent/gateways/AgentGateway.java`                   | Puerto con `sendMessage`, `getTask`, `cancelTask`                                |
| `model/agent/exceptions/AgentException.java`               | Raíz de los fallos del agente                                                    |
| `model/agent/exceptions/TaskNotFoundException.java`        | Traduce `-32004`                                                                 |
| `model/agent/exceptions/InvalidAgentRequestException.java` | Traduce `-32600`, `-32601`, `-32602`                                             |
| `model/agent/exceptions/AgentExecutionException.java`      | Traduce `-32603` y códigos desconocidos                                          |
| `model/agent/exceptions/AgentUnavailableException.java`    | Fallos de transporte y timeout                                                   |

### `domain/usecase`

| Archivo                                    | Propósito                                                |
|--------------------------------------------|----------------------------------------------------------|
| `usecase/agent/TrackAgentTaskUseCase.java` | `sendAndTrack`, `refreshTask`, `cancelTask`, `listTasks` |

### `infrastructure/driven-adapters/agent-client` — módulo nuevo

| Archivo                                             | Propósito                                                                |
|-----------------------------------------------------|--------------------------------------------------------------------------|
| `agentclient/A2AAgentAdapter.java`                  | Cliente JSON-RPC 2.0 sobre `POST /`, con timeout y traducción de errores |
| `agentclient/A2APayloadMapper.java`                 | Mapper obligatorio JSON ⇄ dominio                                        |
| `agentclient/JsonRpcEnvelope.java`                  | Sobres y códigos del protocolo, como detalle del adaptador               |
| `agentclient/config/AgentConnectionProperties.java` | Configuración tipada `agent.url` y `agent.timeout`                       |
| `build.gradle`                                      | Módulo registrado en `settings.gradle` y en `app-service`                |

### Eliminado

| Archivo                                             |  Líneas | Motivo                                                                       |
|-----------------------------------------------------|--------:|------------------------------------------------------------------------------|
| `usecase/chat/AgentChatUseCase.java`                | **537** | Replicaba el cerebro del agente: prompts, intenciones, división de historias |
| `usecase/chat/AgentChatUseCaseTest.java`            |     351 | Probaba código que nunca se ejecutaba                                        |
| `api/RouterRestTest.java`                           |     333 | Probaba el rol de **servidor** A2A que el BFF no tiene                       |
| `api/JsonRpcErrorContractTest.java`                 |     146 | Ídem                                                                         |
| `api/RouterRestLegacyToggleTest.java`               |     106 | Ídem                                                                         |
| `api/RouterRestLegacySunsetTest.java`               |      53 | Ídem                                                                         |
| `api/JsonRpcRequest/Response/Error.java`            |      57 | Sobres del rol de servidor                                                   |
| `mcpclient/adapter/ChatGatewayAdapter.java`         |      85 | Sustituido por `A2AAgentAdapter`                                             |
| `model/chat/gateways/ChatGateway.java`              |      29 | Sustituido por `AgentGateway`                                                |
| `mcpclient/adapter/ChatGatewayAdapterTest.java`     |     217 | Ídem                                                                         |
| `application.yaml`: `a2a.*` y `agent.system-prompt` |      18 | El *system prompt* es del agente y allí ya vive                              |

**Total eliminado: ~1 930 líneas**, de las cuales **708 son código productivo**.

### Modificado

- `UseCasesConfig.java`: se eliminó el `@ComponentScan` con filtros por expresión regular y la
  exclusión a mano de `AgentChatUseCase`. Ahora hay **cinco `@Bean` explícitos**.
- `DevOpsDashboardUseCase.java`: migrado a `AgentGateway` mediante un método `ask()` privado. Sus
  prompts, su resolución de rutas y su fallback **no se tocaron**.
- `application.yaml`: bloque `agent:` con `url` y `timeout`.

---

## 3. Decisiones aplicadas

| ID        | Resolución del usuario                                           | Cómo se materializó                                                                                                                                                                                                                       |
|-----------|------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-01** | Reactivar A2A **como cliente**, con control del estado de tareas | `AgentGateway` + `TrackAgentTaskUseCase`. Se conservó todo el modelo `a2a` y se eliminó el rol de servidor                                                                                                                                |
| **DP-08** | **No bloqueante y reactivo de extremo a extremo**                | `AgentCommand.blocking()` es `false` por defecto y viaja como `configuration.blocking:false`. `TrackAgentTaskUseCase.refreshTask()` da soporte al sondeo del front. **Cero `block()`, cero `subscribe()` manual** en todo el código nuevo |

---

## 4. Métricas obtenidas

| Métrica                                        |          Antes (Fase 01) |         Después |
|------------------------------------------------|-------------------------:|----------------:|
| **Tests fallando**                             |                       15 |        **0** ✅ |
| **Tests totales**                              |                      117 |         **128** |
| Endpoints deprecados consumidos                |                        1 |        **0** ✅ |
| `Task` del agente disponible en el dominio     |                       ❌ |          **✅** |
| Líneas de código productivo con rol equivocado |                      708 |        **0** ✅ |
| Clase productiva más larga                     | `AgentChatUseCase` (537) | `Handler` (415) |
| Cobertura `domain/usecase`                     |                   85,8 % |      **96,5 %** |
| Cobertura `agent-client`                       |                        — |      **85,6 %** |
| Cobertura `reactive-web`                       |               no medible |      **87,1 %** |
| Cobertura `domain/model`                       |                      0 % |          58,4 % |
| Cobertura global                               |               no medible |      **60,7 %** |
| `jacocoMergedReport`                           |               ❌ fallaba | **✅ funciona** |

### Detalle por suite

| Suite                                 |                  Tests |
|---------------------------------------|-----------------------:|
| `A2AAgentAdapterTest`                 |                     24 |
| `DevOpsDashboardUseCaseTest`          |                     18 |
| `AgentCommandTest`                    |                     15 |
| `PlanningRoutesCharacterizationTest`  |                     15 |
| `DashboardRoutesCharacterizationTest` |                     12 |
| `ManagePlanningUseCaseTest`           |                      9 |
| `IngestPlanningSpecUseCaseTest`       |                      8 |
| `TrackAgentTaskUseCaseTest`           |                      8 |
| `SearchPlanningSpecUseCaseTest`       |                      7 |
| `ArchitectureTest`                    |                      6 |
| `UseCasesConfigTest`                  |                      5 |
| `MicrometerMetricPublisherTest`       |                      1 |
| **Total**                             | **128** · **0 fallos** |

---

## 5. Comandos ejecutados

```powershell
.\gradlew.bat :model:test --rerun-tasks
.\gradlew.bat :agent-client:test --rerun-tasks
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test
.\gradlew.bat jacocoMergedReport --no-configuration-cache
```

**Verificación de eliminaciones** (`grep`, no el compilador — `CIERRE-DEL-PLAN.md` §7.3):

```powershell
Select-String -Pattern 'message:send'                      # solo en agent-client, y como comentario
Select-String -Pattern 'AgentChatUseCase|JsonRpcRequest'   # solo en Javadoc
Select-String -Pattern 'ChatGateway'                       # cero referencias productivas
```

---

## 6. Desviaciones respecto a las instrucciones

| Desviación                                                  | Justificación                                                                                                                                                                                                                                                                         |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Se eliminó también `ChatGateway` y `ChatGatewayAdapter`     | No estaba en el plan de la fase, pero al migrar `DevOpsDashboardUseCase` quedaron **sin ningún consumidor**. Mantenerlos habría sido dejar código muerto recién creado                                                                                                                |
| `DevOpsDashboardUseCase` envía `AgentCommand.blocking(...)` | Ese flujo consume el texto del agente de inmediato para construir el tablero: en modo no bloqueante no habría nada que parsear. Se dejó documentado en el Javadoc del método `ask()`, y su rediseño corresponde a la Fase 06. **El resto del BFF es no bloqueante**, conforme a DP-08 |
| No se creó `A2APayloadMapperTest` aparte                    | El mapper se ejercita a través de las 24 pruebas de `A2AAgentAdapterTest`, que lo cubren de punta a punta con JSON real. Cobertura de `agent-client`: 85,6 %                                                                                                                          |
| Los tests de los Value Objects viven en `AgentCommandTest`  | Un único archivo para `AgentCommand` y `AgentInteraction`: son dos records pequeños y complementarios                                                                                                                                                                                 |

---

## 7. Checklist de calidad

| Criterio (`rules/spring-rules.md` §7)                    | Estado                                                                 |
|----------------------------------------------------------|------------------------------------------------------------------------|
| Estructura de paquetes respetada                         | ✅ módulo nuevo en `driven-adapters`                                   |
| `domain/model` sin dependencias técnicas                 | ✅ los Value Objects solo usan `java.*` y Reactor                      |
| `domain/usecase` sin anotaciones de Spring               | ✅ `TrackAgentTaskUseCase` solo usa `@RequiredArgsConstructor`         |
| Mappers de conversión en `driven-adapters`               | ✅ `A2APayloadMapper`                                                  |
| Excepciones técnicas traducidas a excepciones de dominio | ✅ los 6 códigos JSON-RPC y los fallos de transporte                   |
| Sin secretos hardcodeados (S2068)                        | ✅ `agent.url` por variable de entorno                                 |
| Complejidad cognitiva ≤ 15                               | ✅                                                                     |
| Sin números mágicos (S109)                               | ✅ los códigos JSON-RPC son constantes con nombre                      |
| Llaves `{}` en todo control de flujo (S1117)             | ✅                                                                     |
| Convención `givenX_whenY_thenZ`                          | ✅                                                                     |
| Reactivo puro, sin `block()` (DP-08)                     | ✅                                                                     |
| Wiring verificado de verdad                              | ✅ `UseCasesConfigTest` afirma los 5 beans y falla si falta un gateway |
| **Build verde**                                          | ✅ **128/128**                                                         |

---

## 8. Hallazgos

### H-1 · El `@ComponentScan` de `UseCasesConfig` era redundante y peligroso

Al eliminar `AgentChatUseCase` desapareció el `excludeFilter` que lo apuntaba, y al revisarlo se vio
que el `@ComponentScan` con `FilterType.REGEX` **duplicaba** los beans ya declarados con `@Bean`.
Funcionaba por accidente: la definición explícita prevalecía. Se sustituyó por cinco `@Bean`
explícitos. Es la clase de configuración que la Fase 08 dejará tipada.

### H-2 · El módulo `mcp-client` ya casi no tiene razón de ser

Tras retirar `ChatGatewayAdapter`, en `mcp-client` solo quedan `NoOpAgentResponseAdapter` y tres
clases de configuración OAuth2/MCP. Su nombre siempre fue engañoso —el BFF no habla MCP con nadie;
es el agente quien lo hace—, y ahora también está casi vacío. Su cobertura, 24,3 %, es la más baja
del proyecto.

→ Registrado como **D-32**, se evalúa en la **Fase 08**.

### H-3 · `AgentResponseGateway` y `NoOpAgentResponseAdapter` quedaron sin consumidor

Eran dependencias de `AgentChatUseCase`. Siguiendo la lección DP-05 del proyecto agente —donde algo
que parecía código muerto resultó ser un punto de extensión A2A/Kafka deliberado— **no se
eliminaron**: quedan pendientes de decisión del usuario.

→ Registrado como **D-33**, requiere decisión antes de la Fase 08.

### H-4 · `domain/model` sigue con cobertura baja pese a subir de 0 % a 58,4 %

Los 15 tests nuevos cubren el paquete `agent`, pero el paquete `a2a` (`Task`, `Message`, `Part`,
`AgentCard`…) sigue sin pruebas propias. Se atiende en la Fase 07.

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `docs/fases/fase-03.md` — API de tareas del BFF hacia el front
- **Decisiones resueltas:** DP-01, DP-08
- **Decisiones abiertas:** DP-07 (Fase 03), DP-02 (Fase 04), DP-03 (Fase 05), DP-04 (Fase 06), DP-05
  y DP-06 (Fase 07)
- **Deudas nuevas registradas:** **D-32** (módulo `mcp-client` casi vacío y mal nombrado), **D-33**
  (`AgentResponseGateway` sin consumidor)
- **Pendiente de ratificación del usuario:** el destino de `AgentResponseGateway` /
  `NoOpAgentResponseAdapter` (D-33)
- **Estado del build:** 🟢 **verde por primera vez en el plan** — 128 pruebas, 0 fallos

