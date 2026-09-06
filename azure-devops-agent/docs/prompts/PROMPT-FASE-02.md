# PROMPT DE EJECUCIÓN — FASE 02: Adaptador FileSystemSpecAdapter

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 02.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Programación Reactiva (Project Reactor), Spring Boot y testing unitario.

Vamos a ejecutar la FASE 02 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-02-adaptador-filesystem-spec.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-02:
   - Configurar el nuevo submódulo driven-adapter `:spec-storage` en `settings.gradle` y en `applications/app-service/build.gradle`.
   - Crear `infrastructure/driven-adapters/spec-storage/build.gradle`.
   - Implementar `co.com.bancolombia.spec.storage.FileSystemSpecAdapter` implementando el puerto `SpecStoragePort` con manejo reactivo no bloqueante derivado a `Schedulers.boundedElastic()`.
   - Crear `co.com.bancolombia.spec.storage.FileSystemSpecAdapterTest` con JUnit 5, `@TempDir` y `StepVerifier` cubriendo lectura exitosa, archivo no existente (`SpecNotFoundException`), protección contra Path Traversal, listado de specs y guardado.

3. Restricciones no negociables de arquitectura:
   - El adaptador pertenece a `infrastructure/driven-adapters` y depende de `:model`.
   - `domain/model` no se modifica en esta fase (permanece 100% puro e intacto).
   - Ninguna operación de lectura/escritura en disco debe bloquear el hilo reactivo; usar siempre `Schedulers.boundedElastic()`.
   - Mantener las 314 pruebas existentes pasando sin regresiones.

4. Compilación y Validación:
   - Ejecutar `./gradlew :spec-storage:test` y `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que la tarea `validateStructure` del plugin Clean Architecture de Bancolombia apruebe sin advertencias.
   - Verificar que la cobertura en `:spec-storage` sea >= 90%.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-02.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 02 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-03-migracion-draft-handler.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-03.md`.
   - Si se detecta algún hallazgo técnico relevante, registrarlo en el resultado y ajustar el plan según corresponda.
   - Presentar el resumen de cambios para confirmación del commit.
```
