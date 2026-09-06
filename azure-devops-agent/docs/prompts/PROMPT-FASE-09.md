# PROMPT DE EJECUCIÓN — FASE 09: Validación E2E con Caso Real Q3-2026 y Cierre

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 09.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring Boot, WebFlux y testing de integración con JUnit 5 y WebTestClient.

Vamos a ejecutar la FASE 09 (Validación E2E y Cierre Final) del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-09-validacion-e2e-cierre.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-09:
   - Validar de extremo a extremo el flujo conversacional de Program Planning con el caso real de Q3-2026:
     * Añadir prueba E2E / integrativa en `infrastructure/entry-points/reactive-web` (`RouterRestTest.java`) que simule la petición HTTP/JSON-RPC con comando `/plan Q3-2026 6 sprints 34 sp [Canales]`.
     * Verificar que la respuesta retorne código 200, estado COMPLETED y cuerpo con la tabla Markdown de asignación de sprints, respetando la regla DP-PL-02 (HUs hasta QA y HAs HyMS).
     * Asegurar que se simule adecuadamente el guardado documental (`SpecStoragePort.saveSpec`) para el artefacto maestro `ideas_planning_q3_2026.md`.

3. Restricciones no negociables de arquitectura:
   - Respetar la arquitectura hexagonal y la segregación de responsabilidades.
   - No introducir dependencias técnicas ajenas ni romper contratos existentes.
   - Mantener las 406 pruebas existentes pasando al 100% sin regresiones.

4. Compilación y Validación:
   - Ejecutar `./gradlew :reactive-web:test`.
   - Ejecutar `./gradlew test` verificando 100% de pruebas aprobadas en todo el proyecto.
   - Verificar que `validateStructure` apruebe sin advertencias.

5. Entregables de Cierre Definitivo (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-09.md` con mediciones reales y consolidado final del Harness.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 09 como COMPLETADA y cerrar el plan maestro).
   - Presentar el resumen final de cambios para confirmación del commit.
```
