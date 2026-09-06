# FASE 04 — Segregación de la capa de API

> **Estado:** ⚪ PENDIENTE · **Depende de:** FASE 03 🟢 · **Riesgo:** Medio
> **Commit al cerrar:** `refactor(devops_agent): segregar clientes de api por dominio y crear fachada delegante`
> **Siguiente fase:** `fase-05.md`
> **Regla de oro:** las **202 pruebas vigentes deben seguir verdes SIN modificar ningún `.spec.ts` previo.**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 03 cerró el 2026-09-01 (`docs/resultados/RESULTADO-FASE-03.md`) dejando:

- **202 pruebas verdes en 15 archivos** (188 de la Fase 01 + 12 de `core/` + 2 de `domain/`).
- **Existe `src/app/features/devops-agent/domain/`** con `chat-greetings.ts`, `chat-prompts.ts`, `chat-suggestions.ts`, `tech-tags.ts` e `index.ts` (D-06, D-19, D-21 ✅).
- `devops-agent-state.service.ts` bajó a **370 líneas** con **0 líneas de markdown** embebido.
- `chat-input.component.ts` e `info-cards.component.ts` consumen constantes del dominio.
- `pnpm build`: **435.65 kB** initial.
- DP-04 y DP-07 🟢 resueltas; DP-03 🟢 resuelta previamente conservando `POST '/'`.

**Entorno de ejecución (obligatorio en esta máquina).** El registro npm está interceptado por TLS corporativo:

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
corepack pnpm <comando>
```

### 1.2 Problema que resuelve esta fase

El cliente de red `devops-agent-api.service.ts` (133 líneas) mezcla en una única clase cuatro dominios completamente desacoplados del backend:

| Deuda    | Descripción                                                                                      | Evidencia (`services/devops-agent-api.service.ts`)                                         |
|----------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| **D-11** | Monolito de API: 4 dominios no cohesivos en una sola clase                                       | `:22-38` (Chat/Card), `:40-69` (Planeación), `:71-102` (Dashboard/SSE), `:104-122` (Tasks) |
| **D-08** | Cierre definitivo: consumo granular de `core/config/api-endpoints.ts` por servicio especializado | Rutas segregadas por dominio de responsabilidad                                            |
| **D-26** | Preparación: desacoplar a `planning-management` de llamadas directas a HTTP                      | El servicio de planeación quedará listo para inyección directa en Fase 05                  |

### 1.3 Estado esperado al terminar

1. Existe `src/app/features/devops-agent/services/api/` con 4 servicios especializados y su `index.ts`.
2. Ningún servicio de API supera las 60 líneas de código.
3. `DevopsAgentApiService` se transforma en una **fachada delegante** pura sin lógica propia ni llamadas `HttpClient` directas, preservando el 100% de su contrato público (R-5).
4. Las 202 pruebas previas siguen pasando sin haber modificado ningún `.spec.ts` existente.
5. Se añaden suites unitarias para cada uno de los 4 nuevos clientes de API en `services/api/*.spec.ts`.

### 1.4 Archivos involucrados

| Ruta                                                                        | Acción       |
|-----------------------------------------------------------------------------|--------------|
| `src/app/features/devops-agent/services/api/agent-chat-api.service.ts`      | CREAR        |
| `src/app/features/devops-agent/services/api/planning-api.service.ts`        | CREAR        |
| `src/app/features/devops-agent/services/api/dashboard-api.service.ts`       | CREAR        |
| `src/app/features/devops-agent/services/api/tasks-api.service.ts`           | CREAR        |
| `src/app/features/devops-agent/services/api/index.ts`                       | CREAR        |
| `src/app/features/devops-agent/services/api/agent-chat-api.service.spec.ts` | CREAR        |
| `src/app/features/devops-agent/services/api/planning-api.service.spec.ts`   | CREAR        |
| `src/app/features/devops-agent/services/api/dashboard-api.service.spec.ts`  | CREAR        |
| `src/app/features/devops-agent/services/api/tasks-api.service.spec.ts`      | CREAR        |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`        | MODIFICAR    |
| `src/app/features/devops-agent/services/index.ts`                           | MODIFICAR    |
| **Cualquier `.spec.ts` existente de Fases 01, 02 y 03**                     | **NO TOCAR** |

### 1.5 Reglas aplicables

De `rules/angular-rules.md`:

- **§1** — Estructura modular y separación de responsabilidades: servicios de API atómicos.
- **§5** — TypeScript estricto, cero `any`, firmas de retorno explícitas.
- **§6** — Pruebas Vitest GIVEN / WHEN / THEN usando `provideHttpClient()` y `provideHttpClientTesting()`.
- **§7** — Política de No-Asunción.
- **R-3** — Prohibido modificar `.spec.ts` existentes.
- **R-5** — `DevopsAgentApiService` debe conservarse como fachada delegante.
- **R-6** — `environment.apiBaseUrl` debe permanecer vacío.

### 1.6 Decisiones pendientes que bloquean

| ID        | Estado                   | Acción                                                                       |
|-----------|--------------------------|------------------------------------------------------------------------------|
| **DP-03** | 🟢 RESUELTA (2026-08-31) | `tasks-api.service.ts` conserva `TASKS_CANCEL_RPC = '/'` con `POST` JSON-RPC |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar punto de partida

```bash
corepack pnpm test --no-watch    # 202 passed (15 archivos)
corepack pnpm build              # 435.65 kB
```

#### T-02 — Crear `services/api/agent-chat-api.service.ts`

- Métodos: `getAgentCard(): Observable<AgentCard>`, `sendMessage(payload: SendMessageRequest): Observable<SendMessageResponse>`
- Usa `API_ENDPOINTS.AGENT_CARD` y `API_ENDPOINTS.AGENT_MESSAGE`.

#### T-03 — Crear `services/api/planning-api.service.ts`

- Métodos: `uploadPlanning(payload: UploadPlanningRequest): Observable<IngestResponse>`, `getInitiatives(): Observable<Initiative[]>`, `deleteInitiative(id: string): Observable<void>`, `updateInitiativeCell(id: string, cell: string): Observable<Initiative>`, `getInitiativeChunks(id: string): Observable<PlanningChunk[]>`
- Usa `API_ENDPOINTS.PLANNING_UPLOAD`, `API_ENDPOINTS.PLANNING_INITIATIVES`, `initiativeUrl(id)`, `initiativeCellUrl(id)`, `initiativeChunksUrl(id)`.

#### T-04 — Crear `services/api/dashboard-api.service.ts`

- Métodos: `getDashboardData(cell: string, sprint: string): Observable<DashboardData>`, `getDashboardDataStream(cell: string, sprint: string): Observable<DashboardStreamEvent>`
- Usa `dashboardUrl(cell, sprint)` y `dashboardStreamUrl(cell, sprint)`.

#### T-05 — Crear `services/api/tasks-api.service.ts`

- Métodos: `getTasks(): Observable<AgentTask[]>`, `cancelTask(id: string): Observable<CancelTaskResponse>`
- Usa `API_ENDPOINTS.TASKS` y `API_ENDPOINTS.TASKS_CANCEL_RPC` (`POST '/'`, DP-03 🟢).

#### T-06 — Crear `services/api/index.ts`

Barrel export de los 4 servicios atómicos.

#### T-07 — Convertir `DevopsAgentApiService` en fachada delegante

Inyecta `AgentChatApiService`, `PlanningApiService`, `DashboardApiService` y `TasksApiService`. Cada método delega en el servicio atómico correspondiente sin lógica propia.

#### T-08 — Crear pruebas unitarias `services/api/*.spec.ts`

Una suite por cada nuevo servicio de API (GIVEN / WHEN / THEN con `HttpTestingController`).

#### T-09 — Validaciones de cierre

```bash
corepack pnpm test --no-watch
corepack pnpm build
git diff --stat -- 'src/app/**/*.spec.ts'  # SOLO las nuevas suites de api/
```

### 2.2 Qué NO hacer

1. **No eliminar `DevopsAgentApiService`**: rompería R-3 y R-5.
2. **No cambiar la ruta de `cancelTask`** (conservar `POST '/'`, DP-03 🟢).
3. **No modificar ningún `.spec.ts` previo**.
4. **No mover el estado ni separar los stores**: eso corresponde a la Fase 05.

### 2.3 Criterios de aceptación

- [ ] Existe `services/api/` con los 4 servicios especializados y su barrel `index.ts`.
- [ ] `DevopsAgentApiService` no contiene llamadas `http.*` directas; solo delega en los 4 servicios.
- [ ] Ningún archivo de `services/api/` supera las 60 líneas.
- [ ] Las 202 pruebas previas siguen pasando sin modificaciones.
- [ ] Pruebas unitarias nuevas para los 4 servicios de API pasando al 100%.
- [ ] `corepack pnpm build` dentro del presupuesto de 500 kB.

### 2.4 Checklist de calidad (de `rules/angular-rules.md` §8)

- [ ] Todos los servicios tienen `@Injectable({ providedIn: 'root' })`
- [ ] Cero `any` en variables, parámetros o retornos
- [ ] Tipado explícito en todos los métodos públicos
- [ ] Pruebas Vitest en formato GIVEN / WHEN / THEN
- [ ] Sin rutas inventadas (consumo de `core/config/api-endpoints`)
- [ ] `pnpm build` y `pnpm test` correctos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                           | Resultado esperado                |
|-----:|--------------------------------------------------------------------------------------------------|-----------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                                | Confirmar que DP-03 está resuelta |
| P-02 | `corepack pnpm test --no-watch`                                                                  | 202 passed                        |
| P-03 | Crear `services/api/agent-chat-api.service.ts`                                                   | Servicio creado                   |
| P-04 | Crear `services/api/planning-api.service.ts`                                                     | Servicio creado                   |
| P-05 | Crear `services/api/dashboard-api.service.ts`                                                    | Servicio creado                   |
| P-06 | Crear `services/api/tasks-api.service.ts`                                                        | Servicio creado                   |
| P-07 | Crear `services/api/index.ts`                                                                    | Barrel creado                     |
| P-08 | Refactorizar `DevopsAgentApiService` a fachada                                                   | Fachada delegante limpia          |
| P-09 | Crear suites `services/api/*.spec.ts`                                                            | Pruebas nuevas                    |
| P-10 | `corepack pnpm test --no-watch`                                                                  | Todas las pruebas verdes          |
| P-11 | `corepack pnpm build`                                                                            | Correcto y < 500 kB               |
| P-12 | Escribir `docs/resultados/RESULTADO-FASE-04.md`                                                  | Trazabilidad                      |
| P-13 | Actualizar `docs/plan/ESTADO.md`                                                                 | Tablero al día                    |
| P-14 | Generar `docs/fases/fase-05.md`                                                                  | Continuidad garantizada           |
| P-15 | Commit: `refactor(devops_agent): segregar clientes de api por dominio y crear fachada delegante` | Versionado                        |

### 3.1 Contenido mínimo del MD de la Fase 05

**Nombre.** FASE 05 — Separación de stores por flujo y migración reactiva. **Riesgo.** Alto. **Bloqueada por:** DP-01, DP-05, DP-08, DP-10. **Deudas.** D-01, D-02, D-03, D-04, D-12, D-16, D-17, D-26, D-27. **Restricción crítica (R-1):** Migrar `editableInitiatives` en `PlanningManagementComponent` a `signal()` ANTES de separar el `loading` compartido, para solventar el defecto zoneless (D-27 / H-3 de Fase 01).

