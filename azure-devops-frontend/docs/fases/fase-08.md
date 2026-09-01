# FASE 08 — Endurecimiento, erradicación de `console.error` y cierre

> **Estado:** ⚪ PENDIENTE · **Depende de:** FASE 07 🟢 · **Riesgo:** Medio
> **Commit al cerrar:** `refactor(frontend): endurecimiento global, erradicacion de console.error y migracion OnPush`
> **Siguiente fase:** Ninguna (cierre del plan maestro)
> **Regla de oro:** las **291 pruebas vigentes deben seguir verdes SIN modificar contratos de API existentes (R-5) ni la URL base (R-6).**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 07 cerró el 2026-09-01 (`docs/resultados/RESULTADO-FASE-07.md`) dejando:

- **291 pruebas verdes en 29 archivos**.
- `PlanningDashboardComponent` desacoplado y reducido de 753 líneas a **200 líneas** (M-03).
- Cálculos, semáforos y filtros de calidad aislados en `domain/quality-score.ts` respetando **DP-02** y **R-2**.
- Subcomponentes modulares de presentación creados bajo `components/planning-dashboard/`.
- `pnpm build`: **444.56 kB** initial total (< 500 kB).

**Entorno de ejecución (obligatorio en esta máquina):**

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
corepack pnpm <comando>
```

### 1.2 Problema que resuelve esta fase

Esta fase final aborda el endurecimiento transversal de la arquitectura, la erradicación de prácticas no recomendadas y el cierre del plan:

| Deuda           | Descripción                                                                                              | Evidencia / Métricas                                                 |
|-----------------|----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| **D-17 / M-21** | Uso de `console.error` en producción (16 ocurrencias en servicios y componentes)                         | `services/api/*.ts`, `services/state/*.ts`, `components/*.ts`        |
| **D-22 / M-07** | Ausencia de `ChangeDetectionStrategy.OnPush` en componentes                                              | 0 / 13 componentes con OnPush                                        |
| **D-28**        | Respuestas de error que pueden mostrar `[object Object]` al usuario en lugar de mensaje formateado       | `core/interceptors/http-error.interceptor.ts`, `services/state/*.ts` |
| **D-23 / M-20** | Auditoría y completitud de descriptores accesibles `aria-label` en controles interactivos                | Componentes interactivos                                             |
| **M-01**        | `devops-agent-state.service.ts` mide 148 líneas (objetivo de cierre: fachada pura y concisa ≤ 60 líneas) | `services/devops-agent-state.service.ts`                             |
| **M-05 / M-06** | Suscripciones manuales y temporizadores residuales en componentes                                        | `components/*.ts`                                                    |

### 1.3 Estado esperado al terminar

1. Cero `console.error` en código productivo: errores canalizados a través de `NotificationService` del `core/`.
2. Todos los componentes adoptan `ChangeDetectionStrategy.OnPush`.
3. Errores formateados de manera amigable para el usuario evitando `[object Object]`.
4. Accesibilidad auditada con `aria-label` en botones interactivos y de solo icono.
5. `devops-agent-state.service.ts` simplificado a fachada pura mínima (≤ 60 líneas).
6. Todas las pruebas (≥ 291) verdes y build dentro de presupuesto (< 500 kB).
7. Plan maestro concluido con informe final de resultados.

### 1.4 Archivos involucrados

| Ruta                                                                   | Acción                                                        |
|------------------------------------------------------------------------|---------------------------------------------------------------|
| `src/app/core/interceptors/http-error.interceptor.ts`                  | MODIFICAR (manejo seguro de errores)                          |
| `src/app/features/devops-agent/services/api/*.ts`                      | MODIFICAR (sustituir console.error por NotificationService)   |
| `src/app/features/devops-agent/services/state/*.ts`                    | MODIFICAR (sustituir console.error por NotificationService)   |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts` | MODIFICAR (reducir a ≤ 60 líneas)                             |
| `src/app/features/devops-agent/components/**/*.ts`                     | MODIFICAR (añadir OnPush, aria-label, saneamiento)            |
| `src/app/features/devops-agent/pages/**/*.ts`                          | MODIFICAR (añadir OnPush)                                     |
| `src/app/**/*.spec.ts`                                                 | MODIFICAR / MANTENER (adaptar a OnPush y NotificationService) |

### 1.5 Reglas aplicables

- **§1 & §2** — Standalone Components, Control Flow (`@if`, `@for`), Signals y OnPush.
- **§3** — Suscripciones seguras con `takeUntilDestroyed()`, `AsyncPipe`, cero temporizadores huérfanos.
- **§4** — A11Y: `aria-label` en botones interactivos.
- **§5** — TypeScript estricto, cero `any`, tipos de retorno explícitos.
- **§6** — Pruebas unitarias Vitest en formato GIVEN / WHEN / THEN.

### 1.6 Decisiones pendientes que bloquean

*Ninguna. Todas las decisiones del plan maestro están resueltas.*

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar punto de partida

```bash
corepack pnpm test --no-watch    # 291 passed (29 archivos)
corepack pnpm build              # 444.56 kB
```

#### T-02 — Erradicar `console.error` (D-17, M-21) y sanear mensajes de error (D-28)

- Reemplazar llamadas a `console.error` en servicios y componentes por inyecciones de `NotificationService` del `core/`.
- Asegurar que los mensajes de error extraigan cadenas legibles (`err.message` o `err.error?.message`) impidiendo `[object Object]`.

#### T-03 — Migrar componentes a `ChangeDetectionStrategy.OnPush` (D-22, M-07)

- Agregar `changeDetection: ChangeDetectionStrategy.OnPush` en todos los componentes de la aplicación.
- Validar que las vistas que consumen Signals u Observables con `AsyncPipe` se actualicen adecuadamente.

#### T-04 — Simplificar fachada `devops-agent-state.service.ts` (M-01)

- Reducir el tamaño de `DevopsAgentStateService` a ≤ 60 líneas conservando delegaciones contractuales (R-5).

#### T-05 — Completar auditoría de accesibilidad (D-23, M-20)

- Incorporar descriptores `aria-label` en todos los botones de sólo icono o controles interactivos restantes.

#### T-06 — Validaciones finales y cierre del plan

```bash
corepack pnpm test --no-watch
corepack pnpm build
```

### 2.2 Qué NO hacer

1. **No eliminar `DevopsAgentApiService` ni `DevopsAgentStateService`** (R-5).
2. **No modificar `environment.apiBaseUrl`** dejándolo vacío (R-6).
3. **No reintroducir `any` ni directivas deprecadas**.

### 2.3 Criterios de aceptación

- [ ] Cero `console.error` en código de producción (`M-21 = 0`).
- [ ] 100% de los componentes con `ChangeDetectionStrategy.OnPush` (`M-07 = 100%`).
- [ ] `DevopsAgentStateService` mide ≤ 60 líneas (`M-01`).
- [ ] Cero mensajes `[object Object]` en captura de errores (`D-28`).
- [ ] 100% de pruebas verdes (≥ 291 pruebas).
- [ ] `corepack pnpm build` dentro de presupuesto (< 500 kB).

### 2.4 Checklist de calidad (de `rules/angular-rules.md` §8)

- [ ] Todos los componentes tienen `ChangeDetectionStrategy.OnPush`
- [ ] Cero `*ngIf` / `*ngFor` / `*ngSwitch`
- [ ] Suscripciones cerradas con `takeUntilDestroyed()` o `AsyncPipe`
- [ ] Botones de solo icono con `aria-label`
- [ ] Cero `any` en variables, parámetros o retornos
- [ ] Tipado explícito en todos los métodos públicos
- [ ] Pruebas Vitest en formato GIVEN / WHEN / THEN
- [ ] `pnpm build` y `pnpm test` correctos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                                | Resultado esperado     |
|-----:|-------------------------------------------------------------------------------------------------------|------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y verificar baseline                                                       | Contexto confirmado    |
| P-02 | `corepack pnpm test --no-watch`                                                                       | 291 passed             |
| P-03 | Sustituir `console.error` por `NotificationService` y corregir formato de errores                     | M-21 = 0               |
| P-04 | Aplicar `ChangeDetectionStrategy.OnPush` en todos los componentes                                     | M-07 = 100%            |
| P-05 | Simplificar fachada `devops-agent-state.service.ts`                                                   | ≤ 60 líneas            |
| P-06 | Auditar `aria-label` en controles interactivos                                                        | A11Y completada        |
| P-07 | `corepack pnpm test --no-watch`                                                                       | Verde                  |
| P-08 | `corepack pnpm build`                                                                                 | Correcto y < 500 kB    |
| P-09 | Escribir `docs/resultados/RESULTADO-FASE-08.md`                                                       | Trazabilidad de cierre |
| P-10 | Actualizar `docs/plan/ESTADO.md` (Plan Completado)                                                    | Tablero al 100%        |
| P-11 | Commit: `refactor(frontend): endurecimiento global, erradicacion de console.error y migracion OnPush` | Plan finalizado        |

