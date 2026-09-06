# Tablero de Estado — Integración de Program Planning en el Frontend

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio al iniciar cualquier sesión de desarrollo o antes de ejecutar código.  
>
> **Última actualización:** 2026-09-06 · **Fase activa:** `Fase 04 — Modal Lanzador de Program Planning`

---

## 1. Tablero de Fases

|   #    | Fase                                                                              | Archivo de Especificación                        | Resultado / Cierre                |      Estado       | Fecha de Cierre |
|:------:|:----------------------------------------------------------------------------------|:-------------------------------------------------|:----------------------------------|:-----------------:|:---------------:|
| **01** | Contratos de API, Modelos y DTOs (`core/config` y `models`)                       | `fases/FASE-01-contratos-api-modelos-dtos.md`    | `resultados/RESULTADO-FASE-01.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **02** | Servicios de API y Gestión de Estado Reactivo (`services/api` y `services/state`) | `fases/FASE-02-servicios-api-estado-reactivo.md` | `resultados/RESULTADO-FASE-02.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **03** | Visor y Explorador Documental de Specs (`components/planning-specs-explorer`)     | `fases/FASE-03-visor-explorador-specs.md`        | `resultados/RESULTADO-FASE-03.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **04** | Modal Lanzador de Program Planning (`components/program-planning-modal`)          | `fases/FASE-04-modal-lanzador-planeacion.md`     | `resultados/RESULTADO-FASE-04.md` | 🟡 **PENDIENTE**  |        —        |
| **05** | Integración en Página Principal, Navegación a Refinamiento y Cierre E2E           | `fases/FASE-05-integracion-global-cierre-e2e.md` | `resultados/RESULTADO-FASE-05.md` |  ⚪ NO GENERADA   |        —        |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🎯 **Fase 04 Lista para Ejecución:**  
> Abrir `docs/fases/FASE-04-modal-lanzador-planeacion.md` o ejecutar su prompt `docs/prompts/PROMPT-FASE-04.md` para construir `ProgramPlanningModalComponent` en `src/app/features/devops-agent/components/program-planning-modal/`: diálogo modal accesible para configurar y lanzar la planeación macro (quarter, sprints, capacidad por sprint, frentes de valor y objetivos de negocio) conectado reactivamente con `PlanningStateService.triggerProgramPlanning()`.

---

## 3. Decisiones Abiertas que Bloquean

| ID | Bloquea | Estado | Resumen |
| :---: | :---: | :---: | :--- |
| **DP-FE-01** | Fase 03, 05 | 🟢 **RESUELTA** | Coexistencia de vistas: Explorador de Specs como vista principal y vectorización legada como opción secundaria. |
| **DP-FE-02** | Fase 05 | 🟢 **RESUELTA** | Botón *«Refinar HU en este Frente»* precarga el prompt en `RefinementChatStateService` y transiciona al tab de refinamiento. |
| **DP-FE-03** | Fase 03 | 🟢 **RESUELTA** | Renderizado seguro y estilizado de Markdown con utilidades nativas de Angular y Tailwind CSS. |

> ✅ **Cero decisiones bloqueantes abiertas.** Se puede proceder con la ejecución técnica.

---

## 4. Métricas Vivas del Proyecto (`azure-devops-frontend`)

| Métrica                                       | Baseline Inicial | Meta del Plan |     Estado Actual     |
|:----------------------------------------------|:----------------:|:-------------:|:---------------------:|
| **Estado de Compilación (`pnpm build`)**      |        OK        |      OK       | 🟢 Limpio (449.44 kB) |
| **Tipos `any` en Nuevos Modelos y Servicios** |        0         |       0       |         🟢 0          |
| **Violaciones de Arquitectura (`AGENTS.md`)** |        0         |       0       |         🟢 0          |
| **Pruebas de Componentes y Servicios Nuevos** |    291 / 291     | 100% pasando  | 🟢 327 / 327 pasando  |

---

## 5. Bitácora del Plan

|     Fecha      | Evento                                                                                                                                                                                                                                                                                          |
|:--------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **2026-09-06** | Inicialización del **Plan Maestro de Integración de Program Planning en el Frontend**. Resguardo del histórico de desacoplamiento en `docs/historico/plan_desacoplamiento_frontend/`. Fase 01 lista para ejecución.                                                                             |
| **2026-09-06** | **Fase 01 COMPLETADA**: Contratos de API (`PLANNING_PROGRAM`, `PLANNING_SPECS`, `specUrl`) y DTOs inmutables (`ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO`, `SpecDocumentDTO`, `PlanningActiveView`) registrados y compilados limpiamente. Fase 02 lista.                   |
| **2026-09-06** | **Fase 02 COMPLETADA**: Servicios de API (`PlanningApiService`, `DevopsAgentApiService`) y Estado Reactivo (`PlanningStateService`) con Signals y Observables implementados. Integración a `TasksStateService.triggerImmediatePoll()` verificada. 305 pruebas unitarias pasando. Fase 03 lista. |
| **2026-09-06** | **Fase 03 COMPLETADA**: `PlanningSpecsExplorerComponent` implementado con split layout, visor Markdown con tablas dark-mode (DP-FE-03), y botón contextual de refinamiento (DP-FE-02). 327 pruebas unitarias pasando (+22 tests). Bundle limpio en 449.44 kB. Fase 04 lista.                    |
