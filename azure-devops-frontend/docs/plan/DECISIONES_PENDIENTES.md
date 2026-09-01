# DECISIONES PENDIENTES — azure-devops-frontend

> **Regla que aplica:** `rules/angular-rules.md` §7 — Política de No-Asunción.
> **Actualizado:** 2026-08-31
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

- **Estado:** 🔴 ABIERTA · **Bloquea:** Fase 05
- **Evidencia:** `src/app/features/devops-agent/services/devops-agent-state.service.ts:90`
  ```ts
  public readonly messages: Observable<Message[]> = this.refinementMessages; // Por compatibilidad con tests antiguos
  ```
- **Pregunta:** ¿Existe algún consumidor vivo de `state.messages` (dentro o fuera del repositorio)
  o puede eliminarse junto con las pruebas que lo usan?
- **Impacto si se asume mal:** eliminarlo rompe consumidores externos; conservarlo perpetúa un contrato público ambiguo con dos nombres para el mismo flujo.
- **Acción por defecto si sigue abierta:** **detenerse**. No eliminar el alias.

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

- **Estado:** 🔴 ABIERTA · **Bloquea:** Fase 03
- **Evidencia:** `services/devops-agent-state.service.ts:49-87` (~40 líneas de markdown),
  `:127`, `:227`, `:258`, `:393`.
- **Pregunta:** ¿Estos textos están aprobados por negocio y deben conservarse **carácter por carácter**, o pueden ajustarse durante la externalización?
- **Nota concreta:** el mensaje de error del chat (`:393`) dice literalmente *«Asegúrate de que corre en el puerto 8081»*. Es un detalle de infraestructura de desarrollo expuesto al usuario final. ¿Se conserva?
- **Impacto si se asume mal:** modificar copys de cara al usuario sin aprobación.
- **Acción por defecto si sigue abierta:** la Fase 03 **mueve** los textos sin alterar un solo carácter.

---

## DP-05 — Signals frente a `BehaviorSubject` en la capa de estado

- **Estado:** 🔴 ABIERTA · **Bloquea:** Fases 05 y 06
- **Contexto:** `rules/angular-rules.md` §3.2 admite explícitamente `BehaviorSubject` privado expuesto como `Observable` en los servicios de estado, y reserva los Signals para el estado local de UI (§3.1). El proyecto usa Angular 22, donde los Signals son el mecanismo recomendado.
- **Pregunta:** ¿Los nuevos stores se implementan con `signal()` / `computed()` (más idiomático en Angular 22, mejor para `OnPush` y para eliminar los getters O (n) de D-17), o se mantiene el patrón
  `BehaviorSubject` + `AsyncPipe` que describe la regla vigente?
- **Impacto si se asume mal:** decidir por cuenta propia contradice la regla escrita o desaprovecha la plataforma; en ambos casos habría que rehacer las Fases 05, 06 y 07.
- **Acción por defecto si sigue abierta:** **detenerse**. Es la decisión estructural del plan.

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

- **Estado:** ⚪ INFORMATIVA · **Bloquea:** Fase 03 (solo el alcance)
- **Contexto:** todos los copys están en español y embebidos en TypeScript y en plantillas.
- **Pregunta:** ¿Entra `@angular/localize` en el alcance de este plan, o los textos simplemente se centralizan en constantes en español?
- **Acción por defecto si sigue abierta:** centralizar en constantes en español. **No** introducir i18n.

---

## DP-08 — Cambio visual derivado del `loading` por flujo

- **Estado:** 🔴 ABIERTA · **Bloquea:** Fase 05
- **Evidencia:** un único `loading$` (`devops-agent-state.service.ts:27`) alimenta hoy el spinner y el bloqueo de entrada de las cuatro pestañas (`pages/devops-agent-home.page.ts:218, 223, 230,
  235`). El comportamiento observable actual es: **una operación en cualquier flujo bloquea la entrada de todos los demás**.
- **Pregunta:** al separar el estado, cada pestaña tendrá su propio indicador de carga. Un mensaje enviado en el Chat General dejará de bloquear el Asistente de Refinamiento. ¿Se autoriza este cambio de comportamiento visible?
- **Impacto si se asume mal:** cambio de UX no solicitado.
- **Acción por defecto si sigue abierta:** **detenerse** antes de ejecutar la Fase 05.

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

- **Estado:** 🔴 ABIERTA · **Bloquea:** Fase 05
- **Origen:** hallazgo H-3 de la Fase 01 (`docs/resultados/RESULTADO-FASE-01.md` §6).
- **Evidencia:** con `PlanningManagementComponent` montado, dos iniciativas cargadas y
  `loading() === false`, el DOM sigue mostrando *«No hay iniciativas indexadas»*. La app corre **zoneless** (`app.config.ts` no declara zona) y `editableInitiatives` es un campo plano mutado desde una suscripción RxJS, así que la vista nunca se marca como sucia. Congelado en `planning-management.component.spec.ts` →
  `«THEN publishing initiatives alone does NOT repaint the table»`.
- **Por qué funciona hoy:** por accidente. `loadInitiatives()` conmuta el `loading$` **compartido**, ese signal cambia y fuerza el repintado.
- **Pregunta:** ¿se aborda como corrección de defecto dentro de la Fase 05 (migrar el estado de vista a `signal()`) o se adelanta a una fase propia por ser un fallo funcional latente?
- **Impacto si se ignora:** la Fase 05 separa el `loading` por flujo (D-16) y elimina el único disparador reactivo. **La Gestión de Planeaciones dejaría de mostrar datos.**
- **Acción por defecto si sigue abierta:** aplicar la restricción **R-1** de `ESTADO.md` — migrar el estado de vista a signals **antes** de tocar la separación del `loading`.

---

## Bitácora de resoluciones

| ID        | Fecha      | Decisión adoptada                                                                                                                 | Quién   |
|-----------|------------|-----------------------------------------------------------------------------------------------------------------------------------|---------|
| **DP-09** | 2026-08-31 | Derivar los tipos de `azure-devops-backend` / `azure-devops-agent` tal cual.                                                      | Usuario |
| **DP-03** | 2026-08-31 | Conservar `POST '/'` con envoltorio JSON-RPC (acción recomendada).                                                                | Usuario |
| **DP-06** | 2026-08-31 | Parcial: deuda técnica reconocida. Se aplica la acción por defecto (valores literales, sin topes). Sigue abierta para la Fase 06. | Usuario |
