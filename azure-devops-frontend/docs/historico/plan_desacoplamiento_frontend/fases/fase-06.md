# FASE 06 — Flujo de tareas, sondeo y temporizadores seguros

> **Estado:** ⚪ PENDIENTE · **Depende de:** FASE 05 🟢 · **Riesgo:** Medio
> **Commit al cerrar:** `refactor(devops_agent): desacoplar sondeo de tareas y sanear temporizadores`
> **Siguiente fase:** `fase-07.md`
> **Regla de oro:** las **259 pruebas vigentes deben seguir verdes SIN modificar ningún `.spec.ts` previo.**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 05 cerró el 2026-09-01 (`docs/resultados/RESULTADO-FASE-05.md`) dejando:

- **259 pruebas verdes en 24 archivos** (220 previas + 39 de `services/state/`).
- **Existe `src/app/features/devops-agent/services/state/`** con 5 stores atómicos (`GeneralChatStateService`, `RefinementChatStateService`, `PlanningStateService`, `DashboardStateService`, `TasksStateService`) y su `index.ts`.
- `DevopsAgentStateService` es una fachada delegante pura de 148 líneas (R-5 cumplida).
- `pnpm build`: **435.65 kB** initial total.
- El sondeo de tareas en `TasksStateService` sigue utilizando `setTimeout` recursivo sin limpieza en ciclo de vida (D-15, M-06) y `auditStory` en `RefinementChatStateService` retiene acoplamiento cruzado al forzar el sondeo inmediato de tareas (D-18).

**Entorno de ejecución (obligatorio en esta máquina):**

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
corepack pnpm <comando>
```

### 1.2 Problema que resuelve esta fase

El mecanismo de sondeo de tareas presenta fugas de memoria, temporizadores no controlados y acoplamientos entre flujos:

| Deuda    | Descripción                                                                                                        | Evidencia                                                 |
|----------|--------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| **D-02** | Fuga de memoria real: el servicio `providedIn: 'root'` arranca un timer que no se destruye con el ciclo de vida    | `services/state/tasks-state.service.ts`                   |
| **D-15** | Sondeo recursivo con `setTimeout` imperativo en vez de flujo declarativo RxJS                                      | `services/state/tasks-state.service.ts:51-69`             |
| **D-18** | Efecto cruzado F3 -> F5: auditar una historia reinicia el sondeo global de tareas en lugar de ser un evento tipado | `services/state/refinement-chat-state.service.ts:101-105` |
| **M-06** | 3 temporizadores en producción sin limpieza formal                                                                 | `tasks-state.service.ts`, componentes                     |

### 1.3 Estado esperado al terminar

1. `TasksStateService` gestiona el sondeo de forma declarativa con RxJS (`timer`, `switchMap`, `takeUntilDestroyed` o `DestroyRef`), saneando temporizadores huérfanos.
2. Se desacopla el efecto colateral de `auditStory` hacia el sondeo de tareas.
3. Se conservan los contratos públicos y retrocompatibilidad de `TasksStateService` y `DevopsAgentStateService`.
4. Las 259 pruebas siguen pasando al 100% sin modificar ningún `.spec.ts` previo (R-3).

### 1.4 Archivos involucrados

| Ruta                                                                            | Acción                                   |
|---------------------------------------------------------------------------------|------------------------------------------|
| `src/app/features/devops-agent/services/state/tasks-state.service.ts`           | MODIFICAR                                |
| `src/app/features/devops-agent/services/state/tasks-state.service.spec.ts`      | MODIFICAR / EXPANDIR                     |
| `src/app/features/devops-agent/services/state/refinement-chat-state.service.ts` | MODIFICAR                                |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`          | MODIFICAR (ajustar delegación si aplica) |
| **Cualquier `.spec.ts` existente de Fases 01 a 05**                             | **NO TOCAR**                             |

### 1.5 Reglas aplicables

- **§3** — Manejo de Estado Reactivo (Signals & RxJS, prevención de memory leaks con `takeUntilDestroyed` / `DestroyRef`).
- **§5** — TypeScript estricto, cero `any`, tipos de retorno explícitos.
- **§6** — Pruebas Vitest en formato GIVEN / WHEN / THEN.
- **§7** — Política de No-Asunción.
- **R-3** — Prohibido modificar `.spec.ts` existentes.

### 1.6 Decisiones pendientes que bloquean

| ID        | Estado     | Acción si sigue ABIERTA                                                                                                                                |
|-----------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-06** | 🟡 PARCIAL | Conservar valores de `core/config/app-tuning.ts` (`POLLING_INTERVAL_IDLE_MS = 30000`, `POLLING_INTERVAL_ACTIVE_MS = 5000`) sin alterar los intervalos. |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar punto de partida

```bash
corepack pnpm test --no-watch    # 259 passed (24 archivos)
corepack pnpm build              # 435.65 kB
```

#### T-02 — Refactorizar `TasksStateService` con RxJS declarativo

Reemplazar `setTimeout` imperativo por un pipeline reactivo que utilice `timer`, `switchMap` y `takeUntilDestroyed(this.destroyRef)` o cancelación explícita, ajustando dinámicamente el intervalo entre activo e inactivo.

#### T-03 — Desacoplar efecto cruzado de `auditStory`

Revisar la invocación de `triggerImmediatePoll()` en `auditStory` para aislar el flujo de refinamiento respecto al ciclo global de tareas.

#### T-04 — Validaciones de cierre

```bash
corepack pnpm test --no-watch
corepack pnpm build
git diff --stat -- 'src/app/**/*.spec.ts'  # Solo specs de la fase
```

### 2.2 Qué NO hacer

1. **No romper la API pública de `TasksStateService` ni `DevopsAgentStateService`**.
2. **No alterar los intervalos de sondeo** fijados en `app-tuning.ts`.
3. **No modificar ningún `.spec.ts` previo** (R-3).

### 2.3 Criterios de aceptación

- [ ] `TasksStateService` maneja el sondeo reactivamente sin `setTimeout` huérfanos.
- [ ] M-06 reducida (cero temporizadores sin limpieza en `tasks-state`).
- [ ] Las 259 pruebas vigentes siguen verdes.
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

| Paso | Acción                                                                                | Resultado esperado      |
|-----:|---------------------------------------------------------------------------------------|-------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                     | Contexto confirmado     |
| P-02 | `corepack pnpm test --no-watch`                                                       | 259 passed              |
| P-03 | Migrar `TasksStateService` a sondeo declarativo con gestión de ciclo de vida          | Temporizadores saneados |
| P-04 | Validar y expandir tests en `tasks-state.service.spec.ts`                             | Pruebas verdes          |
| P-05 | `corepack pnpm test --no-watch`                                                       | 259+ passed             |
| P-06 | `corepack pnpm build`                                                                 | Correcto y < 500 kB     |
| P-07 | Escribir `docs/resultados/RESULTADO-FASE-06.md`                                       | Trazabilidad            |
| P-08 | Actualizar `docs/plan/ESTADO.md`                                                      | Tablero al día          |
| P-09 | Generar `docs/fases/fase-07.md`                                                       | Checkpoint creado       |
| P-10 | Commit: `refactor(devops_agent): desacoplar sondeo de tareas y sanear temporizadores` | Versionado              |

### 3.1 Contenido mínimo del MD de la Fase 07

**Nombre.** FASE 07 — Descomposición del Tablero de Calidad. **Riesgo.** Alto. **Bloqueada por:** DP-02. **Deudas.** D-09, D-14, D-20, M-03. **Objetivo.** Descomponer el componente gigante `PlanningDashboardComponent` (753 líneas) en subcomponentes de presentación especializados bajo `components/planning-dashboard/` y centralizar los cálculos y umbrales de calidad en `domain/quality-score.ts`.

