# Tablero de Estado — Evaluación de Pull Requests con Azure DevOps MCP

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio al iniciar cualquier sesión de desarrollo o antes de ejecutar código.
>
> **Última actualización:** 2026-09-08 · **Fase activa:** Fase 02 — Casos de Uso de Consulta de PRs
> y Cambios

---

## 1. Tablero de Fases

|   #    | Fase                                               | Archivo de Especificación                              | Resultado / Cierre                     |      Estado       | Fecha de Cierre |
|:------:|:---------------------------------------------------|:-------------------------------------------------------|:---------------------------------------|:-----------------:|:---------------:|
| **01** | Modelos de Dominio y Puertos de Git / Pull Request | `docs/fases/FASE-01-modelos-dominio-pull-request.md`   | `docs/resultados/RESULTADO-FASE-01.md` | 🟢 **COMPLETADA** |   2026-09-08    |
| **02** | Casos de Uso de Consulta de PRs y Cambios          | `docs/fases/FASE-02-casos-uso-pull-request.md`         | `docs/resultados/RESULTADO-FASE-02.md` | 🟡 **PENDIENTE**  |        —        |
| **03** | Adaptador REST para API Git de Azure DevOps        | `docs/fases/FASE-03-adaptador-rest-git.md`             | `docs/resultados/RESULTADO-FASE-03.md` |  ⚪ NO GENERADA   |        —        |
| **04** | Casos de Uso y Adaptador para Feedback de PR       | `docs/fases/FASE-04-feedback-comentarios-pr.md`        | `docs/resultados/RESULTADO-FASE-04.md` |  ⚪ NO GENERADA   |        —        |
| **05** | Entry Points MCP Server (Tools @McpTool)           | `docs/fases/FASE-05-mcp-tools-git.md`                  | `docs/resultados/RESULTADO-FASE-05.md` |  ⚪ NO GENERADA   |        —        |
| **06** | Orquestación y Prompt de Evaluación en el Agente   | `docs/fases/FASE-06-orquestacion-evaluacion-agente.md` | `docs/resultados/RESULTADO-FASE-06.md` |  ⚪ NO GENERADA   |        —        |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🎯 **Fase 02 Lista para Ejecución:**  
> Abrir `docs/prompts/PROMPT-FASE-02.md` y ejecutar las tareas T-01 a T-04 especificadas en
> `docs/fases/FASE-02-casos-uso-pull-request.md` para implementar `GetPullRequestUseCase` y
> `GetPullRequestChangesUseCase` en `domain/usecase`.

---

## 3. Decisiones Abiertas que Bloquean

| ID | Bloquea |       Estado        | Resumen                                                                      |
|:--:|:-------:|:-------------------:|:-----------------------------------------------------------------------------|
| —  |    —    | 🟢 **SIN BLOQUEOS** | Todas las decisiones iniciales (`DP-PR-01` a `DP-PR-03`) han sido resueltas. |

---

## 4. Métricas Vivas del Proyecto

| Métrica                            | Baseline Inicial | Meta del Plan | Estado Actual |
|:-----------------------------------|:----------------:|:-------------:|:-------------:|
| **Tests Unitarios `:model`**       |        42        |     100%      |  🟢 47 / 47   |
| **Cobertura de Código Dominio**    |       94%        |  $\ge 90\%$   |    🟢 94%     |
| **Mutaciones Eliminadas (Pitest)** |       98%        |  $\ge 60\%$   |    🟢 98%     |
| **Violaciones de Arquitectura**    |        0         |       0       |     🟢 0      |
| **Estado del Build**               |        OK        |      OK       |   🟢 Limpio   |

---

## 5. Bitácora del Plan

|     Fecha      | Evento                                                                                                                                                                            |
|:--------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **2026-08-30** | Cierre exitoso del Plan de Refactorización Maestro (Fases 01 a 08). Archivado a `docs/historico/plan_refactorizacion_mcp/`.                                                       |
| **2026-09-08** | Inicialización del nuevo plan maestro para Evaluación de Pull Requests, registro de decisiones y especificación de la Fase 01 bajo el estándar SDD.                               |
| **2026-09-08** | **Cierre de la Fase 01:** Implementadas las entidades inmutables `PullRequest`, `GitChange`, el puerto reactivo `PullRequestPort` y 5 tests unitarios en `:model` con 100% éxito. |
