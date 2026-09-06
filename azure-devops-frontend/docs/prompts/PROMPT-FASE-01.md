# PROMPT DE EJECUCIÓN — FASE 01: Contratos de API, Modelos y DTOs

> Utiliza este prompt para ejecutar de forma autónoma y controlada la Fase 01 en `azure-devops-frontend`.

---

```text
Actúa como un desarrollador experto en Angular 22, TypeScript 6 y Clean Architecture de Bancolombia.

Vamos a ejecutar la FASE 01 del Plan Maestro de Integración de Program Planning en el Frontend (`azure-devops-frontend`):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-01-contratos-api-modelos-dtos.md
   - AGENTS.md (Fase 1 Frontend)
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 y T-02 de FASE-01:
   - Declarar PLANNING_PROGRAM, PLANNING_SPECS y la función helper specUrl(name) en src/app/core/config/api-endpoints.ts.
   - Declarar los DTOs inmutables (ProgramPlanRequestDTO, ProgramPlanResponseDTO, SpecListDTO, SpecDocumentDTO) y PlanningActiveView en src/app/features/devops-agent/models/devops-agent.model.ts.

3. Restricciones no negociables:
   - Cero uso del tipo any.
   - No alterar rutas ni modelos existentes para garantizar retrocompatibilidad.
   - Tipado estricto e inmutabilidad en las interfaces.

4. Compilación y Validación:
   - Ejecutar corepack pnpm build
   - Verificar que el bundle se genere sin advertencias ni errores.

5. Entregables de Cierre (Regla del Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-01.md.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 01 como COMPLETADA).
   - Generar docs/fases/FASE-02-servicios-api-estado-reactivo.md y docs/prompts/PROMPT-FASE-02.md.
   - Presentar la propuesta de commit siguiendo COMMIT_RULES.md.
```
