# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-28 · **Fase activa:** `FASE-04`

---

## Progreso

| # | Fase | Instrucciones | Resultado | Estado |
|---|---|---|---|---|
| 00 | Baseline y caracterización | `fases/FASE-00-...md` | `resultados/RESULTADO-FASE-00.md` | 🟢 COMPLETADA |
| 01 | Externalización de prompts | `fases/FASE-01-...md` | `resultados/RESULTADO-FASE-01.md` | 🟢 COMPLETADA |
| 02 | Value Objects de estimación | `fases/FASE-02-...md` | `resultados/RESULTADO-FASE-02.md` | 🟢 COMPLETADA |
| 03 | Resolución de intención | `fases/FASE-03-resolucion-de-intencion.md` | `resultados/RESULTADO-FASE-03.md` | 🟢 COMPLETADA |
| 04 | Separación de flujos (Strategy) | `fases/FASE-04-separacion-de-flujos.md` | — | 🟡 PENDIENTE |
| 05 | Value Objects de configuración | _(se genera al cerrar Fase 04)_ | — | ⚪ NO GENERADA |
| 06 | Separación del entry-point | _(se genera al cerrar Fase 05)_ | — | ⚪ NO GENERADA |
| 07 | Endurecimiento y cierre | _(se genera al cerrar Fase 06)_ | — | ⚪ NO GENERADA |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

> **Ninguna decisión bloquea la Fase 04.** Puede ejecutarse de inmediato.

---

## Métricas

| Métrica | Baseline | Fase 01 | Fase 02 | Fase 03 | Objetivo |
|---|---:|---:|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | 505 | 446 | **347** | ≤ 100 |
| Líneas `Handler` | 495 | 495 | 495 | 495 | ≤ 150 |
| Clases > 300 líneas | 2 | 2 | 2 | 2 | 0 |
| Tests totales | 23 | 69 | 118 | **152** | — |
| Cobertura `domain/model` | 0% | 0% | 93,1%* | **95,8%** | ≥ 90% |
| Cobertura `domain/usecase` | 0% | 81,5% | 79,9% | **84,5%** | ≥ 90% |
| Cobertura `prompt-template` | n/a | 94,2% | 94,2% | 96,6% | ≥ 90% |
| Constantes de prompt en el dominio | 5 | **0** | 0 | 0 | 0 |
| Bloques de post-procesado duplicados | 2 | 2 | **1** | 1 | 1 |
| `Pattern.compile()` en caliente | 4 | 4 | **0** | 0 | 0 |
| Números mágicos en el caso de uso | 3 | 3 | **0** | 0 | 0 |
| Ramas `if` de enrutamiento en `executeChat()` | 4 | 4 | 4 | **0** | 0 |
| Bloques `onErrorResume` duplicados | 6 | 6 | 6 | 6 | 1 |
| Argumentos del constructor | 9 | 10 | 10 | **11** | ≤ 5 |

> \* La cifra «60,3%» publicada al cerrar la Fase 02 estaba mal calculada (cubiertos de un paquete
> sobre el total del módulo). Recalculada con criterio homogéneo, era 93,1%.

> El constructor sube de forma **deliberada y temporal**; se corrige en la Fase 05 agrupando los
> `String` en Value Objects. Hay un `// TODO Fase 05` en el código.

---

## Bitácora de sesiones

| Fecha | Fase | Qué se hizo | Bloqueos |
|---|---|---|---|
| 2026-08-28 | — | Plan maestro, decisiones pendientes y Fase 00 | DP-01 a DP-05 abiertas |
| 2026-08-28 | 00 | Usuario resolvió DP-01 y DP-02. 23 pruebas de caracterización. Cobertura `usecase` 0% → 81,7%. Código productivo intacto | **DP-07 detectada**; DP-06 abierta |
| 2026-08-28 | 01 | Usuario resolvió DP-04, DP-06 y DP-07. Módulo `prompt-template` con puerto y adaptador. 5 plantillas externalizadas + fragmento con la tabla de horas. Eliminados `buildPrompt()` y la «Regla de Mínimos». 644 → 505 líneas. 69 tests | DP-03 pendiente |
| 2026-08-28 | 02 | Usuario resolvió DP-03. Creados `ComplexityEstimation`, `UncertaintyLevel` y `EstimationSummary`. Umbral `> 8` aplicado; aviso explícito si falta estimación. Duplicación D-04 eliminada. 505 → 446 líneas. 118 tests | Ninguno |
| 2026-08-28 | 03 | DP-01 aplicada: creados `AgentIntent`, `IntentResolution` e `IntentResolver` (100% de cobertura). `executeChat()` pasa a validar + despachar con `switch`; `handleSpecialCommands()` eliminado. Se retiró `buildPrompt()`, que la Fase 01 dejó vivo. 446 → 347 líneas. 152 tests | **Ratificación pendiente:** regla del pronombre relativo |

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


