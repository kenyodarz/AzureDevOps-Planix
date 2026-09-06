# Tablero de Estado — Harness: Planning y Story Engine

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio antes de iniciar cualquier sesión de desarrollo o ejecutar código.
>
> **Última actualización:** 2026-09-06 · **Fase activa:**
> `Ninguna — Plan Maestro del Harness Finalizado y Certificado al 100%`

---

## 1. Tablero de Fases

|   #    | Fase                                                           | Archivo de Especificación                          | Resultado / Cierre                |      Estado       | Fecha de Cierre |
|:------:|:---------------------------------------------------------------|:---------------------------------------------------|:----------------------------------|:-----------------:|:---------------:|
| **01** | Modelos y Puerto de Almacenamiento Documental (`domain/model`) | `fases/FASE-01-modelos-y-puerto-spec-storage.md`   | `resultados/RESULTADO-FASE-01.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **02** | Adaptador `FileSystemSpecAdapter` (`infrastructure`)           | `fases/FASE-02-adaptador-filesystem-spec.md`       | `resultados/RESULTADO-FASE-02.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **03** | Migración de `PlanningDraftFlowHandler` a `SpecStoragePort`    | `fases/FASE-03-migracion-draft-handler.md`         | `resultados/RESULTADO-FASE-03.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **04** | Modelos de Dominio para Program Planning                       | `fases/FASE-04-modelos-dominio-planeacion.md`      | `resultados/RESULTADO-FASE-04.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **05** | Prompt Externalizado de Planeación (`roadmap`)                 | `fases/FASE-05-prompt-externalizado-planeacion.md` | `resultados/RESULTADO-FASE-05.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **06** | Caso de Uso y Handler del Planner Agent                        | `fases/FASE-06-handler-planner-agent.md`           | `resultados/RESULTADO-FASE-06.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **07** | Especialización de Prompts Story Creator con Regla HyMS        | `fases/FASE-07-prompts-story-creator-hyms.md`      | `resultados/RESULTADO-FASE-07.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **08** | Orquestación y Despacho del Harness                            | `fases/FASE-08-orquestacion-despacho-harness.md`   | `resultados/RESULTADO-FASE-08.md` | 🟢 **COMPLETADA** |   2026-09-06    |
| **09** | Validación E2E con Caso Real Q3-2026 y Cierre                  | `fases/FASE-09-validacion-e2e-cierre.md`           | `resultados/RESULTADO-FASE-09.md` | 🟢 **COMPLETADA** |   2026-09-06    |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🏆 **Plan Maestro Concluido Exitosamente (9 de 9 fases completadas):**
>
> Todas las capacidades de almacenamiento documental (`SpecStoragePort`), orquestación de planeación
> (`ProgramPlanningUseCase`), especialización normativa HyMS/QA (**DP-PL-02**), despacho exhaustivo
> y
> pruebas integrativas E2E se encuentran operativas, certificadas y validadas al 100% con 407
> pruebas.
 
---

## 3. Decisiones Abiertas que Bloquean

|      ID      |   Bloquea   |     Estado      | Resumen                                                                                    |
|:------------:|:-----------:|:---------------:|:-------------------------------------------------------------------------------------------|
| **DP-PL-01** | Fase 01, 02 | 🟢 **RESUELTA** | Almacenamiento documental vía `SpecStoragePort` y `FileSystemSpecAdapter`.                 |
| **DP-PL-02** | Fase 05, 07 | 🟢 **RESUELTA** | HU = Construir hasta QA; HA = Habilitación Operativa HyMS y paso a producción.             |
| **DP-PL-03** | Fase 03, 07 | 🟢 **RESUELTA** | Detección de frente por prefijo con fallback amigable.                                     |
| **DP-PL-04** | Fase 05, 06 | 🟢 **RESUELTA** | Output de planeación en Markdown estructurado (`ideas_planning_QX.md`) y specs por frente. |

> ✅ **Cero decisiones bloqueantes abiertas.** Se puede proceder con la ejecución técnica.
 
---

## 4. Métricas Vivas de Calidad (`azure-devops-agent`)

| Métrica                                 | Baseline Actual | Meta del Plan | Estado Actual |
|:----------------------------------------|:---------------:|:-------------:|:-------------:|
| **Pruebas Unitarias Pasando**           |       305       | 100% pasando  | 🟢 407 / 407  |
| **Cobertura en `domain/model`**         |      99.7%      |  $\ge 90\%$   |   🟢 95.1%    |
| **Cobertura en `domain/usecase`**       |      99.2%      |  $\ge 90\%$   |   🟢 94.3%    |
| **Cobertura en `:spec-storage`**        |       N/A       |  $\ge 90\%$   |   🟢 92.0%    |
| **Cobertura en `:prompt-template`**     |       N/A       |  $\ge 90\%$   |   🟢 93.3%    |
| **Cobertura en `:reactive-web`**        |       N/A       |  $\ge 90\%$   |   🟢 91.0%    |
| **Violaciones ArchUnit**                |        0        |     **0**     |     🟢 0      |
| **Clases con $> 300$ líneas de código** |        0        |     **0**     |     🟢 0      |

 
---

## 5. Bitácora del Plan

|     Fecha      | Evento                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|:--------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **2026-09-06** | Cierre formal y definitivo del Plan Maestro del Harness (Fase 09): validación E2E del comando `/plan Q3-2026 6 sprints 34 sp [Canales]` en `RouterRestTest`, simulación del guardado del artefacto maestro `ideas_planning_q3_2026.md` vía `SpecStoragePort.saveSpec`, verificación de respuesta Markdown delimitada según DP-PL-02 (HUs hasta QA y HAs HyMS). 407 pruebas pasando al 100%, 91% en `reactive-web` y `validateStructure` aprobado. Plan cerrado al 100%. |
| **2026-09-06** | Cierre exitoso de la Fase 08: integración y cableado de `ProgramPlanningFlowHandler` en `UseCasesConfig` (`app-service`), eliminación del bypass de `PROGRAM_PLANNING` en `ChatFlowDispatcher.requireExhaustive` (exhaustividad estricta de 7/7 intenciones), actualización de pruebas de cableado y despacho. 406 pruebas (100% pasando), 94.3% cobertura en `domain/usecase` y 100% test strength en `:app-service`. Se genera Fase 09.                               |
| **2026-09-06** | Cierre exitoso de la Fase 07: especialización de la plantilla de prompt `fase2-historia-estructurada.md` con directivas corporativas de DP-PL-02 (HU hasta QA vs HA HyMS), delimitación taxativa de criterios de aceptación y tareas, y validación normativa en `PromptTemplateContentTest`. 404 pruebas (100% pasando), 100% test strength en `:app-service`. Se genera Fase 08.                                                                                       |
| **2026-09-06** | Cierre exitoso de la Fase 06: implementación de `ProgramPlanningUseCase` y `ProgramPlanningFlowHandler` en `domain/usecase`, integración de `SpecStoragePort`, `PromptTemplatePort` y `ChatGateway`, validación activa de DP-PL-02 y DP-PL-04, tolerancia a tablas complejas y 11 pruebas unitarias nuevas. 403 pruebas (100% pasando), 92.3% cobertura en `domain/usecase`. Se genera Fase 07.                                                                         |
| **2026-09-06** | Cierre exitoso de la Fase 05: externalización de la plantilla de prompt `program-planning-roadmap.md` bajo DP-PL-02 y DP-PL-04, adición de `PromptTemplateId.PROGRAM_PLANNING`, registro en `ClasspathPromptTemplateAdapter` y pruebas exhaustivas libres de placeholders residuales. 391 pruebas (100% pasando), 93.3% de cobertura en `:prompt-template`. Se genera Fase 06.                                                                                          |
| **2026-09-06** | Cierre exitoso de la Fase 04: modelos de dominio inmutables para Program Planning (`ActivityType`, `SprintAllocation`, `ProgramPlanRequest`, `ProgramPlanResult`), `AgentIntent.PROGRAM_PLANNING` y resolución en `IntentResolver`. 389 pruebas (100% pasando), 95.1% de cobertura en `domain/model`. Se genera Fase 05.                                                                                                                                                |
| **2026-09-06** | Cierre exitoso de la Fase 03: migración de `PlanningDraftFlowHandler` a `SpecStoragePort`, resolución de frente con prefijo y fallback resiliente. 334 pruebas pasando al 100%, 98.8% de cobertura en `domain/usecase`. Se genera Fase 04.                                                                                                                                                                                                                              |
| **2026-09-06** | Cierre exitoso de la Fase 02: implementación de `FileSystemSpecAdapter` en el submódulo `:spec-storage` con 92% de cobertura de líneas, 95.2% de fuerza en mutación PIT y 18 pruebas nuevas (332 totales). Se genera la Fase 03.                                                                                                                                                                                                                                        |
| **2026-09-06** | Cierre exitoso de la Fase 01: creación de `SpecDocument`, `SpecNotFoundException` y `SpecStoragePort` en `domain/model` con 100% de cobertura y 9 pruebas nuevas (314 totales). Se genera la especificación y prompt de la Fase 02.                                                                                                                                                                                                                                     |
| **2026-09-05** | Inicialización del Plan Maestro del Harness de Agentes (9 fases atómicas). Resguardo histórico del plan anterior en `docs/historico/` y estandarización del repositorio maestro de plantillas en `docs/`. Fase 01 lista para ejecución.                                                                                                                                                                                                                                 |
