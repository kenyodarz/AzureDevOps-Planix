# FASE 04 — Modelos de Dominio para Program Planning (`domain/model`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01, FASE 02, FASE 03 · **Riesgo:** Muy Bajo  
> **Commit al cerrar:**
> `feat(agent): agregar modelos de dominio y resolutor para program planning`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* La **Fase 01** creó el modelo `SpecDocument` y el puerto `SpecStoragePort` en `domain/model`.
* La **Fase 02** proveyó el adaptador `FileSystemSpecAdapter` para almacenamiento no bloqueante de
  specs en Markdown.
* La **Fase 03** migró `PlanningDraftFlowHandler` para consumir `SpecStoragePort`, inyectar specs
  completos y resolver el frente por prefijo con degradación elegante.

### 1.2 Problema que resuelve esta fase

* Actualmente, el agente únicamente reconoce intenciones para historias de usuario individuales
  (`AgentIntent.PLANNING_DRAFT`, `REFINEMENT`, `APPROVAL`, etc.).
* Para orquestar la planeación macro a nivel de programa (Program Planning y generación de roadmaps
  trimestrales/sprints con distribución de HU y HA bajo **DP-PL-02** y **DP-PL-04**), el dominio
  carece de los modelos de datos, enums y reglas de resolución de intención necesarios.
* Es indispensable modelar las entidades y Value Objects inmutables en `domain/model` antes de
  construir los prompts y el caso de uso del planner en las Fases 05 y 06.

### 1.3 Estado esperado al terminar

* Adición de `AgentIntent.PROGRAM_PLANNING` en el enum `AgentIntent`.
* Creación de `ActivityType` en `co.com.bancolombia.model.planning` (`USER_STORY` con tag `"HU"` y
  `ENABLER` con tag `"HA"` conforme a **DP-PL-02**).
* Creación de los records inmutables:
    - `SprintAllocation`: representa la asignación de una HU o HA a un sprint específico con título,
      puntos Fibonacci, frente y dependencias.
    - `ProgramPlanRequest`: encapsula los parámetros de entrada para una planeación (trimestre/Q,
      objetivos, frentes, capacidad).
    - `ProgramPlanResult`: resultado estructurado con el resumen ejecutivo, la lista de asignaciones
      y las especificaciones por frente.
* Actualización de `IntentResolver` para detectar comandos y frases clave de planeación de programa
  (ej. `"/plan"`, `"planear q3"`, `"generar roadmap"`, `"planificar trimestre"`).
* Cobertura de pruebas unitarias $\ge 90\%$ en `domain/model` para todos los nuevos modelos y
  resoluciones de intención.

### 1.4 Archivos involucrados

| Ruta                                                                                          |  Acción   | Propósito                                                                                 |
|:----------------------------------------------------------------------------------------------|:---------:|:------------------------------------------------------------------------------------------|
| `domain/model/src/main/java/co/com/bancolombia/model/agent/AgentIntent.java`                  | MODIFICAR | Agregar el valor `PROGRAM_PLANNING`.                                                      |
| `domain/model/src/main/java/co/com/bancolombia/model/planning/ActivityType.java`              |   CREAR   | Enum que tipifica actividades entre Historias de Usuario (HU) y Habilitadoras (HA).       |
| `domain/model/src/main/java/co/com/bancolombia/model/planning/SprintAllocation.java`          |   CREAR   | Record inmutable que modela la asignación de una actividad a un sprint.                   |
| `domain/model/src/main/java/co/com/bancolombia/model/planning/ProgramPlanRequest.java`        |   CREAR   | Record inmutable con los parámetros de entrada de planeación trimestral.                  |
| `domain/model/src/main/java/co/com/bancolombia/model/planning/ProgramPlanResult.java`         |   CREAR   | Record inmutable con el resultado estructurado de la planeación.                          |
| `domain/model/src/main/java/co/com/bancolombia/model/agent/IntentResolver.java`               | MODIFICAR | Detectar la intención `PROGRAM_PLANNING` con la precedencia adecuada.                     |
| `domain/model/src/test/java/co/com/bancolombia/model/planning/ProgramPlanningModelsTest.java` |   CREAR   | Pruebas unitarias de invariantes, serialización y métodos de conveniencia de los modelos. |
| `domain/model/src/test/java/co/com/bancolombia/model/agent/IntentResolverTest.java`           | MODIFICAR | Casos de prueba para resolución de `PROGRAM_PLANNING`.                                    |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §1 (`domain/model`): Núcleo de negocio puro sin dependencias ni anotaciones de Spring, Jackson
      o JPA.
    - §4 (Calidad de Código): Invariantes en constructores compactos de records; validación
      defensiva con `IllegalArgumentException`.
    - §5 (Testing): Patrón GIVEN/WHEN/THEN con AssertJ.

### 1.6 Decisiones aplicadas

- **DP-PL-02 (Separación HU vs HA):**  
  `ActivityType` define explícitamente `USER_STORY` (construir funcionalidad hasta QA) y `ENABLER`
  (habilitación operativa, setups y paso a producción con HyMS).
- **DP-PL-04 (Salida de Planeación Estructurada):**  
  `ProgramPlanResult` y `SprintAllocation` proporcionan el modelo tipado que soportará la generación
  de `ideas_planning_QX.md` y los specs por frente.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Actualizar `AgentIntent.java`:**
   Agregar el valor `PROGRAM_PLANNING` con su respectiva documentación Javadoc.

2. **Crear `ActivityType.java`:**
   En `co.com.bancolombia.model.planning`:
    - Valores `USER_STORY("HU")` y `ENABLER("HA")`.
    - Método `fromTag(String tag)` y getter `tag()`.

3. **Crear `SprintAllocation.java`:**
   En `co.com.bancolombia.model.planning`:
    - Campos: `int sprintNumber`, `ActivityType type`, `String title`, `String description`,
      `int storyPoints`, `String front`, `List<String> dependencies`.
    - Validar en constructor compacto: `sprintNumber > 0`, `type != null`,
      `title != null && !title.isBlank()`, `storyPoints >= 0`, `dependencies` inmutable (copia
      defensiva, nunca nula).

4. **Crear `ProgramPlanRequest.java`:**
   En `co.com.bancolombia.model.planning`:
    - Campos: `String quarter`, `String objectives`, `List<String> targetFronts`, `int sprintCount`,
      `int maxCapacityPerSprint`.
    - Constructor compacto con validaciones de no-nulos y listas inmutables.

5. **Crear `ProgramPlanResult.java`:**
   En `co.com.bancolombia.model.planning`:
    - Campos: `String quarter`, `String executiveSummary`, `List<SprintAllocation> allocations`,
      `List<String> generatedSpecNames`.

6. **Actualizar `IntentResolver.java`:**
    - Incorporar detección de `PROGRAM_PLANNING` antes del fallback de texto libre:
        - Comandos explícitos: `"/plan"`, `"/roadmap"`, `"/program-planning"`.
        - Palabras clave compuestas o expresiones: `"planear q"`, `"planificar q"`, `"roadmap q"`,
          `"planeacion q"`.

7. **Crear y actualizar pruebas unitarias:**
    - Crear `ProgramPlanningModelsTest.java` verificando construcciones válidas e inválidas
      (invariantes de cada record y enum).
    - En `IntentResolverTest.java`, agregar casos de prueba GIVEN/WHEN/THEN para `PROGRAM_PLANNING`.

8. **Validación:**
    - `.\gradlew.bat :model:test` con 100% pasando y cobertura $\ge 90\%$.
    - `.\gradlew.bat validateStructure`.
    - `.\gradlew.bat test`.

### 2.2 Qué NO hacer

* No agregar anotaciones de Jackson (`@JsonProperty`), JPA ni Spring en `domain/model`.
* No alterar los otros valores ni el orden de resolución de intenciones existentes en
  `IntentResolver`.
* No introducir dependencias externas adicionales en `:model`.

### 2.3 Criterios de Aceptación

- [ ] `AgentIntent.PROGRAM_PLANNING` integrado y documentado.
- [ ] Modelos `ActivityType`, `SprintAllocation`, `ProgramPlanRequest`, `ProgramPlanResult` creados
  con invariantes.
- [ ] `IntentResolver` resuelve `PROGRAM_PLANNING` con precisión.
- [ ] Cobertura de pruebas unitarias en `domain/model` $\ge 90\%$.
- [ ] 100% de pruebas del proyecto pasando (`.\gradlew.bat test`).
- [ ] `validateStructure` pasa sin advertencias.
