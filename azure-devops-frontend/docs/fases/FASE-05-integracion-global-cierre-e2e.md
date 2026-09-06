# FASE 05 — Integración en Página Principal, Navegación a Refinamiento y Cierre E2E

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 03, Fase 04 · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(frontend): integrar centro unificado de planeacion navegacion a refinamiento y cierre e2e`  
> **Siguiente fase:** Ninguna (Fase final de integración frontend)

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- En la **Fase 01** se modelaron los DTOs y contratos inmutables (`ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecListDTO`, `SpecDocumentDTO`, `PlanningActiveView`).
- En la **Fase 02** se implementaron los servicios `PlanningApiService` y `PlanningStateService` con Signals y Observables conectados al backend.
- En la **Fase 03** se construyó el visor y explorador documental `PlanningSpecsExplorerComponent` con renderizado seguro de Markdown y tablas dark-mode (DP-FE-03).
- En la **Fase 04** se construyó el modal reactivo y accesible `ProgramPlanningModalComponent` para configurar y despachar planeaciones trimestrales.

### 1.2 Problema que resuelve esta fase

Los componentes construidos en las Fases 03 y 04 existen de forma aislada en el directorio de componentes, pero aún no están integrados en el centro de planeación principal ni en la vista unificada de `DevopsAgentHomePage` / `PlanningManagementComponent`. Se requiere:

1. Integrar `PlanningSpecsExplorerComponent` y `ProgramPlanningModalComponent` en la pestaña de planeación (`PlanningManagementComponent` / `DevopsAgentHomePage`).
2. Implementar la convivencia de vistas según **DP-FE-01**: el explorador de especificaciones documentales como vista principal y la tabla legada de vectorización como vista alternativa secundaria.
3. Conectar el botón de acción *«Nueva Planeación de Programa»* para desplegar el modal `ProgramPlanningModalComponent`.
4. Materializar la transición fluida de **DP-FE-02**: cuando desde el explorador de specs se emite `refineRequested`, cambiar la pestaña activa de `DevopsAgentHomePage` a *«Refinar HU/HA»* (`refine`) manteniendo el prompt precargado listo para enviar.
5. Ejecutar la suite completa de pruebas unitarias y validación de bundle.

### 1.3 Estado esperado al terminar

1. `PlanningManagementComponent` actualizado para alojar el centro unificado de planeación:
  - Barra de control superior con selector de vista (Explorador Documental vs Vectorización Legada) y botón de acción rápida *«Nuevo Program Planning»*.
  - Renderizado condicional de `PlanningSpecsExplorerComponent` por defecto.
  - Renderizado de la tabla de iniciativas vectorizadas legadas accesible bajo el selector secundario.
  - Incorporación de `ProgramPlanningModalComponent` con binding bidireccional de visibilidad `[(visible)]="showProgramPlanningModal"`.
2. `DevopsAgentHomePage` actualizado para escuchar el evento `refineRequested` emitido desde el explorador y activar la pestaña de refinamiento (`activeTab.set('refine')`).
3. Pruebas unitarias actualizadas y extendidas para `PlanningManagementComponent` y `DevopsAgentHomePage`.
4. 100% de tests unitarios pasando y bundle de producción limpio.

### 1.4 Archivos involucrados

| Ruta                                                                                                 | Acción    |
|:-----------------------------------------------------------------------------------------------------|:----------|
| `src/app/features/devops-agent/components/planning-management/planning-management.component.ts`      | MODIFICAR |
| `src/app/features/devops-agent/components/planning-management/planning-management.component.spec.ts` | MODIFICAR |
| `src/app/features/devops-agent/pages/devops-agent-home.page.ts`                                      | MODIFICAR |
| `src/app/features/devops-agent/pages/devops-agent-home.page.spec.ts`                                 | MODIFICAR |

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Qué hacer

#### T-01 · Integrar en `PlanningManagementComponent`

- Importar `PlanningSpecsExplorerComponent` y `ProgramPlanningModalComponent`.
- Añadir señal reactiva `activeView = signal<PlanningActiveView>('explorer')`.
- Añadir señal reactiva `showPlanningModal = signal<boolean>(false)`.
- Añadir botones en el encabezado:
  * Selector toggle/tab entre *«Especificaciones Documentales»* e *«Iniciativas Vectorizadas (Legado)»*.
  * Botón primario *«Lanzar Planeación»* con icono de brújula o envío que active `showPlanningModal.set(true)`.
- Propagar o reenviar el evento `refineRequested` hacia el contenedor superior.

#### T-02 · Conectar transición a refinamiento en `DevopsAgentHomePage`

- Capturar el evento `refineRequested` proveniente de la sección de planeación.
- Conmutar la pestaña activa: `this.activeTab.set('refine')`.

#### T-03 · Actualizar suites de pruebas unitarias

- Extender `planning-management.component.spec.ts` para validar el cambio de vistas y la apertura del modal.
- Extender `devops-agent-home.page.spec.ts` para validar la transición hacia la pestaña de refinamiento al recibir `refineRequested`.

---

## 3. CRITERIOS DE ACEPTACIÓN

- [ ] `PlanningSpecsExplorerComponent` es la vista por defecto al entrar a la sección de planeación.
- [ ] La vista legada de vectorización permanece accesible a través del selector de vistas (DP-FE-01).
- [ ] El botón *«Lanzar Planeación»* abre el diálogo modal `ProgramPlanningModalComponent`.
- [ ] La acción *«Refinar HU en este Frente»* transiciona a la pestaña *«Refinar HU/HA»* con el prompt precargado (DP-FE-02).
- [ ] 100% de tests unitarios pasando en todo el proyecto.
- [ ] Compilación de producción limpia sin errores ni advertencias.
