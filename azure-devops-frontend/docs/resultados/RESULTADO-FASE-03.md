# RESULTADO — FASE 03: Externalización de copys y prompts a la capa `domain/`

> **Ejecutada:** 2026-09-01 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(devops_agent): externalizar copys y prompts a la capa domain`
> **Instrucciones:** `docs/fases/fase-03.md`
> **Código productivo modificado:** 3 archivos existentes tocados · 5 archivos nuevos de dominio · 1 archivo de test nuevo ·
> **cero `.spec.ts` de las Fases 01 y 02 modificados**

---

## 1. Objetivo de la fase

Externalizar los textos de cara al usuario, los saludos en markdown (~40 líneas), los prompts de negocio para el agente y los catálogos estáticos de UI hacia una capa `features/devops-agent/domain/` pura e inmutable. Se saldan **D-06, D-19 y D-21** respetando la política de No-Asunción y la acción por defecto de **DP-04** (conservar textos carácter por carácter) y **DP-07** (centralizar en constantes en español sin dependencias de i18n). Las 200 pruebas existentes continúan verdes sin modificar ningún `.spec.ts` previo.

---

## 2. Qué se construyó

| Artefacto                                                                     | Acción        | Detalle                                                                                                 |
|-------------------------------------------------------------------------------|---------------|---------------------------------------------------------------------------------------------------------|
| `src/app/features/devops-agent/domain/chat-greetings.ts`                      | **CREADO**    | `GENERAL_GREETING`, `REFINEMENT_GREETING`, `GENERAL_RESET_GREETING`, `REFINEMENT_RESET_GREETING` (D-06) |
| `src/app/features/devops-agent/domain/chat-prompts.ts`                        | **CREADO**    | `buildRefinementPrompt`, `buildAuditPrompt` (D-19)                                                      |
| `src/app/features/devops-agent/domain/chat-suggestions.ts`                    | **CREADO**    | `ChatSuggestion` interface y `CHAT_SUGGESTIONS` con `as const` (D-21)                                   |
| `src/app/features/devops-agent/domain/tech-tags.ts`                           | **CREADO**    | `TECH_TAGS` con `as const` (D-21)                                                                       |
| `src/app/features/devops-agent/domain/index.ts`                               | **CREADO**    | Barrel export de la capa `domain`                                                                       |
| `src/app/features/devops-agent/domain/chat-prompts.spec.ts`                   | **CREADO**    | 2 pruebas unitarias en formato GIVEN / WHEN / THEN                                                      |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`        | MODIFICADO    | Eliminado markdown/literales; consumo desde `domain` (D-06, D-19)                                       |
| `src/app/features/devops-agent/components/chat-input/chat-input.component.ts` | MODIFICADO    | `suggestions` consume `CHAT_SUGGESTIONS` de `domain` (D-21)                                             |
| `src/app/features/devops-agent/components/info-cards/info-cards.component.ts` | MODIFICADO    | `tags` consume `TECH_TAGS` de `domain` (D-21)                                                           |
| **Cualquier `.spec.ts` previo**                                               | **NO TOCADO** | `git diff --stat -- 'src/app/**/*.spec.ts'` → **vacío** ✅                                              |

### 2.1 Pruebas nuevas o reescritas

| Archivo                                             |    Casos antes |           Casos después |
|-----------------------------------------------------|---------------:|------------------------:|
| `features/devops-agent/domain/chat-prompts.spec.ts` | 0 (no existía) |                   **2** |
| *Todas las demás (Fases 01 y 02)*                   |            200 | **200 — sin modificar** |
| **TOTAL**                                           |        **200** |                 **202** |

---

## 3. Decisiones aplicadas

| ID        | Resolución                                                                                                                               | Efecto en esta fase                                                                                                                                                            |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-04** | 🟢 **RESUELTA** (2026-09-01): Se aplicó la acción por defecto segura (R-4) trasladando los copys carácter por carácter sin alteraciones. | Se conservaron todos los literales exactamente como estaban (incluida la errata «aprovadores» de `chat-input` y el mensaje de error del puerto 8081 en el servicio de estado). |
| **DP-07** | 🟢 **RESUELTA** (2026-09-01): Se centralizaron los textos en constantes en español.                                                      | No se añadió `@angular/localize` ni ninguna dependencia externa; las constantes residen en `domain/`.                                                                          |

---

## 4. Métricas obtenidas

|    # | Métrica                                      |      Antes |          Después |          Objetivo |
|-----:|----------------------------------------------|-----------:|-----------------:|------------------:|
| M-01 | Líneas `devops-agent-state.service.ts`       |        424 |          **370** | ≤ 60 (Fase 05/06) |
| M-02 | Líneas del store más grande                  |        424 |          **370** |   ≤ 150 (Fase 05) |
| M-03 | Líneas `planning-dashboard.component.ts`     |        753 |  753 *(intacto)* |   ≤ 200 (Fase 07) |
| M-04 | Ocurrencias de `any` en producción           |          0 |         **0** ✅ |                 0 |
| M-05 | `.subscribe()` en producción                 |         17 |   17 *(intacto)* |           Fase 08 |
| M-06 | Temporizadores sin limpieza                  |          3 |    3 *(intacto)* |        Fase 06/08 |
| M-07 | Componentes con `OnPush`                     |      0 / 9 |            0 / 9 |   100 % (Fase 08) |
| M-08 | Componentes con `.spec.ts`                   |      9 / 9 |     **9 / 9** ✅ |             100 % |
| M-09 | Archivos `.spec.ts`                          |         14 |           **15** |              ≥ 18 |
|    — | Casos de prueba                              |        200 |          **202** |                 — |
| M-10 | Rutas literales en llamadas `http.*`         |          0 |         **0** ✅ |                 0 |
| M-11 | Números mágicos                              |          8 |    8 *(intacto)* |       0 (Fase 07) |
| M-13 | Existe `core/{config,interceptors,services}` |         ✅ |           **✅** |                ✅ |
| M-14 | Existe `environments/`                       |         ✅ |           **✅** |                ✅ |
| M-18 | `pnpm build` — initial total                 |  435.66 kB | **435.65 kB** ✅ |          < 500 kB |
| M-19 | `pnpm test`                                  |    200/200 |   **202/202** ✅ |             verde |
|    — | Markdown (`###`) en `state.service`          | ~40 líneas |         **0** ✅ |                 0 |

---

## 5. Comandos ejecutados

```bash
cd azure-devops-frontend

# --- Verificación inicial y ejecución ---
corepack pnpm test --no-watch    # Test Files 14 passed (14) | Tests 200 passed (200)   ✅
corepack pnpm build              # Initial total 435.66 kB                              ✅

# --- Cierre de fase ---
corepack pnpm test --no-watch    # Test Files 15 passed (15) | Tests 202 passed (202)   ✅
corepack pnpm build              # Initial total 435.65 kB                              ✅

# Verificación de ausencia de markdown en devops-agent-state.service.ts
# grep -c '###' src/app/features/devops-agent/services/devops-agent-state.service.ts
# → 0 ✅

# Verificación de que ningún .spec.ts previo fue modificado (R-3)
git diff --stat -- 'src/app/**/*.spec.ts'
# → (vacío) ✅
```

---

## 6. Hallazgos no previstos

Ninguno. La externalización se realizó de manera limpia sin colisiones ni efectos colaterales en los contratos de pruebas existentes.

---

## 7. Checklist de calidad

| Ítem (`rules/angular-rules.md` §8)                | Estado | Evidencia                                                      |
|---------------------------------------------------|--------|----------------------------------------------------------------|
| Componentes tocados siguen `standalone: true`     | ✅     | `chat-input.component.ts` e `info-cards.component.ts` intactos |
| Cero `*ngIf` / `*ngFor` / `*ngSwitch`             | ✅     | Sintaxis de control flow `@for` preservada                     |
| Estado reactivo con Signals u Observables seguros | ✅     | Sin cambios estructurales en Observables                       |
| Cero fugas de memoria nuevas                      | ✅     | No se abrieron suscripciones manuales                          |
| Botones de solo icono con `aria-label`            | ✅     | Preservados                                                    |
| Cero `any`                                        | ✅     | `M-04 = 0`                                                     |
| Cero llamadas HTTP fuera de la capa de API        | ✅     | Sin cambios                                                    |
| Tipado explícito en métodos públicos              | ✅     | Funciones en `domain/` tienen firma y tipos explícitos         |
| Sin números mágicos                               | ✅     | Constantes inmutables tipadas con `as const`                   |
| Pruebas GIVEN / WHEN / THEN                       | ✅     | `chat-prompts.spec.ts` implementado con esa estructura         |
| Sin copys ni rutas inventadas (§7)                | ✅     | DP-04 aplicada con traslación exacta carácter por carácter     |
| Sin secretos                                      | ✅     | Ninguna credencial o secreto introducido                       |
| `corepack pnpm build` correcto                    | ✅     | 435.65 kB, dentro de presupuesto                               |
| `corepack pnpm test` sin fallos nuevos            | ✅     | 202/202 en 15 suites                                           |
| Sin `.npmrc` ni `strict-ssl=false`                | ✅     | Verificado                                                     |

---

## 8. Desviaciones respecto a las instrucciones

Ninguna. Se siguieron todos los pasos T-01 a T-09 conforme a la especificación de `fase-03.md`.

---

## 9. Estado al cerrar

- Siguiente fase generada: **`docs/fases/fase-04.md`** ✅
- Decisiones resueltas en esta fase: **DP-04** 🟢, **DP-07** 🟢
- Decisiones abiertas: DP-01, DP-02, DP-05, DP-06 *(parcial)*, DP-08, DP-10
- `docs/plan/ESTADO.md` actualizado: **sí**
- Restricción **R-3 cumplida**: cero `.spec.ts` previos modificados
- Siguiente paso: ejecutar **Fase 04 (Segregación de la capa de API)**

