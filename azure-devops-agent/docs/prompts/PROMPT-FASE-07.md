# PROMPT DE EJECUCIÓN — FASE 07: Especialización de Prompts Story Creator con Regla HyMS

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 07.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Spring AI, ingeniería de prompts y testing unitario con JUnit 5 y AssertJ.

Vamos a ejecutar la FASE 07 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-07-prompts-story-creator-hyms.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-07:
   - Especializar la plantilla de prompt `fase2-historia-estructurada.md` en `applications/app-service/src/main/resources/prompts/` incorporando las directivas corporativas de DP-PL-02:
     * Para Historias de Usuario (HU): Alcance estricto de desarrollo, integración y pruebas hasta ambiente QA. Prohibido incluir despliegue a producción o procesos operativos.
     * Para Historias Habilitadoras (HA): Setups de arquitectura/infraestructura o habilitación operativa bajo el marco formal HyMS (Runbook de operación, observabilidad, ciberseguridad y soporte inicial).
     * Asegurar que ambas naturalezas queden perfectamente delimitadas al generar criterios de aceptación y tareas.
   - Robustecer `PromptTemplateContentTest` en `applications/app-service/src/test/java/co/com/bancolombia/prompt/` para validar el contenido normativo de DP-PL-02 en la plantilla estructurada.

3. Restricciones no negociables de arquitectura:
   - Preservar la compatibilidad con todas las pruebas y consumidores existentes.
   - Cero marcadores `{{...}}` sin resolver en tiempo de ejecución.
   - Mantener las 403 pruebas existentes pasando sin regresiones.

4. Compilación y Validación:
   - Ejecutar `./gradlew :app-service:test` y `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que `validateStructure` apruebe sin advertencias.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-07.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 07 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-08-orquestacion-despacho-harness.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-08.md`.
   - Presentar el resumen de cambios para confirmación del commit.
```
