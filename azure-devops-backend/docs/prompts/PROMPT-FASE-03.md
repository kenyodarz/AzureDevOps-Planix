# PROMPT DE EJECUCIÓN — FASE 03: Adaptador FileSystemSpecAdapter

> Utiliza este prompt estructurado para iniciar la ejecución de la Fase 03 en
> `azure-devops-backend`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Domain-Driven Design (DDD) y programación reactiva.

Vamos a ejecutar la FASE 03 del Plan de Integración de Program Planning en el BFF (azure-devops-backend):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-03-adaptador-filesystem-spec.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 a T-03 de FASE-03:
   - Configurar el submódulo Gradle :spec-storage en settings.gradle y build.gradle.
   - Implementar co.com.bancolombia.spec.storage.FileSystemSpecAdapter implementando SpecStoragePort con operaciones no bloqueantes (Schedulers.boundedElastic) y protección contra Path Traversal.
   - Crear co.com.bancolombia.spec.storage.FileSystemSpecAdapterTest con JUnit 5, @TempDir y StepVerifier cubriendo lectura, persistencia, listado y validaciones de seguridad.

3. Restricciones no negociables de arquitectura:
   - El adaptador pertenece a infrastructure/driven-adapters y debe implementar puramente SpecStoragePort.
   - Cero bloqueos en hilos de eventos Netty (compatible con BlockHound).
   - Mantener las 358 pruebas unitarias existentes pasando al 100%.

4. Compilación y Validación:
   - Ejecutar ./gradlew :spec-storage:test
   - Ejecutar ./gradlew test
   - Verificar que validateStructure apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-03.md con mediciones reales.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 03 como COMPLETADA y activar Fase 04).
   - Generar docs/fases/FASE-04-casos-de-uso-planeacion.md y docs/prompts/PROMPT-FASE-04.md.
   - Presentar el resumen de cambios para confirmación del commit.
```
