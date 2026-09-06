# FASE 04 — Modal Lanzador de Program Planning (`components/program-planning-modal`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 02, Fase 03 · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(frontend): crear componente modal lanzador de program planning con validacion reactiva`  
> **Siguiente fase:** `fases/FASE-05-integracion-global-cierre-e2e.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

En la **Fase 01** se modelaron los DTOs `ProgramPlanRequestDTO` y `ProgramPlanResponseDTO`.  
En la **Fase 02** se implementó `PlanningStateService.triggerProgramPlanning()` conectado con la API asíncrona y la orquestación hacia `TasksStateService`.  
En la **Fase 03** se construyó el visor y explorador documental `PlanningSpecsExplorerComponent` para consumir las especificaciones resultantes.

### 1.2 Problema que resuelve esta fase

Actualmente el usuario no tiene un mecanismo visual e interactivo en la interfaz para parametrizar y despachar una nueva planeación macro trimestral. Se requiere un diálogo modal accesible, validado reactivamente y estilizado con la línea de diseño del proyecto (`#0b0f19`, `#f2c94c`, glassmorphism).

### 1.3 Estado esperado al terminar

1. Creación del componente standalone `ProgramPlanningModalComponent` en `src/app/features/devops-agent/components/program-planning-modal/`:
  - Componente `standalone: true`, `ChangeDetectionStrategy.OnPush`.
  - Control Flow nativo (`@if`, `@for`).
  - Propiedades de visibilidad `visible` y evento de salida `visibleChange` (o `output<boolean>()`).
  - Formulario reactivo o inputs con validación en tiempo real:
    * **Quarter:** Formato `Q[1-4]-YYYY` (ej. `Q3-2026`).
    * **Sprints:** Valor numérico entero (1 a 24, por defecto 6).
    * **Capacidad Máxima por Sprint:** Story Points (1 a 500, por defecto 34).
    * **Frentes Técnicos:** Selección interactiva de frentes (ej. *Canales*, *Core*, *DevOps*, *Seguridad*).
    * **Objetivos del Trimestre:** Textarea multilínea con lineamientos estratégicos.
  - Botón de submit deshabilitado si el formulario es inválido o si `planningRunning` está activo.
  - Spinner y estado de envío accesible (`aria-label`).
2. Exportación en `src/app/features/devops-agent/components/index.ts`.
3. Suite completa de pruebas unitarias en `program-planning-modal.component.spec.ts`.
4. Compilación `pnpm build` y tests `pnpm test --watch=false` limpios.

### 1.4 Archivos involucrados

| Ruta                                                                                                       | Acción    |
|:-----------------------------------------------------------------------------------------------------------|:----------|
| `src/app/features/devops-agent/components/program-planning-modal/program-planning-modal.component.ts`      | CREAR     |
| `src/app/features/devops-agent/components/program-planning-modal/program-planning-modal.component.spec.ts` | CREAR     |
| `src/app/features/devops-agent/components/index.ts`                                                        | MODIFICAR |

### 1.5 Reglas aplicables

- `AGENTS.md` (Fase 1 Frontend):
  - `standalone: true` con `ChangeDetectionStrategy.OnPush`.
  - Control Flow nativo (`@if`, `@for`).
  - Botones icon-only y acciones con `aria-label` obligatorio.
  - Cero uso de `any` y tipado estricto.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Qué hacer

#### T-01 · Crear `ProgramPlanningModalComponent`

- Implementar diálogo accesible con backdrop blur (`fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm`).
- Inyectar `PlanningStateService` y `NotificationService`.
- Exponer `@Input() visible = false` y `@Output() visibleChange` (o nuevos primitives `input` / `output`).
- Manejar campos de Quarter, Sprints, Capacidad, Frentes y Objetivos con validación instantánea.
- Al despachar, invocar `planningState.triggerProgramPlanning(payload)` y cerrar el modal al confirmar.

#### T-02 · Exportar en el barril y crear pruebas unitarias

- Re-exportar en `components/index.ts`.
- Crear suite en `program-planning-modal.component.spec.ts` cubriendo:
  * Renderizado condicional según visibilidad.
  * Validaciones de formato para Quarter (ej. inválido con `2026`, válido con `Q4-2026`).
  * Validación de límites numéricos (sprints y capacidad).
  * Selección y deselección de frentes técnicos.
  * Envío del formulario y llamada a `planningState.triggerProgramPlanning`.
  * Accesibilidad y atributos `aria-label`.

### 2.2 Criterios de Aceptación

- [ ] Modal standalone con OnPush y nuevo control flow.
- [ ] Validación estricta de inputs sin admitir valores fuera de rango.
- [ ] 100% de tests unitarios pasando en `program-planning-modal.component.spec.ts`.
- [ ] Cero errores en `corepack pnpm test --watch=false` y `corepack pnpm build`.

---

## 3. ORDEN DE EJECUCIÓN

|   Paso   | Acción                                             | Resultado esperado                     |
|:--------:|:---------------------------------------------------|:---------------------------------------|
| **P-01** | Leer `docs/plan/ESTADO.md` y verificar fase activa | Confirmar Fase 04                      |
| **P-02** | Crear `ProgramPlanningModalComponent`              | Componente modal reactivo y estilizado |
| **P-03** | Crear `program-planning-modal.component.spec.ts`   | Suite de tests pasando al 100%         |
| **P-04** | Exportar en `components/index.ts`                  | Re-export disponible                   |
| **P-05** | Ejecutar `pnpm test --watch=false` y `pnpm build`  | 100% verde y compilación limpia        |
| **P-06** | Generar `docs/resultados/RESULTADO-FASE-04.md`     | Registro de evidencias                 |
| **P-07** | Actualizar `docs/plan/ESTADO.md`                   | Marcar Fase 04 completada              |
| **P-08** | Proponer commit bajo `COMMIT_RULES.md`             | Commit listo para autorizar            |
