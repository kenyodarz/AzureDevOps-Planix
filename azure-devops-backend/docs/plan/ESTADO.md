# Tablero de Estado — Integración de Program Planning en el BFF

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio al iniciar cualquier sesión de desarrollo o antes de ejecutar código.  
>
> **Última actualización:** 2026-09-06 · **Fase activa:**
> `Plan Maestro de Integración Completado al 100% — Listo para integración con Frontend`

---

## 1. Tablero de Fases

|   #    | Fase                                                                          | Archivo de Especificación                          | Resultado / Cierre                |      Estado       | Fecha de Cierre |
|:------:|:------------------------------------------------------------------------------|:---------------------------------------------------|:----------------------------------|:-----------------:|:---------------:|
| **01** | Modelos y Puerto de Almacenamiento Documental (`domain/model`)                | `fases/FASE-01-modelos-y-puerto-spec-storage.md`   | `resultados/RESULTADO-FASE-01.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **02** | Modelos de Dominio para Program Planning (`domain/model`)                     | `fases/FASE-02-modelos-dominio-planeacion.md`      | `resultados/RESULTADO-FASE-02.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **03** | Adaptador Concreto `FileSystemSpecAdapter` (`infrastructure/driven-adapters`) | `fases/FASE-03-adaptador-filesystem-spec.md`       | `resultados/RESULTADO-FASE-03.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **04** | Casos de Uso de Gestión Documental y Planeación (`domain/usecase`)            | `fases/FASE-04-casos-de-uso-planeacion.md`         | `resultados/RESULTADO-FASE-04.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **05** | Entry-Points Reactivos y DTOs (`infrastructure/entry-points/reactive-web`)    | `fases/FASE-05-entry-points-reactivos-planning.md` | `resultados/RESULTADO-FASE-05.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **06** | Cableado en Spring (`app-service`), Validación E2E y Cierre Definitivo        | `fases/FASE-06-cableado-spring-validacion-e2e.md`  | `resultados/RESULTADO-FASE-06.md` | 🟢 **COMPLETADA** |   2026-09-06    |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🏆 **Plan Maestro de Integración de Program Planning en el BFF Culminado Exitosamente:**
>
> Todas las 6 fases técnicas se encuentran ejecutadas y verificadas al 100%. El BFF expone con
> solidez:
> 1. Gestión documental de especificaciones (`GET /api/planning/specs` y
     `GET /api/planning/specs/{specName}`).
> 2. Activación de planeación macro reactiva (`POST /api/planning/program`).
> 3. Trazabilidad completa mediante comando canónico A2A asíncrono y seguimiento de tareas (`Task`).
> 4. Los próximos pasos corresponden a la fase de integración y consumo desde el frontend
     (`azure-devops-frontend`).

---

## 3. Decisiones Abiertas que Bloquean

|      ID       |   Bloquea   |     Estado      | Resumen                                                                                             |
|:-------------:|:-----------:|:---------------:|:----------------------------------------------------------------------------------------------------|
| **DP-BFF-01** | Fase 01, 03 | 🟢 **RESUELTA** | Almacenamiento documental vía `SpecStoragePort` y `FileSystemSpecAdapter`.                          |
| **DP-BFF-02** |   Fase 05   | 🟢 **RESUELTA** | Coexistencia de endpoints legados `/api/planning/**` con nuevas rutas documentales y de planeación. |
| **DP-BFF-03** | Fase 02, 04 | 🟢 **RESUELTA** | Serialización canónica a `/plan ...` y delegación vía `TrackAgentTaskUseCase` (JSON-RPC 2.0).       |

> ✅ **Cero decisiones bloqueantes abiertas.** Se puede proceder con la ejecución técnica.

---

## 4. Métricas Vivas del Proyecto (`azure-devops-backend`)

| Métrica                                                        | Baseline Inicial | Meta del Plan |        Estado Actual         |
|:---------------------------------------------------------------|:----------------:|:-------------:|:----------------------------:|
| **Tests Unitarios Pasando**                                    |       238        | 100% pasando  |         🟢 412 / 412         |
| **Cobertura en `domain/model`**                                |      94.8%       |  $\ge 90\%$   |           🟢 98.6%           |
| **Cobertura en `model.spec`**                                  |       N/A        |  $\ge 90\%$   |           🟢 100%            |
| **Cobertura en `model.planning`**                              |       N/A        |  $\ge 90\%$   |           🟢 100%            |
| **Cobertura en `:spec-storage`**                               |       N/A        |  $\ge 90\%$   |           🟢 98.0%           |
| **Cobertura en `domain/usecase`**                              |      98.9%       |  $\ge 90\%$   |           🟢 98.7%           |
| **Cobertura en `usecase.spec`**                                |       N/A        |  $\ge 90\%$   |           🟢 100%            |
| **Cobertura en `usecase.planning`**                            |       N/A        |  $\ge 90\%$   |           🟢 100%            |
| **Cobertura en `:reactive-web`**                               |      92.0%       |  $\ge 90\%$   |           🟢 93.0%           |
| **Cobertura en `co.com.bancolombia.api.dto`**                  |       N/A        |  $\ge 90\%$   |           🟢 100%            |
| **Cobertura en `co.com.bancolombia.api`**                      |      98.6%       |  $\ge 90\%$   |           🟢 99.0%           |
| **Cobertura en `applications/app-service`**                    |      92.0%       |  $\ge 90\%$   |           🟢 92.0%           |
| **Violaciones de Arquitectura (ArchUnit / validateStructure)** |        0         |       0       |             🟢 0             |
| **Estado del Build (`./gradlew test`)**                        |        OK        |      OK       | 🟢 Limpio (BUILD SUCCESSFUL) |

---

## 5. Bitácora del Plan

|     Fecha      | Evento                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|:--------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **2026-09-06** | Cierre exitoso de la **Fase 06 y Culminación del Plan Maestro**: Cableado formal en `UseCasesConfig.java` (`getSpecDocumentUseCase`, `listAvailableSpecsUseCase`, `triggerProgramPlanningUseCase`), parametrización en `application.yaml` (`specs.storage.path`), vinculación del submódulo `:spec-storage` y 4 nuevas pruebas en `UseCasesConfigTest` (15 tests en `app-service`). Suite consolidada de **412 tests pasando al 100%**. `validateStructure` aprueba sin advertencias. Cierre definitivo del plan de integración del BFF. |
| **2026-09-06** | Cierre exitoso de la **Fase 05**: DTOs inmutables (`ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecDocumentDTO`, `SpecListDTO`), Handlers reactivos (`SpecHandler`, `ProgramPlanningHandler`), registro de rutas en `RouterRest` (DP-BFF-02), actualización de `ApiErrorTranslator` y 17 pruebas unitarias nuevas con `WebTestClient`. Cobertura en `reactive-web` sube a 93% (100% en DTOs, 99% en handlers). Suite total sube a 408 tests pasando al 100%. Activación de la **Fase 06**.                                       |
| **2026-09-06** | Cierre exitoso de la **Fase 04**: Casos de uso `GetSpecDocumentUseCase`, `ListAvailableSpecsUseCase` y `TriggerProgramPlanningUseCase` en `domain/usecase` con 15 pruebas unitarias nuevas (100% cobertura en los nuevos paquetes, 98.7% global en usecase). Suite total sube a 391 tests pasando al 100%. Activación de la **Fase 05**.                                                                                                                                                                                                 |
| **2026-09-06** | Cierre exitoso de la **Fase 03**: Submódulo `:spec-storage` y adaptador reactivo `FileSystemSpecAdapter` con operaciones no bloqueantes (`Schedulers.boundedElastic()`), protección contra Path Traversal y 18 pruebas unitarias pasando al 100% con 98% de cobertura. Suite total sube a 376 tests. Activación de la **Fase 04**.                                                                                                                                                                                                       |
| **2026-09-06** | Cierre exitoso de la **Fase 02**: Modelos inmutables de Program Planning (`ActivityType`, `SprintAllocation`, `ProgramPlanRequest`, `ProgramPlanResult`, `ProgramPlanningCommand`) en `domain/model` con 111 pruebas unitarias y 100% de cobertura. Suite total sube a 358 tests pasando al 100%. Activación de la **Fase 03**.                                                                                                                                                                                                          |
| **2026-09-06** | Cierre exitoso de la **Fase 01**: Modelos y puerto documental (`SpecDocument`, `SpecNotFoundException`, `SpecStoragePort`) incorporados en `domain/model` con 9 pruebas unitarias y 100% de cobertura. Suite total sube a 247 tests pasando al 100%. Activación de la **Fase 02**.                                                                                                                                                                                                                                                       |
| **2026-09-06** | Inicialización del Plan Maestro de Integración de Program Planning y Almacenamiento Documental en el BFF (`azure-devops-backend`). Resguardo histórico del plan anterior en `docs/historico/plan_refactorizacion_bff/`. Fase 01 lista para ejecución.                                                                                                                                                                                                                                                                                    |
