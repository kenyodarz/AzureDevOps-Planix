# PROMPT DE EJECUCIÓN — FASE 04: Modal Lanzador de Program Planning

> Utiliza este prompt para ejecutar de forma autónoma y controlada la Fase 04 en `azure-devops-frontend`.

---

```text
Actúa como un desarrollador experto en Angular 22, TypeScript 6 y Clean Architecture de Bancolombia.

Vamos a ejecutar la FASE 04 del Plan Maestro de Integración de Program Planning en el Frontend (`azure-devops-frontend`):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-04-modal-lanzador-planeacion.md
   - AGENTS.md (Fase 1 Frontend)
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-04:
   - T-01: Crear `ProgramPlanningModalComponent` en `src/app/features/devops-agent/components/program-planning-modal/`:
     * Componente `standalone: true`, `ChangeDetectionStrategy.OnPush`.
     * Control Flow moderno (`@if`, `@for`).
     * Diálogo modal accesible (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`).
     * Formulario con validación en tiempo real:
       - Quarter: Formato `Q[1-4]-YYYY` (ej. `Q3-2026`).
       - Sprints: 1 a 24 (por defecto 6).
       - Capacidad máxima por sprint: 1 a 500 SP (por defecto 34).
       - Frentes técnicos: Selección múltiple interactiva.
       - Objetivos de negocio del trimestre.
     * Integración con `PlanningStateService.triggerProgramPlanning()`.
     * Indicadores de carga (`planningRunning`) y deshabilitación de envío si el formulario es inválido.
     * Botones con `aria-label` estricto.
   - T-02: Exportar en `src/app/features/devops-agent/components/index.ts` y crear la suite de pruebas unitarias exhaustivas en `program-planning-modal.component.spec.ts`.

3. Restricciones no negociables:
   - Cero uso del tipo any.
   - Componentes standalone con detección OnPush.
   - Control flow nativo sin directivas estructurales obsoletas (*ngIf, *ngFor).
   - Accesibilidad estricta (aria-label en acciones).
   - Manejo de memoria seguro.

4. Compilación y Validación:
   - Ejecutar corepack pnpm test --watch=false y asegurar 100% de tests pasando.
   - Ejecutar corepack pnpm build y verificar bundle limpio sin advertencias ni errores.

5. Entregables de Cierre (Regla del Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-04.md.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 04 como COMPLETADA).
   - Generar docs/fases/FASE-05-integracion-global-cierre-e2e.md y docs/prompts/PROMPT-FASE-05.md.
   - Presentar la propuesta de commit siguiendo COMMIT_RULES.md.
```
