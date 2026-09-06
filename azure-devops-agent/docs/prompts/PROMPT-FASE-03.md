# PROMPT DE EJECUCIÓN — FASE 03: Migración de PlanningDraftFlowHandler

> Copia este prompt o utilízalo directamente para instruir al agente en la ejecución de la Fase 03.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture de Bancolombia, Programación Reactiva (Project Reactor), Spring Boot y testing unitario.

Vamos a ejecutar la FASE 03 del plan Harness de Agentes (Planning & Story Engine) en `azure-devops-agent`:

1. Lectura obligatoria antes de iniciar:
   - azure-devops-agent/docs/plan/ESTADO.md
   - azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-agent/docs/fases/FASE-03-migracion-draft-handler.md
   - rules/spring-rules.md
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-03:
   - Modificar `co.com.bancolombia.usecase.chat.handler.PlanningDraftFlowHandler` para inyectar `SpecStoragePort` en lugar de `PlanningVectorStorePort`.
   - Implementar la resolución de frente a partir del texto del usuario (patrón de prefijo `[Frente]` o fallback al documento por defecto `ideas_planning_q3.md`).
   - Obtener el documento reactivamente con `specStoragePort.getSpec(...)` y capturar errores (`onErrorResume`) para permitir una degradación elegante si el archivo no existe.
   - Inyectar el contenido Markdown completo en la variable del prompt correspondiente.
   - Actualizar el wiring del bean en `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`.
   - Actualizar y complementar la suite de pruebas unitarias (`ChatFlowHandlerTest`, `UseCasesConfigWiringTest`) cubriendo carga con prefijo, carga por defecto y resiliencia ante spec no encontrado.

3. Restricciones no negociables de arquitectura:
   - `domain/usecase` no puede tener dependencias ni anotaciones de Spring.
   - Mantener las 332 pruebas existentes pasando sin regresiones.
   - Preservar la reactividad no bloqueante de Project Reactor.

4. Compilación y Validación:
   - Ejecutar `./gradlew :usecase:test`, `./gradlew :app-service:test` y `./gradlew test` verificando 100% pruebas aprobadas.
   - Verificar que `validateStructure` apruebe sin advertencias.
   - Mantener cobertura en `domain/usecase` >= 90%.

5. Entregables de Cierre (Doble Artefacto):
   - Crear `azure-devops-agent/docs/resultados/RESULTADO-FASE-03.md` con mediciones reales.
   - Actualizar `azure-devops-agent/docs/plan/ESTADO.md` (marcar Fase 03 como COMPLETADA).
   - Generar la especificación de la siguiente fase: `azure-devops-agent/docs/fases/FASE-04-modelos-dominio-planeacion.md`.
   - Generar el prompt de la siguiente fase: `azure-devops-agent/docs/prompts/PROMPT-FASE-04.md`.
   - Presentar el resumen de cambios para confirmación del commit.
```
