# PROMPT DE EJECUCIÓN — FASE 01: Modelos y Puerto de Almacenamiento Documental

> Utiliza este prompt estructurado para iniciar la ejecución de la Fase 01 en `azure-devops-backend`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Domain-Driven Design (DDD) y programación reactiva con Project Reactor.

Vamos a ejecutar la FASE 01 del Plan de Integración de Program Planning en el BFF (azure-devops-backend):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-01-modelos-y-puerto-spec-storage.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 a T-04 de FASE-01:
   - Crear SpecDocument.java (record inmutable) en co.com.bancolombia.model.spec.
   - Crear SpecNotFoundException.java en co.com.bancolombia.model.spec.
   - Crear el puerto SpecStoragePort.java en co.com.bancolombia.model.spec.gateways.
   - Implementar pruebas unitarias exhaustivas en SpecDocumentTest.java.

3. Restricciones no negociables de arquitectura:
   - domain/model debe permanecer 100% puro: cero anotaciones de Spring, Jackson o JPA.
   - Inmutabilidad y validación fail-fast de parámetros no nulos.
   - Mantener las 238 pruebas unitarias existentes pasando al 100%.

4. Compilación y Validación:
   - Ejecutar ./gradlew :model:test
   - Ejecutar ./gradlew test
   - Verificar que validateStructure apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-01.md con mediciones reales.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 01 como COMPLETADA y activar Fase 02).
   - Generar docs/fases/FASE-02-modelos-dominio-planeacion.md y docs/prompts/PROMPT-FASE-02.md.
   - Presentar el resumen de cambios para confirmación del commit.
```
