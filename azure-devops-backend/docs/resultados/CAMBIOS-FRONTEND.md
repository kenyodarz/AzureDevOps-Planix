# CAMBIOS REQUERIDOS EN EL FRONTEND — tras la Fase 03 del BFF

> **Origen:** `azure-devops-backend/docs/fases/fase-03.md` (T-12) · **Fecha:** 2026-08-29
> **Repositorio afectado:** `azure-devops-frontend` · **Estado:** ⚠️ **PENDIENTE DE APLICAR**
> **Este documento no modifica el frontend.** Lo redacta el equipo del BFF para que lo aplique el
> equipo del frontend.

---

## 1. Por qué

Hasta ahora el frontend hablaba con **dos servicios**:

```json
// proxy.conf.json — ANTES
{
  "/api": {
    "target": "http://localhost:8081"
  },
  // BFF
  "/message:send": {
    "target": "http://localhost:8082"
  }
  // AGENTE, directo
}
```

Eso tenía tres consecuencias:

1. **El chat se saltaba el BFF por completo.** Cuando el usuario escribía, el BFF no se enteraba: no
   validaba, no registraba y no podía seguir la tarea (D-21).
2. **Dos llamadas estaban rotas.** `GET /.well-known/agent-card.json` y el `POST /` con el sobre
   JSON-RPC `tasks/cancel` no las proxiaba nadie: caían en `:4200` y devolvían el `index.html` de la
   SPA (D-25, D-26).
3. **`GET /api/tasks` funcionaba por accidente**, porque BFF y agente compartían la tabla
   `agent_tasks` (D-24).

El BFF ya expone el contrato completo bajo `/api/**`. **Mientras el frontend no aplique estos
cambios, el chat seguirá llamando al endpoint legacy del agente**, cuya fecha de sunset ya venció.

---

## 2. `proxy.conf.json`

Queda con **una sola entrada**. Eliminar `"/message:send"`.

```jsonc
// proxy.conf.json — DESPUÉS
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

---

## 3. `devops-agent-api.service.ts`

### 3.1 Tabla de equivalencias

| Llamada actual                             | Llamada nueva                          | Método         |
|--------------------------------------------|----------------------------------------|----------------|
| `POST /message:send` (a `:8082`)           | `POST /api/chat/messages`              | `sendMessage`  |
| `GET /.well-known/agent-card.json`         | `GET /api/agent/card`                  | `getAgentCard` |
| `POST /` con sobre JSON-RPC `tasks/cancel` | `POST /api/tasks/{id}/cancel`          | `cancelTask`   |
| `GET /api/tasks`                           | `GET /api/tasks` *(sin cambio de URL)* | `listTasks`    |
| — *(no existía)*                           | `GET /api/tasks/{id}`                  | `getTask`      |

### 3.2 Código

```typescript
// devops-agent-api.service.ts — DESPUÉS

getAgentCard()
:
Observable < AgentCard > {
  return this.http.get<AgentCard>('/api/agent/card');
}

sendMessage(payload
:
SendMessageRequest
):
Observable < ChatMessageResponse > {
  return this.http.post<ChatMessageResponse>('/api/chat/messages', payload);
}

listTasks()
:
Observable < AgentTask[] > {
  return this.http.get<AgentTask[]>('/api/tasks');
}

getTask(id
:
string
):
Observable < AgentTask > {
  return this.http.get<AgentTask>(`/api/tasks/${id}`);
}

cancelTask(id
:
string
):
Observable < AgentTask > {
  return this.http.post<AgentTask>(`/api/tasks/${id}/cancel`, {});
}
```

> ⚠️ **`cancelTask` deja de construir un sobre JSON-RPC a mano.** El protocolo JSON-RPC es un
> detalle
> de la conversación **BFF ↔ agente**; el frontend no debe conocerlo. Basta un `POST` con cuerpo
> vacío.

---

## 4. Cuerpo de `POST /api/chat/messages`

Es el mismo que se enviaba al agente, **simplificado**: sin `role`, sin `messageId` y sin
`configuration`. Esos tres los pone el BFF.

```jsonc
{
  "message": {
    "contextId": "ctx-abc",              // opcional, ver §5
    "parts": [ { "text": "Analiza el sprint 247" } ]
  }
}
```

**Respuesta** (`ChatMessageResponse`):

```jsonc
{
  "reply": "Recibido, empiezo el análisis",
  "task": {                              // null si el agente no abrió tarea
    "id": "task-42",
    "contextId": "ctx-abc",
    "origin": "AGENT",
    "status": { "state": "working", "message": "En marcha", "timestamp": "2026-08-29T00:00:00Z" }
  }
}
```

---

## 5. `contextId`: el BFF lo genera si no lo envías

- Si el frontend **envía** `message.contextId`, el BFF lo propaga tal cual hacia el agente.
- Si **no lo envía**, el BFF genera uno y lo devuelve dentro de `task.contextId`.

**Acción para el frontend:** guardar el `contextId` de la primera respuesta y reenviarlo en los
mensajes siguientes de la misma conversación. Sin eso, cada mensaje abre una conversación nueva y el
agente pierde el hilo.

---

## 6. Cambio de modelo: `AgentTask` gana `origin`

`GET /api/tasks` devuelve ahora la **unión** de dos fuentes, cada una etiquetada:

| `origin`  | Quién creó la tarea                       | Ejemplo                 |
|-----------|-------------------------------------------|-------------------------|
| `'AGENT'` | El agente, al procesar un mensaje de chat | «refina esta historia»  |
| `'BFF'`   | El propio BFF, en el stream del dashboard | «analiza el sprint 247» |

```typescript
export interface AgentTask {
  id: string;
  contextId: string;
  origin: 'AGENT' | 'BFF';   // ← NUEVO
  status: AgentTaskStatus;
}

export interface AgentTaskStatus {
  state: 'submitted' | 'working' | 'input-required' | 'auth-required'
      | 'completed' | 'canceled' | 'rejected' | 'failed';
  message: string | null;
  timestamp: string | null;
}
```

Los ocho valores de `state` **no cambian**: siguen llegando en minúsculas y con guion
(`input-required`), tal como el frontend ya los interpreta.

`origin` es aditivo: el panel de progreso seguirá funcionando sin usarlo, pero permite agrupar o
filtrar por procedencia.

---

## 7. Códigos de error nuevos

Las rutas nuevas **ya no devuelven 400 para todo**. El frontend debe distinguir:

|    HTTP | Significado                                | Sugerencia de UI                                 |
|--------:|--------------------------------------------|--------------------------------------------------|
| **400** | Petición mal formada o parámetro inválido  | Mensaje al usuario; es culpa del cliente         |
| **404** | La tarea no existe                         | Retirarla del panel de progreso                  |
| **502** | El agente falló ejecutando (p. ej. el LLM) | «El agente tuvo un problema»; reintentable       |
| **503** | El agente no está disponible               | «Servicio no disponible»; reintentar con backoff |
| **500** | Fallo interno del BFF                      | Error genérico                                   |

El cuerpo de error mantiene la forma actual: `{ "error": "mensaje" }`.

> **No reintentar en bucle ante 502/503.** El sondeo de tareas es cada 5 s con tareas activas; un
> reintento agresivo sobre un agente caído lo empeora.

---

## 8. Lo que NO cambia

- `/api/planning/**` y `/api/devops/**`: contrato idéntico, incluido el stream SSE del dashboard.
- Los ocho valores de `TaskState`.
- El polling adaptativo (5 s con tareas activas, 30 s sin ellas). Sigue siendo necesario.

---

## 9. Mejora futura anotada, no implementada

El sondeo de 5 s existe porque el BFF no tiene forma de avisar al frontend. Se anotó como mejora
futura un `TaskUpdateNotifierPort` de salida por **SSE o WebSocket** que empujaría los cambios de
estado y eliminaría el polling. Encaja con DP-08 (no bloqueante de extremo a extremo) y sustituiría
al `AgentResponseGateway` que se retiró en esta fase por ser un canal de salida del rol equivocado.

**No está planificada todavía.** Requiere decisión del propietario.

---

## 10. Checklist para el equipo del frontend

- [ ] `proxy.conf.json` queda con una sola entrada, `/api`.
- [ ] `sendMessage` apunta a `POST /api/chat/messages` con el cuerpo simplificado de §4.
- [ ] `getAgentCard` apunta a `GET /api/agent/card`.
- [ ] `cancelTask` apunta a `POST /api/tasks/{id}/cancel` con cuerpo vacío y **sin sobre JSON-RPC**.
- [ ] `AgentTask` incorpora `origin: 'AGENT' | 'BFF'`.
- [ ] El `contextId` devuelto se guarda y se reenvía en los mensajes siguientes.
- [ ] El manejo de errores distingue 400 / 404 / 502 / 503.
- [ ] Verificado de punta a punta: con el agente **apagado**, el panel de tareas sigue mostrando las
  de origen `BFF` en lugar de quedarse vacío o romperse.

