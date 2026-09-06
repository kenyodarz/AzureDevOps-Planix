# FASE 03 — Visor y Explorador Documental de Specs (`components/planning-specs-explorer`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 02 · **Riesgo:** Medio  
> **Commit al cerrar:** `feat(frontend): crear componente visor y explorador de especificaciones documentales`  
> **Siguiente fase:** `fases/FASE-04-modal-lanzador-planeacion.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

En la **Fase 01** se definieron los contratos de API y modelos tipados (`SpecListDTO`, `SpecDocumentDTO`, `specUrl`, `PlanningActiveView`).  
En la **Fase 02** se extendieron `PlanningApiService` y `DevopsAgentApiService` para consultar specs y documentos, y se equipó a `PlanningStateService` con estado reactivo (`availableSpecs`, `selectedSpec`, `loadingSpecs`) y métodos de orquestación (`loadAvailableSpecs`, `selectSpec`, `clearSelectedSpec`).

### 1.2 Problema que resuelve esta fase

Actualmente, el usuario en el frontend no tiene forma visual de:

1. Ver el catálogo de especificaciones documentales generadas por el agente de planeación (`ideas_planning_QX.md`, `frente_*.md`).
2. Explorar y leer en pantalla el contenido Markdown formateado con estilo técnico de alto contraste y tablas de capacidad legibles.
3. Transicionar ágilmente desde la visualización de un frente técnico hacia el refinamiento de HU/HA en el chat asistido (DP-FE-02).

### 1.3 Estado esperado al terminar

1. Creación del componente standalone `PlanningSpecsExplorerComponent` en `src/app/features/devops-agent/components/planning-specs-explorer/`:
  - Barra lateral o selector de especificaciones disponibles (`availableSpecsSignal` / `availableSpecs`).
  - Visor principal con renderizado estilizado y seguro de Markdown (DP-FE-03, `MarkdownParserPipe` / utilidades nativas).
  - Indicadores de carga (`loadingSpecs`) y estado vacío cuando no hay spec seleccionado o no hay specs disponibles.
  - Cabecera del visor con metadatos del documento, nombre del archivo, botón de refresco y botón de acción rápida *«Refinar HU en este Frente»* (DP-FE-02) activo cuando el documento es un frente técnico.
2. Integración de `PlanningSpecsExplorerComponent` en `components/index.ts`.
3. Pruebas unitarias completas en `planning-specs-explorer.component.spec.ts` con cobertura de todos los estados (carga, selección, renderizado, emisión de eventos).
4. Compilación `pnpm build` y tests `pnpm test --watch=false` limpios.

### 1.4 Archivos involucrados

| Ruta                                                                                                         | Acción    |
|:-------------------------------------------------------------------------------------------------------------|:----------|
| `src/app/features/devops-agent/components/planning-specs-explorer/planning-specs-explorer.component.ts`      | CREAR     |
| `src/app/features/devops-agent/components/planning-specs-explorer/planning-specs-explorer.component.spec.ts` | CREAR     |
| `src/app/features/devops-agent/components/index.ts`                                                          | MODIFICAR |

### 1.5 Reglas aplicables

- `AGENTS.md` (Fase 1 Frontend):
  - `standalone: true` con `ChangeDetectionStrategy.OnPush`.
  - Control Flow nativo: `@if`, `@for`, `@switch` (cero `*ngIf`, `*ngFor`).
  - Uso de `signal()` y señales computadas (`computed()`) para estado de UI.
  - Botones icon-only con `aria-label` obligatorio.
  - Suscripciones seguras con `takeUntilDestroyed()`.
  - Cero uso de `any` y tipado estricto.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Qué hacer

#### T-01 · Crear `PlanningSpecsExplorerComponent`

- Inyectar `PlanningStateService` y `RefinementChatStateService`.
- Al inicializar (`ngOnInit` o constructor), invocar `planningState.loadAvailableSpecs()`.
- Si existen specs y no hay uno seleccionado, seleccionar por defecto el primero o la especificación maestra (`ideas_planning_*.md`).
- Diseñar interfaz dividida (Split Layout):
  - **Panel lateral (Selector de Specs):**
    - Listado reactivo de specs usando `@for (spec of availableSpecs(); track spec)`.
    - Indicador visual del spec activo (`selectedSpec()?.name === spec`).
    - Distinción visual mediante badges/iconos entre Spec Maestro (`ideas_planning_*.md`) y Specs de Frente (`frente_*.md`).
    - Botón para recargar la lista (`aria-label="Recargar catálogo de especificaciones"`).
  - **Panel central (Visor Documental):**
    - Cabecera con título del archivo, ruta y acciones.
    - Si el archivo corresponde a un frente (`name.startsWith('frente_')`), mostrar botón destacado:
      *«Refinar HU en este Frente»* (DP-FE-02), el cual emite un evento o precarga el prompt en `RefinementChatStateService` y notifica al contenedor para cambiar al tab de refinamiento.
    - Área de contenido con scroll estilizado y renderizado Markdown aplicando clases Tailwind con la paleta dark-mode del proyecto (`bg-slate-900/60`, `text-slate-200`, bordes `slate-800`, acentos en amarillo/dorado `#f2c94c` y verde esmeralda `#10b981`).
    - Manejo de estados: `@if (loadingSpecs())` con skeleton/spinner, y estado vacío si `!selectedSpec()`.

#### T-02 · Exportar en el barril y crear pruebas unitarias

- Re-exportar `PlanningSpecsExplorerComponent` en `src/app/features/devops-agent/components/index.ts`.
- Crear suite en `planning-specs-explorer.component.spec.ts` validando:
  - Renderizado inicial y llamada a `loadAvailableSpecs()`.
  - Cambio de spec al hacer clic en un ítem de la lista.
  - Visibilidad condicional del botón de refinamiento en frentes técnicos.
  - Interacción con `RefinementChatStateService` al pulsar *«Refinar HU en este Frente»*.
  - Accesibilidad (`aria-label` presente en botones).

### 2.2 Criterios de Aceptación

- [ ] Componente standalone con OnPush y nuevo control flow (@if, @for).
- [ ] Explorador de specs documental operativo con navegación lateral.
- [ ] Botón de refinamiento contextual funcional según DP-FE-02.
- [ ] 100% de tests unitarios pasando en `planning-specs-explorer.component.spec.ts`.
- [ ] Cero errores en `corepack pnpm test --watch=false` y `corepack pnpm build`.

---

## 3. ORDEN DE EJECUCIÓN

|   Paso   | Acción                                             | Resultado esperado                   |
|:--------:|:---------------------------------------------------|:-------------------------------------|
| **P-01** | Leer `docs/plan/ESTADO.md` y verificar fase activa | Confirmar arranque en Fase 03        |
| **P-02** | Crear `PlanningSpecsExplorerComponent`             | Componente estructurado y estilizado |
| **P-03** | Crear `planning-specs-explorer.component.spec.ts`  | Tests unitarios exhaustivos pasando  |
| **P-04** | Exportar en `components/index.ts`                  | Re-export disponible                 |
| **P-05** | Ejecutar `pnpm test --watch=false` y `pnpm build`  | 100% verde y compilación limpia      |
| **P-06** | Generar `docs/resultados/RESULTADO-FASE-03.md`     | Registro de evidencias               |
| **P-07** | Actualizar `docs/plan/ESTADO.md`                   | Marcar Fase 03 completada            |
| **P-08** | Proponer commit bajo `COMMIT_RULES.md`             | Commit listo para autorizar          |
