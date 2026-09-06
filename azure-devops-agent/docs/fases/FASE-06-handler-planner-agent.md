# FASE 06 — Caso de Uso y Handler del Planner Agent

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 04, FASE 05 · **Riesgo:** Medio  
> **Commit al cerrar:**
> `feat(agent): implementar caso de uso y handler de flujo para program planning`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* La **Fase 01** creó el modelo `SpecDocument` y el puerto `SpecStoragePort` en `domain/model`.
* La **Fase 02** implementó `FileSystemSpecAdapter` para lectura y escritura de especificaciones
  Markdown.
* La **Fase 03** integró `SpecStoragePort` en `PlanningDraftFlowHandler` con resolución de frentes
  por prefijo.
* La **Fase 04** introdujo los modelos puros de dominio para Program Planning (`ActivityType`,
  `SprintAllocation`, `ProgramPlanRequest`, `ProgramPlanResult`) y el intent
  `AgentIntent.PROGRAM_PLANNING` en `IntentResolver`.
* La **Fase 05** externalizó la plantilla de prompt `program-planning-roadmap.md` con soporte para
  DP-PL-02 y DP-PL-04, e integró `PromptTemplateId.PROGRAM_PLANNING`.

### 1.2 Problema que resuelve esta fase

* Aunque el sistema reconoce la intención `PROGRAM_PLANNING` y dispone de los modelos puros y la
  plantilla externalizada, carece de la lógica de orquestación a nivel de aplicación que consuma los
  specs existentes, arme las variables dinámicas del prompt, consulte al LLM, procese la respuesta
  estructurada y persista los artefactos resultantes (`ideas_planning_QX.md` y especificaciones por
  frente) conforme a **DP-PL-04**.
* Es necesario implementar el caso de uso `ProgramPlanningUseCase` (o handler orquestador
  `ProgramPlanningFlowHandler`) en `domain/usecase` que integre `SpecStoragePort`,
  `PromptTemplatePort` y `ChatGateway`.

### 1.3 Estado esperado al terminar

* Creación de `ProgramPlanningUseCase` en
  `domain/usecase/src/main/java/co/com/bancolombia/usecase/planning/`:
    - Recibe `ProgramPlanRequest` con trimestre, objetivos, frentes y capacidad por sprint.
    - Carga el contexto de specs existentes a través de `SpecStoragePort`.
    - Renderiza el prompt usando `PromptTemplatePort` con `PromptTemplateId.PROGRAM_PLANNING`.
    - Invoca a `ChatGateway` para generar el roadmap estratégico.
    - Parsea la respuesta del LLM extrayendo las asignaciones por sprint (`SprintAllocation`),
      validando la regla de separación HU/HA (**DP-PL-02**).
    - Persiste el archivo maestro de planeación Markdown (`ideas_planning_{quarter}.md`) y los specs
      por frente mediante `SpecStoragePort` (**DP-PL-04**).
    - Retorna `ProgramPlanResult`.
* Creación de `ProgramPlanningFlowHandler` en
  `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/` que implementa
  `ChatFlowHandler` para atender `AgentIntent.PROGRAM_PLANNING`.
* Pruebas unitarias exhaustivas con Mockito y AssertJ en `ProgramPlanningUseCaseTest` y
  `ProgramPlanningFlowHandlerTest`.
* 100% de pruebas pasando y 0 violaciones de arquitectura en `validateStructure`.

### 1.4 Archivos involucrados

| Ruta                                                                                                       | Acción | Propósito                                                                      |
|:-----------------------------------------------------------------------------------------------------------|:------:|:-------------------------------------------------------------------------------|
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/planning/ProgramPlanningUseCase.java`             | CREAR  | Caso de uso principal de orquestación de Program Planning.                     |
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/ProgramPlanningFlowHandler.java`     | CREAR  | Handler del flujo conversacional para atender intenciones de planeación macro. |
| `domain/usecase/src/test/java/co/com/bancolombia/usecase/planning/ProgramPlanningUseCaseTest.java`         | CREAR  | Pruebas unitarias aisladas del caso de uso.                                    |
| `domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/handler/ProgramPlanningFlowHandlerTest.java` | CREAR  | Pruebas unitarias del handler conversacional.                                  |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §1 (`domain/usecase`): Clases puras sin anotaciones de Spring (`@Service`, `@Component`).
      Inyección por constructor con `final`.
    - §4 (Calidad de Código): Sin números mágicos, llaves explícitas, manejo seguro de opcionales.
    - §5 (Testing): Pruebas con patrón GIVEN/WHEN/THEN y Mockito.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. Implementar `ProgramPlanningUseCase` en `domain/usecase`:
    - Inyectar `PromptTemplatePort`, `SpecStoragePort` y `ChatGateway`.
    - Cargar contexto de specs por frente o generales.
    - Ensamblar variables (`trimestre`, `objetivos`, `frentes`, `capacidadSprint`, `specsContexto`)
      y renderizar `PROGRAM_PLANNING`.
    - Enviar prompt y parsear respuesta a `ProgramPlanResult`.
    - Guardar el documento del roadmap en `SpecStoragePort` (`ideas_planning_{quarter}.md`).
2. Implementar `ProgramPlanningFlowHandler`:
    - Responder al intent `AgentIntent.PROGRAM_PLANNING`.
    - Extraer argumentos de la conversación y delegar a `ProgramPlanningUseCase`.
3. Crear las suites de pruebas unitarias cubriendo casos de éxito, resiliencia y errores de parsing
   o storage.
4. Validar compilación, cobertura >= 90% en `:usecase` y ausencia de regresiones.

### 2.2 Qué NO hacer

* No introducir dependencias de Spring Boot en `domain/usecase`.
* No alterar la lógica de los otros flow handlers existentes.
* No asumir formatos ambiguos sin validación defensiva.

### 2.3 Criterios de Aceptación

- [ ] `ProgramPlanningUseCase` implementado y probado unitariamente.
- [ ] `ProgramPlanningFlowHandler` creado y cubierto con tests unitarios.
- [ ] Persistencia de `ideas_planning_QX.md` verificada mediante `SpecStoragePort`.
- [ ] Cobertura en `domain/usecase` mantenida en $\ge 90\%$.
- [ ] 100% de pruebas pasando en todo el proyecto.
- [ ] `validateStructure` pasa sin advertencias.
