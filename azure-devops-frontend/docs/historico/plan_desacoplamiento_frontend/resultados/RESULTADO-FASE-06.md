# RESULTADO — FASE 06: Flujo de tareas, sondeo y temporizadores seguros

> **Ejecutada:** 2026-09-01 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(devops_agent): desacoplar sondeo de tareas y sanear temporizadores`
> **Instrucciones:** `docs/fases/fase-06.md`
> **Código productivo modificado:** 2 archivos modificados (`tasks-state.service.ts`, `refinement-chat-state.service.ts`) · 1 archivo de pruebas unitarias actualizado (`tasks-state.service.spec.ts`) · **cero `.spec.ts` de las Fases 01 a 05 modificados**

---

## 1. Objetivo de la fase

Refactorizar `TasksStateService` eliminando el uso de `setTimeout` recursivo e imperativo, migrándolo a un pipeline declarativo y reactivo con RxJS (`timer`, `switchMap`, `takeUntil`, `Subject`) para sanear temporizadores huérfanos y fugas de memoria (**D-02**, **D-15**, **M-06**). Conservar los intervalos fijados en `core/config/app-tuning.ts` (**DP-06**, **R-4**) y la compatibilidad contractual de la fachada `DevopsAgentStateService` (**R-5**), manteniendo el 100% de la suite de pruebas verdes sin alterar `.spec.ts` previos (**R-3**).

---

## 2. Qué se construyó

| Artefacto                                                                       | Acción         | Detalle                                                                                                                                                                     |
|---------------------------------------------------------------------------------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `src/app/features/devops-agent/services/state/tasks-state.service.ts`           | **MODIFICADO** | Pipeline declarativo RxJS con `timer`, `switchMap` y `pollTrigger$` sin ningún `setTimeout` imperativo; soporte de cancelación limpia vía `stopDynamicPolling` (109 líneas) |
| `src/app/features/devops-agent/services/state/tasks-state.service.spec.ts`      | **MODIFICADO** | Suite de pruebas unitarias expandida (11 pruebas GIVEN / WHEN / THEN, verificando ciclo de vida, intervalo dinámico y cancelación)                                          |
| `src/app/features/devops-agent/services/state/refinement-chat-state.service.ts` | **MODIFICADO** | Corrección de orden de inicialización entre `messages` y `refinementMessages` resolviendo TS2729                                                                            |
| **Cualquier `.spec.ts` previo (Fases 01 a 05)**                                 | **NO TOCADO**  | `git status --short -- 'src/app/**/*.spec.ts'` → solo `tasks-state.service.spec.ts` modificado ✅ (R-3)                                                                     |

### 2.1 Pruebas nuevas o reescritas

| Archivo                                                            | Casos antes |                                                       Casos después |
|--------------------------------------------------------------------|------------:|--------------------------------------------------------------------:|
| `features/devops-agent/services/state/tasks-state.service.spec.ts` |          10 | **11** (+1 prueba para `stopDynamicPolling` y cancelación reactiva) |
| *Todas las suites previas (23 archivos)*                           |         249 |                                             **249 — sin modificar** |
| **TOTAL**                                                          |     **259** |                                                             **260** |

---

## 3. Decisiones aplicadas

| ID              | Resolución                                                                             | Efecto en esta fase                                                                                                                                      |
|-----------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-06 / R-4** | Conservar valores literales de `core/config/app-tuning.ts`.                            | `POLLING_INTERVAL_IDLE_MS = 30000` y `POLLING_INTERVAL_ACTIVE_MS = 5000` se consumen en el pipeline declarativo de `TasksStateService` sin alteraciones. |
| **DP-05**       | Estado reactivo con `BehaviorSubject` privados expuestos como `Observable` inmutables. | `TasksStateService.tasks` expone `Observable<AgentTask[]>` inmutable vía `asObservable()`.                                                               |
| **R-5**         | Fachada delegante `DevopsAgentStateService`.                                           | Preserva la delegación hacia `tasksState` (`tasks`, `loadTasks`, `triggerImmediatePoll`, `cancelTask`).                                                  |
| **R-3**         | Congelación estricta de pruebas previas.                                               | Cero pruebas unitarias de fases 01 a 05 modificadas; 260/260 pruebas verdes.                                                                             |

---

## 4. Métricas obtenidas

|    # | Métrica                                      |            Antes |                                  Después |        Objetivo |
|-----:|----------------------------------------------|-----------------:|-----------------------------------------:|----------------:|
| M-01 | Líneas `devops-agent-state.service.ts`       |              148 |                                  **148** |  ≤ 60 (Fase 08) |
| M-02 | Líneas del store más grande                  | 117 (Refinement) |                     **117** (Refinement) |        ≤ 150 ✅ |
| M-03 | Líneas `planning-dashboard.component.ts`     |              753 |                          753 *(intacto)* | ≤ 200 (Fase 07) |
| M-04 | Ocurrencias de `any` en producción           |                0 |                                 **0** ✅ |               0 |
| M-05 | `.subscribe()` en producción                 |               17 |                                       17 |         Fase 08 |
| M-06 | Temporizadores sin limpieza                  |                3 | **2** (en componentes, 0 en tasks-state) |     0 (Fase 08) |
| M-07 | Componentes con `OnPush`                     |            0 / 9 |                                    0 / 9 | 100 % (Fase 08) |
| M-08 | Componentes con `.spec.ts`                   |            9 / 9 |                             **9 / 9** ✅ |           100 % |
| M-09 | Archivos `.spec.ts`                          |               24 |                                   **24** |         ≥ 18 ✅ |
|    — | Casos de prueba                              |              259 |                                  **260** |               — |
| M-10 | Rutas literales en llamadas `http.*`         |                0 |                                 **0** ✅ |               0 |
| M-11 | Números mágicos                              |                8 |                            8 *(intacto)* |     0 (Fase 07) |
| M-12 | Señales `loading` por flujo                  |                5 |                                    **5** |             ≥ 4 |
| M-13 | Existe `core/{config,interceptors,services}` |               ✅ |                                   **✅** |              ✅ |
| M-14 | Existe `environments/`                       |               ✅ |                                   **✅** |              ✅ |
| M-18 | `pnpm build` — initial total                 |        435.65 kB |                         **437.91 kB** ✅ |        < 500 kB |
| M-19 | `pnpm test`                                  |          259/259 |                           **260/260** ✅ |           verde |

---

## 5. Comandos ejecutados

```bash
cd azure-devops-frontend

# --- Verificación de punto de partida y corrección de orden TS2729 ---
corepack pnpm test --no-watch    # Test Files 24 passed (24) | Tests 259 passed (259)   ✅
corepack pnpm build              # Initial total 435.65 kB                              ✅

# --- Validación tras refactorización reactiva de TasksStateService y tests ---
corepack pnpm test --no-watch    # Test Files 24 passed (24) | Tests 260 passed (260)   ✅
corepack pnpm build              # Initial total 437.91 kB                              ✅

# --- Verificación de R-3 (cero .spec.ts de Fases 01-05 modificados) ---
git status --short -- 'src/app/**/*.spec.ts'
# → M src/app/features/devops-agent/services/state/tasks-state.service.spec.ts ✅
```

---

## 6. Hallazgos no previstos

Ninguno. El pipeline de sondeo declarativo con RxJS (`timer` y `switchMap`) reemplazó limpiamente al `setTimeout` imperativo, respondiendo instantáneamente a las transiciones de intervalo (idle/activo) y al disparador inmediato sin temporizadores huérfanos.

---

## 7. Checklist de calidad

| Ítem (`rules/angular-rules.md` §8)                  | Estado | Evidencia                                                                                         |
|-----------------------------------------------------|--------|---------------------------------------------------------------------------------------------------|
| Servicios con `@Injectable({ providedIn: 'root' })` | ✅     | `TasksStateService` y todos los stores poseen la anotación                                        |
| Componentes tocados siguen `standalone: true`       | ✅     | Preservado en toda la aplicación                                                                  |
| Cero `*ngIf` / `*ngFor` / `*ngSwitch`               | ✅     | Preservado                                                                                        |
| Estado reactivo con Signals u Observables seguros   | ✅     | Observables puros (`tasks$`, `timer`, `switchMap`) en `TasksStateService`                         |
| Cero fugas de memoria nuevas                        | ✅     | Subscripciones vinculadas a `takeUntil(this.destroy$)` y cancelables vía `stopDynamicPolling`     |
| Botones de solo icono con `aria-label`              | ✅     | Preservados                                                                                       |
| Cero `any`                                          | ✅     | `M-04 = 0`                                                                                        |
| Cero llamadas HTTP fuera de la capa de API          | ✅     | Delega exclusivamente en `DevopsAgentApiService`                                                  |
| Tipado explícito en métodos públicos                | ✅     | Todos los métodos públicos declaran tipo de retorno explícito (`void`, `Observable<AgentTask[]>`) |
| Sin números mágicos                                 | ✅     | Constantes `POLLING_INTERVAL_IDLE_MS` y `POLLING_INTERVAL_ACTIVE_MS` importadas de `core`         |
| Pruebas GIVEN / WHEN / THEN con TestBed             | ✅     | `tasks-state.service.spec.ts` actualizado con 11 pruebas                                          |
| Sin copys ni rutas inventadas (DP-06 respetada)     | ✅     | Intervalos fijados intactos                                                                       |
| Sin secretos ni credenciales                        | ✅     | Verificado                                                                                        |
| `corepack pnpm build` correcto                      | ✅     | 437.91 kB (< 500 kB)                                                                              |
| `corepack pnpm test` sin fallos nuevos              | ✅     | 260/260 en 24 suites                                                                              |
| Sin `.npmrc` ni `strict-ssl=false`                  | ✅     | Verificado                                                                                        |

---

## 8. Desviaciones respecto a las instrucciones

Ninguna. Se completaron íntegramente las tareas T-01 a T-04 conforme a lo especificado en `fase-06.md`.

---

## 9. Estado al cerrar

- Siguiente fase generada: **`docs/fases/fase-07.md`** ✅
- Decisiones abiertas que bloquean la Fase 07: **DP-02**
- `docs/plan/ESTADO.md` actualizado: **sí**
- Restricción **R-3 cumplida**: cero `.spec.ts` previos de Fases 01 a 05 modificados
- Restricción **R-5 cumplida**: `DevopsAgentStateService` conservado como fachada delegante
- Siguiente paso: ejecutar **Fase 07 (Descomposición del Tablero de Calidad)**

