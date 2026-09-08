# DECISIONES PENDIENTES — Evaluación de Pull Requests con Azure DevOps MCP

> **Regla rectora:** Política de No-Asunción (`docs/README.md`).  
> **Última actualización:** 2026-09-08
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

### DP-PR-01 — Alcance de Operaciones de PR en el MCP (Lectura vs. Escritura)

- **Estado:** 🟢 **RESUELTA (2026-09-08)** · **Bloqueaba:** Fase 01, Fase 04, Fase 05
- **Contexto y Problema:** Un agente puede operar de forma pasiva (solo leyendo el PR y emitiendo el
  veredicto en la conversación del chat) o activa (publicando comentarios directamente en los hilos
  del PR en Azure DevOps).
- **Alternativas Evaluadas:**
    1. *Solo lectura:* Consultar metadatos y diffs; el veredicto queda únicamente en la sesión de
       chat.
    2. *Lectura y comentarios en hilo (threads):* Permitir que el agente publique observaciones
       técnicas directamente en el PR de Azure DevOps con rol `MCP.AZURE_DEVOPS.WRITE`.
- **Decisión Adoptada:** Implementar lectura completa en las Fases 01 a 03 y añadir la capacidad de
  escritura de comentarios en la Fase 04, protegida por seguridad RBAC estricta.
- **Impacto en Código:** `PullRequestPort`, `CreatePullRequestCommentUseCase`,
  `AzureDevOpsGitTools`.

---

### DP-PR-02 — Manejo de Diffs Grandes y Límites de Tokens en el Agente

- **Estado:** 🟢 **RESUELTA (2026-09-08)** · **Bloqueaba:** Fase 01, Fase 02
- **Contexto y Problema:** Un PR con cientos de archivos o miles de líneas modificadas puede saturar
  la ventana de contexto del LLM y degradar la calidad de la evaluación.
- **Alternativas Evaluadas:**
    1. *Descargar todo el diff sin control:* Simple de implementar pero propenso a errores por
       exceso de tokens.
    2. *Resumen de cambios + paginación/filtrado:* Retornar lista de archivos afectados y permitir
       al agente consultar el diff específico de archivos de interés o limitar el diff por
       tamaño/líneas.
- **Decisión Adoptada:** Retornar lista de cambios (`GitChange`) con tipo de cambio (Add, Edit,
  Delete) y permitir consultar el detalle del diff con truncamiento seguro configurable.
- **Impacto en Código:** Modelo `GitChange`, DTOs de respuesta y casos de uso en `domain/usecase`.

---

### DP-PR-03 — Ubicación del Entry Point MCP para Git

- **Estado:** 🟢 **RESUELTA (2026-09-08)** · **Bloqueaba:** Fase 05
- **Contexto y Problema:** Decidir si las nuevas herramientas MCP de Git deben colocarse dentro de
  la clase existente `AzureDevOpsTools.java` o en una clase separada.
- **Alternativas Evaluadas:**
    1. *Agregar en `AzureDevOpsTools.java`:* Centraliza todo, pero incrementa el tamaño de la clase
       y mezcla conceptos de Work Items con Git (viola Principio de Responsabilidad Única).
    2. *Crear `AzureDevOpsGitTools.java`:* Mantiene la separación de responsabilidades clara y
       modular conforme a Clean Architecture.
- **Decisión Adoptada:** Crear una clase independiente `AzureDevOpsGitTools.java` en
  `infrastructure/entry-points/mcp-server/src/main/java/co/com/bancolombia/mcp/tools/`.
- **Impacto en Código:** Módulo `azure-devops-mcp/infrastructure/entry-points/mcp-server`.
