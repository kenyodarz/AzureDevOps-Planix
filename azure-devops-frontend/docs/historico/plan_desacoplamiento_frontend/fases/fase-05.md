# FASE 05 — Separación de stores por flujo y migración reactiva

> **Estado:** ⚪ PENDIENTE · **Depende de:** FASE 04 🟢 · **Riesgo:** Alto
> **Commit al cerrar:** `refactor(devops_agent): separar stores por flujo y migrar estado a reactividad pura`
> **Siguiente fase:** `fase-06.md`
> **Regla de oro:** las **220 pruebas vigentes deben seguir verdes SIN modificar ningún `.spec.ts` previo.**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 04 cerró el 2026-09-01 (`docs/resultados/RESULTADO-FASE-04.md`) dejando:

- **220 pruebas verdes en 19 archivos** (188 de Fase 01 + 12 de `core/` + 2 de `domain/` + 18 de `services/api/`).
- **Existe `src/app/features/devops-agent/services/api/`** con 4 clientes atómicos (`AgentChatApiService`, `PlanningApiService`, `DashboardApiService`, `TasksApiService`) y su `index.ts`.
- `DevopsAgentApiService` es una fachada delegante pura sin llamadas `HttpClient` directas (R-5 cumplida).
- `pnpm build`: **435.65 kB** initial.
- `state.service.ts` sigue conteniendo 370 líneas y centraliza el estado de los 4 flujos en un único store monolítico.

**Entorno de ejecución (obligatorio en esta máquina):**

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
corepack pnpm <comando>
```

### 1.2 Problema que resuelve esta fase

El store monolítico `devops-agent-state.service.ts` (370 líneas) concentra estados y efectos colaterales de dominios no relacionados, provocando acoplamiento, getters ineficientes y un defecto crítico en modo zoneless:

| Deuda    | Descripción                                                                                                  | Evidencia                                                                                       |
|----------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| **D-01** | Store monolítico: mezcla Chat General, Refinamiento, Dashboard y Planeación                                  | `services/devops-agent-state.service.ts` (370 líneas)                                           |
| **D-02** | Estado no cohesivo: 4 flujos comparten un único servicio de estado                                           | Mezcla de `generalMessages$`, `refinementMessages$`, `initiatives$`, `dashboardData$`, `tasks$` |
| **D-03** | `generalMessages$` y `refinementMessages$` en el mismo servicio                                              | `services/devops-agent-state.service.ts:31-34`                                                  |
| **D-04** | `initiatives$` y `planningChunks$` en el mismo servicio que los chats                                        | `services/devops-agent-state.service.ts:35-36`                                                  |
| **D-12** | `dashboardData$` en el mismo servicio que los chats                                                          | `services/devops-agent-state.service.ts:37`                                                     |
| **D-16** | Un único `loading$` compartido para los 4 flujos bloquea la app entera                                       | `services/devops-agent-state.service.ts:27`                                                     |
| **D-17** | Getters síncronos $O(n)$ sobre `BehaviorSubject` para buscar mensajes                                        | `services/devops-agent-state.service.ts:98-106`                                                 |
| **D-26** | `planning-management.component.ts` llama directamente a `DevopsAgentApiService` para chunks en vez del store | `components/planning-management/planning-management.component.ts:60-64`                         |
| **D-27** | En modo zoneless, la tabla de planeaciones no se repinta si no rebota el `loading` compartido (H-3)          | `components/planning-management/planning-management.component.ts:31`                            |

### 1.3 Estado esperado al terminar

1. Existe `src/app/features/devops-agent/services/state/` con stores especializados por flujo:
  - `general-chat-state.service.ts`
  - `refinement-chat-state.service.ts`
  - `planning-state.service.ts`
  - `dashboard-state.service.ts`
  - `tasks-state.service.ts`
  - `index.ts`
2. `DevopsAgentStateService` se conserva como fachada delegante para preservar el 100% de la compatibilidad con los componentes y tests existentes (R-3).
3. Cada store maneja su propio `loading$` / `loading` independiente (D-16).
4. `PlanningManagementComponent` migra `editableInitiatives` a `signal()` para solventar el defecto zoneless D-27 (aplicando **R-1** antes de independizar `loading`).
5. Las 220 pruebas previas siguen pasando al 100% sin modificar ningún `.spec.ts` existente.
6. Se añaden pruebas unitarias para cada nuevo store en `services/state/*.spec.ts`.

### 1.4 Archivos involucrados

| Ruta                                                                                              | Acción                        |
|---------------------------------------------------------------------------------------------------|-------------------------------|
| `src/app/features/devops-agent/services/state/general-chat-state.service.ts`                      | CREAR                         |
| `src/app/features/devops-agent/services/state/refinement-chat-state.service.ts`                   | CREAR                         |
| `src/app/features/devops-agent/services/state/planning-state.service.ts`                          | CREAR                         |
| `src/app/features/devops-agent/services/state/dashboard-state.service.ts`                         | CREAR                         |
| `src/app/features/devops-agent/services/state/tasks-state.service.ts`                             | CREAR                         |
| `src/app/features/devops-agent/services/state/index.ts`                                           | CREAR                         |
| `src/app/features/devops-agent/services/state/general-chat-state.service.spec.ts`                 | CREAR                         |
| `src/app/features/devops-agent/services/state/refinement-chat-state.service.spec.ts`              | CREAR                         |
| `src/app/features/devops-agent/services/state/planning-state.service.spec.ts`                     | CREAR                         |
| `src/app/features/devops-agent/services/state/dashboard-state.service.spec.ts`                    | CREAR                         |
| `src/app/features/devops-agent/services/state/tasks-state.service.spec.ts`                        | CREAR                         |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`                            | MODIFICAR (fachada delegante) |
| `src/app/features/devops-agent/services/index.ts`                                                 | MODIFICAR                     |
| `src/app/features/devops-agent/components/planning-management/planning-management.component.ts`   | MODIFICAR (migrar a signal)   |
| `src/app/features/devops-agent/components/planning-management/planning-management.component.html` | MODIFICAR (lectura de signal) |
| **Cualquier `.spec.ts` existente de Fases 01, 02, 03 y 04**                                       | **NO TOCAR**                  |

### 1.5 Reglas aplicables

- **§1** — Arquitectura por feature: separación estricta de responsabilidades en servicios de estado.
- **§3** — Manejo de estado reactivo (Signals & RxJS).
- **§5** — TypeScript estricto, cero `any`, tipos de retorno explícitos.
- **§6** — Pruebas Vitest en formato GIVEN / WHEN / THEN.
- **§7** — Política de No-Asunción.
- **R-1** — **Restricción crítica:** Migrar `editableInitiatives` a `signal()` ANTES de independizar el `loading`.
- **R-3** — Prohibido modificar `.spec.ts` existentes.

### 1.6 Decisiones pendientes que bloquean

| ID        | Estado     | Acción si sigue ABIERTA                                                                                                        |
|-----------|------------|--------------------------------------------------------------------------------------------------------------------------------|
| **DP-01** | 🔴 ABIERTA | Conservar alias público `messages` delegando en `refinementChatState.messages`. No eliminar.                                   |
| **DP-05** | 🔴 ABIERTA | Consultar si se usan Signals o BehaviorSubject en los nuevos stores. Mantener compatibilidad de observables hacia el exterior. |
| **DP-08** | 🔴 ABIERTA | Confirmar cambio de UX derivado de `loading` por flujo.                                                                        |
| **DP-10** | 🔴 ABIERTA | Aplicar R-1 como corrección obligatoria antes de independizar `loading`.                                                       |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar punto de partida

```bash
corepack pnpm test --no-watch    # 220 passed (19 archivos)
corepack pnpm build              # 435.65 kB
```

#### T-02 — Aplicar Restricción R-1 en `PlanningManagementComponent`

Migrar `editableInitiatives: EditableInitiative[]` a `editableInitiatives = signal<EditableInitiative[]>([])`. Actualizar la plantilla para leer `editableInitiatives()` preservando la reactividad en modo zoneless.

#### T-03 — Crear `services/state/general-chat-state.service.ts`

Maneja `messages$`, `loading$`, `sendMessage()`, `resetChat()`, inicializando con `GENERAL_GREETING`.

#### T-04 — Crear `services/state/refinement-chat-state.service.ts`

Maneja `messages$`, `loading$`, `sendMessage()`, `sendRefinementPrompt()`, `sendAuditPrompt()`, `resetChat()`, inicializando con `REFINEMENT_GREETING`.

#### T-05 — Crear `services/state/planning-state.service.ts`

Maneja `initiatives$`, `chunks$`, `loading$`, `loadInitiatives()`, `uploadPlanning()`, `deleteInitiative()`, `updateCell()`, `loadChunks()`.

#### T-06 — Crear `services/state/dashboard-state.service.ts`

Maneja `dashboardData$`, `loading$`, `loadDashboardData()`, `listenToDashboardStream()`.

#### T-07 — Crear `services/state/tasks-state.service.ts`

Maneja `tasks$`, `loading$`, `loadTasks()`, `cancelTask()`.

#### T-08 — Crear `services/state/index.ts`

Barrel export de los nuevos stores.

#### T-09 — Convertir `DevopsAgentStateService` en fachada delegante

Inyecta los 5 stores especializados y delega todas las llamadas, conservando el 100% de las propiedades observables y métodos públicos para satisfacer los contratos existentes (R-3, R-5, DP-01).

#### T-10 — Crear suites unitarias `services/state/*.spec.ts`

Pruebas unitarias para cada nuevo store en formato GIVEN / WHEN / THEN.

#### T-11 — Validaciones de cierre

```bash
corepack pnpm test --no-watch
corepack pnpm build
git diff --stat -- 'src/app/**/*.spec.ts'  # SOLO las nuevas suites
```

### 2.2 Qué NO hacer

1. **No eliminar `DevopsAgentStateService`**: rompería los contratos públicos de los componentes y tests existentes.
2. **No eliminar el alias `messages`** mientras DP-01 no se resuelva.
3. **No separar `loading` sin haber aplicado R-1 en `PlanningManagementComponent`**.
4. **No modificar ningún `.spec.ts` previo**.

### 2.3 Criterios de aceptación

- [ ] Existe `services/state/` con los 5 stores especializados y su barrel `index.ts`.
- [ ] `PlanningManagementComponent` utiliza `signal()` para su estado de vista y pasa las pruebas zoneless.
- [ ] `DevopsAgentStateService` delega limpiamente en los stores atómicos sin retener lógica de negocio monolítica.
- [ ] Las 220 pruebas previas continúan verdes sin modificar ningún `.spec.ts` existente.
- [ ] Pruebas unitarias nuevas para los 5 stores en `services/state/*.spec.ts`.
- [ ] `corepack pnpm build` dentro de presupuesto (< 500 kB).

### 2.4 Checklist de calidad (de `rules/angular-rules.md` §8)

- [ ] Todos los servicios tienen `@Injectable({ providedIn: 'root' })`
- [ ] Cero `any` en variables, parámetros o retornos
- [ ] Tipado explícito en todos los métodos públicos
- [ ] Pruebas Vitest en formato GIVEN / WHEN / THEN
- [ ] Reactividad limpia sin suscripciones manuales descontroladas
- [ ] `pnpm build` y `pnpm test` correctos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                        | Resultado esperado               |
|-----:|-----------------------------------------------------------------------------------------------|----------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y resolver decisiones bloqueantes DP-01, DP-05, DP-08, DP-10       | Decisiones claras                |
| P-02 | `corepack pnpm test --no-watch`                                                               | 220 passed                       |
| P-03 | Aplicar R-1 en `PlanningManagementComponent`                                                  | Reactividad zoneless corregida   |
| P-04 | Crear los 5 stores atómicos en `services/state/`                                              | Stores creados                   |
| P-05 | Crear `services/state/index.ts`                                                               | Barrel creado                    |
| P-06 | Refactorizar `DevopsAgentStateService` a fachada                                              | Fachada delegante limpia         |
| P-07 | Crear suites `services/state/*.spec.ts`                                                       | Pruebas nuevas                   |
| P-08 | `corepack pnpm test --no-watch`                                                               | Todas las pruebas verdes         |
| P-09 | `corepack pnpm build`                                                                         | Correcto y < 500 kB              |
| P-10 | Escribir `docs/resultados/RESULTADO-FASE-05.md`                                               | Trazabilidad                     |
| P-11 | Actualizar `docs/plan/ESTADO.md`                                                              | Tablero al día                   |
| P-12 | Generar `docs/fases/fase-06.md`                                                               | Checkpoint de continuidad creado |
| P-13 | Commit: `refactor(devops_agent): separar stores por flujo y migrar estado a reactividad pura` | Versionado                       |

### 3.1 Contenido mínimo del MD de la Fase 06

**Nombre.** FASE 06 — Flujo de tareas, sondeo y temporizadores seguros. **Riesgo.** Medio. **Bloqueada por:** DP-05, DP-06. **Deudas.** D-13, D-15, D-18, M-06. **Objetivo.** Desacoplar el sondeo recursivo de tareas (`setTimeout`), corregir fugas de memoria en temporizadores y migrar a operadores RxJS declarativos (`timer`, `switchMap`, `takeUntilDestroyed`).

