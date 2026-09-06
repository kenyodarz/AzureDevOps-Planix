# DECISIONES PENDIENTES — Integración de Program Planning en el BFF

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

### DP-BFF-01 — Repositorio de Almacenamiento Documental de Specs (Spec Storage)
- **Estado:** 🟢 **RESUELTA (2026-09-06)** · **Bloqueaba:** Fase 01, Fase 03
- **Contexto y Problema:** El Agente (`azure-devops-agent`) genera los roadmaps maestros (`ideas_planning_QX.md`) y lee las especificaciones por frente (`frente_*.md`) mediante `SpecStoragePort` en disco local/workspace. El BFF necesita acceder a los mismos documentos para servirlos al Frontend.
- **Alternativas Evaluadas:**
  1. *Opción A (FileSystem Compartido):* Implementar `FileSystemSpecAdapter` en el BFF apuntando a la ruta compartida de especificaciones del workspace (configurable vía `specs.storage.path`).
  2. *Opción B (Llamada HTTP al Agente):* Modificar el Agente para exponer rutas REST de specs y consumirlas vía WebClient.
- **Decisión Adoptada:** Se adopta la **Opción A**: reutilizar el patrón desacoplado `SpecStoragePort` con `FileSystemSpecAdapter`. Es de alto rendimiento, reactivo y no añade sobrecarga de red ni dependencias de servicio para lectura de archivos locales.
- **Impacto en Código:** `domain/model/spec/gateways/SpecStoragePort.java`, `infrastructure/driven-adapters/spec-storage/FileSystemSpecAdapter.java`.

---

### DP-BFF-02 — Coexistencia de Rutas Legadas de Vectorización (`pgvector`)
- **Estado:** 🟢 **RESUELTA (2026-09-06)** · **Bloqueaba:** Fase 05
- **Contexto y Problema:** En el BFF existen rutas legadas `/api/planning/ingest`, `/api/planning/initiatives` y `/api/planning/search` que operan contra `pgvector-store` en PostgreSQL.
- **Alternativas Evaluadas:**
  1. *Opción A:* Borrar inmediatamente los endpoints legados.
  2. *Opción B:* Mantener los endpoints legados sin modificaciones y registrar los nuevos endpoints documentales bajo `/api/planning/specs/**` y `/api/planning/program`.
- **Decisión Adoptada:** Se adopta la **Opción B** para garantizar cero regresiones en la suite de 238 pruebas preexistentes y permitir una migración limpia y controlada de la UI.
- **Impacto en Código:** `infrastructure/entry-points/reactive-web/RouterRest.java`.

---

### DP-BFF-03 — Protocolo de Invocación de Program Planning hacia el Agente
- **Estado:** 🟢 **RESUELTA (2026-09-06)** · **Bloqueaba:** Fase 02, Fase 04
- **Contexto y Problema:** Cómo debe el BFF enviar la solicitud de planeación al Agente cuando el Frontend invoque `POST /api/planning/program`.
- **Decisión Adoptada:** `TriggerProgramPlanningUseCase` encapsula la solicitud en un `ProgramPlanningCommand`, valida sus parámetros de dominio, construye el comando canónico A2A (`/plan [Quarter] [Sprints] [Capacidad] [Frentes]`) y lo despacha a través de la infraestructura existente `TrackAgentTaskUseCase` $\to$ `A2AAgentAdapter` sobre el protocolo estándar JSON-RPC 2.0 (`message/send`). Esto asegura que el Agente asocie una `Task` trazable que el frontend pueda seguir y monitorear.
- **Impacto en Código:** `domain/usecase/planning/TriggerProgramPlanningUseCase.java`.
