# FASE 04 — Casos de Uso de Gestión Documental y Planeación (`domain/usecase`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 01, Fase 02, Fase 03 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(backend): implementar casos de uso para gestion documental y program planning`  
> **Siguiente fase:** `fases/FASE-05-entry-points-reactivos-planning.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- En la **Fase 01** se construyeron los modelos y el puerto `SpecStoragePort` en
  `domain/model/spec`.
- En la **Fase 02** se implementaron los modelos inmutables de Program Planning
  (`ProgramPlanRequest`, `ProgramPlanResult`, `ProgramPlanningCommand`, `SprintAllocation`,
  `ActivityType`) en `domain/model/planning`.
- En la **Fase 03** se implementó y probó el adaptador `FileSystemSpecAdapter` en
  `infrastructure/driven-adapters/spec-storage`.

### 1.2 Problema que resuelve esta fase

Los modelos de dominio y el puerto de infraestructura ya existen, pero la lógica de orquestación y
reglas de aplicación del negocio aún no están implementadas en la capa de aplicación/casos de uso
(`domain/usecase`).
Se requiere:

1. Orquestar la lectura íntegra de especificaciones y el listado de documentos de especificación a
   través de `SpecStoragePort`.
2. Implementar el caso de uso `TriggerProgramPlanningUseCase` que resuelva la decisión
   **DP-BFF-03**, validando el `ProgramPlanRequest`, generando el `ProgramPlanningCommand` canónico
   y despachándolo de forma asíncrona hacia el Agente autónomo a través de
   `TrackAgentTaskUseCase.sendAndTrack(...)`.

### 1.3 Estado esperado al terminar

Existirán en `domain/usecase`:

1. `co.com.bancolombia.usecase.spec.GetSpecDocumentUseCase`:
    - Consume `SpecStoragePort`.
    - Método `Mono<SpecDocument> execute(String specName)`.
    - Valida `specName` y propaga errores correspondientes.
2. `co.com.bancolombia.usecase.spec.ListAvailableSpecsUseCase`:
    - Consume `SpecStoragePort`.
    - Método `Flux<String> execute()`.
3. `co.com.bancolombia.usecase.planning.TriggerProgramPlanningUseCase`:
    - Inyecta `TrackAgentTaskUseCase`.
    - Método `Mono<AgentInteraction> execute(ProgramPlanRequest request)` (o sobrecargado con
      `contextId`).
    - Construye `ProgramPlanningCommand` canónico, genera un `AgentCommand.nonBlocking(...)` y lo
      envía a través de `TrackAgentTaskUseCase.sendAndTrack(...)`.
4. Pruebas unitarias completas con Mockito en JUnit 5 y `StepVerifier` con cobertura $\ge 90\%$ y
   cero dependencias de Spring.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Tareas a Ejecutar

#### T-01 · Implementar Casos de Uso Documentales

**Ubicación:** `domain/usecase/src/main/java/co/com/bancolombia/usecase/spec/`

- `GetSpecDocumentUseCase.java`
- `ListAvailableSpecsUseCase.java`

#### T-02 · Implementar Caso de Uso de Planeación

**Ubicación:** `domain/usecase/src/main/java/co/com/bancolombia/usecase/planning/`

- `TriggerProgramPlanningUseCase.java`

#### T-03 · Pruebas Unitarias en `domain/usecase`

**Ubicación:** `domain/usecase/src/test/java/co/com/bancolombia/usecase/`

- `spec/GetSpecDocumentUseCaseTest.java`
- `spec/ListAvailableSpecsUseCaseTest.java`
- `planning/TriggerProgramPlanningUseCaseTest.java`

---

## 3. CRITERIOS DE ACEPTACIÓN

- [ ] Cero anotaciones de Spring en `domain/usecase`.
- [ ] Inyección de dependencias mediante constructores puros (`@RequiredArgsConstructor`).
- [ ] `./gradlew :usecase:test` pasa al 100% con cobertura $\ge 90\%$.
- [ ] `./gradlew test` mantiene todas las pruebas pasando sin regresiones.
- [ ] `validateStructure` aprueba sin advertencias.
- [ ] Generación de `RESULTADO-FASE-04.md` y actualización de `ESTADO.md`.
