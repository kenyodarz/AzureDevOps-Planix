# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-28 · **Fase activa:** `FASE-01`

---

## Progreso

| # | Fase | Archivo de instrucciones | Estado | Cerrada el | Commit |
|---|---|---|---|---|---|
| 00 | Baseline y caracterización | `fases/FASE-00-baseline-y-caracterizacion.md` | 🟢 COMPLETADA | 2026-08-28 | `test(agent_chat): agregar pruebas de caracterizacion del enrutamiento de flujos` |
| 01 | Externalización de prompts | `fases/FASE-01-externalizacion-de-prompts.md` | 🔴 BLOQUEADA (DP-04, DP-06) | — | — |
| 02 | Value Objects de estimación | _(se genera al cerrar Fase 01)_ | ⚪ NO GENERADA | — | — |
| 03 | Resolución de intención | _(se genera al cerrar Fase 02)_ | ⚪ NO GENERADA | — | — |
| 04 | Separación de flujos (Strategy) | _(se genera al cerrar Fase 03)_ | ⚪ NO GENERADA | — | — |
| 05 | Value Objects de configuración | _(se genera al cerrar Fase 04)_ | ⚪ NO GENERADA | — | — |
| 06 | Separación del entry-point | _(se genera al cerrar Fase 05)_ | ⚪ NO GENERADA | — | — |
| 07 | Endurecimiento y cierre | _(se genera al cerrar Fase 06)_ | ⚪ NO GENERADA | — | — |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## Métricas

| Métrica | Baseline (pre Fase 00) | Actual | Objetivo |
|---|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | 644 | ≤ 100 |
| Líneas `Handler` | 495 | 495 | ≤ 150 |
| Clases > 300 líneas | 2 | 2 | 0 |
| **Cobertura `domain/usecase`** | **0%** (sin tests) | **81,7%** (884/1082 instr.) | ≥ 90% |
| Cobertura `domain/model` | 0% (sin tests) | 0% (sin tests) | ≥ 90% |
| Tests en `domain/usecase` | 0 | **23** | — |
| Mutation score `domain/usecase` (pitest) | n/d | 62% (72/116), test strength 81% | ≥ 80% |
| Constructores > 5 parámetros | 1 | 1 | 0 |

---

## Bitácora de sesiones

| Fecha | Fase | Qué se hizo | Bloqueos encontrados |
|---|---|---|---|
| 2026-08-28 | — | Creación del plan maestro, decisiones pendientes y Fase 00 | DP-01 a DP-05 abiertas |
| 2026-08-28 | 00 | Usuario resolvió DP-01 (corregir precedencia) y DP-02 (umbral `> 8`). Creados `AgentChatUseCaseCharacterizationTest` (16 tests) y `AgentChatUseCaseParsingTest` (7 tests). Build verde. Cobertura de `domain/usecase` 0% → 81,7%. Código productivo intacto. | **DP-07 detectada** (código inalcanzable en `buildPrompt`). **DP-06 abierta** (equivalencia puntos→horas). 2 advertencias ArchUnit preexistentes. |

---

## Bloqueos activos

| ID | Descripción | Bloquea a | Desde |
|---|---|---|---|
| DP-04 | Nombres y ruta de los recursos de prompts | **Fase 01** | 2026-08-28 |
| DP-06 | Tabla de equivalencia Story Points → horas | **Fase 01**, Fase 02 | 2026-08-28 |
| DP-03 | Default silencioso de 1 punto (recomendación: Opción C) | Fase 02 | 2026-08-28 |
| DP-07 | `buildPrompt()` inalcanzable: ¿el frontend envía `CREATE_STRUCTURED_USER_STORY`? | Fase 03, 04 | 2026-08-28 |
| DP-05 | Camino muerto `chat()` async | Fase 07 | 2026-08-28 |

---

## Hallazgos técnicos para fases posteriores

| Hallazgo | Detectado en | Atender en |
|---|---|---|
| ArchUnit `Rule_2.7` violada 5 veces («Beans classes should only have final attributes»). Hoy es advertencia; el plugin avisa: *«This will cause a build error in future»*. | Fase 00 | Fase 07 |
| ArchUnit `Rule_2.2` violada 1 vez («Domain classes should not be named with technology suffixes»). | Fase 00 | Fase 07 |
| `jacocoMergedReport` falla con la configuration cache de Gradle por un problema del plugin `pitest` (`pitestReportAggregate`). Workaround: `--no-configuration-cache`. | Fase 00 | Fase 07 |
| `domain/model` no tiene ningún test (0% de cobertura). | Fase 00 | Fase 07 |

