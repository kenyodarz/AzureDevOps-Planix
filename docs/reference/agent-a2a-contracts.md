# Referencia de Contratos: Protocolo A2A y JSON-RPC 2.0 (`azure-devops-agent`)

> **Cuadrante Diátaxis: Referencia (Information-Oriented)**  
> Especificación técnica exhaustiva y neutral del transporte síncrono/asíncrono, métodos JSON-RPC
> 2.0 y metadatos del agente.

---

## 1. Endpoints de Transporte

| Método | Ruta                                                                     | Protocolo          | Descripción                                                           |
|--------|--------------------------------------------------------------------------|--------------------|-----------------------------------------------------------------------|
| `POST` | `/`                                                                      | JSON-RPC 2.0       | Canal principal de mensajería y despacho de tareas del agente.        |
| `GET`  | `/.well-known/agent-card.json`<br/>`/.well-known/agent.json`<br/>`/card` | HTTP JSON          | Metadatos y capacidades del agente para descubrimiento en la red A2A. |
| `GET`  | `/api/tasks`                                                             | REST JSON          | Consulta y monitoreo del estado de tareas en memoria.                 |
| `POST` | `/message:send`                                                          | REST *(Deprecado)* | Endpoint legacy. Emite cabeceras `Deprecation`, `Sunset` y `Link`.    |

---

## 2. Especificación JSON-RPC 2.0 (`POST /`)

Todas las peticiones deben incluir la cabecera `Content-Type: application/json` y cumplir con el
estándar JSON-RPC 2.0.

### 2.1 Método: `message/send`

Envía un mensaje conversacional o comando al agente para su resolución y despacho.

#### Payload de Solicitud:

```json
{
  "jsonrpc": "2.0",
  "id": "req-101",
  "method": "message/send",
  "params": {
    "message": "/plan Q3-2026 6 sprints 34 sp [Canales]",
    "contextId": "ctx-session-01",
    "metadata": {
      "front": "Canales",
      "author": "scrum-master@bancolombia.com.co"
    }
  }
}
```

#### Respuesta Exitosa (`200 OK`):

```json
{
  "jsonrpc": "2.0",
  "id": "req-101",
  "result": {
    "taskId": "task-8f92a1",
    "status": "COMPLETED",
    "intent": "PROGRAM_PLANNING",
    "content": "# Plan de Trabajo Q3-2026\n\n## Capacidad del Trimestre\n...",
    "metadata": {
      "specDocument": "ideas_planning_q3_2026.md",
      "sprintsTotal": 6,
      "capacityPoints": 204
    }
  }
}
```

#### Catálogo de Errores JSON-RPC:

| Código   | Mensaje          | Causa                                                            |
|----------|------------------|------------------------------------------------------------------|
| `-32700` | Parse error      | JSON malformado en el cuerpo de la petición.                     |
| `-32600` | Invalid Request  | Estructura JSON-RPC inválida (falta `jsonrpc`, `id` o `method`). |
| `-32601` | Method not found | El método solicitado no existe en el dispatcher.                 |
| `-32602` | Invalid params   | Parámetros incompletos o tipos incompatibles.                    |
| `-32000` | Server error     | Excepción controlada durante la ejecución del caso de uso.       |

---

### 2.2 Método: `tasks/get`

Consulta el estado actual de una tarea asíncrona o proceso en ejecución.

#### Payload de Solicitud:

```json
{
  "jsonrpc": "2.0",
  "id": "req-102",
  "method": "tasks/get",
  "params": {
    "taskId": "task-8f92a1"
  }
}
```

---

### 2.3 Método: `tasks/cancel`

Interrumpe la ejecución de una tarea en curso en el store de tareas.

#### Payload de Solicitud:

```json
{
  "jsonrpc": "2.0",
  "id": "req-103",
  "method": "tasks/cancel",
  "params": {
    "taskId": "task-8f92a1"
  }
}
```

---

## 3. Contrato Agent Card (`GET /.well-known/agent-card.json`)

Provee la ficha descriptiva para descubrimiento automático entre agentes en el ecosistema bancario:

```json
{
  "schemaVersion": "1.0",
  "name": "AzureDevOpsAgent",
  "description": "Agente autónomo para gobierno, planeación y auditoría sobre Azure DevOps.",
  "version": "1.0.0",
  "capabilities": [
    "program-planning",
    "story-refinement",
    "complexity-estimation",
    "quality-audit",
    "pull-request-evaluation"
  ],
  "protocols": ["json-rpc-2.0", "a2a-rest"],
  "endpoints": {
    "rpc": "/"
  }
}
```
