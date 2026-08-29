# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-28 · **Fase activa:** `FASE-02`

---

## Progreso

| # | Fase | Archivo de instrucciones | Estado | Cerrada el |
|---|---|---|---|---|
| 00 | Baseline y caracterización | `fases/FASE-00-baseline-y-caracterizacion.md` | 🟢 COMPLETADA | 2026-08-28 |
| 01 | Externalización de prompts | `fases/FASE-01-externalizacion-de-prompts.md` | 🟢 COMPLETADA | 2026-08-28 |
| 02 | Value Objects de estimación | `fases/FASE-02-value-objects-de-estimacion.md` | 🟡 PENDIENTE (DP-03 por confirmar) | — |
| 03 | Resolución de intención | _(se genera al cerrar Fase 02)_ | ⚪ NO GENERADA | — |
| 04 | Separación de flujos (Strategy) | _(se genera al cerrar Fase 03)_ | ⚪ NO GENERADA | — |
| 05 | Value Objects de configuración | _(se genera al cerrar Fase 04)_ | ⚪ NO GENERADA | — |
| 06 | Separación del entry-point | _(se genera al cerrar Fase 05)_ | ⚪ NO GENERADA | — |
| 07 | Endurecimiento y cierre | _(se genera al cerrar Fase 06)_ | ⚪ NO GENERADA | — |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## Métricas

| Métrica | Baseline | Tras Fase 00 | Tras Fase 01 | Objetivo |
|---|---:|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | 644 | **505** | ≤ 100 |
| Líneas `Handler` | 495 | 495 | 495 | ≤ 150 |
| Clases > 300 líneas | 2 | 2 | 2 | 0 |
| Constantes de prompt en el dominio | 5 | 5 | **0** | 0 |
| Tests totales del proyecto | 23 | 46 | **69** | — |
| Cobertura `domain/usecase` | 0% | 81,7% | 81,5% | ≥ 90% |
| Cobertura `domain/model` | 0% | 0% | 0% | ≥ 90% |
| Cobertura `prompt-template` | n/a | n/a | **94,2%** | ≥ 90% |
| Constructores > 5 parámetros | 1 (9 args) | 1 (9 args) | 1 (**10 args**) | 0 |

> El constructor sube a 10 argumentos de forma **deliberada y temporal**: se corrige en la Fase 05
> agrupando los `String` en Value Objects. Hay un `// TODO Fase 05` en el código.

---

## Bitácora de sesiones

| Fecha | Fase | Qué se hizo | Bloqueos |
|---|---|---|---|
| 2026-08-28 | — | Plan maestro, decisiones pendientes y Fase 00 | DP-01 a DP-05 abiertas |
| 2026-08-28 | 00 | Usuario resolvió DP-01 y DP-02. Creados 23 tests de caracterización. Cobertura de `usecase` 0% → 81,7%. Código productivo intacto. | **DP-07 detectada**; DP-06 abierta |
| 2026-08-28 | 01 | Usuario resolvió DP-04, DP-06 y DP-07. Creado módulo `prompt-template` con `PromptTemplatePort` + adaptador de classpath. 5 plantillas externalizadas + fragmento compartido con la tabla de horas. Eliminados `buildPrompt()` y la «Regla de Mínimos de 5 puntos». Use case 644 → 505 líneas. 69 tests verdes. | DP-03 pendiente para Fase 02 |

---

## Decisiones

### Resueltas

| ID | Decisión | Aplicada en |
|---|---|---|
| DP-01 | Corregir precedencia de intención: `(ID: n)` gana sobre palabras clave | Fase 03 |
| DP-02 | Umbral de división `> 8` | Fase 02 |
| DP-04 | Ruta y nombres de las plantillas aprobados | ✅ Fase 01 |
| DP-06 | Estimación por horas; se elimina la «Regla de Mínimos de 5 puntos» | ✅ Fase 01 |
| DP-07 | Eliminar `buildPrompt()` y su plantilla (dead code) | ✅ Fase 01 |

### Abiertas

| ID | Descripción | Bloquea | Desde |
|---|---|---|---|
| DP-03 | Qué mostrar si el LLM no devuelve estimación (recomendación: aviso explícito) | Fase 02 | 2026-08-28 |
| DP-05 | Camino muerto `chat()` async contra `NoOpAgentResponseAdapter` | Fase 07 | 2026-08-28 |

### Ratificación pendiente

| Asunto | Detalle |
|---|---|
| Eliminación de la «Regla de Mínimos de 5 puntos» | Derivada por implicación de la tabla de horas de DP-06, no confirmada palabra por palabra. Revertir es trivial: reintroducir el párrafo en `prompts/fase2-historia-estructurada.md`. |

---

## Hallazgos técnicos para fases posteriores

| Hallazgo | Detectado en | Atender en |
|---|---|---|
| ArchUnit `Rule_2.7` violada 5 veces («Beans classes should only have final attributes»). Hoy advertencia; el plugin avisa: *«This will cause a build error in future»*. | Fase 00 | Fase 07 |
| ArchUnit `Rule_2.2` violada 1 vez («Domain classes should not be named with technology suffixes»). | Fase 00 | Fase 07 |
| `jacocoMergedReport` falla con la configuration cache de Gradle por el plugin `pitest`. Workaround: `--no-configuration-cache`. | Fase 00 | Fase 07 |
| `domain/model` sigue sin tests (0% de cobertura). La Fase 02 crea los primeros. | Fase 00 | Fase 02 / 07 |

