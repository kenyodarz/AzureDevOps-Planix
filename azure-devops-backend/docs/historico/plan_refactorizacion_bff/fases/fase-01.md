# FASE 01 — Baseline y red de seguridad

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** 🟡 PENDIENTE
> **Deudas objetivo:** D-17, D-18 (parcial) · **Decisiones requeridas:** ninguna
> **Fase siguiente:** `fase-02.md` (se genera al cerrar esta)

---

## 1. Contexto

### 1.1 Estado actual del sistema

`azure-devops-backend` es el **BFF** del sistema: Spring Boot 4.1 + WebFlux con el scaffold de Clean
Architecture de Bancolombia (`model`, `usecase`, `reactive-web`, `pgvector-store`, `mcp-client`,
`task-store-inmemory`, `s3-repository`, `metrics`, `app-service`).

Su misión es ser el único interlocutor del front, validar, delegar en el agente (`:8082`) y
**exponer el estado de las tareas** que el agente ejecuta. Hoy cumple esa misión solo a medias.

Conviven **cuatro flujos**:

| Flujo                              | Rutas                                                                 | Estado                                                                                       |
|------------------------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| **F1** Planificación vectorial     | `/api/planning/**`                                                    | Vivo · **0 pruebas**                                                                         |
| **F2** Auditoría del backlog (SSE) | `/api/devops/dashboard`, `/api/devops/dashboard/stream`, `/api/tasks` | Vivo · solo 4 pruebas del caso de uso                                                        |
| **F3** Delegación al agente        | `ChatGatewayAdapter` → `POST :8082/message:send`                      | Vivo · **0 pruebas de integración**; usa el endpoint legacy vencido y **descarta la `Task`** |
| **F4** Servidor A2A / JSON-RPC     | ninguna                                                               | **Rol equivocado**: sin bean, sin ruta, 15 pruebas en rojo. Se retira en la Fase 02          |

**El build está en rojo:** 41 tests, **15 fallidos**, todos en `reactive-web`, todos por golpear
rutas (`/message:send`, `/`, `/card`, `/.well-known/agent.json`) que `RouterRest` no expone.

**El front habla con dos backends** (`proxy.conf.json`): `/api` → `:8081` (BFF) y `/message:send` →
`:8082` (agente, directo). Además llama a `/.well-known/agent-card.json` y a `POST /`
(`tasks/cancel`), que **no los sirve nadie**.

### 1.2 Dependencias de fases previas

Ninguna. Esta es la fase inicial.

### 1.3 Alcance específico de esta fase

Esta fase **no modifica una sola línea de código productivo**. Su único propósito es construir la
red que hará verificables las siete fases siguientes:

1. Congelar el comportamiento observable de F1, F2 y **F3 (la delegación al agente)** con pruebas de
   caracterización.
2. Reparar `UseCasesConfigTest`, que hoy **da verde mientras el contexto de Spring no arranca**.
3. Publicar el baseline numérico reproducible y el **mapa de integración** front ↔ BFF ↔ agente.

> **Lección heredada del plan del agente (`CIERRE-DEL-PLAN.md` §7.1 y §7.2):** las pruebas de
> caracterización fueron lo único que hizo verificables las refactorizaciones posteriores; y un test
> que atrapa la excepción del arranque de Spring y responde `assertTrue(true)` deja pasar un
> cableado roto durante fases enteras. Aquí se da exactamente el mismo caso.

### 1.4 Nota sobre el criterio de «build verde»

Esta fase **no puede** dejar el build verde: los 15 fallos son consecuencia de D-01/D-02 y su
resolución depende de **DP-01**, que se decide en la Fase 02. Por tanto el criterio de salida se
relaja de forma explícita y solo para esta fase:

> Al cerrar la Fase 01 los fallos deben ser **exactamente los mismos 15**, ni uno más, y todas las
> pruebas nuevas deben estar en verde.

---

## 2. Instrucciones

### T-01 · Registrar el baseline reproducible

Crear `docs/resultados/baseline-fase-01.md` con:

- Salida de `gradlew test`: totales por suite y detalle de los 15 fallos (nombre del test y motivo).
- Tabla de líneas por clase productiva (top 10).
- Cobertura por módulo tras `gradlew jacocoMergedReport`.
- **Violaciones de ArchUnit**: `ArchitectureTest` las degrada a `WARNING` mediante
  `checkWithWarning`, así que hoy no se ven. Ejecutar `gradlew :app-service:test --tests
  "*ArchitectureTest*" -i` y transcribir cada línea `ARCHITECTURE_RULE_VIOLATED`. Esta cifra es el
  baseline de la métrica «Violaciones de ArchUnit» del plan maestro, hoy sin medir.

> `build/issues.json` no sirve como fuente: se genera vacío aunque haya violaciones (defecto ya
> documentado en el proyecto agente).

### T-02 · Reparar `UseCasesConfigTest` (D-17)

**Archivo:**
`applications/app-service/src/test/java/co/com/bancolombia/config/UseCasesConfigTest.java`

Hoy el test envuelve la carga del contexto en un `try/catch` que traga
`UnsatisfiedDependencyException` y termina en verde. El log del baseline lo confirma:

```
UnsatisfiedDependencyException: Error creating bean with name 'ingestPlanningSpecUseCase' ...
No qualifying bean of type 'PlanningVectorStorePort' available
```

Reescribirlo para que el wiring se verifique de verdad:

- Usar `ApplicationContextRunner` con `UseCasesConfig` y **mocks registrados** de
  `PlanningVectorStorePort` y `ChatGateway`.
- Afirmar que **cada uno** de los cuatro beans existe: `ingestPlanningSpecUseCase`,
  `searchPlanningSpecUseCase`, `managePlanningUseCase`, `devOpsDashboardUseCase`.
- Añadir un caso negativo: sin el mock del puerto, el arranque **debe** fallar (`hasFailed()`).
- Afirmar que `defaultOrg`, `defaultProject` e `isMcpEnabled` llegan al bean con los valores
  esperados, incluyendo sus valores por defecto.
- **Prohibido** capturar excepciones para dar el test por bueno.

### T-03 · Caracterizar F1 — Planificación vectorial

**Archivo nuevo:**
`infrastructure/entry-points/reactive-web/src/test/java/co/com/bancolombia/api/PlanningRoutesCharacterizationTest.java`

`@WebFluxTest` con `RouterRest` + `Handler` y los casos de uso mockeados. Congelar, tal como se
comportan **hoy** (aunque el comportamiento sea discutible: eso se corrige en la Fase 07, no aquí):

| Caso                                                  | Aserción                                                                               |
|-------------------------------------------------------|----------------------------------------------------------------------------------------|
| `POST /api/planning/ingest` correcto                  | 200 y `{"message":"Planeación ingesta y vectorizada exitosamente"}`                    |
| `POST /api/planning/ingest` con fallo del caso de uso | **400** y `{"error": "..."}`                                                           |
| `GET /api/planning/search`                            | 200, lista JSON; los defaults `initiativeId=""` y `maxResults=3` llegan al caso de uso |
| `GET /api/planning/initiatives`                       | 200, lista JSON                                                                        |
| `GET /api/planning/initiatives/{id}/chunks`           | 200, `id` propagado                                                                    |
| `DELETE /api/planning/initiatives/{id}`               | 200, cuerpo vacío                                                                      |
| `PUT /api/planning/initiatives/{id}/cell`             | 200, `cell` propagado                                                                  |
| `GET /api/tasks`                                      | 200, lista de tareas                                                                   |
| Fallo en cualquiera de las anteriores                 | **400** — dejar constancia de que hoy *todo* error es 400 (D-16)                       |

### T-04 · Caracterizar F2 — Auditoría del backlog

**Archivo nuevo:**
`infrastructure/entry-points/reactive-web/src/test/java/co/com/bancolombia/api/DashboardRoutesCharacterizationTest.java`

Es el flujo más frágil y el que más se va a mover en las Fases 04 y 05. Congelar:

| Caso                                                  | Aserción                                                                                                                   |
|-------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `GET /api/devops/dashboard` sin `cell` o sin `sprint` | 400 y el mensaje exacto `"La célula y el sprint son parámetros requeridos"`                                                |
| `GET /api/devops/dashboard` correcto                  | 200 y el JSON del caso de uso **tal cual**, sin reempaquetar                                                               |
| `GET /api/devops/dashboard/stream`                    | `Content-Type: text/event-stream`                                                                                          |
| Secuencia de eventos SSE                              | primero un evento `INITIAL`, después **N** eventos `BATCH_UPDATE`                                                          |
| **Tamaño de lote**                                    | con 25 ítems se emiten **3** `BATCH_UPDATE` (10 + 10 + 5) y `getBatchAudit` se invoca 3 veces con los CSV de IDs correctos |
| Métricas recalculadas                                 | tras el último lote, `avgQualityScore` = media entera y `undocumentedCount` = ítems sin CA **o** sin DoD                   |
| Ciclo de vida de `Task`                               | `TaskStoreGateway.save` se invoca con `WORKING` y luego con `COMPLETED`                                                    |
| Fallo del LLM con `mcpEnabled = true`                 | se guarda una `Task` en `FAILED` y el stream propaga el error                                                              |
| Fallo del LLM con `mcpEnabled = false`                | se emite el stream mock: `INITIAL` con scores a `0` y luego `BATCH_UPDATE` completo                                        |
| Fallo de **un** lote                                  | el stream **no** se rompe; se emite el estado actual (`onErrorResume` de `auditBatchAndEmit`)                              |

> Estos tests son la definición operativa de lo que las Fases 04 y 05 no pueden romper. Escribirlos
> con detalle es la parte más valiosa de esta fase.

### T-05 · Caracterizar el dominio del dashboard

**Archivo:**
`domain/usecase/src/test/java/co/com/bancolombia/usecase/dashboard/DevOpsDashboardUseCaseTest.java`
(ampliar los 4 tests actuales)

Congelar la resolución de rutas antes de extraerla a Value Objects en la Fase 04. Capturar el prompt
enviado a `ChatGateway` con un `ArgumentCaptor` y afirmar sobre él:

| Entrada                             | `AreaPath`/`IterationPath` esperado en el prompt |
|-------------------------------------|--------------------------------------------------|
| `cell` = `"EQU1096 - EXODIA"`       | `"Vicepresidencia...\EQU1096 - EXODIA"`          |
| `cell` ya prefijada con el proyecto | se deja intacta                                  |
| `sprint` = `"Sprint 247"`           | `"Vicepresidencia...\<AÑO_ACTUAL>\Sprint 247"`   |
| `sprint` = `"Otro"`                 | `"Vicepresidencia...\Otro"`                      |
| `cell` o `sprint` vacíos / nulos    | `IllegalArgumentException`                       |

Añadir además: `getBatchAudit` formatea el prompt con `batchSize`, `defaultProject`, `defaultOrg` e
`idsCsv` **en ese orden** (nótese que proyecto y organización van invertidos respecto a los otros
dos prompts: es el comportamiento actual y hay que congelarlo tal cual, no arreglarlo aquí), y
`saveDashboardReport` escribe el `.md` con la cabecera, la tabla de métricas y la tabla de ítems
esperadas.

### T-06 · Caracterizar los casos de uso de planificación

**Archivos nuevos** en `domain/usecase/src/test/java/co/com/bancolombia/usecase/`:

- `ingestplanning/IngestPlanningSpecUseCaseTest.java` — troceo del Markdown en `PlanningChunk`:
  cuántos chunks salen, qué `sectionName` recibe cada uno, qué pasa con un Markdown vacío o sin
  encabezados.
- `searchplanning/SearchPlanningSpecUseCaseTest.java` — delegación al puerto y propagación de
  parámetros.
- `manageplanning/ManagePlanningUseCaseTest.java` — los cuatro métodos delegan al puerto.

Convención obligatoria `given / when / then` con `@ExtendWith(MockitoExtension.class)`
(`rules/spring-rules.md` §5).

### T-07 · Caracterizar F3 — la delegación al agente

**Archivo:**
`infrastructure/driven-adapters/mcp-client/src/test/java/co/com/bancolombia/mcpclient/adapter/ChatGatewayAdapterTest.java`
(ampliar los 3 tests actuales)

Es el punto que la Fase 02 va a reescribir por completo, y hoy no hay ninguna prueba que fije qué
manda el BFF por el cable. Usar `MockWebServer` y congelar:

| Caso                                    | Aserción                                                                                                          |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Ruta y verbo                            | `POST /message:send` — dejar constancia de que es el **endpoint legacy** del agente                               |
| Cuerpo enviado                          | `message.role = "user"`, `message.contextId` = el `contextId` recibido, `message.parts[0].text` = el prompt       |
| Respuesta con `message`                 | devuelve el texto plano de `parts` concatenado                                                                    |
| Respuesta con bloque `<think>…</think>` | el bloque se elimina (`filterReasoning`)                                                                          |
| Respuesta sin `message`                 | devuelve cadena vacía                                                                                             |
| **Respuesta que incluye `task`**        | 🔴 **se descarta**. Afirmarlo explícitamente: es la prueba que documenta D-23 y la que la Fase 02 deberá invertir |
| Error HTTP del agente (500)             | el `Mono` termina en error y se propaga                                                                           |

> Escribir el caso de la `task` descartada aunque «pase» hoy: es la evidencia ejecutable de la deuda
> que motiva toda la Fase 02.

### T-08 · Registrar el mapa de integración

Añadir a `docs/resultados/baseline-fase-01.md` una sección **«Mapa de integración actual»** con la
tabla de las 4 llamadas del front (§2.1 del plan maestro), su destino real según
`proxy.conf.json`, y su estado (OK / rota / salta el BFF). Es el documento que la Fase 03 usará para
verificar que el front pasa a hablar con un solo servicio.

**No se modifica el frontend en esta fase**, ni en ninguna otra de este plan (DP-07).

### T-09 · Validaciones de cierre

- `gradlew test` → **exactamente 15 fallos**, los mismos 15 del baseline. Cualquier fallo nuevo es
  un test mal escrito, no un hallazgo.
- Ningún fichero bajo `src/main/` ha cambiado. Verificar con `git status`.
- El informe `docs/resultados/baseline-fase-01.md` está completo, incluida la cifra de violaciones
  de ArchUnit.

---

## 3. Orden de ejecución

> Marcar cada casilla al completarla. Este checklist **es** el punto de reanudación ante un reinicio
> de sesión: continuar por el primer paso sin marcar.

- [x] **P-01** — `gradlew clean test` y volcar la salida cruda a `build/baseline-test.log`.
- [x] **P-02** — `gradlew jacocoMergedReport` y anotar la cobertura por módulo.
- [x] **P-03** — `gradlew :app-service:test --tests "*ArchitectureTest*" -i` y extraer las líneas
  `ARCHITECTURE_RULE_VIOLATED`.
- [x] **P-04** — Redactar `docs/resultados/baseline-fase-01.md` (T-01).
- [x] **P-05** — Reescribir `UseCasesConfigTest` con `ApplicationContextRunner` (T-02). Debe pasar
  del verde falso al verde real; si al reescribirlo se pone rojo, **el hallazgo se documenta y se
  arregla aquí**: es wiring, no comportamiento.
- [x] **P-06** — `PlanningRoutesCharacterizationTest` (T-03) — 9 casos.
- [x] **P-07** — `DashboardRoutesCharacterizationTest` (T-04) — 10 casos. Paso más largo; empezar
  por el caso feliz del stream y añadir los bordes después.
- [x] **P-08** — Ampliar `DevOpsDashboardUseCaseTest` (T-05) — 7 casos nuevos.
- [x] **P-09** — Tests de los tres casos de uso de planificación (T-06).
- [x] **P-09b** — Ampliar `ChatGatewayAdapterTest` con `MockWebServer` (T-07) — 7 casos, incluido el
  de la `Task` descartada.
- [x] **P-09c** — Añadir el «Mapa de integración actual» al informe de baseline (T-08).
- [x] **P-10** — `gradlew test`: confirmar 15 fallos y **ni uno más**; todas las pruebas nuevas en
  verde (T-09).
- [x] **P-11** — `git status`: confirmar que no se tocó `src/main/`.
- [x] **P-12** — Rellenar el bloque **Resultado** de abajo y actualizar §9 del plan maestro.
- [x] **P-13** — **Generar `docs/fases/fase-02.md`** con el estado real alcanzado: cliente A2A del
  agente y retirada del rol de servidor. Dejar constancia de que **DP-08** (llamada bloqueante o no
  bloqueante) debe resolverse antes de escribir el `AgentGateway`.

---

## 4. Resultado

> Informe completo: **`docs/resultados/RESULTADO-FASE-01.md`**

- **Fecha de cierre:** 2026-08-29
- **Tests totales / fallidos:** **117 / 15** (los mismos 15 heredados, ni uno más)
- **Pruebas nuevas añadidas:** **51** (41 → 117, de las cuales 25 sustituyen a 4 pruebas falsas)
- **Cobertura `domain/usecase`:** 85,8 % (inflada por `AgentChatUseCase`, que es código muerto)
- **Violaciones de ArchUnit medidas:** **10** (6 de `Rule_2.7`, 4 de `Rule_2.2`)
- **Líneas de `src/main` modificadas:** **0**
- **Hallazgos no previstos:**
    - **H-1** → nueva deuda **D-31**: un fallo del agente en el stream SSE devuelve 500 en vez de un
      evento de error, y el front lo silencia.
    - **H-3**: eran **4** las pruebas que daban verde en falso, no una. `ChatGatewayAdapterTest` ni
      siquiera instanciaba el adaptador.
    - **H-4**: el adaptador al agente no declara timeout.
- **Deudas nuevas detectadas:** D-31

