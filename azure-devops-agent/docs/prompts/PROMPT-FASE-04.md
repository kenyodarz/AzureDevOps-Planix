# PROMPT DE EJECUCIÓN — FASE 04: Modelos de Dominio para Program Planning

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 04.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Domain-Driven Design (DDD) y testing unitario con JUnit 5 y AssertJ.

Vamos a ejecutar la FASE 04 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-04-modelos-dominio-planeacion.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-04:
   - Agregar el valor `PROGRAM_PLANNING` en `co.com.bancolombia.model.agent.AgentIntent`.
   - Crear enum `co.com.bancolombia.model.planning.ActivityType` con soporte para HU (`USER_STORY`) y HA (`ENABLER`) según DP-PL-02.
   - Crear records inmutables en `co.com.bancolombia.model.planning`:
     * `SprintAllocation`: modela asignaciones a sprints (sprintNumber, activityType, title, description, storyPoints, front, dependencies).
     * `ProgramPlanRequest`: parámetros de entrada para planeación trimestral.
     * `ProgramPlanResult`: resultado estructurado con resumen, asignaciones y specs generados.
   - Actualizar `co.com.bancolombia.model.agent.IntentResolver` para reconocer comandos y frases clave de planeación de programa (`"/plan"`, `"planear q"`, etc.).
   - Crear suite de pruebas unitarias exhaustiva `ProgramPlanningModelsTest` y actualizar `IntentResolverTest` cubriendo la nueva intención y todos los invariantes de los modelos.

3. Restricciones no negociables de arquitectura:
   - `domain/model` debe ser 100% puro: sin anotaciones ni dependencias de Spring, Jackson o JPA.
   - Mantener las 334 pruebas existentes pasando sin regresiones.
   - Usar constructores compactos con validaciones defensivas de invariantes (`IllegalArgumentException`).

4. Compilación y Validación:
   - Ejecutar `./gradlew :model:test` y `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que `validateStructure` apruebe sin advertencias.
   - Mantener cobertura en `domain/model` >= 90%.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-04.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 04 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-05-prompt-externalizado-planeacion.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-05.md`.
   - Presentar el resumen de cambios para confirmación del commit.
```
