# Tablero de Estado — Integración de Program Planning en el BFF

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio al iniciar cualquier sesión de desarrollo o antes de ejecutar código.  
>
> **Última actualización:** 2026-09-06 · **Fase activa:**
> `Fase 02 — Modelos de Dominio para Program Planning`

---

## 1. Tablero de Fases

|   #    | Fase                                                                          | Archivo de Especificación                          | Resultado / Cierre                |      Estado       | Fecha de Cierre |
|:------:|:------------------------------------------------------------------------------|:---------------------------------------------------|:----------------------------------|:-----------------:|:---------------:|
| **01** | Modelos y Puerto de Almacenamiento Documental (`domain/model`)                | `fases/FASE-01-modelos-y-puerto-spec-storage.md`   | `resultados/RESULTADO-FASE-01.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **02** | Modelos de Dominio para Program Planning (`domain/model`)                     | `fases/FASE-02-modelos-dominio-planeacion.md`      | `resultados/RESULTADO-FASE-02.md` | 🟡 **PENDIENTE**  |        —        |
| **03** | Adaptador Concreto `FileSystemSpecAdapter` (`infrastructure/driven-adapters`) | `fases/FASE-03-adaptador-filesystem-spec.md`       | `resultados/RESULTADO-FASE-03.md` |  ⚪ NO GENERADA   |        —        |
| **04** | Casos de Uso de Gestión Documental y Planeación (`domain/usecase`)            | `fases/FASE-04-casos-de-uso-planeacion.md`         | `resultados/RESULTADO-FASE-04.md` |  ⚪ NO GENERADA   |        —        |
| **05** | Entry-Points Reactivos y DTOs (`infrastructure/entry-points/reactive-web`)    | `fases/FASE-05-entry-points-reactivos-planning.md` | `resultados/RESULTADO-FASE-05.md` |  ⚪ NO GENERADA   |        —        |
| **06** | Cableado en Spring (`app-service`), Validación E2E y Cierre Definitivo        | `fases/FASE-06-cableado-spring-validacion-e2e.md`  | `resultados/RESULTADO-FASE-06.md` |  ⚪ NO GENERADA   |        —        |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🎯 **Fase 02 Lista para Ejecución:**  
>
Abrir [FASE-02-modelos-dominio-planeacion.md](file:///c:/Users/minaj/Work/GitHub/Labs/AzureDevOps/azure-devops-backend/docs/fases/FASE-02-modelos-dominio-planeacion.md)
o ejecutar su
prompt [PROMPT-FASE-02.md](file:///c:/Users/minaj/Work/GitHub/Labs/AzureDevOps/azure-devops-backend/docs/prompts/PROMPT-FASE-02.md)
para modelar `ProgramPlanRequest`, `ProgramPlanResult`, `SprintAllocation`, `ActivityType` y
`ProgramPlanningCommand` en `domain/model`.

---

## 3. Decisiones Abiertas que Bloquean

| ID | Bloquea | Estado | Resumen |
| :---: | :---: | :---: | :--- |
| **DP-BFF-01** | Fase 01, 03 | 🟢 **RESUELTA** | Almacenamiento documental vía `SpecStoragePort` y `FileSystemSpecAdapter`. |
| **DP-BFF-02** | Fase 05 | 🟢 **RESUELTA** | Coexistencia de endpoints legados `/api/planning/**` con nuevas rutas documentales. |
| **DP-BFF-03** | Fase 02, 04 | 🟢 **RESUELTA** | Serialización canónica a `/plan ...` y delegación vía `TrackAgentTaskUseCase` (JSON-RPC 2.0). |

> ✅ **Cero decisiones bloqueantes abiertas.** Se puede proceder con la ejecución técnica.

---

## 4. Métricas Vivas del Proyecto (`azure-devops-backend`)

| Métrica                                                        | Baseline Inicial | Meta del Plan |        Estado Actual         |
|:---------------------------------------------------------------|:----------------:|:-------------:|:----------------------------:|
| **Tests Unitarios Pasando**                                    |       238        | 100% pasando  |         🟢 247 / 247         |
| **Cobertura en `domain/model`**                                |      94.8%       |  $\ge 90\%$   |           🟢 95.2%           |
| **Cobertura en `model.spec`**                                  |       N/A        |  $\ge 90\%$   |           🟢 100%            |
| **Cobertura en `domain/usecase`**                              |      98.9%       |  $\ge 90\%$   |           🟢 98.9%           |
| **Violaciones de Arquitectura (ArchUnit / validateStructure)** |        0         |       0       |             🟢 0             |
| **Estado del Build (`./gradlew test`)**                        |        OK        |      OK       | 🟢 Limpio (BUILD SUCCESSFUL) |

---

## 5. Bitácora del Plan

|     Fecha      | Evento                                                                                                                                                                                                                                                                             |
|:--------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **2026-09-06** | Cierre exitoso de la **Fase 01**: Modelos y puerto documental (`SpecDocument`, `SpecNotFoundException`, `SpecStoragePort`) incorporados en `domain/model` con 9 pruebas unitarias y 100% de cobertura. Suite total sube a 247 tests pasando al 100%. Activación de la **Fase 02**. |
| **2026-09-06** | Inicialización del Plan Maestro de Integración de Program Planning y Almacenamiento Documental en el BFF (`azure-devops-backend`). Resguardo histórico del plan anterior en `docs/historico/plan_refactorizacion_bff/`. Fase 01 lista para ejecución.                              |
