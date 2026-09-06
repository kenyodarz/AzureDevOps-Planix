# PROMPT DE EJECUCIÓN — FASE 06: Caso de Uso y Handler del Planner Agent

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 06.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring AI, patrones de orquestación de LLMs y testing unitario con JUnit 5, Mockito y AssertJ.

Vamos a ejecutar la FASE 06 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-06-handler-planner-agent.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-06:
   - Implementar el caso de uso puro `ProgramPlanningUseCase` en `domain/usecase/src/main/java/co/com/bancolombia/usecase/planning/`:
     * Orquestar la lectura de specs documentales existentes vía `SpecStoragePort`.
     * Renderizar el prompt con `PromptTemplatePort` y `PromptTemplateId.PROGRAM_PLANNING`.
     * Invocar al LLM vía `ChatGateway`.
     * Parsea la respuesta del LLM para extraer el resumen, las asignaciones (`SprintAllocation`) validando DP-PL-02 (HU vs HA) y calcular métricas.
     * Persistir el roadmap maestro `ideas_planning_{quarter}.md` a través de `SpecStoragePort` (DP-PL-04).
   - Implementar el handler conversacional `ProgramPlanningFlowHandler` en `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/` para atender `AgentIntent.PROGRAM_PLANNING`.
   - Crear las pruebas unitarias exhaustivas con Mockito y AssertJ para `ProgramPlanningUseCaseTest` y `ProgramPlanningFlowHandlerTest`.

3. Restricciones no negociables de arquitectura:
   - `domain/usecase` debe ser 100% puro: cero anotaciones de Spring (`@Service`, `@Component`), inyección estricta por constructor.
   - Mantener las 391 pruebas existentes pasando sin regresiones.
   - Manejo defensivo ante respuestas nulas, vacías o malformadas del LLM.

4. Compilación y Validación:
   - Ejecutar `./gradlew :usecase:test` y `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que `validateStructure` apruebe sin advertencias.
   - Mantener cobertura en `domain/usecase` >= 90%.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-06.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 06 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-07-prompts-story-creator-hyms.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-07.md`.
   - Presentar el resumen de cambios para confirmación del commit.
```
