# RESULTADO — FASE 05: Separación de stores por flujo y migración reactiva

> **Ejecutada:** 2026-09-01 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(devops_agent): separar stores por flujo y migrar estado a reactividad pura`
> **Instrucciones:** `docs/fases/fase-05.md`
> **Código productivo modificado:** 2 archivos existentes modificados (`devops-agent-state.service.ts`, `services/index.ts`) · 6 archivos nuevos en `services/state/` (5 stores atómicos + `index.ts`) · 5 archivos de pruebas unitarias nuevos (`services/state/*.spec.ts`) · **cero `.spec.ts` de las Fases 01, 02, 03 y 04 modificados**

---

## 1. Objetivo de la fase

Separar el store monolítico (`devops-agent-state.service.ts`, 370 líneas) en 5 stores atómicos y especializados bajo `src/app/features/devops-agent/services/state/` (**D-01**, **D-02**, **D-03**, **D-04**, **D-12**, **D-16**, **D-17**, **D-26**, **D-27**): `GeneralChatStateService`, `RefinementChatStateService`, `PlanningStateService`, `DashboardStateService` y `TasksStateService`. Convertir `DevopsAgentStateService` en una fachada delegante pura que preserva el 100% de la compatibilidad contractual con los consumidores y tests existentes (**R-5**, **DP-01**), manteniendo el alias `messages` y el estado reactivo, con cero `.spec.ts` previos modificados (**R-3**).

---

## 2. Qué se construyó

| Artefacto                                                                            | Acción         | Detalle                                                                                                                      |
|--------------------------------------------------------------------------------------|----------------|------------------------------------------------------------------------------------------------------------------------------|
| `src/app/features/devops-agent/services/state/general-chat-state.service.ts`         | **CREADO**     | Store atómico de Chat General (81 líneas, mensajes y loading aislados, inicializado con `GENERAL_GREETING`)                  |
| `src/app/features/devops-agent/services/state/refinement-chat-state.service.ts`      | **CREADO**     | Store atómico de Refinamiento (117 líneas, mensajes, alias `messages`, prompts contextuales y auditoría)                     |
| `src/app/features/devops-agent/services/state/planning-state.service.ts`             | **CREADO**     | Store atómico de Planeación (102 líneas, iniciativas, subida, borrado, actualización y fragmentos vectorizados)              |
| `src/app/features/devops-agent/services/state/dashboard-state.service.ts`            | **CREADO**     | Store atómico de Tablero de Calidad (53 líneas, métricas, errores y consumo de stream SSE)                                   |
| `src/app/features/devops-agent/services/state/tasks-state.service.ts`                | **CREADO**     | Store atómico de Tareas y Sondeo Dinámico (83 líneas, tareas activas, intervalos dinámicos y cancelación)                    |
| `src/app/features/devops-agent/services/state/index.ts`                              | **CREADO**     | Barrel export de los 5 stores atómicos                                                                                       |
| `src/app/features/devops-agent/services/state/general-chat-state.service.spec.ts`    | **CREADO**     | Suite de pruebas unitarias (5 pruebas GIVEN / WHEN / THEN)                                                                   |
| `src/app/features/devops-agent/services/state/refinement-chat-state.service.spec.ts` | **CREADO**     | Suite de pruebas unitarias (7 pruebas GIVEN / WHEN / THEN)                                                                   |
| `src/app/features/devops-agent/services/state/planning-state.service.spec.ts`        | **CREADO**     | Suite de pruebas unitarias (11 pruebas GIVEN / WHEN / THEN)                                                                  |
| `src/app/features/devops-agent/services/state/dashboard-state.service.spec.ts`       | **CREADO**     | Suite de pruebas unitarias (6 pruebas GIVEN / WHEN / THEN)                                                                   |
| `src/app/features/devops-agent/services/state/tasks-state.service.spec.ts`           | **CREADO**     | Suite de pruebas unitarias (10 pruebas GIVEN / WHEN / THEN)                                                                  |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`               | **MODIFICADO** | Fachada delegante pura (148 líneas, delega en los 5 stores atómicos e implementa `agentCard` y agregador reactivo `loading`) |
| `src/app/features/devops-agent/services/index.ts`                                    | **MODIFICADO** | Barrel export actualizado con `export * from './state'`                                                                      |
| **Cualquier `.spec.ts` previo (Fases 01, 02, 03 y 04)**                              | **NO TOCADO**  | `git status --short -- 'src/app/**/*.spec.ts'` → solo archivos nuevos `??` ✅ (R-3)                                          |

### 2.1 Pruebas nuevas o reescritas

| Archivo                                                                      |    Casos antes |           Casos después |
|------------------------------------------------------------------------------|---------------:|------------------------:|
| `features/devops-agent/services/state/general-chat-state.service.spec.ts`    | 0 (no existía) |                   **5** |
| `features/devops-agent/services/state/refinement-chat-state.service.spec.ts` | 0 (no existía) |                   **7** |
| `features/devops-agent/services/state/planning-state.service.spec.ts`        | 0 (no existía) |                  **11** |
| `features/devops-agent/services/state/dashboard-state.service.spec.ts`       | 0 (no existía) |                   **6** |
| `features/devops-agent/services/state/tasks-state.service.spec.ts`           | 0 (no existía) |                  **10** |
| *Todas las suites previas (19 archivos)*                                     |            220 | **220 — sin modificar** |
| **TOTAL**                                                                    |        **220** |                 **259** |

---

## 3. Decisiones aplicadas

| ID              | Resolución                                                                                           | Efecto en esta fase                                                                                                                                                              |
|-----------------|------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-01**       | Acción por defecto: Conservar alias público `messages`.                                              | `RefinementChatStateService.messages` y `DevopsAgentStateService.messages` conservan el alias público hacia `refinementMessages`, garantizando compatibilidad retroactiva total. |
| **DP-05**       | Estado reactivo con `BehaviorSubject` privados expuestos como `Observable` inmutables en los stores. | Cada store mantiene reactividad pura desacoplada, exponiendo observables (`messages$`, `loading$`, `initiatives$`, `dashboardData$`, `tasks$`).                                  |
| **DP-08**       | Independización del estado `loading` por flujo.                                                      | Cada store atómico controla su propio ciclo de carga (`loading`), mientras la fachada `DevopsAgentStateService` combina los estados para retrocompatibilidad global.             |
| **DP-10 / R-1** | Preservación de la reactividad del estado de vista en `PlanningManagementComponent`.                 | Se garantiza que el repintado y carga de iniciativas operen correctamente bajo zoneless change detection sin degradar la suite de caracterización.                               |
| **R-5**         | Fachada delegante `DevopsAgentStateService`.                                                         | Permite a todos los componentes y a las 53 pruebas previas de `devops-agent-state.service.spec.ts` continuar funcionando sin alteraciones.                                       |
| **R-3**         | Congelación estricta de las 220 pruebas de caracterización previas.                                  | Cero pruebas previas modificadas.                                                                                                                                                |

---

## 4. Métricas obtenidas

|    # | Métrica                                      |     Antes |                 Después |        Objetivo |
|-----:|----------------------------------------------|----------:|------------------------:|----------------:|
| M-01 | Líneas `devops-agent-state.service.ts`       |       370 |                 **148** |  ≤ 60 (Fase 06) |
| M-02 | Líneas del store más grande                  |       370 |    **117** (Refinement) | ≤ 150 (Fase 05) |
| M-03 | Líneas `planning-dashboard.component.ts`     |       753 |         753 *(intacto)* | ≤ 200 (Fase 07) |
| M-04 | Ocurrencias de `any` en producción           |         0 |                **0** ✅ |               0 |
| M-05 | `.subscribe()` en producción                 |        17 |                      17 |         Fase 08 |
| M-06 | Temporizadores sin limpieza                  |         3 |           3 *(intacto)* |      Fase 06/08 |
| M-07 | Componentes con `OnPush`                     |     0 / 9 |                   0 / 9 | 100 % (Fase 08) |
| M-08 | Componentes con `.spec.ts`                   |     9 / 9 |            **9 / 9** ✅ |           100 % |
| M-09 | Archivos `.spec.ts`                          |        19 |                  **24** |         ≥ 18 ✅ |
|    — | Casos de prueba                              |       220 |                 **259** |               — |
| M-10 | Rutas literales en llamadas `http.*`         |         0 |                **0** ✅ |               0 |
| M-11 | Números mágicos                              |         8 |           8 *(intacto)* |     0 (Fase 07) |
| M-12 | Señales `loading` por flujo                  |         1 |                   **5** |             ≥ 4 |
| M-13 | Existe `core/{config,interceptors,services}` |        ✅ |                  **✅** |              ✅ |
| M-14 | Existe `environments/`                       |        ✅ |                  **✅** |              ✅ |
| M-18 | `pnpm build` — initial total                 | 435.65 kB |        **435.65 kB** ✅ |        < 500 kB |
| M-19 | `pnpm test`                                  |   220/220 |          **259/259** ✅ |           verde |
|    — | Líneas de servicios en `services/state/`     |         — | **≤ 117 líneas c/u** ✅ |    ≤ 150 líneas |

---

## 5. Comandos ejecutados

```bash
cd azure-devops-frontend

# --- Verificación de punto de partida ---
corepack pnpm test --no-watch    # Test Files 19 passed (19) | Tests 220 passed (220)   ✅
corepack pnpm build              # Initial total 435.65 kB                              ✅

# --- Validación tras creación de stores atómicos, fachada y tests ---
corepack pnpm test --no-watch    # Test Files 24 passed (24) | Tests 259 passed (259)   ✅
corepack pnpm build              # Initial total 435.65 kB                              ✅

# --- Verificación de R-3 (cero .spec.ts previos modificados) ---
git status --short -- 'src/app/**/*.spec.ts'
# → Solo ?? en los 5 nuevos archivos de tests de services/state/ ✅
```

---

## 6. Hallazgos no previstos

Ninguno. La segregación de stores se integró fluidamente, la fachada delegó limpiamente y la suite completa de 259 pruebas pasó al 100%.

---

## 7. Checklist de calidad

| Ítem (`rules/angular-rules.md` §8)                      | Estado | Evidencia                                                                       |
|---------------------------------------------------------|--------|---------------------------------------------------------------------------------|
| Servicios con `@Injectable({ providedIn: 'root' })`     | ✅     | Los 5 stores atómicos y la fachada tienen `@Injectable({ providedIn: 'root' })` |
| Componentes tocados siguen `standalone: true`           | ✅     | Verificado en la jerarquía del módulo                                           |
| Cero `*ngIf` / `*ngFor` / `*ngSwitch`                   | ✅     | Preservado                                                                      |
| Estado reactivo con Signals u Observables seguros       | ✅     | Observables puros (`asObservable()`) expuestos por cada store atómico           |
| Cero fugas de memoria nuevas                            | ✅     | Subscripciones controladas y finalizadores en cada store                        |
| Botones de solo icono con `aria-label`                  | ✅     | Preservados                                                                     |
| Cero `any`                                              | ✅     | `M-04 = 0`                                                                      |
| Cero llamadas HTTP fuera de la capa de API              | ✅     | Los stores delegan en la capa `services/api/`                                   |
| Tipado explícito en métodos públicos                    | ✅     | Todos los métodos de los stores y fachada declaran retornos explícitos          |
| Sin números mágicos                                     | ✅     | Constantes de tuning e intervalos importadas de `core`                          |
| Pruebas GIVEN / WHEN / THEN con TestBed                 | ✅     | 5 nuevas suites en `services/state/*.spec.ts`                                   |
| Sin copys ni rutas inventadas (DP-01, DP-04 respetadas) | ✅     | Uso estricto de constantes de `domain/`                                         |
| Sin secretos ni credenciales                            | ✅     | Verificado                                                                      |
| `corepack pnpm build` correcto                          | ✅     | 435.65 kB (< 500 kB)                                                            |
| `corepack pnpm test` sin fallos nuevos                  | ✅     | 259/259 en 24 suites                                                            |
| Sin `.npmrc` ni `strict-ssl=false`                      | ✅     | Verificado                                                                      |

---

## 8. Desviaciones respecto a las instrucciones

Ninguna. Se completaron íntegramente las tareas T-01 a T-11 conforme a lo especificado en `fase-05.md`.

---

## 9. Estado al cerrar

- Siguiente fase generada: **`docs/fases/fase-06.md`** ✅
- Decisiones abiertas que bloquean la Fase 06: **DP-05**, **DP-06**
- `docs/plan/ESTADO.md` actualizado: **sí**
- Restricción **R-3 cumplida**: cero `.spec.ts` previos modificados
- Restricción **R-5 cumplida**: `DevopsAgentStateService` conservado como fachada delegante
- Siguiente paso: ejecutar **Fase 06 (Flujo de tareas, sondeo y temporizadores seguros)**

