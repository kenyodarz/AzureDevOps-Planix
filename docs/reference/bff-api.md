# Referencia de API: BFF Reactivo (`azure-devops-backend`)

> **Cuadrante Diátaxis: Referencia (Information-Oriented)**  
> Especificación técnica exhaustiva de los endpoints REST y flujos de eventos Server-Sent Events
> (SSE) expuestos por el Backend-for-Frontend (`:8081`).

---

## 1. Resumen de Rutas y Operaciones

| Método   | Ruta                                    | Tipo    | Handler                  | Descripción                                                |
|----------|-----------------------------------------|---------|--------------------------|------------------------------------------------------------|
| `POST`   | `/api/planning/ingest`                  | REST    | `Handler`                | Ingesta y almacenamiento de artefactos de planeación.      |
| `GET`    | `/api/planning/search`                  | REST    | `Handler`                | Búsqueda semántica de iniciativas y fragmentos.            |
| `GET`    | `/api/planning/initiatives`             | REST    | `Handler`                | Listado general de iniciativas registradas.                |
| `GET`    | `/api/planning/initiatives/{id}/chunks` | REST    | `Handler`                | Obtención de fragmentos/chunks de una iniciativa.          |
| `DELETE` | `/api/planning/initiatives/{id}`        | REST    | `Handler`                | Eliminación lógica de una iniciativa.                      |
| `PUT`    | `/api/planning/initiatives/{id}/cell`   | REST    | `Handler`                | Actualización de celda de roadmap/planeación.              |
| `GET`    | `/api/devops/dashboard`                 | REST    | `Handler`                | Resumen consolidado para dashboard de calidad.             |
| `GET`    | `/api/devops/dashboard/stream`          | **SSE** | `Handler`                | Flujo reactivo en tiempo real para métricas de dashboard.  |
| `POST`   | `/api/chat/messages`                    | REST    | `TaskHandler`            | Enrutamiento de mensaje hacia `azure-devops-agent`.        |
| `GET`    | `/api/tasks/stream`                     | **SSE** | `TaskHandler`            | Flujo de eventos reactivos para sincronización de tareas.  |
| `GET`    | `/api/tasks`                            | REST    | `TaskHandler`            | Listado de tareas y estados de ejecución.                  |
| `GET`    | `/api/tasks/{id}`                       | REST    | `TaskHandler`            | Consulta de detalle y estado de una tarea puntual.         |
| `POST`   | `/api/tasks/{id}/cancel`                | REST    | `TaskHandler`            | Cancelación de tarea en curso.                             |
| `GET`    | `/api/agent/card`                       | REST    | `TaskHandler`            | Proxy hacia la Agent Card del agente.                      |
| `GET`    | `/api/planning/specs`                   | REST    | `SpecHandler`            | Listado de especificaciones Markdown disponibles en disco. |
| `GET`    | `/api/planning/specs/{specName}`        | REST    | `SpecHandler`            | Contenido de un archivo Markdown de especificación.        |
| `POST`   | `/api/planning/program`                 | REST    | `ProgramPlanningHandler` | Disparo del wizard de Program Planning.                    |

---

## 2. Detalle de Endpoints Clave

### 2.1 Disparo de Program Planning (`POST /api/planning/program`)

Detona la orquestación de planeación para un trimestre e iniciativa.

#### Solicitud (`application/json`):

```json
{
  "quarter": "Q3-2026",
  "sprintsCount": 6,
  "velocityPerSprint": 34,
  "frontName": "Canales",
  "initiativeDescription": "Habilitación de autenticación biométrica y refinamiento de historias para App Empresas."
}
```

#### Respuesta Exitosa (`200 OK`):

```json
{
  "planId": "plan-q3-2026-canales",
  "status": "GENERATED",
  "specFileName": "ideas_planning_q3_2026.md",
  "totalStoryPoints": 204,
  "sprintsAllocated": 6,
  "storiesGenerated": 12,
  "summaryMarkdown": "# Plan de Trabajo Q3-2026\n\n..."
}
```

---

### 2.2 Sincronización Reactiva de Tareas (`GET /api/tasks/stream`)

Canal unidireccional **Server-Sent Events (`text/event-stream`)** para notificar cambios de estado
en segundo plano al frontend:

```text
event: task-update
data: {"taskId":"task-8f92a1","status":"PROCESSING","progressPercentage":45,"message":"Consultando capacidad en Azure DevOps..."}

event: task-update
data: {"taskId":"task-8f92a1","status":"COMPLETED","progressPercentage":100,"message":"Documento generado exitosamente."}
```

---

### 2.3 Explorador de Especificaciones (`GET /api/planning/specs`)

Lista los documentos generados por el agente persistidos a través de `SpecStoragePort`:

#### Respuesta Exitosa (`200 OK`):

```json
{
  "specs": [
    {
      "name": "ideas_planning_q3_2026.md",
      "front": "Canales",
      "lastModified": "2026-09-06T18:30:00Z",
      "sizeBytes": 14200
    }
  ]
}
```
