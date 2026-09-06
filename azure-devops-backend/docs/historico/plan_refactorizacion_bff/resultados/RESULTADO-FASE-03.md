# RESULTADO — FASE 03: API de tareas del BFF hacia el frontend

> **Ejecutada:** 2026-08-29 · **Estado final:** 🟢 COMPLETADA
> **Instrucciones:** `docs/fases/fase-03.md` · **Plan:** `docs/plan/plan-maestro.md` v2.0
> **Hito:** **el frontend pasa a hablar con un solo servicio** — 2 → **1**

---

## 1. Objetivo de la fase

Exponer al frontend el contrato completo del BFF bajo `/api/**` y **cortar la integración por base
de datos compartida**: las tareas del agente se le piden al agente, no se leen de una tabla ajena.
Añadir de paso la validación de entrada, que era la misión declarada del BFF y no cumplía.

Deudas atacadas: **D-21, D-24, D-25, D-26, D-28, D-30, D-33**.

---

## 2. Qué se construyó

### `domain/model` — el origen de las tareas

| Archivo                        | Propósito                                                                                                                                               |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `model/agent/TaskOrigin.java`  | Enumerado `AGENT` / `BFF`. Materializa DP-07-bis                                                                                                        |
| `model/agent/TrackedTask.java` | Record `Task` + `TaskOrigin`, con factorías `fromAgent` / `fromBff`. **No se tocó `Task`**: es el modelo A2A compartido y debe seguir fiel al protocolo |

**Modificado:** `model/agent/gateways/AgentGateway.java` gana `listTasks()` y `getAgentCard()`, con
Javadoc que advierte de que en el agente **no son JSON-RPC sino REST plano**.

### `domain/usecase`

**Modificado:** `usecase/agent/TrackAgentTaskUseCase.java`

- `listTasks()` reescrito: combina agente + BFF, deduplica por identificador y **degrada** a las
  tareas del BFF si el agente no responde.
- `sendAndTrack`, `refreshTask` y `cancelTask` **dejan de llamar a `taskStoreGateway.save(...)`**.
- Nuevo `getAgentCard()`.

### `infrastructure/driven-adapters/agent-client`

**Modificado:** `A2AAgentAdapter.java` — método privado `get(path, type)` separado de `dispatch`,
porque las rutas REST no llevan sobre JSON-RPC ni pueden fallar con los códigos `-326xx`. Comparten
timeout y traducción de fallos de transporte.

**Modificado:** `A2APayloadMapper.java` — `toTasks(...)` y `toAgentCard(...)` con sus auxiliares
para `AgentProvider`, `AgentCapabilities`, `AgentInterface` y `AgentSkill`.

### `infrastructure/entry-points/reactive-web` — el contrato hacia el frontend

| Archivo                                 | Propósito                                                                       |
|-----------------------------------------|---------------------------------------------------------------------------------|
| `api/TaskHandler.java`                  | Las cinco rutas nuevas, el mapa de errores HTTP y la propagación de `contextId` |
| `api/RequestValidator.java`             | Validación de entrada dentro de la cadena reactiva (D-28)                       |
| `api/dto/task/TaskResponse.java`        | DTO de tarea con `origin`                                                       |
| `api/dto/task/TaskStatusResponse.java`  | Estado con el valor A2A en minúsculas                                           |
| `api/dto/task/ChatMessageResponse.java` | `reply` + `task`                                                                |
| `api/dto/task/TaskDtoMapper.java`       | Mapper estático dominio → DTO                                                   |

**Modificado:** `RouterRest.java` — las cinco rutas registradas **antes** del fallback de SPA.
**Modificado:** `Handler.java` — pierde `handleListTasks()` (pasa a `TaskHandler`) y su
`handleSearchPlanning` valida `maxResults`.

### Eliminado (D-33 → A)

| Archivo                                           | Líneas | Motivo                                                                             |
|---------------------------------------------------|-------:|------------------------------------------------------------------------------------|
| `model/chat/gateways/AgentResponseGateway.java`   |     37 | Puerto de **salida** del rol de servidor A2A ya retirado                           |
| `mcpclient/adapter/NoOpAgentResponseAdapter.java` |     17 | Su única implementación, sin consumidor                                            |
| `model/chat/ClientRequest.java`                   |     17 | Sin consumidor; además violaba la regla ArchUnit de sufijos técnicos en el dominio |

**Total eliminado: 71 líneas productivas.** El paquete `model/chat` **no** queda vacío: conserva
`gateways/TaskStoreGateway.java`, que sigue en uso para las tareas propias del BFF.

---

## 3. Decisiones aplicadas

| ID            | Resolución del usuario                      | Cómo se materializó                                                                                                                                              |
|---------------|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-07**     | **Opción B: rutas nuevas bajo `/api/**`**   | Las cinco rutas en `TaskHandler` + `RouterRest`. Las heredadas (`POST /`, `/.well-known/...`) siguen deliberadamente sin existir, y hay una prueba que lo afirma |
| **DP-07-bis** | **B2: combinar ambas fuentes con `origin`** | `TrackedTask` + `TaskOrigin`; `listTasks()` hace merge, dedup y degradación. El BFF dejó de escribir tareas ajenas                                               |
| **D-33**      | **A: eliminar**                             | Los tres archivos borrados, verificado con `grep` antes y después                                                                                                |
| **DP-08**     | No bloqueante de extremo a extremo          | `AgentCommand.nonBlocking(...)` en el chat; `grep` de `block()`/`subscribe()`/`toFuture()` en `src/main` → **cero**                                              |

---

## 4. Métricas obtenidas

| Métrica                                     | Antes (Fase 02) |                         Después |
|---------------------------------------------|----------------:|--------------------------------:|
| **Tests totales**                           |             128 |                         **153** |
| **Tests fallando**                          |               0 |                        **0** ✅ |
| **Servicios con los que habla el frontend** |               2 |                        **1** ✅ |
| **Llamadas del front a rutas inexistentes** |               2 |                        **0** ✅ |
| Rutas nuevas expuestas                      |               0 |                           **5** |
| El BFF escribe tareas del agente en BD      |              sí |                       **no** ✅ |
| Validación de entrada en el BFF             |         ninguna |              `RequestValidator` |
| Líneas productivas eliminadas               |               — |                          **71** |
| Cobertura `domain/usecase`                  |          96,5 % | **96,4 %** (objetivo ≥ 90 %) ✅ |
| Cobertura `reactive-web`                    |          87,1 % | **89,8 %** (objetivo ≥ 85 %) ✅ |
| Cobertura `agent-client`                    |          85,6 % |                      **91,4 %** |
| Cobertura `domain/model`                    |          58,4 % |                      **88,3 %** |
| Cobertura global                            |          60,7 % |                      **66,8 %** |

### Detalle por suite

| Suite                                 |   Tests |                          Δ |
|---------------------------------------|--------:|---------------------------:|
| `A2AAgentAdapterTest`                 |      30 |                         +6 |
| `DevOpsDashboardUseCaseTest`          |      18 |                          — |
| `AgentCommandTest`                    |      15 |                          — |
| `PlanningRoutesCharacterizationTest`  |      15 | ±0 (−2 movidas, +2 nuevas) |
| **`TaskRoutesTest`**                  |  **15** |            **+15 (nueva)** |
| `TrackAgentTaskUseCaseTest`           |      12 |                         +4 |
| `DashboardRoutesCharacterizationTest` |      12 |                          — |
| `ManagePlanningUseCaseTest`           |       9 |                          — |
| `IngestPlanningSpecUseCaseTest`       |       8 |                          — |
| `SearchPlanningSpecUseCaseTest`       |       7 |                          — |
| `ArchitectureTest`                    |       6 |                          — |
| `UseCasesConfigTest`                  |       5 |                          — |
| `MicrometerMetricPublisherTest`       |       1 |                          — |
| **Total**                             | **153** |                    **+25** |

### Contrato expuesto

| Verbo  | Ruta                     | Delega en                        | Sustituye a                                     |
|--------|--------------------------|----------------------------------|-------------------------------------------------|
| `POST` | `/api/chat/messages`     | `sendAndTrack`                   | `POST /message:send` a `:8082`                  |
| `GET`  | `/api/tasks`             | `listTasks` (unión con `origin`) | lectura de la tabla compartida                  |
| `GET`  | `/api/tasks/{id}`        | `refreshTask`                    | — (no existía)                                  |
| `POST` | `/api/tasks/{id}/cancel` | `cancelTask`                     | `POST /`, que estaba roto                       |
| `GET`  | `/api/agent/card`        | `getAgentCard`                   | `/.well-known/agent-card.json`, que estaba roto |

---

## 5. Comandos ejecutados

```powershell
.\gradlew.bat compileJava
.\gradlew.bat :agent-client:test --rerun-tasks -x pitest
.\gradlew.bat :usecase:test      --rerun-tasks -x pitest
.\gradlew.bat :reactive-web:test --rerun-tasks -x pitest
.\gradlew.bat test                                   # 153 / 0 fallos
.\gradlew.bat jacocoMergedReport --no-configuration-cache
```

**Verificación de eliminaciones** (`grep`, no el compilador):

```powershell
# AgentResponseGateway | NoOpAgentResponseAdapter | model.chat.ClientRequest en src/main
#   → cero referencias
# .block() | .blockFirst | .blockLast | .subscribe( | .toFuture( en src/main
#   → cero referencias  (DP-08)
```

---

## 6. Desviaciones respecto a las instrucciones

| Desviación                                                                                                                                                                   | Justificación                                                                                                                                                                                                                                                                                                                                                            |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Las dos pruebas de `GET /api/tasks` no se «ajustaron» en `PlanningRoutesCharacterizationTest`, sino que se **sustituyeron por una** que afirma el cambio de dueño de la ruta | Su contrato completo, con `origin` y los códigos de error, se prueba en `TaskRoutesTest`, que es donde corresponde. Duplicarlo en la suite de caracterización habría sido ruido. La prueba que queda congela lo relevante: la ruta sigue publicada y **ya no toca `taskStoreGateway`**                                                                                   |
| `listTasks()` usa `Flux.concat` en vez de `Flux.merge` como sugería el guion de T-03                                                                                         | `merge` intercala por orden de llegada, lo que hace **no determinista** cuál de dos tareas duplicadas gana. Con `concat`, el agente se emite primero y la regla «prevalece la del agente» se cumple siempre. Sin `concat` la deduplicación pedida sería un volado                                                                                                        |
| Se añadió una prueba extra de `maxResults` negativo                                                                                                                          | T-07 pedía cubrir «no numérico **o negativo**»; con una sola prueba solo quedaba cubierto el primer caso                                                                                                                                                                                                                                                                 |
| `DashboardRoutesCharacterizationTest` se modificó                                                                                                                            | Ajuste **mecánico de wiring**: `RouterRest` ahora recibe también `TaskHandler`, y sin el bean el contexto de Spring no levanta. No se tocó ninguna aserción; **no cuenta como prueba invertida**                                                                                                                                                                         |
| `UseCasesConfigTest.CapturingAgentGateway` se modificó                                                                                                                       | Ídem: es un doble de prueba que implementa `AgentGateway` y debía cubrir los dos métodos nuevos                                                                                                                                                                                                                                                                          |
| La validación de `query` vacía y de `ingest` sin `initiativeId` **no se implementó**                                                                                         | Ambas ya tienen prueba de caracterización que congela el comportamiento actual (`givenSearchFails_whenGet_then400`, y el caso de uso valida `initiativeId` internamente). Cambiarlo habría roto pruebas de la Fase 01 **sin que el plan lo autorizase**. `RequestValidator.requireText(...)` queda disponible para cuando la Fase 07 unifique el mapa de errores (DP-06) |

---

## 7. Checklist de calidad

| Criterio (`rules/spring-rules.md` §7)                      | Estado                                                                        |
|------------------------------------------------------------|-------------------------------------------------------------------------------|
| Estructura de paquetes respetada                           | ✅ DTOs en `entry-points`, modelo en `domain/model`                           |
| `domain/model` sin dependencias técnicas                   | ✅ `TaskOrigin` y `TrackedTask` solo usan `java.*`                            |
| `domain/usecase` sin anotaciones de Spring                 | ✅ solo `@RequiredArgsConstructor`                                            |
| Mappers de conversión en su capa                           | ✅ `A2APayloadMapper` en `driven-adapters`, `TaskDtoMapper` en `entry-points` |
| El entry-point no devuelve el dominio                      | ✅ D-15 saldada para estas rutas                                              |
| Excepciones técnicas traducidas a dominio                  | ✅ y ahora también de dominio a HTTP (T-08)                                   |
| Sin secretos hardcodeados (S2068)                          | ✅                                                                            |
| Complejidad cognitiva ≤ 15                                 | ✅ el método más largo de `TaskHandler` son 9 líneas                          |
| Sin números mágicos (S109)                                 | ✅ `DEFAULT_MAX_RESULTS`, `MIN_RESULTS` con nombre                            |
| Llaves `{}` en todo control de flujo (S1117)               | ✅                                                                            |
| Convención `givenX_whenY_thenZ`                            | ✅ en las 25 pruebas nuevas                                                   |
| Reactivo puro, sin `block()` (DP-08)                       | ✅ verificado con `grep`                                                      |
| Eliminaciones verificadas con `grep`, no con el compilador | ✅                                                                            |
| **Build verde**                                            | ✅ **153 / 153**                                                              |

---

## 8. Hallazgos

### H-1 · El agente no declara la forma del cuerpo de `GET /api/tasks`

`fase-03.md` §5 documenta que la ruta existe y es REST, pero **no su contrato de respuesta**: no se
sabe si devuelve un array en la raíz o un objeto envolvente. Por la política de No-Asunción no se
eligió una y punto: `A2APayloadMapper.toTasks(...)` **acepta ambas** y resuelve a lista vacía —nunca
a excepción— ante cualquier otra forma. Hay una prueba por cada caso.

Es una tolerancia deliberada, no una suposición. Aun así conviene fijarlo.

→ Registrado como **D-34**, a resolver cuando se aborde el contrato compartido (D-29, Fase 07).

### H-2 · `Flux.merge` habría hecho la deduplicación no determinista

El guion de T-03 proponía `Flux.merge`, pero `merge` intercala por orden de llegada: con la misma
tarea en ambas fuentes, cuál sobrevive depende de quién responda antes. La regla «prevalece la del
agente» quedaba al azar. Se usó `Flux.concat`, que garantiza el orden y hace la regla determinista.

El coste es que el listado del BFF no empieza a emitirse hasta que el agente termina. Es asumible:
ambas fuentes son pequeñas y hay timeout.

### H-3 · `ClientRequest` violaba una regla de ArchUnit y nadie lo había notado

`ArchitectureTest` reporta `Rule_2.2: Domain classes should not be named with technology suffixes`
como **advertencia**, no como fallo, así que la violación llevaba tiempo pasando inadvertida. Al
eliminar `ClientRequest` la cuenta baja de 3 a 2 clases infractoras.

Que estas reglas sean advertencia y no error es lo que permite que la deuda se acumule en silencio.

→ La Fase 08 debe **poner ArchUnit a cero y en modo fallo**, como ya pide su objetivo.

### H-4 · El mapa de errores HTTP quedó partido en dos

`TaskHandler` traduce cinco excepciones a cinco códigos distintos. `Handler` sigue devolviendo **400
para todo**, incluidos los fallos que no son culpa del cliente (D-16). Conviven dos criterios en el
mismo entry-point.

Es coherente con el plan —el mapa completo es DP-06, Fase 07— pero es una inconsistencia visible
para el frontend mientras dure. Está documentada en `CAMBIOS-FRONTEND.md` §7.

### H-5 · La tabla `agent_tasks` sigue físicamente compartida

El BFF **dejó de escribir** las tareas del agente, que era el objetivo de D-24, pero la tabla sigue
siendo la misma y el agente sigue escribiendo en ella. Por eso `listTasks()` necesita deduplicar:
una tarea del agente puede llegar también por `taskStoreGateway.findAll()`.

La deduplicación es un parche con fecha de caducidad. Cuando DP-05 separe las tablas, sobra.

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `docs/fases/fase-04.md` — prompts y datos fuera del dominio
- **Decisiones resueltas en esta fase:** DP-07 (B), DP-07-bis (B2), D-33 (A)
- **Decisiones abiertas:** DP-02 (Fase 04), DP-03 (Fase 05), DP-04 (Fase 06), DP-05 y DP-06 (Fase
    07)
- **Deudas nuevas registradas:** **D-34** (el agente no declara el contrato de respuesta de
  `GET /api/tasks`)
- **Pruebas de la Fase 01 invertidas o ajustadas:** **3 invertidas** + 1 sustituida
    1. `givenNonNumericMaxResults_whenGet_then500NotBadRequest` → `..._thenBadRequest` (500 →
       **400**)
    2. `givenRoutesTheFrontendCalls_whenInvoked_thenBffDoesNotServeThemYet` →
       `givenTheNewContractRoutes_whenInvoked_thenBffServesThem`
    3. En `TrackAgentTaskUseCaseTest`, las de `refreshTask` y `cancelTask` pasan a
       `verify(taskStoreGateway, never()).save(...)`
    4. Las dos de `GET /api/tasks` se sustituyen por
       `givenTasksRoute_whenGet_thenItIsServedByTaskHandler`
- **Pendiente fuera de este repositorio:** aplicar `docs/resultados/CAMBIOS-FRONTEND.md` en
  `azure-devops-frontend`. **Hasta entonces el chat sigue llamando al endpoint legacy del agente.**
- **Estado del build:** 🟢 **verde — 153 pruebas, 0 fallos**

