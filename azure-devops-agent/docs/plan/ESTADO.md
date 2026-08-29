# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-29 · **Fase activa:** `FASE-06`

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
| 06 | Separación del entry-point | `fases/FASE-06-separacion-del-entry-point.md` | — | 🟡 PENDIENTE |
| 07 | Endurecimiento y cierre | _(se genera al cerrar Fase 06)_ | — | ⚪ NO GENERADA |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

> **Ninguna decisión bloquea la Fase 06.** Puede ejecutarse de inmediato.

---

## Métricas

| Métrica | Baseline | Fase 03 | Fase 04 | Fase 05 | Objetivo |
|---|---:|---:|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | 347 | **89** | 89 | ≤ 100 ✅ |
| Líneas `Handler` | 495 | 495 | 495 | 495 | ≤ 150 |
| Clases > 300 líneas | 2 | 2 | **1** | 1 | 0 |
| Tests totales | 23 | 152 | 186 | **207** | — |
| Cobertura `domain/model` | 0% | 95,8% | 95,8% | **96,1%** | ≥ 90% ✅ |
| Cobertura `domain/usecase` | 0% | 84,5% | **95,4%** | 95,4% | ≥ 90% ✅ |
| Cobertura `prompt-template` | n/a | 96,6% | 96,6% | 96,6% | ≥ 90% ✅ |
| Ramas `if` de enrutamiento en `executeChat()` | 4 | **0** | 0 | 0 | 0 ✅ |
| Bloques `onErrorResume` duplicados | 6 | 6 | **1** | 1 | 1 ✅ |
| Argumentos del constructor | 9 | 11 | **4** | 4 | ≤ 5 ✅ |
| `String` de configuración sueltos en constructores | 5 | 5 | 10 | **0** | 0 ✅ |
| Lecturas de recursos del classpath al arrancar | 3 | 3 | 6 | **3** | 3 ✅ |

> El repunte a 10 `String` sueltos en la Fase 04 fue transitorio: al repartir los flujos, la
> configuración se distribuyó entre los handlers antes de tiparse en la Fase 05.

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
| `Handler.java` (495 líneas) es ya la única clase de más de 300 líneas del proyecto | Fase 04 | Fase 06 |





