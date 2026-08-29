# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-28 · **Fase activa:** `FASE-00`

---

## Progreso

| # | Fase | Archivo de instrucciones | Estado | Cerrada el | Commit |
|---|---|---|---|---|---|
| 00 | Baseline y caracterización | `fases/FASE-00-baseline-y-caracterizacion.md` | 🟡 PENDIENTE | — | — |
| 01 | Externalización de prompts | _(se genera al cerrar Fase 00)_ | ⚪ NO GENERADA | — | — |
| 02 | Value Objects de estimación | _(se genera al cerrar Fase 01)_ | ⚪ NO GENERADA | — | — |
| 03 | Resolución de intención | _(se genera al cerrar Fase 02)_ | ⚪ NO GENERADA | — | — |
| 04 | Separación de flujos (Strategy) | _(se genera al cerrar Fase 03)_ | ⚪ NO GENERADA | — | — |
| 05 | Value Objects de configuración | _(se genera al cerrar Fase 04)_ | ⚪ NO GENERADA | — | — |
| 06 | Separación del entry-point | _(se genera al cerrar Fase 05)_ | ⚪ NO GENERADA | — | — |
| 07 | Endurecimiento y cierre | _(se genera al cerrar Fase 06)_ | ⚪ NO GENERADA | — | — |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## Métricas (se actualizan en cada fase)

| Métrica | Baseline | Actual | Objetivo |
|---|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | 644 | ≤ 100 |
| Líneas `Handler` | 495 | 495 | ≤ 150 |
| Clases > 300 líneas | 2 | 2 | 0 |
| Cobertura `domain/usecase` | _por medir_ | — | ≥ 90% |
| Cobertura `domain/model` | _por medir_ | — | ≥ 90% |
| Constructores > 5 parámetros | 1 | 1 | 0 |

---

## Bitácora de sesiones

| Fecha | Fase | Qué se hizo | Bloqueos encontrados |
|---|---|---|---|
| 2026-08-28 | — | Creación del plan maestro, decisiones pendientes y Fase 00 | DP-01 a DP-05 abiertas |

---

## Bloqueos activos

| ID | Descripción | Bloquea a | Desde |
|---|---|---|---|
| DP-01 | Precedencia de `isGeneralFlow` sobre auditoría/refinamiento | Fase 03, 04 | 2026-08-28 |
| DP-02 | Umbral de complejidad 13 vs 8 | Fase 02 | 2026-08-28 |
| DP-03 | Default silencioso de 1 punto | Fase 02 | 2026-08-28 |
| DP-04 | Nombres de recursos de prompts | Fase 01 | 2026-08-28 |
| DP-05 | Camino muerto `chat()` async | Fase 07 | 2026-08-28 |

> **Nota:** la Fase 00 **no está bloqueada** por ninguna decisión: su propósito es precisamente
> documentar el comportamiento actual sin modificarlo.

