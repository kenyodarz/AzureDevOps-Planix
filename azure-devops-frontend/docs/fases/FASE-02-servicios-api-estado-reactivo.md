# FASE 02 — Servicios de API y Gestión de Estado Reactivo (`services/api` y `services/state`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 01 · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(frontend): implementar servicios de api y estado reactivo para program planning y specs`  
> **Siguiente fase:** `fases/FASE-03-visor-explorador-specs.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

En la **Fase 01** se registraron los contratos de endpoints en `src/app/core/config/api-endpoints.ts` (`PLANNING_PROGRAM`, `PLANNING_SPECS` y el helper `specUrl`) y se modelaron los DTOs inmutables en `src/app/features/devops-agent/models/devops-agent.model.ts` (`ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO`, `SpecDocumentDTO` y `PlanningActiveView`).

### 1.2 Problema que resuelve esta fase

Actualmente:

1. `PlanningApiService` solo gestiona las operaciones de ingestión y consulta de iniciativas vectorizadas legadas (`uploadPlanning`, `getInitiatives`, `deleteInitiative`, `updateInitiativeCell`, `getInitiativeChunks`).
2. `PlanningStateService` solo maneja estado de carga de texto plano e iniciativas vectorizadas; no cuenta con estado reactivo para gestionar el catálogo de especificaciones Markdown (`SpecListDTO`), el documento de spec seleccionado (`SpecDocumentDTO`), ni la ejecución asíncrona de planes macro (`ProgramPlanResponseDTO` $\to$ notificación a `TasksStateService`).
3. `DevopsAgentApiService` no expone los métodos correspondientes como fachada.

### 1.3 Estado esperado al terminar

1. `PlanningApiService` expone:
  - `triggerProgramPlanning(payload: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO>` (`POST /api/planning/program`).
  - `getAvailableSpecs(): Observable<SpecListDTO>` (`GET /api/planning/specs`).
  - `getSpecDocument(name: string): Observable<SpecDocumentDTO>` (`GET /api/planning/specs/{name}`).
2. `DevopsAgentApiService` delega estos métodos hacia `PlanningApiService`.
3. `PlanningStateService` incorpora:
  - Signals / Observables para `availableSpecs`, `selectedSpec`, `loadingSpecs`, `planningRunning`.
  - Método `loadAvailableSpecs()` para poblar la lista de specs.
  - Método `selectSpec(name: string)` para cargar y seleccionar un spec.
  - Método `triggerProgramPlanning(request: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO>` que activa el flag `planningRunning`, delega al API, y al recibir la tarea encolada (202), invoca `triggerImmediatePoll()` en `TasksStateService`.
4. Pruebas unitarias actualizadas y pasando al 100% en `planning-api.service.spec.ts` y `planning-state.service.spec.ts`.
5. Compilación `pnpm build` sin errores ni advertencias.

### 1.4 Archivos involucrados

| Ruta                                                                          | Acción    |
|-------------------------------------------------------------------------------|-----------|
| `src/app/features/devops-agent/services/api/planning-api.service.ts`          | MODIFICAR |
| `src/app/features/devops-agent/services/api/planning-api.service.spec.ts`     | MODIFICAR |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`          | MODIFICAR |
| `src/app/features/devops-agent/services/devops-agent-api.service.spec.ts`     | MODIFICAR |
| `src/app/features/devops-agent/services/state/planning-state.service.ts`      | MODIFICAR |
| `src/app/features/devops-agent/services/state/planning-state.service.spec.ts` | MODIFICAR |

### 1.5 Reglas aplicables

- `AGENTS.md` (Fase 1 Frontend): Usar `signal()` para estado de UI o `BehaviorSubject` para estado compartido; evitar fugas de memoria con `takeUntilDestroyed()`; tipado estricto y cero `any`.
- Clean Architecture: Servicios desacoplados con responsabilidades claras (API = transporte HTTP, State = gestión de estado y coordinación).

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 · Extender `PlanningApiService` y `DevopsAgentApiService`

En `src/app/features/devops-agent/services/api/planning-api.service.ts`:

- Importar `API_ENDPOINTS`, `specUrl`, `ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO`, `SpecDocumentDTO`.
- Implementar:
  ```typescript
  triggerProgramPlanning(request: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO> {
    return this.http.post<ProgramPlanResponseDTO>(API_ENDPOINTS.PLANNING_PROGRAM, request);
  }

  getAvailableSpecs(): Observable<SpecListDTO> {
    return this.http.get<SpecListDTO>(API_ENDPOINTS.PLANNING_SPECS);
  }

  getSpecDocument(name: string): Observable<SpecDocumentDTO> {
    return this.http.get<SpecDocumentDTO>(specUrl(name));
  }
  ```
- Delegar los tres métodos en `DevopsAgentApiService`.
- Actualizar `planning-api.service.spec.ts` y `devops-agent-api.service.spec.ts` cubriendo casos de éxito y error.

#### T-02 · Extender `PlanningStateService` con gestión reactiva

En `src/app/features/devops-agent/services/state/planning-state.service.ts`:

- Incorporar señales o subjects reactivos:
  - `availableSpecs`: lista de nombres de specs disponibles (`string[]`).
  - `selectedSpec`: documento Markdown actualmente activo (`SpecDocumentDTO | null`).
  - `loadingSpecs`: indicador booleano de consulta de specs.
  - `planningRunning`: indicador booleano de envío de planeación.
- Implementar métodos de orquestación:
  - `loadAvailableSpecs(): void`
  - `selectSpec(name: string): void`
  - `clearSelectedSpec(): void`
  - `triggerProgramPlanning(request: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO>`: coordina el flag `planningRunning`, inyecta `TasksStateService` y llama a `tasksState.triggerImmediatePoll()` al recibir la respuesta.
- Actualizar `planning-state.service.spec.ts` para cubrir todas las nuevas señales, métodos y flujos de error con notificaciones.

### 2.2 Qué NO hacer

- NO eliminar los métodos existentes de iniciativas vectorizadas para no romper la retrocompatibilidad con componentes existentes.
- NO introducir tipos `any`.
- NO omitir las pruebas unitarias.

### 2.3 Criterios de aceptación

- [ ] `triggerProgramPlanning`, `getAvailableSpecs` y `getSpecDocument` expuestos en `PlanningApiService` y `DevopsAgentApiService`.
- [ ] Estado reactivo de specs (`availableSpecs`, `selectedSpec`, `loadingSpecs`, `planningRunning`) funcional en `PlanningStateService`.
- [ ] `TasksStateService.triggerImmediatePoll()` invocado tras el encolado exitoso de una planeación macro.
- [ ] Cobertura de tests unitarios actualizada al 100% en los servicios intervenidos.
- [ ] `corepack pnpm build` y `corepack pnpm test --watch=false` limpios con cero errores.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                          | Resultado esperado                      |
|-----:|---------------------------------------------------------------------------------|-----------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`               | Confirmar que la fase no está bloqueada |
| P-02 | Ejecutar tarea T-01 (`PlanningApiService`, `DevopsAgentApiService` y sus tests) | Métodos HTTP testeados                  |
| P-03 | Ejecutar tarea T-02 (`PlanningStateService` y sus tests)                        | Estado reactivo testeado                |
| P-04 | Ejecutar `corepack pnpm test --watch=false` y `corepack pnpm build`             | 100% tests pasando y build verde        |
| P-05 | Escribir `docs/resultados/RESULTADO-FASE-02.md`                                 | Evidencias y métricas registradas       |
| P-06 | Actualizar `docs/plan/ESTADO.md`: Fase 02 🟢 COMPLETADA                         | Tablero al día                          |
| P-07 | Generar `docs/fases/FASE-03-visor-explorador-specs.md` y prompt                 | Continuidad lista                       |
| P-08 | Presentar al usuario propuesta de commit                                        | Listo para autorización                 |
