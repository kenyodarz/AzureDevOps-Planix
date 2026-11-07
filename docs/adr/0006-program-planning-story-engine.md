# ADR-0006: Motor de Program Planning y Generación Downstream Estructurada (DP-PL-04)

* **Fecha:** 2026-09-06
* **Estado:** Aceptado
* **Autor(es):** Agile & Agent Engineering Team
* **Aprobadores:** Lead Architect

---

## 1. Contexto y Planteamiento del Problema

La planificación de un trimestre o release (Program Planning / PI Planning) involucra balancear la
capacidad del equipo de ingeniería (sprints, puntos de historia, vacaciones, ceremonias) con la
descomposición de épicas e iniciativas complejas. Los LLMs tienden a generar respuestas dispersas o
planes que sobreasignan la capacidad en los primeros sprints si no se les impone una estructura
matemática y un formato de salida estricto.

---

## 2. Opciones Evaluadas

| Opción                                                                                                                                                                | Ventajas                                                                                                                                                                                    | Desventajas                                                                                                     |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| **Opción A:** Chat no estructurado solicitando plan en texto libre                                                                                                    | Interacción conversacional flexible.                                                                                                                                                        | Salidas heterogéneas, sin validación de capacidad por sprint, imposible de parsear por herramientas downstream. |
| **Opción B:** Motor de optimización lineal externo (CPLEX / Python)                                                                                                   | Asignación matemática perfecta.                                                                                                                                                             | Excesiva complejidad operativa; no aprovecha el razonamiento contextual del LLM sobre dependencias técnicas.    |
| **Opción C:** Caso de uso `ProgramPlanningUseCase` con modelos de dominio (`SprintAllocation`, `ProgramPlanRequest`) y plantilla Markdown estructurada (**DP-PL-04**) | Balance entre razonamiento contextual del LLM y restricción estricta de capacidad; salida en Markdown estandarizado (`ideas_planning_QX.md`); persistencia automática en `SpecStoragePort`. | Requiere tipar en el dominio modelos inmutables de asignación y sprints.                                        |

---

## 3. Decisión Adoptada

**Adoptamos la Opción C (DP-PL-04):**

* El dominio modela formalmente:
    - `ActivityType`: Tipos de actividad (Construcción, Refinamiento, QA, Acompañamiento HyMS).
    - `SprintAllocation`: Asignación de puntos y capacidad real por sprint.
    - `ProgramPlanRequest` y `ProgramPlanResult`: Contratos inmutables de entrada y salida del plan.
* La salida del caso de uso genera un artefacto maestro Markdown estructurado
  (`ideas_planning_qX_YYYY.md`) que incluye:
    1. Tabla de diagnóstico de capacidad y restricciones de equipo.
    2. Roadmap delimitado de HUs (hasta QA) y HAs (HyMS).
    3. Matriz de riesgos y mitigaciones técnicas.
* El documento se persiste automáticamente a través de `SpecStoragePort` y se sincroniza en tiempo
  real al frontend vía SSE.

---

## 4. Consecuencias y Compromisos

### Positivas:

* Planes consistentes, reproducibles y directamente consumibles por Scrum Masters y herramientas de
  BI.
* Validación de extremo a extremo probada con éxito en `RouterRestTest` simulando el caso real
  Q3-2026.
* 407 pruebas en verde certificando el flujo completo de planeación en el agente.

### Negativas / Riesgos Aceptados:

* Tablas Markdown complejas requieren un formateo cuidadoso en el prompt para evitar errores de
  renderizado en visores web simples.

---

## 5. Referencias y Enlaces Relacionados

* `azure-devops-agent/docs/fases/FASE-06-handler-planner-agent.md`
* `azure-devops-agent/docs/fases/FASE-09-validacion-e2e-cierre.md`
