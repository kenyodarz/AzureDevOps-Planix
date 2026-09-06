# FASE 05 — Prompt Externalizado de Planeación (`roadmap`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 04 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(agent): agregar plantilla externalizada de prompt para program planning y roadmap`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* La **Fase 01** creó el modelo `SpecDocument` y el puerto `SpecStoragePort` en `domain/model`.
* La **Fase 02** proveyó el adaptador `FileSystemSpecAdapter` para almacenamiento documental
  Markdown.
* La **Fase 03** conectó `PlanningDraftFlowHandler` con `SpecStoragePort` para inyectar specs
  completos.
* La **Fase 04** definió los modelos puros de dominio para Program Planning (`ActivityType` con HU y
  HA
  según **DP-PL-02**, `SprintAllocation`, `ProgramPlanRequest`, `ProgramPlanResult`,
  `AgentIntent.PROGRAM_PLANNING`
  y actualización de `IntentResolver`).

### 1.2 Problema que resuelve esta fase

* Para generar roadmaps de sprints trimestrales y distribuir el trabajo entre Historias de Usuario
  (HU) e
  Historias Habilitadoras (HA) de acuerdo a **DP-PL-02** y **DP-PL-04**, el LLM necesita un sistema
  de prompts
  especializado, robusto y externalizado en el classpath.
* Actualmente, el enum `PromptTemplateId` y el adaptador `ClasspathPromptTemplateAdapter` en el
  módulo
  `infrastructure/driven-adapters/prompt-template` únicamente conocen las plantillas de historias
  individuales (`PLANNING_DRAFT`, `STRUCTURED_STORY`, `STORY_DIVISION`, `STORY_REFINEMENT`,
  `QUALITY_AUDIT`).
* Es necesario crear la plantilla Markdown `program-planning-roadmap.md` e integrarla formalmente en
  el
  motor de plantillas para que la Fase 06 (`ProgramPlanningFlowHandler`) pueda renderizar el prompt
  con sus
  variables dinámicas.

### 1.3 Estado esperado al terminar

* Creación de la plantilla
  `applications/app-service/src/main/resources/prompts/program-planning-roadmap.md`:
    - Instrucciones claras para el rol de Agile Planner y Enterprise Architect.
    - Regla rectora **DP-PL-02**: HU (construcción funcional hasta QA) vs HA (setups técnicos y paso
      a
      producción bajo HyMS: Runbook, ciberseguridad, observabilidad).
    - Formato de salida estructurado conforme a **DP-PL-04**: resumen ejecutivo, tabla de roadmap
      por sprints
      con numeración, tipo (HU/HA), título, story points (Fibonacci: 1, 2, 3, 5, 8, 13), frente y
      dependencias.
    - Placeholders dinámicos: `{{trimestre}}`, `{{objetivos}}`, `{{frentes}}`,
      `{{capacidadSprint}}`,
      `{{specsContexto}}`.
* Adición del valor `PROGRAM_PLANNING` en el enum
  `co.com.bancolombia.model.prompt.PromptTemplateId`.
* Registro de la plantilla en `ClasspathPromptTemplateAdapter.TEMPLATE_FILES`.
* Adición de variables de prueba y validación exhaustiva en `ClasspathPromptTemplateAdapterTest`.
* Verificación de compilación, 100% pruebas pasando y 0 violaciones de arquitectura.

### 1.4 Archivos involucrados

| Ruta                                                                                                                                     |  Acción   | Propósito                                                                                |
|:-----------------------------------------------------------------------------------------------------------------------------------------|:---------:|:-----------------------------------------------------------------------------------------|
| `applications/app-service/src/main/resources/prompts/program-planning-roadmap.md`                                                        |   CREAR   | Plantilla Markdown del prompt de planeación macro y roadmap trimestral.                  |
| `domain/model/src/main/java/co/com/bancolombia/model/prompt/PromptTemplateId.java`                                                       | MODIFICAR | Agregar el identificador `PROGRAM_PLANNING`.                                             |
| `infrastructure/driven-adapters/prompt-template/src/main/java/co/com/bancolombia/prompttemplate/ClasspathPromptTemplateAdapter.java`     | MODIFICAR | Registrar `program-planning-roadmap.md` en el catálogo de plantillas cargadas al inicio. |
| `infrastructure/driven-adapters/prompt-template/src/test/java/co/com/bancolombia/prompttemplate/ClasspathPromptTemplateAdapterTest.java` | MODIFICAR | Pruebas de renderizado y validación de variables requeridas para `PROGRAM_PLANNING`.     |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §1 (`domain/model`): `PromptTemplateId` puro.
    - §4 (Calidad de Código): Validación de variables requeridas; fallo preventivo ante variables
      faltantes.
    - §5 (Testing): Pruebas GIVEN/WHEN/THEN con AssertJ.

### 1.6 Decisiones aplicadas

- **DP-PL-02 (Separación HU vs HA):**  
  El prompt estipula explícitamente la división de responsabilidades: HU construidas y probadas
  hasta QA;
  HA para la habilitación operativa y salida a producción bajo proceso formal HyMS.
- **DP-PL-04 (Salida de Planeación Estructurada):**  
  El prompt guía al modelo para producir el formato estándar que alimentará `ProgramPlanResult` y
  los
  archivos Markdown de spec por frente.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Actualizar `PromptTemplateId.java`:**
    - Agregar el valor `PROGRAM_PLANNING` con Javadoc explicativo.

2. **Crear `program-planning-roadmap.md`:**
    - Ubicarlo en `applications/app-service/src/main/resources/prompts/`.
    - Incorporar directivas de rol, reglas de capacidad por sprint, dependencias secuenciales, regla
      HyMS y
      variables `{{trimestre}}`, `{{objetivos}}`, `{{frentes}}`, `{{capacidadSprint}}`,
      `{{specsContexto}}`.

3. **Actualizar `ClasspathPromptTemplateAdapter.java`:**
    - Mapear `PromptTemplateId.PROGRAM_PLANNING` a `"program-planning-roadmap.md"`.

4. **Actualizar `ClasspathPromptTemplateAdapterTest.java`:**
    - Agregar caso de renderizado para `PromptTemplateId.PROGRAM_PLANNING` con verificación de
      sustitución de
      todos sus placeholders.
    - Incluir `PROGRAM_PLANNING` en el test de exhaustividad de plantillas cargadas.

5. **Validación:**
    - `.\gradlew.bat :prompt-template:test`.
    - `.\gradlew.bat validateStructure`.
    - `.\gradlew.bat test`.

### 2.2 Qué NO hacer

* No alterar las plantillas existentes (`fase1-propuesta-inicial.md`, etc.).
* No introducir dependencias externas en `:prompt-template` ni en `:model`.
* No dejar placeholders sin documentar o sin validar en las pruebas unitarias.

### 2.3 Criterios de Aceptación

- [ ] `PromptTemplateId.PROGRAM_PLANNING` declarado y documentado.
- [ ] `program-planning-roadmap.md` creado con soporte estricto para **DP-PL-02** y **DP-PL-04**.
- [ ] `ClasspathPromptTemplateAdapter` carga y renderiza la plantilla exitosamente.
- [ ] Cobertura de pruebas unitarias en `:prompt-template` mantenida en $\ge 90\%$.
- [ ] 100% de pruebas del proyecto pasando (`.\gradlew.bat test`).
- [ ] `validateStructure` pasa sin advertencias.
