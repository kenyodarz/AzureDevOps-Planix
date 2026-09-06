# PROMPT DE EJECUCIÓN — FASE 05: Entry-Points Reactivos y DTOs de Planning

> Utiliza este prompt estructurado para iniciar la ejecución de la Fase 05 en
> `azure-devops-backend`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring WebFlux reactivo y Domain-Driven Design (DDD).

Vamos a ejecutar la FASE 05 del Plan de Integración de Program Planning en el BFF (azure-devops-backend):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-05-entry-points-reactivos-planning.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 a T-04 de FASE-05:
   - Crear los DTOs de entrada y salida en infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/dto/.
   - Implementar SpecHandler y ProgramPlanningHandler en infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/.
   - Configurar las nuevas rutas en RouterRest.java respetando DP-BFF-02 (coexistencia con rutas legadas).
   - Crear las pruebas unitarias con WebTestClient y Mockito en reactive-web con cobertura >= 90%.

3. Restricciones no negociables de arquitectura:
   - Cero lógica de negocio en entry points; delegación estricta a casos de uso.
   - Mantener las 391 pruebas unitarias existentes pasando al 100%.

4. Compilación y Validación:
   - Ejecutar ./gradlew :reactive-web:test
   - Ejecutar ./gradlew test
   - Verificar que validateStructure apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-05.md con mediciones reales.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 05 como COMPLETADA y activar Fase 06).
   - Generar docs/fases/FASE-06-cableado-spring-validacion-e2e.md y docs/prompts/PROMPT-FASE-06.md.
   - Presentar el resumen de cambios para confirmación del commit.
```
