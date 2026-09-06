# FASE 08 — Orquestación y Despacho del Harness

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 06, FASE 07 · **Riesgo:** Bajo  
> **Commit al cerrar:**  
> `feat(agent): integrar y despachar flujo de program planning en el harness de agentes`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* La **Fase 06** construyó el caso de uso `ProgramPlanningUseCase` y el handler de flujo
  `ProgramPlanningFlowHandler` en `domain/usecase`.
* La **Fase 07** especializó las directivas normativas de **DP-PL-02** en la plantilla estructurada
  de historias (`fase2-historia-estructurada.md`), asegurando que tanto el Roadmap como la
  generación de historias downstream respeten la delimitación de HUs (hasta QA) y HAs (marcos HyMS).
* Sin embargo, `ProgramPlanningFlowHandler` no está aún registrado como bean en `UseCasesConfig`
  (`applications/app-service`), y `ChatFlowDispatcher` contiene una excepción explícita
  (`if (intent == AgentIntent.PROGRAM_PLANNING) continue;`) que puentea la verificación de
  exhaustividad al arrancar.

### 1.2 Problema que resuelve esta fase

* Cuando un usuario o sistema externo interactúa con el agente a través del canal conversacional
  (`AgentChatUseCase`) solicitando una planeación (`/plan`, `/roadmap` o lenguaje natural afín), el
  despachador central `ChatFlowDispatcher` debe enrutar la petición a `ProgramPlanningFlowHandler`.
* Es necesario cerrar el circuito de orquestación y despacho del Harness:
    1. Registrar `ProgramPlanningFlowHandler` en `UseCasesConfig` con todas sus dependencias puras
       (`ProgramPlanningUseCase`, etc.).
    2. Eliminar la exclusión temporal en
       `ChatFlowDispatcher.requireExhaustive(Map<AgentIntent, ChatFlowHandler>)`, consagrando que
       los 7 intents del dominio son atendidos de forma exhaustiva y estricta en tiempo de arranque.
    3. Validar el despacho y cableado en pruebas de integración livianas
       (`UseCasesConfigWiringTest`) y pruebas de despacho (`ChatFlowDispatcherTest` y
       `AgentChatUseCaseTest`).

### 1.3 Estado esperado al terminar

* `ProgramPlanningFlowHandler` expuesto como bean de Spring en `UseCasesConfig` e inyectado en
  `ChatFlowDispatcher`.
* `ChatFlowDispatcher` 100% exhaustivo para todos los valores del enum `AgentIntent` (incluido
  `PROGRAM_PLANNING`), sin omisiones ni excepciones condicionales.
* Pruebas de cableado en `UseCasesConfigWiringTest` verificando la presencia y funcionamiento del
  bean `programPlanningFlowHandler`.
* 100% de pruebas pasando en todo el proyecto (mínimo 404 pruebas) y cero regresiones.

### 1.4 Archivos involucrados

| Ruta                                                                                               |  Acción   | Propósito                                                                                |
|:---------------------------------------------------------------------------------------------------|:---------:|:-----------------------------------------------------------------------------------------|
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/ChatFlowDispatcher.java`     | MODIFICAR | Remover bypass temporal de `PROGRAM_PLANNING` y exigir exhaustividad absoluta.           |
| `domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/handler/ChatFlowDispatcherTest.java` | MODIFICAR | Actualizar pruebas unitarias de exhaustividad incorporando `ProgramPlanningFlowHandler`. |
| `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`             | MODIFICAR | Registrar bean de `ProgramPlanningFlowHandler`.                                          |
| `applications/app-service/src/test/java/co/com/bancolombia/config/UseCasesConfigWiringTest.java`   | MODIFICAR | Validar que el contexto levante con el nuevo handler cableado.                           |
| `docs/plan/ESTADO.md`                                                                              | MODIFICAR | Actualizar tablero y bitácora del plan.                                                  |
| `docs/resultados/RESULTADO-FASE-08.md`                                                             |   CREAR   | Documento de cierre con métricas de la Fase 08.                                          |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §1 (`applications/app-service`): Wiring explícito en clases de configuración con
      `@Configuration` y `@Bean`.
    - §3 (Principios SOLID): Dependency Inversion y Single Responsibility en el orquestador y
      despachador.
    - §5 (Testing): Pruebas de integración de wiring (`@ContextConfiguration` / Spring slice)
      limpias.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. En `ChatFlowDispatcher.java`:
    - Eliminar el bloque `if (intent == AgentIntent.PROGRAM_PLANNING) continue;` en
      `requireExhaustive`.
2. En `UseCasesConfig.java`:
    - Declarar el bean
      `@Bean public ProgramPlanningFlowHandler programPlanningFlowHandler(ProgramPlanningUseCase programPlanningUseCase)`.
3. En `ChatFlowDispatcherTest.java`:
    - Actualizar los tests unitarios para incluir un mock o stub de `ProgramPlanningFlowHandler` al
      validar el índice de intenciones.
4. En `UseCasesConfigWiringTest.java`:
    - Aserción de que el bean `programPlanningFlowHandler` existe en el `ApplicationContext`.
5. Ejecutar `./gradlew test` y `./gradlew validateStructure`.
6. Generar el artefacto de resultado `RESULTADO-FASE-08.md` y preparar la Fase 09
   (`Validación E2E con Caso Real Q3-2026 y Cierre`).

### 2.2 Qué NO hacer

* No alterar contratos ni clases de `domain/model`.
* No añadir lógica de negocio en `UseCasesConfig` (solo wiring).
* No alterar los otros 6 handlers de flujo existentes.

### 2.3 Criterios de Aceptación

- [ ] `ChatFlowDispatcher` exige exhaustividad total de `AgentIntent.values()`.
- [ ] `ProgramPlanningFlowHandler` cableado como bean en `UseCasesConfig`.
- [ ] `UseCasesConfigWiringTest` pasa exitosamente verificando la presencia del nuevo bean.
- [ ] 100% de pruebas pasando en todo el proyecto.
- [ ] `validateStructure` pasa sin advertencias.
