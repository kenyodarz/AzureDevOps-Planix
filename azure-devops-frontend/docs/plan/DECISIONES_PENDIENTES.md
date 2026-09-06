# DECISIONES PENDIENTES — Integración de Program Planning en el Frontend

> **Regla rectora:** Política de No-Asunción (`.github/copilot-instructions.md`).  
> **Última actualización:** 2026-09-06  
> 
> Si una decisión que bloquea la fase activa se encuentra marcada como 🔴 **ABIERTA**, el desarrollador o agente de IA debe **detenerse inmediatamente y solicitar confirmación al usuario**. Queda estrictamente prohibido asumir o implementar por criterio propio una alternativa abierta.

---

## Leyenda de Estados

| Estado | Significado |
| :---: | :--- |
| 🔴 **ABIERTA** | Requiere pronunciamiento o confirmación del usuario. Bloquea la fase indicada. |
| 🟢 **RESUELTA** | Decisión confirmada y acordada. Registra la fecha, la justificación y el impacto en código. |
| ⚪ **INFORMATIVA** | Lineamiento acordado que no bloquea la ejecución de la fase en curso. |

---

## Registro de Decisiones

### DP-FE-01 — Convivencia del Explorador Documental de Specs con la Vista Legada de Vectorización
- **Estado:** 🟢 **RESUELTA (2026-09-06)** · **Bloqueaba:** Fase 03, Fase 05
- **Contexto y Problema:** La pestaña *«Gestión de Planeaciones»* mostraba anteriormente una tabla con las iniciativas indexadas en PostgreSQL/pgvector. Con la transición al almacenamiento documental (`SpecStoragePort`), se requiere decidir cómo estructurar la vista.
- **Alternativas Evaluadas:**
  1. *Opción A:* Eliminar por completo la vista legada de PostgreSQL/pgvector.
  2. *Opción B:* Transformar la pestaña de *«Gestión de Planeaciones»* en un centro unificado de planeación, donde la vista por defecto sea el nuevo **Explorador de Specs Documentales** (`ideas_planning_QX.md` y `frente_*.md`), y se proporcione un selector de vista para acceder a la gestión legada si es necesario.
- **Decisión Adoptada:** Se adopta la **Opción B** para evitar cualquier rotura funcional o regresión en los tests preexistentes y ofrecer una transición transparente hacia el modelo documental.
- **Impacto en Código:** `PlanningManagementComponent`, `PlanningSpecsExplorerComponent`.

---

### DP-FE-02 — Flujo de Transición desde un Spec de Frente hacia el Asistente de Refinamiento
- **Estado:** 🟢 **RESUELTA (2026-09-06)** · **Bloqueaba:** Fase 05
- **Contexto y Problema:** Cómo debe interactuar la UI cuando el usuario consulte un documento de especificación de un frente técnico (ej. `frente_canales.md`) y desee crear una HU/HA a partir de él.
- **Alternativas Evaluadas:**
  1. *Opción A:* Obligar al usuario a copiar manualmente el texto del Markdown y pegarlo en el chat de refinamiento.
  2. *Opción B:* Proveer un botón de acción rápida *«Refinar HU en este Frente»* en la cabecera del visor del spec. Al hacer clic, se precarga el prompt contextualizado en `RefinementChatStateService` y se activa de inmediato la pestaña *«Refinar HU/HA»* con el input listo para enviar.
- **Decisión Adoptada:** Se adopta la **Opción B**. Maximiza la fluidez entre el nivel estratégico (roadmap del Q) y el nivel táctico (creación de HU/HA con el marco HyMS).
- **Impacto en Código:** `PlanningSpecsExplorerComponent`, `RefinementChatStateService`, `DevopsAgentHomePage`.

---

### DP-FE-03 — Renderizado de Markdown Seguro en Angular 22
- **Estado:** 🟢 **RESUELTA (2026-09-06)** · **Bloqueaba:** Fase 03
- **Contexto y Problema:** Los documentos de especificación (`ideas_planning_QX.md` y `frente_*.md`) son archivos Markdown estructurados con encabezados, tablas de capacidad, listas de tareas y bloques de código. Se debe renderizar de forma segura y visualmente consistente sin introducir librerías externas vulnerables o pesadas.
- **Decisión Adoptada:** Implementar un parser/formateador liviano y sanitizado basado en utilidades nativas de Angular (`DomSanitizer`), estilizándolo con clases de Tailwind CSS coherentes con la estética glassmorphic y dark-mode del proyecto (`#0b0f19`, `#f2c94c`, `#10b981`).
- **Impacto en Código:** `PlanningSpecsExplorerComponent`.
