# FASE 09 — Validación E2E con Caso Real Q3-2026 y Cierre

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 08 · **Riesgo:** Bajo  
> **Commit al cerrar:**  
> `test(agent): validar e2e el flujo completo de program planning y cerrar el harness de agentes`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* En las **Fases 01 a 07** se construyó la infraestructura documental (`SpecStoragePort`,
  `FileSystemSpecAdapter`), la migración de `PlanningDraftFlowHandler`, el modelo de dominio de
  planeación (`ProgramPlanRequest`, `ProgramPlanResult`, `ActivityType`, `SprintAllocation`), la
  plantilla de prompt externalizada `program-planning-roadmap.md`, el caso de uso
  `ProgramPlanningUseCase`, el handler `ProgramPlanningFlowHandler`, y la especialización normativa
  HyMS/QA (**DP-PL-02**).
* En la **Fase 08** se integró formalmente `ProgramPlanningFlowHandler` en `UseCasesConfig`
  (`applications/app-service`) y se cerró la exhaustividad en `ChatFlowDispatcher` para todos los 7
  valores de `AgentIntent`.
* El sistema compila y cuenta con 406 pruebas unitarias y de integración pasando al 100%.

### 1.2 Problema que resuelve esta fase

* Aunque cada componente cuenta con pruebas unitarias, slices y tests de wiring, es indispensable
  certificar el **comportamiento integral de extremo a extremo (E2E)** con el caso de negocio real
  del banco: la planeación trimestral del **Q3-2026**.
* Es necesario validar que:
    1. Una petición entrante a través del entry-point HTTP
       (`infrastructure/entry-points/reactive-web`) invocando `/plan` o mensaje conversacional de
       planeación recorra la cadena completa:
       `RouterRest` -> `AgentChatUseCase` -> `IntentResolver` (`PROGRAM_PLANNING`) ->
       `ChatFlowDispatcher` -> `ProgramPlanningFlowHandler` -> `ProgramPlanningUseCase` ->
       `PromptTemplatePort` / `SpecStoragePort` / `ChatGateway`.
    2. El artefacto maestro Markdown del roadmap (`ideas_planning_q3_2026.md`) sea persistido
       fielmente vía `SpecStoragePort`.
    3. La respuesta entregada al cliente contenga el formato Markdown enriquecido con resumen
       ejecutivo, métricas, HUs y HAs delimitadas según **DP-PL-02** y tablas de asignación por
       sprint.
    4. Concluir y certificar el cierre formal del Plan Maestro del Harness en
       `azure-devops-agent/docs/plan/ESTADO.md`.

### 1.3 Estado esperado al terminar

* Prueba de integración / E2E en `infrastructure/entry-points/reactive-web` (o suite representativa
  de extremo a extremo) simulando el flujo conversacional completo de planeación para Q3-2026.
* Verificación de persistencia documental y respuesta estructurada.
* 100% de pruebas del proyecto pasando limpiamente sin regresiones.
* `validateStructure` aprobado según los estándares de Bancolombia Clean Architecture.
* Documento de resultados `RESULTADO-FASE-09.md` y tablero `ESTADO.md` actualizado con el 100% de
  las 9 fases en estado 🟢 **COMPLETADA**.

### 1.4 Archivos involucrados

| Ruta                                                                                                |  Acción   | Propósito                                                                                |
|:----------------------------------------------------------------------------------------------------|:---------:|:-----------------------------------------------------------------------------------------|
| `infrastructure/entry-points/reactive-web/src/test/java/co/com/bancolombia/api/RouterRestTest.java` | MODIFICAR | Añadir escenario E2E para el comando `/plan` verificando respuesta 200 y cuerpo Markdown |
| `docs/plan/ESTADO.md`                                                                               | MODIFICAR | Marcar Fase 09 como COMPLETADA y cerrar formalmente el Plan Maestro.                     |
| `docs/resultados/RESULTADO-FASE-09.md`                                                              |   CREAR   | Informe de cierre definitivo con métricas finales consolidadas del Harness.              |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §5 (Testing): Pruebas slice web con `@WebFluxTest` o `WebTestClient` simulando los usecases o
      gateways.
    - §7 (Checklist de Entrega): Cero credenciales, patrones Given/When/Then, 100% pruebas pasando.
* `COMMIT_RULES.md`: Formato `test(scope): descripción`.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. En `RouterRestTest.java` (o clase de prueba E2E correspondiente en `entry-points/reactive-web`):
    - Agregar prueba representativa:
      `givenPlanningRequest_whenPostCommand_thenReturnsRoadmapMarkdown`.
    - Simular el flujo A2A / JSON-RPC con `/plan Q3-2026 6 sprints 34 sp [Canales]`.
    - Verificar la estructura de la respuesta JSON-RPC / A2A conteniendo el estado `COMPLETED` y el
      cuerpo Markdown del roadmap.
2. Ejecutar `./gradlew test` y `./gradlew validateStructure`.
3. Elaborar `RESULTADO-FASE-09.md` documentando las métricas de cierre final de todo el plan.
4. Actualizar `ESTADO.md` consolidando el cierre del Harness de Agentes (Fases 01 a 09 completadas).

### 2.2 Qué NO hacer

* No alterar la lógica de negocio ni modelos ya aprobados en fases previas.
* No romper los tests existentes de `reactive-web`.
* No incorporar dependencias externas no autorizadas.

### 2.3 Criterios de Aceptación

- [ ] Prueba E2E / integrativa del comando `/plan` ejecutándose y pasando en `reactive-web`.
- [ ] 100% de pruebas pasando en todo el proyecto.
- [ ] `validateStructure` aprobado sin advertencias.
- [ ] Tablero de `ESTADO.md` con 9 de 9 fases completadas.
- [ ] Informe final de cierre `RESULTADO-FASE-09.md` creado.
