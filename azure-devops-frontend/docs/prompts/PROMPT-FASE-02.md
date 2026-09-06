# PROMPT DE EJECUCIÓN — FASE 02: Servicios de API y Gestión de Estado Reactivo

> Utiliza este prompt para ejecutar de forma autónoma y controlada la Fase 02 en `azure-devops-frontend`.

---

```text
Actúa como un desarrollador experto en Angular 22, TypeScript 6 y Clean Architecture de Bancolombia.

Vamos a ejecutar la FASE 02 del Plan Maestro de Integración de Program Planning en el Frontend (`azure-devops-frontend`):

1. Lectura obligatoria antes de iniciar:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/FASE-02-servicios-api-estado-reactivo.md
   - AGENTS.md (Fase 1 Frontend)
   - COMMIT_RULES.md

2. Ejecuta estrictamente las tareas técnicas T-01 y T-02 de FASE-02:
   - T-01: Extender PlanningApiService y la fachada DevopsAgentApiService con los métodos:
     * triggerProgramPlanning(request: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO>
     * getAvailableSpecs(): Observable<SpecListDTO>
     * getSpecDocument(name: string): Observable<SpecDocumentDTO>
     Actualizar sus pruebas unitarias en planning-api.service.spec.ts y devops-agent-api.service.spec.ts.
   - T-02: Extender PlanningStateService con estado reactivo (Signals / Observables) para availableSpecs, selectedSpec, loadingSpecs y planningRunning.
     Implementar loadAvailableSpecs(), selectSpec(name), clearSelectedSpec() y triggerProgramPlanning(request).
     Integrar la notificación a TasksStateService.triggerImmediatePoll() cuando una planeación sea encolada exitosamente (202).
     Actualizar sus pruebas unitarias en planning-state.service.spec.ts.

3. Restricciones no negociables:
   - Cero uso del tipo any.
   - Preservar métodos existentes de iniciativas vectorizadas para asegurar retrocompatibilidad.
   - Manejo seguro de memoria (takeUntilDestroyed / suscripciones controladas).
   - Tipado estricto e inmutabilidad.

4. Compilación y Validación:
   - Ejecutar corepack pnpm test --watch=false y asegurar 100% de tests pasando.
   - Ejecutar corepack pnpm build y verificar que el bundle compile sin advertencias ni errores.

5. Entregables de Cierre (Regla del Doble Artefacto):
   - Crear docs/resultados/RESULTADO-FASE-02.md.
   - Actualizar docs/plan/ESTADO.md (marcar Fase 02 como COMPLETADA).
   - Generar docs/fases/FASE-03-visor-explorador-specs.md y docs/prompts/PROMPT-FASE-03.md.
   - Presentar la propuesta de commit siguiendo COMMIT_RULES.md.
```
