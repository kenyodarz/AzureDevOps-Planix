# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-29 · **Fase activa:** `FASE-07`

---

## Progreso

| # | Fase | Instrucciones | Resultado | Estado |
|---|---|---|---|---|
| 00 | Baseline y caracterización | `fases/FASE-00-...md` | `resultados/RESULTADO-FASE-00.md` | 🟢 COMPLETADA |
| 01 | Externalización de prompts | `fases/FASE-01-...md` | `resultados/RESULTADO-FASE-01.md` | 🟢 COMPLETADA |
| 02 | Value Objects de estimación | `fases/FASE-02-...md` | `resultados/RESULTADO-FASE-02.md` | 🟢 COMPLETADA |
| 03 | Resolución de intención | `fases/FASE-03-resolucion-de-intencion.md` | `resultados/RESULTADO-FASE-03.md` | 🟢 COMPLETADA |
| 04 | Separación de flujos (Strategy) | `fases/FASE-04-separacion-de-flujos.md` | `resultados/RESULTADO-FASE-04.md` | 🟢 COMPLETADA |
| 05 | Value Objects de configuración | `fases/FASE-05-value-objects-de-configuracion.md` | `resultados/RESULTADO-FASE-05.md` | 🟢 COMPLETADA |
| 06 | Separación del entry-point | `fases/FASE-06-separacion-del-entry-point.md` | `resultados/RESULTADO-FASE-06.md` | 🟢 COMPLETADA |
| 07 | Endurecimiento y cierre | `fases/FASE-07-endurecimiento-y-cierre.md` | — | 🔴 BLOQUEADA |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

> ⛔ **La Fase 07 está bloqueada por DP-05.** Es la última decisión abierta y debe resolverla
> **el usuario** antes de escribir una sola línea de esa fase.

---

## Métricas

| Métrica | Baseline | Fase 03 | Fase 04 | Fase 05 | Fase 06 | Objetivo |
|---|---:|---:|---:|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | 347 | **89** | 89 | 89 | ≤ 100 ✅ |
| Líneas `Handler` | 495 | 495 | 495 | 495 | **112** | ≤ 150 ✅ |
| Clases > 300 líneas | 2 | 2 | **1** | 1 | **0** | 0 ✅ |
| Tests totales | 23 | 152 | 186 | 207 | **251** | — |
| Cobertura `domain/model` | 0% | 95,8% | 95,8% | **96,1%** | 96,1% | ≥ 90% ✅ |
| Cobertura `domain/usecase` | 0% | 84,5% | **95,4%** | 95,4% | 95,4% | ≥ 90% ✅ |
| Cobertura `prompt-template` | n/a | 96,6% | 96,6% | 96,6% | 96,6% | ≥ 90% ✅ |
| Ramas `if` de enrutamiento en `executeChat()` | 4 | **0** | 0 | 0 | 0 | 0 ✅ |
| Bloques `onErrorResume` duplicados | 6 | 6 | **1** | 1 | 1 | 1 ✅ |
| Argumentos del constructor | 9 | 11 | **4** | 4 | 4 | ≤ 5 ✅ |
| `String` de configuración sueltos en constructores | 5 | 5 | 10 | **0** | 0 | 0 ✅ |
| Lecturas de recursos del classpath al arrancar | 3 | 3 | 6 | **3** | 3 | 3 ✅ |
| Responsabilidades en `Handler` | 4 | 4 | 4 | 4 | **1** | 1 ✅ |
| Códigos de error JSON-RPC como literales sueltos | 6 | 6 | 6 | 6 | **0** | 0 ✅ |

> El repunte a 10 `String` sueltos en la Fase 04 fue transitorio: al repartir los flujos, la
> configuración se distribuyó entre los handlers antes de tiparse en la Fase 05.
>
> **Con la Fase 06 se alcanzan todas las métricas de éxito del plan maestro §5.** La clase más
> larga del proyecto pasa a ser `PgVectorPlanningAdapter`, con 240 líneas.

---

## Bitácora de sesiones

| Fecha | Fase | Qué se hizo | Bloqueos |
|---|---|---|---|
| 2026-08-28 | — | Plan maestro, decisiones pendientes y Fase 00 | DP-01 a DP-05 abiertas |
| 2026-08-28 | 00 | Usuario resolvió DP-01 y DP-02. 23 pruebas de caracterización. Cobertura `usecase` 0% → 81,7%. Código productivo intacto | **DP-07 detectada**; DP-06 abierta |
| 2026-08-28 | 01 | Usuario resolvió DP-04, DP-06 y DP-07. Módulo `prompt-template` con puerto y adaptador. 5 plantillas externalizadas. Eliminada la «Regla de Mínimos». 644 → 505 líneas. 69 tests | DP-03 pendiente |
| 2026-08-28 | 02 | Usuario resolvió DP-03. Creados `ComplexityEstimation`, `UncertaintyLevel` y `EstimationSummary`. Umbral `> 8`; aviso explícito si falta estimación. 505 → 446 líneas. 118 tests | Ninguno |
| 2026-08-28 | 03 | DP-01 aplicada: `AgentIntent`, `IntentResolution` e `IntentResolver` (100% cobertura). `executeChat()` valida + despacha. Eliminados `handleSpecialCommands()` y `buildPrompt()`. 446 → 347 líneas. 152 tests | **Ratificación pendiente:** regla del pronombre relativo |
| 2026-08-28 | 04 | D-02 saldada: seis `ChatFlowHandler` + `ChatFlowDispatcher` con fail-fast y **un único** `onErrorResume`. Extraída `A2AResponseFactory`. Constructor 11 → 4. 347 → **89 líneas**. 186 tests | Ninguno |
| 2026-08-29 | 05 | D-03 saldada: `AzureDevOpsScope` y `CorporateKnowledge` (100% cobertura). Cero `String` sueltos en handlers; recursos leídos una sola vez. **Descubierto que ningún test validaba el cableado de Spring**: añadido `UseCasesConfigWiringTest`. 207 tests | Ninguno |
| 2026-08-29 | 06 | D-10 y D-11 saldadas: `JsonRpcPayloadMapper`, `JsonRpcResponseFactory`, `JsonRpcDispatcher` y `AgentCardProvider`, las cuatro al **100%** de cobertura. `Handler` 495 → **112 líneas** y una sola responsabilidad. **Cero clases > 300 líneas.** Contrato JSON-RPC intacto: `git diff` vacío sobre las 4 suites. Eliminado el `JsonMapper` muerto del constructor. 251 tests | Ninguno |

---

## Decisiones

### Resueltas

| ID | Decisión | Aplicada en |
|---|---|---|
| DP-01 | Corregir precedencia de intención: `(ID: n)` gana sobre palabras clave | ✅ Fase 03 |
| DP-02 | Umbral de división `> 8` | ✅ Fase 02 |
| DP-03 | Aviso explícito si el modelo no estima | ✅ Fase 02 |
| DP-04 | Ruta y nombres de las plantillas | ✅ Fase 01 |
| DP-06 | Estimación por horas; se elimina la «Regla de Mínimos de 5 puntos» | ✅ Fase 01 |
| DP-07 | Eliminar `buildPrompt()` y su plantilla (dead code) | ✅ Fase 01 (plantilla) + Fase 03 (método) |

### Abiertas

| ID | Descripción | Bloquea |
|---|---|---|
| DP-05 | Camino muerto `chat()` async contra `NoOpAgentResponseAdapter` | Fase 07 |

### Ratificación pendiente

| Asunto | Detalle |
|---|---|
| Eliminación de la «Regla de Mínimos de 5 puntos» | Derivada por implicación de la tabla de horas de DP-06, no confirmada palabra por palabra. Revertir es reintroducir el párrafo en `prompts/fase2-historia-estructurada.md` |
| Regla del pronombre relativo en las palabras clave genéricas | Fase 03. Una palabra clave precedida de `que`/`quien`/`cual` no enruta a General, porque describe el sistema a construir y no una petición al agente. Sin ella no es posible cumplir a la vez las dos pruebas obligatorias de la fase. Revertir es borrar los tres lookbehind de `GENERAL_KEYWORD_PATTERN` |

---

## Hallazgos técnicos para fases posteriores

| Hallazgo | Detectado en | Atender en |
|---|---|---|
| ArchUnit `Rule_2.7` violada 5 veces. Hoy advertencia; el plugin avisa: *«This will cause a build error in future»* | Fase 00 | Fase 07 |
| ArchUnit `Rule_2.2` violada 1 vez | Fase 00 | Fase 07 |
| `jacocoMergedReport` incompatible con la configuration cache por el plugin `pitest`. Workaround: `--no-configuration-cache` | Fase 00 | Fase 07 |
| Los ~15 modelos A2A de `domain/model` siguen sin pruebas y lastran la cobertura del módulo | Fase 02 | Fase 07 |
| Verificar las eliminaciones de código con `grep`, no solo con el build: `buildPrompt()` sobrevivió dos fases | Fase 03 | Práctica permanente |
| `AgentChatUseCase` cubre 75,9%: lo que falta es el camino `chat()` asíncrono, sin pruebas | Fase 04 | Fase 07 (DP-05) |
| **`UseCasesConfigTest` (andamiaje) descarta `UnsatisfiedDependencyException`: un cableado roto pasaría como prueba verde.** Mitigado con `UseCasesConfigWiringTest` | Fase 05 | Fase 07 |
| ~~`Handler.java` (495 líneas) es ya la única clase de más de 300 líneas del proyecto~~ **SALDADO en la Fase 06** | Fase 04 | ✅ Fase 06 |
| **`Handler` recibía un `JsonMapper` por constructor que nunca usaba**: sobrevivió seis fases porque el compilador no advierte de un campo asignado y jamás leído | Fase 06 | ✅ eliminado en la Fase 06 |
| Las 4 suites del entry-point fijan `Handler` como **único bean** del contexto (`@ContextConfiguration`). Cualquier colaborador nuevo que se quiera registrar como `@Component` obliga a renegociar esas suites | Fase 06 | Fase 07 |
| `Handler` se queda en 86,1%: sin cubrir el camino de error de `handleListTasks` y el `onErrorResume` del endpoint legacy | Fase 06 | Fase 07 |
| La Agent Card sigue siendo Java compilado; ya está aislada, externalizarla a `resources/agent-card.json` es ahora un cambio de una sola clase | Fase 06 | Fase 07 (opcional) |





