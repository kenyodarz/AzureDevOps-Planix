# PROMPT DE EJECUCIÓN — FASE 01: Modelos y Puerto de Almacenamiento Documental

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 01.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Programación Reactiva (Project Reactor) y testing unitario.

Vamos a ejecutar la FASE 01 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-01-modelos-y-puerto-spec-storage.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-01:
   - Crear `co.com.bancolombia.model.spec.SpecDocument` (record inmutable con validación de invariantes).
   - Crear `co.com.bancolombia.model.spec.SpecNotFoundException` (excepción de dominio limpia).
   - Crear `co.com.bancolombia.model.spec.gateways.SpecStoragePort` (interfaz reactiva con Mono/Flux).
   - Crear `co.com.bancolombia.model.spec.SpecDocumentTest` cubriendo el 100% de los nuevos tipos y ramas.

3. Restricciones no negociables de arquitectura:
   - `domain/model` debe permanecer 100% puro: cero anotaciones de Spring, Jackson, Lombok en entidades de dominio ni dependencias de infraestructura.
   - No modificar clases de `infrastructure` ni `applications` en esta fase.
   - Mantener las pruebas existentes pasando sin regresiones.

4. Compilación y Validación:
   - Ejecutar `./gradlew :agent-model:test` y verificar 100% pruebas aprobadas.
   - Verificar que la cobertura en `domain/model` se mantenga >= 90%.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-01.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 01 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-02-adaptador-filesystem-spec.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-02.md`.
   - Si se detecta algún hallazgo técnico relevante, registrarlo en el resultado y ajustar el plan maestro o la Fase 02 según corresponda.
   - Presentar el resumen de cambios para confirmación del commit.
```
