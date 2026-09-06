# FASE 02 — Modelos de Dominio para Program Planning (`domain/model`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 01 · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(backend): crear modelos de dominio inmutables para program planning`  
> **Siguiente fase:** `fases/FASE-03-adaptador-filesystem-spec.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

En la **Fase 01** se crearon las abstracciones documentales fundamentales (`SpecDocument`,
`SpecNotFoundException` y `SpecStoragePort`) en `co.com.bancolombia.model.spec`.

### 1.2 Problema que resuelve esta fase

El subdominio `planning` en `azure-devops-backend` solo cuenta con la entidad legado
`PlanningChunk`, diseñada para fragmentos vectoriales de Postgres. Carece de los modelos puros de
dominio necesarios para representar la solicitud, el comando hacia el agente, la asignación de
sprints y el resultado estructurado de la planeación trimestral macro (Program Planning).

### 1.3 Estado esperado al terminar

Existirán en `domain/model` (`co.com.bancolombia.model.planning`):

1. El enum `ActivityType` tipando las actividades de planeación (`HU` para historias de usuario,
   `HA` para habilitadores de arquitectura).
2. El record inmutable `SprintAllocation` que modela la asignación de una actividad a un sprint.
3. El record inmutable `ProgramPlanRequest` con validaciones fail-fast de invariantes de negocio.
4. El record inmutable `ProgramPlanResult` con métodos de consulta y agregación de capacidad
   (`totalStoryPoints`, filtros por sprint o tipo).
5. El record inmutable `ProgramPlanningCommand` que materializa la decisión **DP-BFF-03**
   (serialización canónica a `/plan [Quarter] [Sprints] [Capacidad] [Frentes]`).
6. Pruebas unitarias exhaustivas con JUnit 5 y AssertJ en
   `domain/model/src/test/java/.../planning/`.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Tareas a Ejecutar

#### T-01 · Crear el Enum `ActivityType`

**Ubicación:** `domain/model/src/main/java/co/com/bancolombia/model/planning/ActivityType.java`

- Valores: `HU` (Historia de Usuario), `HA` (Habilitador de Arquitectura).
- Métodos auxiliares para parseo tolerante a mayúsculas/minúsculas y prefijos.

#### T-02 · Crear el Record `SprintAllocation`

**Ubicación:** `domain/model/src/main/java/co/com/bancolombia/model/planning/SprintAllocation.java`

- Atributos: `int sprintNumber`, `String initiativeId`, `String title`, `int storyPoints`,
  `ActivityType type`, `String front`.
- Constructor compacto con validación: `sprintNumber > 0`, `storyPoints > 0`, `title` no nulo ni
  blanco, `type` no nulo.

#### T-03 · Crear el Record `ProgramPlanRequest`

**Ubicación:**
`domain/model/src/main/java/co/com/bancolombia/model/planning/ProgramPlanRequest.java`

- Atributos: `String quarter`, `String objectives`, `List<String> targetFronts`, `int sprintCount`,
  `int maxCapacityPerSprint`.
- Constructor compacto con validaciones fail-fast: no nulabilidad, enteros estrictamente positivos,
  lista inmutable.

#### T-04 · Crear el Record `ProgramPlanResult`

**Ubicación:** `domain/model/src/main/java/co/com/bancolombia/model/planning/ProgramPlanResult.java`

- Atributos: `String quarter`, `String executiveSummary`, `List<SprintAllocation> allocations`,
  `List<String> generatedSpecNames`.
- Métodos de dominio:
    - `int totalStoryPoints()`
    - `List<SprintAllocation> allocationsForSprint(int sprintNumber)`
    - `List<SprintAllocation> allocationsForType(ActivityType type)`

#### T-05 · Crear el Value Object `ProgramPlanningCommand` (Decisión DP-BFF-03)

**Ubicación:**
`domain/model/src/main/java/co/com/bancolombia/model/planning/ProgramPlanningCommand.java`

- Encapsula la solicitud y genera el payload canónico para despachar al Agente vía JSON-RPC 2.0 /
  `TrackAgentTaskUseCase`.
- Método `String toCanonicalCommand()` que produce:  
  `/plan [quarter] [sprintCount] [maxCapacityPerSprint] [fronts...]`

#### T-06 · Suite de Pruebas Unitarias Exhaustivas

**Ubicación:** `domain/model/src/test/java/co/com/bancolombia/model/planning/`

- `ProgramPlanRequestTest.java`
- `ProgramPlanResultTest.java`
- `ProgramPlanningCommandTest.java`
- `SprintAllocationTest.java`
- Cobertura $\ge 90\%$ y 100% de ramas cubiertas.

---

## 3. CRITERIOS DE ACEPTACIÓN

- [ ] Modelos de dominio inmutables y 100% puros (cero anotaciones técnicas).
- [ ] Suite de pruebas pasando al 100% (`./gradlew :model:test` y `./gradlew test`).
- [ ] Cero regresiones en los 247 tests existentes.
- [ ] Generación de `RESULTADO-FASE-02.md`, actualización de `ESTADO.md` y especificación de
  `FASE-03`.
