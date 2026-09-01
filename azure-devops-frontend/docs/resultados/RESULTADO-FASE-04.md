# RESULTADO — FASE 04: Segregación de la capa de API

> **Ejecutada:** 2026-09-01 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(devops_agent): segregar clientes de api por dominio y crear fachada delegante`
> **Instrucciones:** `docs/fases/fase-04.md`
> **Código productivo modificado:** 2 archivos existentes modificados · 5 archivos de servicios nuevos en `services/api/` · 4 archivos de test nuevos · **cero `.spec.ts` de las Fases 01, 02 y 03 modificados**

---

## 1. Objetivo de la fase

Segregar el cliente de API monolítico (`devops-agent-api.service.ts`) en 4 clientes atómicos especializados por dominio bajo `services/api/` (**D-11**, **D-08**, **D-26**): `AgentChatApiService`, `PlanningApiService`, `DashboardApiService` y `TasksApiService`. Convertir `DevopsAgentApiService` en una fachada delegante pura para preservar el 100% de la compatibilidad contractual con los consumidores y tests existentes (**R-5**), manteniendo la ruta `POST '/'` con JSON-RPC para la cancelación de tareas (**DP-03** 🟢) y sin tocar ningún `.spec.ts` previo (**R-3**).

---

## 2. Qué se construyó

| Artefacto                                                                   | Acción         | Detalle                                                                                                                                          |
|-----------------------------------------------------------------------------|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `src/app/features/devops-agent/services/api/agent-chat-api.service.ts`      | **CREADO**     | Cliente atómico de Chat/AgentCard (22 líneas, `getAgentCard`, `sendMessage`)                                                                     |
| `src/app/features/devops-agent/services/api/planning-api.service.ts`        | **CREADO**     | Cliente atómico de Planeación (40 líneas, `uploadPlanning`, `getInitiatives`, `deleteInitiative`, `updateInitiativeCell`, `getInitiativeChunks`) |
| `src/app/features/devops-agent/services/api/dashboard-api.service.ts`       | **CREADO**     | Cliente atómico de Dashboard y SSE (52 líneas, `getDashboardData`, `getDashboardDataStream`)                                                     |
| `src/app/features/devops-agent/services/api/tasks-api.service.ts`           | **CREADO**     | Cliente atómico de Tareas (29 líneas, `getTasks`, `cancelTask` vía `POST '/'`)                                                                   |
| `src/app/features/devops-agent/services/api/index.ts`                       | **CREADO**     | Barrel export de los 4 clientes atómicos                                                                                                         |
| `src/app/features/devops-agent/services/api/agent-chat-api.service.spec.ts` | **CREADO**     | Suite de pruebas unitarias (2 pruebas GIVEN / WHEN / THEN)                                                                                       |
| `src/app/features/devops-agent/services/api/planning-api.service.spec.ts`   | **CREADO**     | Suite de pruebas unitarias (5 pruebas GIVEN / WHEN / THEN)                                                                                       |
| `src/app/features/devops-agent/services/api/dashboard-api.service.spec.ts`  | **CREADO**     | Suite de pruebas unitarias (9 pruebas GIVEN / WHEN / THEN, con stub de EventSource para D-40)                                                    |
| `src/app/features/devops-agent/services/api/tasks-api.service.spec.ts`      | **CREADO**     | Suite de pruebas unitarias (2 pruebas GIVEN / WHEN / THEN)                                                                                       |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`        | **MODIFICADO** | Fachada delegante pura (64 líneas, delega en los 4 servicios atómicos)                                                                           |
| `src/app/features/devops-agent/services/index.ts`                           | **MODIFICADO** | Barrel export actualizado con `export * from './api'`                                                                                            |
| **Cualquier `.spec.ts` previo (Fases 01, 02 y 03)**                         | **NO TOCADO**  | `git diff --stat -- 'src/app/**/*.spec.ts'` → **vacío** ✅ (R-3)                                                                                 |

### 2.1 Pruebas nuevas o reescritas

| Archivo                                                             |    Casos antes |           Casos después |
|---------------------------------------------------------------------|---------------:|------------------------:|
| `features/devops-agent/services/api/agent-chat-api.service.spec.ts` | 0 (no existía) |                   **2** |
| `features/devops-agent/services/api/planning-api.service.spec.ts`   | 0 (no existía) |                   **5** |
| `features/devops-agent/services/api/dashboard-api.service.spec.ts`  | 0 (no existía) |                   **9** |
| `features/devops-agent/services/api/tasks-api.service.spec.ts`      | 0 (no existía) |                   **2** |
| *Todas las suites previas (15 archivos)*                            |            202 | **202 — sin modificar** |
| **TOTAL**                                                           |        **202** |                 **220** |

---

## 3. Decisiones aplicadas

| ID        | Resolución                                                                                                  | Efecto en esta fase                                                                                                                          |
|-----------|-------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-03** | 🟢 **RESUELTA** (2026-08-31): Se conserva `POST '/'` con envoltorio JSON-RPC para la cancelación de tareas. | `TasksApiService.cancelTask` invoca `API_ENDPOINTS.TASKS_CANCEL_RPC` (`'/'`) enviando el payload JSON-RPC 2.0 estándar.                      |
| **R-5**   | Restricción de diseño: Conservar `DevopsAgentApiService` como fachada delegante.                            | Los 18 tests de caracterización de `devops-agent-api.service.spec.ts` y las pruebas de `planning-management` continúan pasando íntegramente. |
| **R-6**   | Restricción de entorno: `environment.apiBaseUrl` permanece vacío (`''`).                                    | Garantiza resolución relativa homogénea en pruebas y en producción sin alterar URLs.                                                         |

---

## 4. Métricas obtenidas

|    # | Métrica                                      |     Antes |                Después |          Objetivo |
|-----:|----------------------------------------------|----------:|-----------------------:|------------------:|
| M-01 | Líneas `devops-agent-state.service.ts`       |       370 |        370 *(intacto)* | ≤ 60 (Fase 05/06) |
| M-02 | Líneas del store más grande                  |       370 |        370 *(intacto)* |   ≤ 150 (Fase 05) |
| M-03 | Líneas `planning-dashboard.component.ts`     |       753 |        753 *(intacto)* |   ≤ 200 (Fase 07) |
| M-04 | Ocurrencias de `any` en producción           |         0 |               **0** ✅ |                 0 |
| M-05 | `.subscribe()` en producción                 |        17 |         17 *(intacto)* |           Fase 08 |
| M-06 | Temporizadores sin limpieza                  |         3 |          3 *(intacto)* |        Fase 06/08 |
| M-07 | Componentes con `OnPush`                     |     0 / 9 |                  0 / 9 |   100 % (Fase 08) |
| M-08 | Componentes con `.spec.ts`                   |     9 / 9 |           **9 / 9** ✅ |             100 % |
| M-09 | Archivos `.spec.ts`                          |        15 |                 **19** |           ≥ 18 ✅ |
|    — | Casos de prueba                              |       202 |                **220** |                 — |
| M-10 | Rutas literales en llamadas `http.*`         |         0 |               **0** ✅ |                 0 |
| M-11 | Números mágicos                              |         8 |          8 *(intacto)* |       0 (Fase 07) |
| M-13 | Existe `core/{config,interceptors,services}` |        ✅ |                 **✅** |                ✅ |
| M-14 | Existe `environments/`                       |        ✅ |                 **✅** |                ✅ |
| M-18 | `pnpm build` — initial total                 | 435.65 kB |       **435.65 kB** ✅ |          < 500 kB |
| M-19 | `pnpm test`                                  |   202/202 |         **220/220** ✅ |             verde |
|    — | Líneas de servicios en `services/api/`       |         — | **≤ 52 líneas c/u** ✅ |       ≤ 60 líneas |

---

## 5. Comandos ejecutados

```bash
cd azure-devops-frontend

# --- Verificación de punto de partida ---
corepack pnpm test --no-watch    # Test Files 15 passed (15) | Tests 202 passed (202)   ✅
corepack pnpm build              # Initial total 435.65 kB                              ✅

# --- Validación tras creación de clientes y fachada ---
corepack pnpm test --no-watch    # Test Files 19 passed (19) | Tests 220 passed (220)   ✅
corepack pnpm build              # Initial total 435.65 kB                              ✅

# --- Verificación de R-3 (cero .spec.ts previos modificados) ---
git diff --stat -- 'src/app/**/*.spec.ts'
# → (vacío) ✅
```

---

## 6. Hallazgos no previstos

Ninguno. La segregación se ejecutó sin fricción, las dependencias delegaron de manera transparente y las suites de caracterización previas mantuvieron 100% de efectividad.

---

## 7. Checklist de calidad

| Ítem (`rules/angular-rules.md` §8)                           | Estado | Evidencia                                                                          |
|--------------------------------------------------------------|--------|------------------------------------------------------------------------------------|
| Servicios con `@Injectable({ providedIn: 'root' })`          | ✅     | Los 4 servicios atómicos y la fachada tienen `@Injectable({ providedIn: 'root' })` |
| Componentes tocados siguen `standalone: true`                | ✅     | No se modificaron componentes de UI                                                |
| Cero `*ngIf` / `*ngFor` / `*ngSwitch`                        | ✅     | Preservado                                                                         |
| Estado reactivo con Signals u Observables seguros            | ✅     | Observables puros retornados por los servicios de API                              |
| Cero fugas de memoria nuevas                                 | ✅     | Sin suscripciones manuales abiertas                                                |
| Botones de solo icono con `aria-label`                       | ✅     | Preservados                                                                        |
| Cero `any`                                                   | ✅     | `M-04 = 0`                                                                         |
| Cero llamadas HTTP fuera de la capa de API                   | ✅     | Todas las llamadas centralizadas en `services/api/`                                |
| Tipado explícito en métodos públicos                         | ✅     | Todos los métodos en clientes y fachada tienen retornos explícitos                 |
| Sin números mágicos                                          | ✅     | Constantes y endpoints tipados                                                     |
| Pruebas GIVEN / WHEN / THEN con `provideHttpClientTesting()` | ✅     | 4 nuevas suites en `services/api/*.spec.ts`                                        |
| Sin copys ni rutas inventadas (DP-03 respetada)              | ✅     | Uso estricto de `API_ENDPOINTS` y helpers de ruta                                  |
| Sin secretos ni credenciales                                 | ✅     | Verificado                                                                         |
| `corepack pnpm build` correcto                               | ✅     | 435.65 kB (< 500 kB)                                                               |
| `corepack pnpm test` sin fallos nuevos                       | ✅     | 220/220 en 19 suites                                                               |
| Sin `.npmrc` ni `strict-ssl=false`                           | ✅     | Verificado                                                                         |

---

## 8. Desviaciones respecto a las instrucciones

Ninguna. Se completaron íntegramente las tareas T-01 a T-09 conforme a lo especificado en `fase-04.md`.

---

## 9. Estado al cerrar

- Siguiente fase generada: **`docs/fases/fase-05.md`** ✅
- Decisiones abiertas que bloquean la Fase 05: **DP-01**, **DP-05**, **DP-08**, **DP-10** (aplicar restricción crítica **R-1**)
- `docs/plan/ESTADO.md` actualizado: **sí**
- Restricción **R-3 cumplida**: cero `.spec.ts` previos modificados
- Restricción **R-5 cumplida**: `DevopsAgentApiService` conservado como fachada delegante
- Siguiente paso: resolver decisiones bloqueantes y ejecutar **Fase 05 (Separación de stores por flujo y migración reactiva)**

