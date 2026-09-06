# FASE 07 — Descomposición del Tablero de Calidad

> **Estado:** ⚪ PENDIENTE · **Depende de:** FASE 06 🟢 · **Riesgo:** Alto
> **Commit al cerrar:** `refactor(devops_agent): descomponer tablero de calidad y modularizar subcomponentes`
> **Siguiente fase:** `fase-08.md`
> **Regla de oro:** las **260 pruebas vigentes deben seguir verdes SIN modificar ningún `.spec.ts` previo de Fases 01 a 05 (R-3) ni unificar umbrales sin resolver DP-02 (R-2).**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 06 cerró el 2026-09-01 (`docs/resultados/RESULTADO-FASE-06.md`) dejando:

- **260 pruebas verdes en 24 archivos**.
- `TasksStateService` migrado a un pipeline declarativo RxJS con intervalos dinámicos (D-02, D-15, M-06 saneadas en tasks-state).
- `DevopsAgentStateService` conservado como fachada delegante pura (R-5).
- `pnpm build`: **437.91 kB** initial total (< 500 kB).
- El componente `PlanningDashboardComponent` sigue siendo un archivo monolítico de **753 líneas** con lógica de cálculo, filtrado, colores, gráficos y tablas mezcladas en la presentación (**M-03**, **D-09**, **D-14**, **D-20**, **D-23**).

**Entorno de ejecución (obligatorio en esta máquina):**

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
corepack pnpm <comando>
```

### 1.2 Problema que resuelve esta fase

El componente de Tablero de Calidad acumula alta complejidad y mezcla responsabilidades:

| Deuda    | Descripción                                                                                         | Evidencia                                                       |
|----------|-----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| **M-03** | `PlanningDashboardComponent` contiene 753 líneas en un único archivo (objetivo: ≤ 200)              | `components/planning-dashboard/planning-dashboard.component.ts` |
| **D-09** | Cálculos de calidad y categorización incrustados en métodos privados del componente                 | `planning-dashboard.component.ts:620-730`                       |
| **D-14** | Números mágicos de umbrales de calidad hardcodeados en el componente (`90`, `70`, `50`, `80`, `13`) | `planning-dashboard.component.ts:702-718`                       |
| **D-20** | Componente monolítico sin subcomponentes de presentación                                            | `planning-dashboard.component.ts`                               |
| **D-23** | Ausencia de atributos `aria-label` en controles interactivos y botones de solo icono                | `planning-dashboard.component.ts`                               |

### 1.3 Estado esperado al terminar

1. Existe `src/app/features/devops-agent/domain/quality-score.ts` con funciones puras y tipadas para calcular métricas de calidad, etiquetas de salud y colores de semáforo respetando **R-2** y **DP-02**.
2. `PlanningDashboardComponent` queda reducido a un smart component coordinador (≤ 200 líneas) delegando la UI en dumb components bajo `src/app/features/devops-agent/components/planning-dashboard/`:

- `quality-summary-cards.component.ts` (Tarjetas resumen de métricas)
- `quality-stories-table.component.ts` (Tabla de historias con filtros y estado)
- `quality-chart.component.ts` (Gráfico de distribución)
- `story-audit-modal.component.ts` (Modal de detalle y auditoría)

3. Accesibilidad mejorada con `aria-label` en botones interactivos.
4. Las 260 pruebas siguen pasando al 100% (R-3) y se crean suites unitarias para el dominio y nuevos subcomponentes.

### 1.4 Archivos involucrados

| Ruta                                                                                             | Acción                             |
|--------------------------------------------------------------------------------------------------|------------------------------------|
| `src/app/features/devops-agent/domain/quality-score.ts`                                          | CREAR                              |
| `src/app/features/devops-agent/domain/quality-score.spec.ts`                                     | CREAR                              |
| `src/app/features/devops-agent/domain/index.ts`                                                  | MODIFICAR (exportar quality-score) |
| `src/app/features/devops-agent/components/planning-dashboard/planning-dashboard.component.ts`    | MODIFICAR (reducir a ≤ 200 líneas) |
| `src/app/features/devops-agent/components/planning-dashboard/quality-summary-cards.component.ts` | CREAR                              |
| `src/app/features/devops-agent/components/planning-dashboard/quality-stories-table.component.ts` | CREAR                              |
| `src/app/features/devops-agent/components/planning-dashboard/quality-chart.component.ts`         | CREAR                              |
| `src/app/features/devops-agent/components/planning-dashboard/story-audit-modal.component.ts`     | CREAR                              |
| `src/app/features/devops-agent/components/planning-dashboard/index.ts`                           | CREAR / MODIFICAR                  |
| `src/app/features/devops-agent/components/planning-dashboard/*.spec.ts`                          | CREAR                              |
| **Cualquier `.spec.ts` existente de Fases 01 a 05**                                              | **NO TOCAR**                       |

### 1.5 Reglas aplicables

- **§1 & §2** — Componentes Standalone (`standalone: true`), Control Flow (`@if`, `@for`), inputs/outputs tipados.
- **§4** — Accesibilidad (A11Y): `aria-label` obligatorio en botones de solo icono.
- **§5** — TypeScript estricto, cero `any`, tipos de retorno explícitos.
- **§6** — Pruebas Vitest en formato GIVEN / WHEN / THEN.
- **R-2** — Preservar literalmente la discrepancia de umbrales `90/70/50` vs `80/50` según DP-02.
- **R-3** — Prohibido modificar `.spec.ts` previos de Fases 01 a 05.

### 1.6 Decisiones pendientes que bloquean

| ID        | Estado     | Acción si sigue ABIERTA                                                                                                                                                                                            |
|-----------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-02** | 🔴 ABIERTA | Aplicar acción por defecto: trasladar los umbrales **literalmente tal como están** a `domain/quality-score.ts`, incluida la discrepancia `90/70/50` (etiqueta) frente a `80/50` (color y filtro), sin unificarlos. |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar punto de partida

```bash
corepack pnpm test --no-watch    # 260 passed (24 archivos)
corepack pnpm build              # 437.91 kB
```

#### T-02 — Crear `domain/quality-score.ts`

Extraer funciones puras para clasificar scores, obtener colores, calcular promedios y totalizadores, trasladando los valores exactos (R-2).

#### T-03 — Crear subcomponentes dumb de presentación

- `QualitySummaryCardsComponent`: renderiza las métricas consolidadas.
- `QualityStoriesTableComponent`: tabla de historias con selector de filtro y badges.
- `QualityChartComponent`: visualización gráfica de distribución.
- `StoryAuditModalComponent`: modal para visualizar y disparar auditoría de historias.

#### T-04 — Refactorizar `PlanningDashboardComponent`

Convertir `PlanningDashboardComponent` en coordinador limpio (M-03 ≤ 200 líneas) integrando los subcomponentes con `@if` y `@for`.

#### T-05 — Validaciones de cierre

```bash
corepack pnpm test --no-watch
corepack pnpm build
git diff --stat -- 'src/app/**/*.spec.ts'
```

### 2.2 Qué NO hacer

1. **No unificar los umbrales de calidad** mientras DP-02 siga abierta (R-2).
2. **No modificar ningún `.spec.ts` previo** (R-3).
3. **No introducir `any` ni directivas deprecadas (`*ngIf`, `*ngFor`)**.

### 2.3 Criterios de aceptación

- [ ] `PlanningDashboardComponent` mide ≤ 200 líneas (M-03).
- [ ] Cálculos de calidad desacoplados en `domain/quality-score.ts`.
- [ ] Subcomponentes modulares en `components/planning-dashboard/`.
- [ ] Accesibilidad mejorada con `aria-label` descriptivos.
- [ ] Las 260 pruebas vigentes siguen verdes más las nuevas suites.
- [ ] `corepack pnpm build` dentro de presupuesto (< 500 kB).

### 2.4 Checklist de calidad (de `rules/angular-rules.md` §8)

- [ ] Todos los componentes creados son `standalone: true`
- [ ] Cero `*ngIf` / `*ngFor` / `*ngSwitch`: solo `@if` / `@for` / `@switch`
- [ ] Estado reactivo con Signals u Observables seguros
- [ ] Botones de solo icono con `aria-label`
- [ ] Cero `any` en variables, parámetros o retornos
- [ ] Tipado explícito en todos los métodos públicos
- [ ] Pruebas Vitest en formato GIVEN / WHEN / THEN
- [ ] `pnpm build` y `pnpm test` correctos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                        | Resultado esperado     |
|-----:|-----------------------------------------------------------------------------------------------|------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                             | Contexto confirmado    |
| P-02 | `corepack pnpm test --no-watch`                                                               | 260 passed             |
| P-03 | Crear `domain/quality-score.ts` y su `.spec.ts`                                               | Cálculos aislados      |
| P-04 | Crear subcomponentes de presentación en `components/planning-dashboard/`                      | Subcomponentes creados |
| P-05 | Refactorizar `PlanningDashboardComponent` reduciendo su tamaño                                | ≤ 200 líneas           |
| P-06 | `corepack pnpm test --no-watch`                                                               | Verde                  |
| P-07 | `corepack pnpm build`                                                                         | Correcto y < 500 kB    |
| P-08 | Escribir `docs/resultados/RESULTADO-FASE-07.md`                                               | Trazabilidad           |
| P-09 | Actualizar `docs/plan/ESTADO.md`                                                              | Tablero al día         |
| P-10 | Generar `docs/fases/fase-08.md`                                                               | Checkpoint creado      |
| P-11 | Commit: `refactor(devops_agent): descomponer tablero de calidad y modularizar subcomponentes` | Versionado             |

### 3.1 Contenido mínimo del MD de la Fase 08

**Nombre.** FASE 08 — Endurecimiento, erradicación de `console.error` y cierre. **Riesgo.** Medio. **Deudas.** D-17, D-22, D-28, M-05, M-06, M-07, M-21. **Objetivo.** Erradicar `console.error` en producción reemplazándolos por `NotificationService` del `core/`, migrar componentes a `ChangeDetectionStrategy.OnPush`, sanear suscripciones residuales con `takeUntilDestroyed` y auditar accesibilidad completa.

