# PROMPT DE EJECUCIÓN — FASE 04: Casos de Uso de Planeación y Specs

> Utiliza este prompt estructurado para iniciar la ejecución de la Fase 04 en
> `azure-devops-backend`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Domain-Driven Design (DDD) y programación reactiva.

Vamos a ejecutar la FASE 04 del Plan de Integración de Program Planning en el BFF (azure-devops-backend):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-04-casos-de-uso-planeacion.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 a T-03 de FASE-04:
   - Implementar GetSpecDocumentUseCase y ListAvailableSpecsUseCase en co.com.bancolombia.usecase.spec consumiendo SpecStoragePort.
   - Implementar TriggerProgramPlanningUseCase en co.com.bancolombia.usecase.planning delegando en TrackAgentTaskUseCase con el comando canónico A2A.
   - Crear las pruebas unitarias con Mockito y StepVerifier en domain/usecase con cobertura >= 90%.

3. Restricciones no negociables de arquitectura:
   - Cero anotaciones de Spring en domain/usecase.
   - Mantener las 376 pruebas unitarias existentes pasando al 100%.

4. Compilación y Validación:
   - Ejecutar ./gradlew :usecase:test
   - Ejecutar ./gradlew test
   - Verificar que validateStructure apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-04.md con mediciones reales.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 04 como COMPLETADA y activar Fase 05).
   - Generar docs/fases/FASE-05-entry-points-reactivos-planning.md y docs/prompts/PROMPT-FASE-05.md.
   - Presentar el resumen de cambios para confirmación del commit.
```
