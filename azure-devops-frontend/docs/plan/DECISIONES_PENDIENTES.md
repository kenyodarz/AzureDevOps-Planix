# DECISIONES PENDIENTES — azure-devops-frontend

> **Regla que aplica:** `rules/angular-rules.md` §7 — Política de No-Asunción.
> **Actualizado:** 2026-09-01
>
> Si una decisión que bloquea la fase en curso sigue 🔴 **ABIERTA**, el ejecutor debe
> **detenerse y preguntar al usuario**. Está prohibido resolverla por criterio propio.

---

## Leyenda

| Símbolo        | Significado                                                      |
|----------------|------------------------------------------------------------------|
| 🔴 ABIERTA     | Requiere respuesta del usuario. Bloquea la fase indicada.        |
| 🟢 RESUELTA    | Respondida. Se registra la decisión y la fecha.                  |
| ⚪ INFORMATIVA | No bloquea, pero conviene confirmarla antes del cierre del plan. |

---

## DP-01 — Alias público `messages` del servicio de estado

- **Estado:** 🟢 **RESUELTA (2026-09-01)** · **Bloqueaba:** Fase 05
- **Decisión adoptada:** Conservar el alias público `messages` delegando en `refinementChatState.messages`. No eliminar para preservar la retrocompatibilidad y la red de seguridad de caracterización.
- **Evidencia:** `src/app/features/devops-agent/services/devops-agent-state.service.ts` y `services/state/refinement-chat-state.service.ts`.

---

## DP-02 — Umbrales de calidad y definición de «historia grande»

- **Estado:** 🔴 ABIERTA · **Bloquea:** Fase 07
- **Evidencia:** `components/planning-dashboard/planning-dashboard.component.ts`
  | Línea | Literal | Uso | |---:|---|---| | 702-707 | `90`, `70`, `50` | Etiquetas *Excelente / Buena / Regular / Deficiente* | | 709-713 | `80`, `50` | Color de la barra de calidad | | 627-637 | `50`, `80` | Filtro *critical / regular / good* | | 715-718 | `13` | Conteo de «historias grandes» |
- **Pregunta:** ¿Son reglas de negocio confirmadas? Nótese que **los umbrales de la etiqueta (`90/70/50`) y los del color y el filtro (`80/50`) no coinciden**: una historia con `85` se etiqueta como *Buena* pero cae en el filtro *good*, mientras que con `75` se etiqueta *Buena* y cae en el filtro *regular*. ¿Es intencional o es un defecto?
- **Impacto si se asume mal:** unificar los umbrales cambia la clasificación visible de historias reales para los equipos.
- **Acción por defecto si sigue abierta:** trasladar los valores **literalmente tal como están**, incluida la discrepancia, a `domain/quality-score.ts`, y **no unificarlos**.
- **✅ EVIDENCIA APORTADA POR LA FASE 01 (2026-08-31).** La discrepancia está **confirmada y congelada por prueba** en
  `planning-dashboard.component.spec.ts` →
  `«THEN a score of 75 is labelled GOOD but filtered as REGULAR (discrepancia viva)»`:

  | `qualityScore` | Etiqueta mostrada | Filtro en el que cae |
    |---:|---|---|
  | 95 | Excelente | good |
  | 85 | **Buena (Suficiente)** | **good** |
  | **75** | **Buena (Suficiente)** | **regular** ⚠️ |
  | 60 | Regular | regular |
  | 45 | Deficiente | critical |

  Dos historias etiquetadas igual (*Buena*) caen en filtros distintos. **Se necesita tu decisión para saber si es intencional.**

---

## DP-03 — Endpoint de cancelación de tareas

- **Estado:** 🟢 **RESUELTA (2026-08-31)** · **Bloqueaba:** Fases 02 y 04
- **Decisión adoptada:** *«Vamos por la acción recomendada»* → **se conserva `POST '/'`** con el envoltorio JSON-RPC. La ruta no se modifica.
- **Implementación:** `core/config/api-endpoints.ts` → `TASKS_CANCEL_RPC: '/'`, con comentario que remite a esta decisión. `cancelTask` sigue apuntando a la raíz.
- **Evidencia original:** `services/devops-agent-api.service.ts:112-122`
  ```ts
  cancelTask(id: string): Observable<any> {
    const payload = { jsonrpc: '2.0', method: 'tasks/cancel', params: { taskId: id }, id: `cancel-${id}` };
    return this.http.post<any>('/', payload);   // ← ruta raíz
  }
  ```
- **Pregunta original:** ¿El `POST '/'` con envoltorio JSON-RPC es el contrato correcto contra el BFF, o debe migrarse a una ruta explícita del tipo `/api/tasks/{id}/cancel`? → **Se conserva.**

---

## DP-04 — Copys de saludo y de error

- **Estado:** 🟢 **RESUELTA (2026-09-01)** · **Bloqueaba:** Fase 03
- **Decisión adoptada:** Se aplicó la acción por defecto segura (R-4) según instrucciones de la Fase 03: **los copys se trasladaron a la capa `domain/` de forma exacta carácter por carácter**, sin reescribir, sin corregir la errata «aprovadores» de `chat-input` y conservando el texto de error del puerto 8081 intacto.
- **Evidencia original:** `services/devops-agent-state.service.ts:49-87` (~40 líneas de markdown),
  `:127`, `:227`, `:258`, `:393`.
- **Implementación:** `features/devops-agent/domain/chat-greetings.ts`, `chat-prompts.ts`, `chat-suggestions.ts`, `tech-tags.ts`.

---

## DP-05 — Signals frente a `BehaviorSubject` en la capa de estado

- **Estado:** 🟢 **RESUELTA (2026-09-01)** · **Bloqueaba:** Fases 05 y 06
- **Decisión adoptada:** Cada store encapsula `BehaviorSubject` privados exponiendo `Observable` inmutables hacia el exterior vía `.asObservable()`, conforme a `rules/angular-rules.md` §3.2.
- **Implementación:** `services/state/*.service.ts` y fachada delegante `DevopsAgentStateService`.

---

## DP-06 — Intervalos de sondeo de tareas

- **Estado:** 🟡 **PARCIAL (2026-08-31)** · **Bloqueaba:** Fase 02 · **Sigue abierta para:** Fase 06
- **Respuesta del usuario:** *«Esta parte no era para actualizar el dashboard en vivo, pero no supe si hacerlo bien y me quedó esta deuda técnica.»* No se confirmaron los valores ni se definió un tope de reintentos.
- **Acción aplicada (la acción por defecto, que conserva el comportamiento):** los valores se trasladaron **literalmente** a `core/config/app-tuning.ts` como `POLLING_INTERVAL_IDLE_MS = 30000`
  y `POLLING_INTERVAL_ACTIVE_MS = 5000`, **sin modificarlos y sin añadir topes**.
- **Pendiente para la Fase 06:** decidir si el sondeo debe rediseñarse (¿el tablero necesita actualización en vivo por SSE en lugar de sondeo de tareas?), si se define un tope de ciclos y si se corrige la fuga del `setTimeout` recursivo (D-15 / M-06).
- **Evidencia original:** `services/devops-agent-state.service.ts:40` (`30000`), `:135` (`5000`),
  `:330` (`30000`), `:341` (`5000 : 30000`).
- **Pregunta original:** ¿`5 s` con tareas activas y `30 s` en reposo son valores acordados con negocio o son provisionales? ¿Debe existir un tope de reintentos? → **Provisionales; sin tope.**
- **Impacto si se asume mal:** afecta carga sobre el BFF y consumo de batería del cliente.

---

## DP-07 — Internacionalización

- **Estado:** 🟢 **RESUELTA (2026-09-01)** · **Bloqueaba:** Fase 03 (solo el alcance)
- **Decisión adoptada:** Centralizar en constantes en español en `features/devops-agent/domain/`. **No** se introduce `@angular/localize` ni librerías externas de i18n.
- **Implementación:** `features/devops-agent/domain/` agrupa todos los catálogos y mensajes en constantes tipadas inmutables.

---

## DP-08 — Cambio visual derivado del `loading` por flujo

- **Estado:** 🟢 **RESUELTA (2026-09-01)** · **Bloqueaba:** Fase 05
- **Decisión adoptada:** Cada nuevo store atómico controla su propio ciclo de carga (`loading`), mientras la fachada `DevopsAgentStateService` combina los estados para retrocompatibilidad total sin romper componentes heredados ni suites de pruebas.
- **Implementación:** `services/state/*.service.ts` y `combineLatest` en `DevopsAgentStateService`.

---

## DP-09 — Tipos reales de las respuestas hoy declaradas como `any`

- **Estado:** 🟢 **RESUELTA (2026-08-31)** · **Bloqueaba:** Fase 02
- **Decisión adoptada:** *«Sí, deriva los tres tipos del backend tal cual.»* Se autoriza usar
  `azure-devops-backend` / `azure-devops-agent` como fuente de verdad del contrato. **Ningún campo fue inventado.**
- **Tipos creados en `models/devops-agent.model.ts`:**

  | Tipo | Origen (fuente de verdad) | Forma |
    |---|---|---|
  | `IngestResponse` | `azure-devops-backend/.../api/Handler.java:60-69` | `{ message: string }` |
  | `PlanningChunk` | `azure-devops-backend/.../dto/planning/PlanningChunkResponse.java:23-29` | `{ id, initiativeId, sectionName, content, metadata }` — camelCase |
  | `JsonRpcError` / `JsonRpcResponse<T>` | `azure-devops-agent/.../api/JsonRpcResponse.java:13-20` | `{ jsonrpc, id, result?, error? }` |
  | `CancelTaskResponse` | `azure-devops-agent/.../JsonRpcResponseFactory.java:74-82` | `JsonRpcResponse<{ task: AgentTask }>` |
  | `SendMessageResponse` | Ya existía; `auditStory` era deducible sin riesgo | Sin cambios |

- **Resultado:** M-04 pasa de **9 a 0** ocurrencias de `any`. D-05 y D-24 saldadas.
- **Nota de contraste registrada:** `Initiative` llega en **snake_case** (adaptador pgvector) y
  `PlanningChunk` en **camelCase** (DTO Java serializado por Jackson). La discrepancia es del backend y **no se unificó** en el frontend.

---

## DP-10 — Corrección del defecto de repintado en modo zoneless (D-27)

- **Estado:** 🟢 **RESUELTA (2026-09-01)** · **Bloqueaba:** Fase 05
- **Decisión adoptada:** Mantener coherencia en el flujo de iniciativas y ciclo de vida zoneless asegurando que la fachada y el store emitan reactivamente los cambios de estado de carga, respetando R-1 y R-3.
- **Implementación:** `PlanningStateService` y `PlanningManagementComponent`.

---

## Bitácora de resoluciones

| ID        | Fecha      | Decisión adoptada                                                                                                                 | Quién          |
|-----------|------------|-----------------------------------------------------------------------------------------------------------------------------------|----------------|
| **DP-09** | 2026-08-31 | Derivar los tipos de `azure-devops-backend` / `azure-devops-agent` tal cual.                                                      | Usuario        |
| **DP-03** | 2026-08-31 | Conservar `POST '/'` con envoltorio JSON-RPC (acción recomendada).                                                                | Usuario        |
| **DP-06** | 2026-08-31 | Parcial: deuda técnica reconocida. Se aplica la acción por defecto (valores literales, sin topes). Sigue abierta para la Fase 06. | Usuario        |
| **DP-04** | 2026-09-01 | Mover los copys a `domain/` de forma exacta carácter por carácter (R-4 segura).                                                   | Plan / Usuario |
| **DP-07** | 2026-09-01 | Centralizar constantes en español sin dependencias de i18n.                                                                       | Plan / Usuario |
| **DP-01** | 2026-09-01 | Conservar alias público `messages` delegando en `refinementChatState.messages`.                                                   | Plan / Usuario |
| **DP-05** | 2026-09-01 | BehaviorSubjects encapsulados en stores atómicos exponiendo Observables inmutables.                                               | Plan / Usuario |
| **DP-08** | 2026-09-01 | Separar loading independiente por flujo en stores atómicos con agregador en fachada.                                              | Plan / Usuario |
| **DP-10** | 2026-09-01 | Preservar reactividad y ciclo de repintado en modo zoneless respetando R-1 y R-3.                                                 | Plan / Usuario |
