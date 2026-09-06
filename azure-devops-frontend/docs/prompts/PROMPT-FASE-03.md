# PROMPT DE EJECUCIÓN — FASE 03: Visor y Explorador Documental de Specs

> Utiliza este prompt para ejecutar de forma autónoma y controlada la Fase 03 en `azure-devops-frontend`.

---

```text
Actúa como un desarrollador experto en Angular 22, TypeScript 6 y Clean Architecture de Bancolombia.

Vamos a ejecutar la FASE 03 del Plan Maestro de Integración de Program Planning en el Frontend (`azure-devops-frontend`):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md (especialmente DP-FE-01, DP-FE-02 y DP-FE-03)
   - docs/fases/FASE-03-visor-explorador-specs.md
   - AGENTS.md (Fase 1 Frontend)
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-03:
   - T-01: Crear `PlanningSpecsExplorerComponent` en `src/app/features/devops-agent/components/planning-specs-explorer/`:
     * Componente `standalone: true`, `ChangeDetectionStrategy.OnPush`.
     * Control Flow moderno (`@if`, `@for`, `@switch`).
     * Selector lateral de especificaciones (`ideas_planning_QX.md`, `frente_*.md`).
     * Visor Markdown seguro con estilos dark-mode y tablas estilizadas.
     * Botón contextual «Refinar HU en este Frente» para transicionar al chat con prompt precargado (DP-FE-02).
     * Indicadores de carga (`loadingSpecs`) y estado vacío.
     * Botones icon-only con `aria-label`.
   - T-02: Exportar en `src/app/features/devops-agent/components/index.ts` y crear la suite de pruebas unitarias completas en `planning-specs-explorer.component.spec.ts`.

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
   - Crear docs/resultados/RESULTADO-FASE-03.md.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 03 como COMPLETADA).
   - Generar docs/fases/FASE-04-modal-lanzador-planeacion.md y docs/prompts/PROMPT-FASE-04.md.
   - Presentar la propuesta de commit siguiendo COMMIT_RULES.md.
```
