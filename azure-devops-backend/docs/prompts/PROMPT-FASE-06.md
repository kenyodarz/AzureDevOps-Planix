# PROMPT DE EJECUCIÓN — FASE 06: Cableado en Spring, Validación E2E y Cierre Definitivo

> Utiliza este prompt estructurado para iniciar la ejecución de la Fase 06 en
> `azure-devops-backend`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring Boot y Domain-Driven Design (DDD).

Vamos a ejecutar la FASE 06 (Cierre Definitivo) del Plan de Integración de Program Planning en el BFF (azure-devops-backend):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-06-cableado-spring-validacion-e2e.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 a T-04 de FASE-06:
   - Registrar los @Bean de GetSpecDocumentUseCase, ListAvailableSpecsUseCase y TriggerProgramPlanningUseCase en applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java.
   - Configurar la propiedad specs.storage.path en applications/app-service/src/main/resources/application.yaml.
   - Verificar y actualizar las pruebas de configuración y contexto en applications/app-service/src/test/java/.
   - Ejecutar la suite completa de pruebas y validación estructural.

3. Restricciones no negociables de arquitectura:
   - Cero anotaciones de Spring en domain/usecase (el wiring se realiza estrictamente en app-service).
   - Mantener todas las pruebas unitarias existentes (408+) pasando al 100%.

4. Compilación y Validación:
   - Ejecutar ./gradlew :app-service:test
   - Ejecutar ./gradlew test
   - Verificar que validateStructure apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-06.md con mediciones reales.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 06 como COMPLETADA y cerrar el plan maestro).
   - Presentar el resumen final de la integración y propuesta de commit.
```
