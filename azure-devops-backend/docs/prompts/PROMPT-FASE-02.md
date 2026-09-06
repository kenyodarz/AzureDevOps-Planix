# PROMPT DE EJECUCIÓN — FASE 02: Modelos de Dominio para Program Planning

> Utiliza este prompt estructurado para iniciar la ejecución de la Fase 02 en
> `azure-devops-backend`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Domain-Driven Design (DDD) y programación reactiva.

Vamos a ejecutar la FASE 02 del Plan de Integración de Program Planning en el BFF (azure-devops-backend):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-02-modelos-dominio-planeacion.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 a T-06 de FASE-02:
   - Crear ActivityType.java en co.com.bancolombia.model.planning.
   - Crear SprintAllocation.java en co.com.bancolombia.model.planning.
   - Crear ProgramPlanRequest.java en co.com.bancolombia.model.planning.
   - Crear ProgramPlanResult.java en co.com.bancolombia.model.planning.
   - Crear ProgramPlanningCommand.java en co.com.bancolombia.model.planning (materializando DP-BFF-03).
   - Implementar pruebas unitarias exhaustivas en domain/model/src/test/java/co/com/bancolombia/model/planning/.

3. Restricciones no negociables de arquitectura:
   - domain/model debe permanecer 100% puro: cero anotaciones de Spring, Jackson o JPA.
   - Inmutabilidad y validación fail-fast de parámetros no nulos.
   - Mantener las 247 pruebas unitarias existentes pasando al 100%.

4. Compilación y Validación:
   - Ejecutar ./gradlew :model:test
   - Ejecutar ./gradlew test
   - Verificar que validateStructure apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-02.md con mediciones reales.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 02 como COMPLETADA y activar Fase 03).
   - Generar docs/fases/FASE-03-adaptador-filesystem-spec.md y docs/prompts/PROMPT-FASE-03.md.
   - Presentar el resumen de cambios para confirmación del commit.
```
