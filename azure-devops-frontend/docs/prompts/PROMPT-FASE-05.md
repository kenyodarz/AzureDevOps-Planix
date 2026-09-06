# PROMPT DE EJECUCIÓN — FASE 05: Integración Global, Transición a Refinamiento y Cierre E2E

> Utiliza este prompt para ejecutar de forma autónoma y controlada la Fase 05 en `azure-devops-frontend`.

---

```text
Actúa como un desarrollador experto en Angular 22, TypeScript 6 y Clean Architecture de Bancolombia.

Vamos a ejecutar la FASE 05 del Plan Maestro de Integración de Program Planning en el Frontend (`azure-devops-frontend`):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-05-integracion-global-cierre-e2e.md
   - AGENTS.md (Fase 1 Frontend)
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas de FASE-05:
   - T-01: Integrar en `PlanningManagementComponent`:
     * Incorporar `PlanningSpecsExplorerComponent` como vista principal por defecto (`activeView = 'explorer'`).
     * Mantener la tabla de iniciativas legada bajo el selector secundario (`activeView = 'legacy-vector'`) resolviendo DP-FE-01.
     * Incorporar botón de acción rápida para desplegar `ProgramPlanningModalComponent`.
     * Reenviar el evento `refineRequested` hacia el contenedor superior.
   - T-02: Conectar la navegación en `DevopsAgentHomePage`:
     * Escuchar `refineRequested` y conmutar la pestaña activa a `refine` (DP-FE-02), donde el prompt ya estará precargado en el chat.
   - T-03: Actualizar suites de pruebas unitarias en `planning-management.component.spec.ts` y `devops-agent-home.page.spec.ts`.

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
   - Crear docs/resultados/RESULTADO-FASE-05.md.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 05 y Plan Maestro como COMPLETADOS).
   - Presentar la propuesta de commit siguiendo COMMIT_RULES.md.
```
