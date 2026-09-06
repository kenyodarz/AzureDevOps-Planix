# PROMPT DE EJECUCIÓN — FASE 05: Prompt Externalizado de Planeación (`roadmap`)

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 05.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring AI, ingeniería de prompts y testing unitario con JUnit 5 y AssertJ.

Vamos a ejecutar la FASE 05 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-05-prompt-externalizado-planeacion.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-05:
   - Agregar el valor `PROGRAM_PLANNING` en `co.com.bancolombia.model.prompt.PromptTemplateId`.
   - Crear la plantilla de prompt externalizada `applications/app-service/src/main/resources/prompts/program-planning-roadmap.md`:
     * Aplicar las reglas de negocio de DP-PL-02: HU (hasta QA) vs HA (habilitación y paso a producción HyMS).
     * Aplicar el formato estructurado de DP-PL-04 (resumen ejecutivo, tablas de roadmap por sprints, puntos Fibonacci, dependencias y specs por frente).
     * Soportar los placeholders: `{{trimestre}}`, `{{objetivos}}`, `{{frentes}}`, `{{capacidadSprint}}` y `{{specsContexto}}`.
   - Registrar la plantilla en `co.com.bancolombia.prompttemplate.ClasspathPromptTemplateAdapter.TEMPLATE_FILES`.
   - Actualizar la suite de pruebas unitarias `ClasspathPromptTemplateAdapterTest` para verificar el renderizado completo y libre de placeholders residuales de `PROGRAM_PLANNING`.

3. Restricciones no negociables de arquitectura:
   - `domain/model` debe ser 100% puro: `PromptTemplateId` sin dependencias técnicas.
   - Mantener las 389 pruebas existentes pasando sin regresiones.
   - El adaptador debe validar la existencia y resolución de todas las variables requeridas.

4. Compilación y Validación:
   - Ejecutar `./gradlew :prompt-template:test` y `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que `validateStructure` apruebe sin advertencias.
   - Mantener cobertura en `:prompt-template` >= 90%.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-05.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 05 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-06-handler-planner-agent.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-06.md`.
   - Presentar el resumen de cambios para confirmación del commit.
```
