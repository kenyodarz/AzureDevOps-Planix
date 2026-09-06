# PROMPT DE EJECUCIÓN — FASE 08: Orquestación y Despacho del Harness

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 08.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring Boot, inyección de dependencias y testing con JUnit 5 y AssertJ.

Vamos a ejecutar la FASE 08 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-08-orquestacion-despacho-harness.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-08:
   - Integrar y cablear `ProgramPlanningFlowHandler` en el contenedor Spring:
     * Registrar el bean `@Bean public ProgramPlanningFlowHandler programPlanningFlowHandler(ProgramPlanningUseCase programPlanningUseCase)` en `UseCasesConfig.java` (`applications/app-service`).
   - Eliminar el bypass/exclusión temporal en `ChatFlowDispatcher.requireExhaustive` (`domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/ChatFlowDispatcher.java`):
     * Exigir que todas las intenciones de `AgentIntent` (incluyendo `PROGRAM_PLANNING`) tengan un handler registrado de manera estricta y exhaustiva.
   - Actualizar las pruebas unitarias y de configuración:
     * Actualizar `ChatFlowDispatcherTest` para incluir `ProgramPlanningFlowHandler` en los escenarios de exhaustividad.
     * Actualizar `UseCasesConfigWiringTest` en `app-service` para verificar la existencia del bean `programPlanningFlowHandler`.

3. Restricciones no negociables de arquitectura:
   - Respetar la pureza del dominio (`domain/model` y `domain/usecase` sin anotaciones técnicas de Spring).
   - Inyección por constructor y wiring exclusivo en `applications/app-service`.
   - Mantener las 404 pruebas existentes pasando sin regresiones.

4. Compilación y Validación:
   - Ejecutar `./gradlew :usecase:test` y `./gradlew :app-service:test`.
   - Ejecutar `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que `validateStructure` apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-08.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 08 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-09-validacion-e2e-cierre.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-09.md`.
   - Presentar el resumen de cambios para confirmación del commit.
```
