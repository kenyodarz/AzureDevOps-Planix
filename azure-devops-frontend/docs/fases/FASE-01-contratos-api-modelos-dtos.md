# FASE 01 — Contratos de API, Modelos y DTOs (`core/config` y `models`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Ninguna · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(frontend): registrar contratos de api y dtos para program planning y specs`  
> **Siguiente fase:** `fases/FASE-02-servicios-api-estado-reactivo.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos
El BFF (`azure-devops-backend`) completó con éxito su integración y expone:
- `POST /api/planning/program`: activación estructurada de planeación macro (`ProgramPlanRequestDTO` $\to$ `ProgramPlanResponseDTO`).
- `GET /api/planning/specs`: listado de especificaciones documentales (`SpecListDTO`).
- `GET /api/planning/specs/{specName}`: lectura íntegra de un documento Markdown (`SpecDocumentDTO`).

### 1.2 Problema que resuelve esta fase
Actualmente `azure-devops-frontend`:
1. Solo tiene declaradas en `src/app/core/config/api-endpoints.ts` las rutas obsoletas hacia la vectorización en PostgreSQL (`/api/planning/ingest`, `/api/planning/initiatives`).
2. No cuenta con las interfaces TypeScript ni DTOs para mapear las solicitudes y respuestas de Program Planning ni los documentos de especificaciones en `src/app/features/devops-agent/models/devops-agent.model.ts`.

### 1.3 Estado esperado al terminar
1. `src/app/core/config/api-endpoints.ts` expone las constantes `API_ENDPOINTS.PLANNING_PROGRAM`, `API_ENDPOINTS.PLANNING_SPECS` y la función helper `specUrl(name: string)`.
2. `src/app/features/devops-agent/models/devops-agent.model.ts` declara las interfaces inmutables `ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO`, `SpecDocumentDTO` y el tipo de unión `PlanningActiveView`.
3. `pnpm build` compila con cero errores y cero advertencias de tipo `any`.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `src/app/core/config/api-endpoints.ts` | MODIFICAR |
| `src/app/features/devops-agent/models/devops-agent.model.ts` | MODIFICAR |

### 1.5 Reglas aplicables
- `AGENTS.md` (Fase 1 Frontend): Tipado estricto, cero `any`, inmutabilidad.
- Clean Architecture: Modelos puros y desacoplados de infraestructura externa.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 · Declarar endpoints en `api-endpoints.ts`
En `src/app/core/config/api-endpoints.ts`:
- Agregar al objeto `API_ENDPOINTS`:
  ```typescript
  PLANNING_PROGRAM: `${BASE}/api/planning/program`,
  PLANNING_SPECS: `${BASE}/api/planning/specs`,
  ```
- Exportar la función helper:
  ```typescript
  export const specUrl = (name: string): string =>
    `${API_ENDPOINTS.PLANNING_SPECS}/${encodeURIComponent(name)}`;
  ```

#### T-02 · Declarar modelos DTO en `devops-agent.model.ts`
En `src/app/features/devops-agent/models/devops-agent.model.ts`:
- Definir `ProgramPlanRequestDTO`:
  ```typescript
  export interface ProgramPlanRequestDTO {
    quarter: string;
    sprintCount: number;
    maxCapacityPerSprint: number;
    targetFronts: string[];
    objectives: string;
    contextId?: string;
  }
  ```
- Definir `ProgramPlanResponseDTO`:
  ```typescript
  export interface ProgramPlanResponseDTO {
    summary: string;
    quarter: string;
    sprintCount: number;
    maxCapacityPerSprint: number;
    targetFronts: string[];
    contextId: string;
    task: AgentTask;
  }
  ```
- Definir `SpecListDTO` y `SpecDocumentDTO`:
  ```typescript
  export interface SpecListDTO {
    specs: string[];
    total: number;
  }

  export interface SpecDocumentDTO {
    name: string;
    content: string;
    path: string;
  }
  ```
- Definir el tipo de vista activa:
  ```typescript
  export type PlanningActiveView = 'explorer' | 'new-plan' | 'legacy-vector';
  ```

### 2.2 Qué NO hacer
- NO modificar rutas existentes ni alterar la firma de endpoints preexistentes.
- NO introducir tipos `any` ni librerías externas.
- NO alterar la lógica de los servicios en esta fase (se aborda en la Fase 02).

### 2.3 Criterios de aceptación
- [ ] `PLANNING_PROGRAM` y `PLANNING_SPECS` registrados en `API_ENDPOINTS`.
- [ ] Helper `specUrl` codifica apropiadamente el nombre del archivo.
- [ ] DTOs `ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO` y `SpecDocumentDTO` exportados y fuertemente tipados.
- [ ] `pnpm build` finaliza con éxito.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar que la fase no está bloqueada |
| P-02 | Ejecutar tareas T-01 y T-02 | Contratos tipados implementados |
| P-03 | Ejecutar `pnpm build` | Bundle generado exitosamente |
| P-04 | Escribir `docs/resultados/RESULTADO-FASE-01.md` | Evidencia y métricas reales |
| P-05 | Actualizar `docs/plan/ESTADO.md`: Fase 01 🟢 COMPLETADA | Tablero actualizado |
| P-06 | Generar `docs/fases/FASE-02-servicios-api-estado-reactivo.md` y prompt | Continuidad lista |
| P-07 | Presentar al usuario propuesta de commit | Listo para autorización |
