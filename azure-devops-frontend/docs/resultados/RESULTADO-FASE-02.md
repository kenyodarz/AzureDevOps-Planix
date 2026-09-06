# RESULTADO — FASE 02: Servicios de API y Gestión de Estado Reactivo (`services/api` y `services/state`)

> **Ejecutada:** 2026-09-06 · **Estado final:** 🟢 COMPLETADA  
> **Commit propuesto:** `feat(frontend): implementar servicios de api y estado reactivo para program planning y specs`  
> **Instrucciones:** `docs/fases/FASE-02-servicios-api-estado-reactivo.md`  
> **Código productivo y pruebas intervenidos:** 5 archivos modificados (`planning-api.service.ts`, `devops-agent-api.service.ts`, `planning-state.service.ts`, `devops-agent-state.service.ts` y sus respectivos `.spec.ts`).

---

## 1. Objetivo de la fase

Dotar a la capa de servicios de frontend (`services/api` y `services/state`) de las capacidades para:

1. Consumir los endpoints de Program Planning (`POST /api/planning/program`) y gestión documental de Specs (`GET /api/planning/specs`, `GET /api/planning/specs/{name}`) a través de `PlanningApiService` y la fachada `DevopsAgentApiService`.
2. Implementar reactividad completa en `PlanningStateService` mediante Observables y Signals (`availableSpecs`, `selectedSpec`, `loadingSpecs`, `planningRunning`), orquestando las acciones documentales (`loadAvailableSpecs`, `selectSpec`, `clearSelectedSpec`) y de planeación macro (`triggerProgramPlanning`).
3. Notificar reactivamente a `TasksStateService.triggerImmediatePoll()` al recibir la respuesta HTTP 202 (Accepted) con la tarea encolada del agente.
4. Mantener retrocompatibilidad absoluta con las operaciones preexistentes de vectorización de iniciativas (`uploadPlanning`, `getInitiatives`, etc.) y 100% de cobertura en tests unitarios.

---

## 2. Qué se construyó

| Artefacto                                                                     | Acción     | Detalle                                                                                                                                                                                                                                                             |
|:------------------------------------------------------------------------------|:-----------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `src/app/features/devops-agent/services/api/planning-api.service.ts`          | MODIFICADO | Agregados métodos `triggerProgramPlanning`, `getAvailableSpecs` y `getSpecDocument`.                                                                                                                                                                                |
| `src/app/features/devops-agent/services/api/planning-api.service.spec.ts`     | MODIFICADO | Añadidas 3 suites de pruebas unitarias verificando rutas, verbos HTTP, payloads y respuestas (subiendo de 5 a 8 tests).                                                                                                                                             |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`          | MODIFICADO | Expuestos los 3 nuevos métodos delegados a `PlanningApiService` en la fachada de API.                                                                                                                                                                               |
| `src/app/features/devops-agent/services/devops-agent-api.service.spec.ts`     | MODIFICADO | Agregadas 3 pruebas de delegación en la fachada (subiendo de 18 a 21 tests).                                                                                                                                                                                        |
| `src/app/features/devops-agent/services/state/planning-state.service.ts`      | MODIFICADO | Agregados Observables y Signals (`availableSpecs`, `selectedSpec`, `loadingSpecs`, `planningRunning`), inyección de `TasksStateService`, e implementación de `loadAvailableSpecs()`, `selectSpec(name)`, `clearSelectedSpec()` y `triggerProgramPlanning(request)`. |
| `src/app/features/devops-agent/services/state/planning-state.service.spec.ts` | MODIFICADO | Pruebas exhaustivas de inicialización, éxito, error, banderas de carga, llamadas a `TasksStateService.triggerImmediatePoll()` y actualización de Signals (subiendo de 11 a 19 tests).                                                                               |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`        | MODIFICADO | Expuestos los nuevos observables y métodos de conveniencia hacia la fachada central unificada.                                                                                                                                                                      |

### 2.1 Pruebas Unitarias

| Archivo                                                   | Casos antes | Casos después |         Estado          |
|:----------------------------------------------------------|:-----------:|:-------------:|:-----------------------:|
| `planning-api.service.spec.ts`                            |      5      |       8       |    🟢 8 PASSED (+3)     |
| `devops-agent-api.service.spec.ts`                        |     18      |      21       |    🟢 21 PASSED (+3)    |
| `planning-state.service.spec.ts`                          |     11      |      19       |    🟢 19 PASSED (+8)    |
| **Suite completa del frontend (`ng test --watch=false`)** |   **291**   |    **305**    | 🟢 **305 PASSED (+14)** |

---

## 3. Decisiones aplicadas

| ID                  | Resolución                                                                           | Efecto en esta fase                                                                                                                                                                         |
|:--------------------|:-------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AGENTS.md**       | Usar `signal()` para estado simple de UI y `BehaviorSubject` para estado compartido. | `PlanningStateService` gestiona el estado interno con `BehaviorSubject` expuestos como Observables y deriva `Signal` con `toSignal` para ergonomía en componentes standalone de Angular 22. |
| **AGENTS.md**       | Desacoplamiento y cero `any`.                                                        | Se usaron los contratos inmutables `ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO` y `SpecDocumentDTO` sin un solo uso de `any`.                                           |
| **Plan Maestro §3** | Coordinación `PlanningState` $\to$ `TasksState`.                                     | Al recibir la respuesta del encolado macro (HTTP 202), `PlanningStateService` ejecuta `tasksState.triggerImmediatePoll()` acelerando el ciclo de pooling de tareas activas.                 |

---

## 4. Métricas obtenidas

| # | Métrica                                  |          Antes |        Después |                   Meta |
|--:|:-----------------------------------------|---------------:|---------------:|-----------------------:|
| 1 | **Estado de Compilación (`pnpm build`)** | OK (0 errores) | OK (0 errores) |           🟢 0 errores |
| 2 | **Tamaño bundle inicial**                |      445.87 kB |      445.87 kB | 🟢 Presupuesto intacto |
| 3 | **Tipos `any` introducidos**             |              0 |              0 |                   🟢 0 |
| 4 | **Tests unitarios pasando**              |      291 / 291 |      305 / 305 |        🟢 100% pasando |

---

## 5. Comandos ejecutados

```bash
# 1. Ejecución de suite de tests completa
$ corepack pnpm test --watch=false
Test Files  29 passed (29)
     Tests  305 passed (305)
  Duration  9.20s

# 2. Compilación de producción
$ corepack pnpm build
Application bundle generation complete. [4.554 seconds]
Initial total: 445.87 kB | Estimated transfer size: 103.12 kB
```

---

## 6. Checklist de calidad

- [x] `triggerProgramPlanning`, `getAvailableSpecs` y `getSpecDocument` implementados en `PlanningApiService`.
- [x] Fachada `DevopsAgentApiService` actualizada con delegación directa.
- [x] `PlanningStateService` enriquecido con Observables y Signals para catálogo de specs y ejecución de planeación.
- [x] Invocación reactiva a `TasksStateService.triggerImmediatePoll()` al confirmar el encolado del plan.
- [x] Pruebas unitarias actualizadas al 100% (14 nuevos casos de prueba, 0 fallos).
- [x] Compilación de producción `pnpm build` sin errores ni advertencias.
- [x] Cero uso de `any` y retrocompatibilidad total preservada.
