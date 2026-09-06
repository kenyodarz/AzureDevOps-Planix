# DECISIONES PENDIENTES — Harness: Planning y Story Engine

> **Regla rectora:** Política de No-Asunción (`.github/copilot-instructions.md`).  
> **Última actualización:** 2026-09-05
>
> Si una decisión que bloquea la fase activa se encuentra marcada como 🔴 **ABIERTA**, el
> desarrollador o agente de IA debe **detenerse inmediatamente y solicitar confirmación al usuario**.
> Queda estrictamente prohibido asumir o implementar por criterio propio una alternativa abierta.

---

## Leyenda de Estados

|       Estado       | Significado                                                                                 |
|:------------------:|:--------------------------------------------------------------------------------------------|
|   🔴 **ABIERTA**   | Requiere pronunciamiento o confirmación del usuario. Bloquea la fase indicada.              |
|  🟢 **RESUELTA**   | Decisión confirmada y acordada. Registra la fecha, la justificación y el impacto en código. |
| ⚪ **INFORMATIVA** | Lineamiento acordado que no bloquea la ejecución de la fase en curso.                       |

---

## Registro de Decisiones

### DP-PL-01 — Almacenamiento Documental de Specs (Spec Storage)

- **Estado:** 🟢 **RESUELTA (2026-09-05)** · **Bloqueaba:** Fase 01, Fase 02
- **Contexto:** Se utilizaba `PlanningVectorStorePort` (pgvector en PostgreSQL) con chunks de 500
  tokens. Los documentos de planeación son de 5 a 20 KB; fragmentarlos destruía el entendimiento
  global y requería levantar infraestructura pesada.
- **Decisión Adoptada:** Crear el puerto `SpecStoragePort` e implementar `FileSystemSpecAdapter`
  para leer y escribir archivos Markdown completos en disco local/workspace, dejando la interfaz
  abierta para S3 en el futuro.
- **Impacto en Código:** `domain/model/src/.../model/spec/`,
  `infrastructure/driven-adapters/spec-storage/`.

---

### DP-PL-02 — Separación de Alcance: HU (hasta QA) vs HA (HyMS en Producción)

- **Estado:** 🟢 **RESUELTA (2026-09-05)** · **Bloqueaba:** Fase 05, Fase 07
- **Contexto:** En el marco de Bancolombia, enviar a producción requiere cumplir el proceso de HyMS.
  No se debe mezclar el desarrollo con la habilitación operativa en la misma historia.
- **Decisión Adoptada:**
    * **HU (Historia de Usuario):** Construir funcionalidad desde diseño hasta pruebas en **QA**.
    * **HA (Historia Habilitadora):** Setups técnicos previos y el proceso formal de **Paso a
      Producción bajo HyMS** (ciberseguridad, observabilidad, Runbook de operación y soporte
      post-producción).
- **Impacto en Código:** `applications/app-service/src/main/resources/prompts/`.

---

### DP-PL-03 — Selección del Frente para Inyección de Contexto

- **Estado:** 🟢 **RESUELTA (2026-09-05)** · **Bloqueaba:** Fase 03, Fase 07
- **Contexto:** Al pasar una tarea en bruto, el agente necesita saber qué archivo de spec cargar.
- **Decisión Adoptada:** Si el usuario incluye el frente en el texto o prefijo (ej.
  `[Aegis Engine]`), el agente carga ese archivo. Si no lo incluye, utiliza el frente por defecto
  configurado (`Marketplace - Tools`) o solicita aclaración interactiva.
- **Impacto en Código:** `PlanningDraftFlowHandler`, `ChatFlowContext`.

---

### DP-PL-04 — Formato de Salida del Agente Planner

- **Estado:** 🟢 **RESUELTA (2026-09-05)** · **Bloqueaba:** Fase 05, Fase 06
- **Contexto:** Qué artefactos debe producir el agente al planificar el Q.
- **Decisión Adoptada:** Generar el archivo maestro `ideas_planning_QX.md` con tablas de roadmap por
  sprints, puntos Fibonacci y dependencias, y generar o actualizar los archivos de spec individuales
  por cada frente (`frente_*.md`).
- **Impacto en Código:** `ProgramPlanningFlowHandler`, prompt `program-planning-roadmap.md`.
