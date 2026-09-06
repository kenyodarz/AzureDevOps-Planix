# Tablero de Estado — Integración de Program Planning en el Frontend

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio al iniciar cualquier sesión de desarrollo o antes de ejecutar código.  
> 
> **Última actualización:** 2026-09-06 · **Fase activa:** `Fase 01 — Contratos de API, Modelos y DTOs`

---

## 1. Tablero de Fases

| # | Fase | Archivo de Especificación | Resultado / Cierre | Estado | Fecha de Cierre |
| :---: | :--- | :--- | :--- | :---: | :---: |
| **01** | Contratos de API, Modelos y DTOs (`core/config` y `models`) | `fases/FASE-01-contratos-api-modelos-dtos.md` | `resultados/RESULTADO-FASE-01.md` | 🟡 **PENDIENTE** | — |
| **02** | Servicios de API y Gestión de Estado Reactivo (`services/api` y `services/state`) | `fases/FASE-02-servicios-api-estado-reactivo.md` | `resultados/RESULTADO-FASE-02.md` | ⚪ NO GENERADA | — |
| **03** | Visor y Explorador Documental de Specs (`components/planning-specs-explorer`) | `fases/FASE-03-visor-explorador-specs.md` | `resultados/RESULTADO-FASE-03.md` | ⚪ NO GENERADA | — |
| **04** | Modal Lanzador de Program Planning (`components/program-planning-modal`) | `fases/FASE-04-modal-lanzador-planeacion.md` | `resultados/RESULTADO-FASE-04.md` | ⚪ NO GENERADA | — |
| **05** | Integración en Página Principal, Navegación a Refinamiento y Cierre E2E | `fases/FASE-05-integracion-global-cierre-e2e.md` | `resultados/RESULTADO-FASE-05.md` | ⚪ NO GENERADA | — |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🎯 **Fase 01 Lista para Ejecución:**  
> Abrir `docs/fases/FASE-01-contratos-api-modelos-dtos.md` o ejecutar su prompt `docs/prompts/PROMPT-FASE-01.md` para registrar los endpoints en `api-endpoints.ts` y las interfaces DTO en `devops-agent.model.ts`.

---

## 3. Decisiones Abiertas que Bloquean

| ID | Bloquea | Estado | Resumen |
| :---: | :---: | :---: | :--- |
| **DP-FE-01** | Fase 03, 05 | 🟢 **RESUELTA** | Coexistencia de vistas: Explorador de Specs como vista principal y vectorización legada como opción secundaria. |
| **DP-FE-02** | Fase 05 | 🟢 **RESUELTA** | Botón *«Refinar HU en este Frente»* precarga el prompt en `RefinementChatStateService` y transiciona al tab de refinamiento. |
| **DP-FE-03** | Fase 03 | 🟢 **RESUELTA** | Renderizado seguro y estilizado de Markdown con utilidades nativas de Angular y Tailwind CSS. |

> ✅ **Cero decisiones bloqueantes abiertas.** Se puede proceder con la ejecución técnica.

---

## 4. Métricas Vivas del Proyecto (`azure-devops-frontend`)

| Métrica | Baseline Inicial | Meta del Plan | Estado Actual |
| :--- | :---:| :---:| :---: |
| **Estado de Compilación (`pnpm build`)** | OK | OK | 🟢 Limpio (445.87 kB) |
| **Tipos `any` en Nuevos Modelos** | 0 | 0 | 🟢 0 |
| **Violaciones de Arquitectura (`AGENTS.md`)** | 0 | 0 | 🟢 0 |
| **Pruebas de Componentes y Servicios Nuevos** | N/A | 100% pasando | 🟢 Pendiente inicio |

---

## 5. Bitácora del Plan

| Fecha | Evento |
| :---: | :--- |
| **2026-09-06** | Inicialización del **Plan Maestro de Integración de Program Planning en el Frontend**. Resguardo del histórico de desacoplamiento en `docs/historico/plan_desacoplamiento_frontend/`. Fase 01 lista para ejecución. |
