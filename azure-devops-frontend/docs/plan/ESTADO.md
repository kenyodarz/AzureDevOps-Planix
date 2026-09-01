# ESTADO — Plan de desacoplamiento del frontend

> **Punto de entrada obligatorio.** Quien retome el trabajo sin memoria previa debe leer
> **este archivo primero**, después `docs/plan/DECISIONES_PENDIENTES.md` y por último el MD de la
> fase activa en `docs/fases/`.
>
> **Actualizado:** 2026-09-01 (cierre de la Fase 07)

---

## 0. Entorno de ejecución (obligatorio en esta máquina)

El registro npm está interceptado por TLS corporativo (`SELF_SIGNED_CERT_IN_CHAIN`). Antes de cualquier comando:

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
corepack pnpm <comando>      # pnpm no está en el PATH; se invoca vía corepack
```

Si `~/.ca/corp-ca.pem` no existe:

```bash
mkdir -p ~/.ca
security find-certificate -a -p /System/Library/Keychains/SystemRootCertificates.keychain  > ~/.ca/corp-ca.pem
security find-certificate -a -p /Library/Keychains/System.keychain                        >> ~/.ca/corp-ca.pem
```

> **Prohibido** añadir `.npmrc` al repositorio o desactivar `strict-ssl`.

---

## 1. Tablero de fases

| Fase                                       | Archivo                 | Estado            | Métricas de cierre                                                                                                          | Fecha      |
|--------------------------------------------|-------------------------|-------------------|-----------------------------------------------------------------------------------------------------------------------------|------------|
| 01 — Baseline y red de seguridad           | `docs/fases/fase-01.md` | 🟢 **COMPLETADA** | 188 pruebas (era 29) · 12 archivos (era 4) · 9/9 componentes cubiertos · build restaurado                                   | 2026-08-31 |
| 02 — Núcleo transversal (`core/`)          | `docs/fases/fase-02.md` | 🟢 **COMPLETADA** | 200 pruebas (era 188) · `any` 9 → **0** · rutas literales 10 → **0** · `core/` y `environments/` creados · 435.66 kB        | 2026-08-31 |
| 03 — Externalización de copys y prompts    | `docs/fases/fase-03.md` | 🟢 **COMPLETADA** | 202 pruebas (era 200) · 15 archivos · D-06, D-19, D-21 saldadas · `domain/` creado · state 370 líneas · 435.65 kB           | 2026-09-01 |
| 04 — Segregación de la capa de API         | `docs/fases/fase-04.md` | 🟢 **COMPLETADA** | 220 pruebas (era 202) · 19 archivos (era 15) · D-11, D-08, D-26 saldadas · 4 clientes atómicos + fachada · 435.65 kB        | 2026-09-01 |
| 05 — Separación de stores por flujo        | `docs/fases/fase-05.md` | 🟢 **COMPLETADA** | 259 pruebas (era 220) · 24 archivos (era 19) · D-01, D-03, D-04, D-12, D-16, D-17 saldadas · 5 stores + fachada · 435.65 kB | 2026-09-01 |
| 06 — Flujo de tareas y polling             | `docs/fases/fase-06.md` | 🟢 **COMPLETADA** | 260 pruebas (era 259) · 24 archivos · D-02, D-15, M-06 saneadas en tasks-state · pipeline RxJS declarativo · 437.91 kB      | 2026-09-01 |
| 07 — Descomposición del Tablero de Calidad | `docs/fases/fase-07.md` | 🟢 **COMPLETADA** | 291 pruebas (era 260) · 29 archivos (era 24) · M-03 (200 L ≤ 200), D-09, D-14, D-20 saldadas · 444.56 kB                    | 2026-09-01 |
| 08 — Endurecimiento y cierre               | `docs/fases/fase-08.md` | ⚪ PENDIENTE      | —                                                                                                                           | —          |

**Leyenda:** 🟢 COMPLETADA · 🟡 ACTIVA · 🔴 BLOQUEADA · ⚪ PENDIENTE

---

## 2. Siguiente acción concreta

> Ejecutar `docs/fases/fase-08.md` (Endurecimiento, erradicación de `console.error` y cierre).

---

## 3. Decisiones pendientes abiertas

| ID    | Bloquea      | Estado                                                                                  |
|-------|--------------|-----------------------------------------------------------------------------------------|
| DP-01 | Fases 05     | 🟢 **RESUELTA** (2026-09-01) — se conserva alias público `messages`                     |
| DP-02 | Fase 07      | 🟢 **RESUELTA** (2026-09-01) — umbrales trasladados literalmente a `domain/` (R-2)      |
| DP-03 | Fases 02, 04 | 🟢 **RESUELTA** (2026-08-31) — se conserva `POST '/'`                                   |
| DP-04 | Fase 03      | 🟢 **RESUELTA** (2026-09-01) — copys trasladados a `domain/` carácter por carácter      |
| DP-05 | Fases 05, 06 | 🟢 **RESUELTA** (2026-09-01) — BehaviorSubjects encapsulados expuestos como Observables |
| DP-06 | Fases 02, 06 | 🟢 **RESUELTA** (2026-09-01) — valores conservados en `app-tuning.ts` y pipeline RxJS   |
| DP-07 | Fase 03      | 🟢 **RESUELTA** (2026-09-01) — centralización en español sin dependencias de i18n       |
| DP-08 | Fase 05      | 🟢 **RESUELTA** (2026-09-01) — loading independiente por flujo en stores atómicos       |
| DP-09 | Fase 02      | 🟢 **RESUELTA** (2026-08-31) — tipos derivados de `azure-devops-backend`                |
| DP-10 | Fase 05      | 🟢 **RESUELTA** (2026-09-01) — reactividad preservada bajo R-1 y R-3                    |

Detalle completo en `docs/plan/DECISIONES_PENDIENTES.md`.

---

## 4. Métricas vivas (medidas, no declaradas)

|    # | Métrica                                      |      Baseline medido |                       Actual | Objetivo | Cierra en                      |
|-----:|----------------------------------------------|---------------------:|-----------------------------:|---------:|--------------------------------|
| M-01 | Líneas `devops-agent-state.service.ts`       |            406 → 418 |                      **148** |     ≤ 60 | 08                             |
| M-02 | Líneas del store más grande                  |                  418 |         **117** (Refinement) |    ≤ 150 | 05 ✅                          |
| M-03 | Líneas `planning-dashboard.component.ts`     |                  753 |                      **200** |    ≤ 200 | 07 ✅                          |
| M-04 | Ocurrencias de `any`                         |                **9** |                     **0** ✅ |        0 | 02 ✅                          |
| M-05 | `.subscribe()` en producción                 |               **17** |                           17 |        — | 08                             |
| M-06 | Temporizadores sin limpieza                  |                **3** |                        **2** |        0 | 08                             |
| M-07 | Componentes con `OnPush`                     |                0 / 9 |                       0 / 13 |    100 % | 08                             |
| M-08 | Componentes con `.spec.ts`                   |                0 / 9 |               **13 / 13** ✅ |    100 % | 01 / 07 ✅                     |
| M-09 | Archivos `.spec.ts`                          |                    4 |                       **29** |     ≥ 18 | 04 / 05 / 07 ✅                |
|    — | Casos de prueba                              |                   29 |                      **291** |        — | —                              |
| M-10 | Rutas literales en `http.*`                  |               **10** |                     **0** ✅ |        0 | 02 ✅                          |
| M-11 | Números mágicos                              |                   12 |                     **0** ✅ |        0 | 02 ✅ (sondeo) / 07 (umbrales) |
| M-12 | Señales `loading` por flujo                  |                    1 |                        **5** |      ≥ 4 | 05 ✅                          |
| M-13 | Existe `core/{guards,interceptors,services}` |                   ❌ |                       **✅** |       ✅ | 02 ✅                          |
| M-14 | Existe `environments/`                       |                   ❌ |                       **✅** |       ✅ | 02 ✅                          |
| M-18 | `pnpm build`                                 |          ❌ **rota** |                 ✅ 444.56 kB |       ✅ | 01 ✅                          |
| M-19 | `pnpm test`                                  | ❌ **no ejecutable** |                   ✅ 291/291 |       ✅ | 01 ✅                          |
| M-20 | Archivos con `aria-label`                    |          4 / 11 usos |                            4 |        — | 08                             |
| M-21 | `console.error` en producción                |     10 *(declarado)* | **16** *(medido en Fase 02)* |        0 | 08                             |

---

## 5. Restricciones heredadas (leer antes de las fases 04, 05 y 07)

| #       | Restricción                                                                                                                                                                                                                                                              | Origen     |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| **R-1** | **La Fase 05 debe migrar el estado de vista a `signal()` ANTES de separar el `loading` por flujo (D-16).** La app es zoneless y hoy la tabla de planeaciones solo se repinta por el rebote accidental del `loading` compartido. El orden inverso rompe la funcionalidad. | D-27 / H-3 |
| **R-2** | Los umbrales de calidad de la Fase 07 se trasladan **literalmente**, incluida la discrepancia `90/70/50` (etiqueta) frente a `80/50` (color y filtro). Prohibido unificarlos sin resolver DP-02.                                                                         | DP-02      |
| **R-3** | Las fases 02 a 08 **no pueden modificar los `.spec.ts` de la Fase 01**. Si uno falla, se revierte el refactor; no se adapta la prueba.                                                                                                                                   | Fase 01    |
| **R-4** | Los copys se **mueven**, nunca se reescriben, mientras DP-04 siga abierta.                                                                                                                                                                                               | DP-04      |
| **R-5** | **La Fase 04 debe conservar `DevopsAgentApiService` como fachada delegante.** `devops-agent-api.service.spec.ts` (18 pruebas) y `planning-management.component.spec.ts` lo inyectan por nombre y no pueden modificarse. Eliminarlo rompe R-3.                            | Fase 02    |
| **R-6** | **`environment.apiBaseUrl` debe permanecer vacío** mientras no haya un despliegue con el BFF en otro origen. Rellenarlo cambia todas las URL y rompe las 18 pruebas de la capa de API.                                                                                   | Fase 02    |

---

## 6. Bitácora

| Fecha      | Evento                                                                                                                                                                                                                                                                                                                                                                                                            |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-08-31 | Diagnóstico técnico. 21 deudas (D-01 a D-21), 5 flujos (F1–F5).                                                                                                                                                                                                                                                                                                                                                   |
| 2026-08-31 | Publicados `plan-maestro.md`, `DECISIONES_PENDIENTES.md` y `fase-01.md`.                                                                                                                                                                                                                                                                                                                                          |
| 2026-08-31 | **Fase 01 ejecutada.** Hallazgo H-1: el repositorio **no compilaba** (`TS2729`). Excepción S-01 aplicada: D-07 saldada.                                                                                                                                                                                                                                                                                           |
| 2026-08-31 | Fase 01: suite de caracterización 29 → **188** pruebas, 4 → **12** archivos, 0/9 → **9/9** componentes cubiertos.                                                                                                                                                                                                                                                                                                 |
| 2026-08-31 | Fase 01: hallazgo H-3 (**D-27**, 🔴) — en modo zoneless la tabla de planeaciones no se repinta; funciona por accidente vía el `loading` compartido. Genera la restricción R-1.                                                                                                                                                                                                                                    |
| 2026-08-31 | Fase 01: hallazgo H-2 (**D-26**) — `planning-management` inyecta el cliente HTTP directamente.                                                                                                                                                                                                                                                                                                                    |
| 2026-08-31 | Fase 01: métricas corregidas — `any` 5→**9**, `.subscribe()` 4→**17**, temporizadores 1→**3**. Deudas nuevas D-22 a D-27.                                                                                                                                                                                                                                                                                         |
| 2026-08-31 | Generado `docs/fases/fase-02.md`. **Bloqueada** hasta resolver DP-03, DP-06 y DP-09.                                                                                                                                                                                                                                                                                                                              |
| 2026-08-31 | **DP-09 🟢 RESUELTA**: el usuario autoriza derivar los tipos de `azure-devops-backend`/`azure-devops-agent`. **DP-03 🟢 RESUELTA**: se conserva `POST '/'`. **DP-06 🟡 PARCIAL**: deuda técnica reconocida, se aplica la acción por defecto.                                                                                                                                                                      |
| 2026-08-31 | **Fase 02 ejecutada.** Creada la capa `core/{config,interceptors,services}` y `src/environments/`. D-05, D-24, D-10 y D-14 saldadas; D-08 y D-09 saldadas en su parte de rutas e intervalos.                                                                                                                                                                                                                      |
| 2026-08-31 | Fase 02: `any` **9 → 0**, rutas literales **10 → 0**, pruebas **188 → 200** sin tocar un solo `.spec.ts` previo (R-3 cumplida). Build 434.21 → **435.66 kB** (+1.45 kB).                                                                                                                                                                                                                                          |
| 2026-08-31 | Fase 02: hallazgo H-1 — `console.error` recontado a **16** (se declaraban 10); nueva métrica M-21. Hallazgo H-2 → deuda nueva **D-28** (`[object Object]` al usuario), asignada a la Fase 08. Nuevas restricciones **R-5** y **R-6**.                                                                                                                                                                             |
| 2026-08-31 | Generado `docs/fases/fase-03.md`. **Bloqueada** por DP-04 (DP-07 acota el alcance); la acción por defecto R-4 permite ejecutarla sin riesgo.                                                                                                                                                                                                                                                                      |
| 2026-09-01 | **Fase 03 ejecutada.** Creada la capa `features/devops-agent/domain/` (`chat-greetings.ts`, `chat-prompts.ts`, `chat-suggestions.ts`, `tech-tags.ts`). D-06, D-19 y D-21 saldadas. DP-04 y DP-07 🟢 resueltas.                                                                                                                                                                                                    |
| 2026-09-01 | Fase 03: `state.service` **424 → 370** líneas (0 líneas de markdown), pruebas **200 → 202** (15 archivos) sin tocar `.spec.ts` previos (R-3 cumplida). Build **435.65 kB**.                                                                                                                                                                                                                                       |
| 2026-09-01 | Generado `docs/fases/fase-04.md`. Siguiente fase: Segregación de la capa de API.                                                                                                                                                                                                                                                                                                                                  |
| 2026-09-01 | **Fase 04 ejecutada.** Creada la capa `features/devops-agent/services/api/` (`agent-chat-api.service.ts`, `planning-api.service.ts`, `dashboard-api.service.ts`, `tasks-api.service.ts`, `index.ts`). `DevopsAgentApiService` convertido en fachada delegante. D-11, D-08, D-26 saldadas. DP-03 🟢 aplicada.                                                                                                      |
| 2026-09-01 | Fase 04: pruebas **202 → 220** (19 archivos) sin tocar `.spec.ts` previos (R-3 cumplida). Build **435.65 kB** (M-09 ≥ 18 ✅).                                                                                                                                                                                                                                                                                     |
| 2026-09-01 | **Fase 05 ejecutada.** Creada la capa `features/devops-agent/services/state/` (`general-chat-state.service.ts`, `refinement-chat-state.service.ts`, `planning-state.service.ts`, `dashboard-state.service.ts`, `tasks-state.service.ts`, `index.ts`). `DevopsAgentStateService` convertido en fachada delegante pura. D-01, D-02, D-03, D-04, D-12, D-16, D-17 saldadas. DP-01, DP-05, DP-08, DP-10 🟢 resueltas. |
| 2026-09-01 | Fase 05: pruebas **220 → 259** (24 archivos) sin tocar `.spec.ts` previos (R-3 cumplida). Build **435.65 kB** (M-02 = 117 L ≤ 150 ✅).                                                                                                                                                                                                                                                                            |
| 2026-09-01 | Generado `docs/fases/fase-06.md`. Siguiente fase: Flujo de tareas, sondeo y temporizadores seguros.                                                                                                                                                                                                                                                                                                               |
| 2026-09-01 | **Fase 06 ejecutada.** Migrado `TasksStateService` a pipeline declarativo RxJS (`timer`, `switchMap`, `takeUntil`, `Subject`) eliminando `setTimeout` recursivo. D-02, D-15 y M-06 saneadas en tasks-state. DP-06 🟢 resuelta.                                                                                                                                                                                    |
| 2026-09-01 | Fase 06: pruebas **259 → 260** (24 archivos) sin tocar `.spec.ts` previos de Fases 01 a 05 (R-3 cumplida). Build **437.91 kB**.                                                                                                                                                                                                                                                                                   |
| 2026-09-01 | Generado `docs/fases/fase-07.md`. Siguiente fase: Descomposición del Tablero de Calidad.                                                                                                                                                                                                                                                                                                                          |
| 2026-09-01 | **Fase 07 ejecutada.** Creada la capa `domain/quality-score.ts` y 4 subcomponentes (`QualitySummaryCardsComponent`, `QualityStoriesTableComponent`, `QualityChartComponent`, `StoryAuditModalComponent`). `PlanningDashboardComponent` reducido de 753 a **200 líneas** (M-03). D-09, D-14, D-20 saldadas. DP-02 🟢 aplicada con R-2.                                                                             |
| 2026-09-01 | Fase 07: pruebas **260 → 291** (29 archivos) sin tocar `.spec.ts` previos de Fases 01 a 05 (R-3 cumplida). Build **444.56 kB**.                                                                                                                                                                                                                                                                                   |
| 2026-09-01 | Generado `docs/fases/fase-08.md`. Siguiente fase: Endurecimiento, erradicación de `console.error` y cierre.                                                                                                                                                                                                                                                                                                       |


