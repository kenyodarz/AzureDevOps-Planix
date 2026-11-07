# Catálogo de Herramientas MCP (`azure-devops-mcp`)

> **Cuadrante Diátaxis: Referencia (Information-Oriented)**  
> Especificación técnica exhaustiva del catálogo de herramientas expuestas por el servidor Model
> Context Protocol (MCP) en `:8082`.

---

## 1. Visión General del Servidor MCP

El microservicio `azure-devops-mcp` implementa la especificación **Model Context Protocol (MCP)**,
permitiendo que agentes de IA y modelos LLM interactúen con Azure DevOps a través de herramientas
semánticas tipadas (`@McpTool`).

---

## 2. Herramientas de Gestión de Trabajo (Work Items & Sprints)

### `list_work_items_by_team_and_sprint`

Consulta todos los Work Items (Épicas, Features, User Stories, Bugs) asignados a un equipo en un
sprint específico.

* **Parámetros:**
    - `organization` *(string, requerido)*: Nombre de la organización de Azure DevOps.
    - `project` *(string, requerido)*: Nombre o UUID del proyecto.
    - `team` *(string, requerido)*: Nombre del equipo.
    - `iterationPath` *(string, requerido)*: Ruta de la iteración (ej. `Proyecto\\Sprint 34`).

### `get_work_item`

Recupera los detalles completos de un Work Item mediante su identificador numérico.

* **Parámetros:**
    - `id` *(integer, requerido)*: ID del Work Item.
    - `expand` *(string, opcional)*: Nivel de expansión (`all`, `relations`, `fields`).

### `get_work_items_batch`

Consulta masiva de múltiples Work Items en una única invocación HTTP.

* **Parámetros:**
    - `ids` *(array de integers, requerido)*: Lista de IDs a recuperar.
    - `fields` *(array de strings, opcional)*: Campos específicos a proyectar.

### `create_work_item`

Crea un nuevo elemento de trabajo (User Story, Task, Bug) con sus campos iniciales.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto destino.
    - `type` *(string, requerido)*: Tipo de elemento (`User Story`, `Task`, `Bug`, `Feature`).
    - `operations` *(array de JsonPatchOperation, requerido)*: Operaciones RFC 6902
      (`/fields/System.Title`, `/fields/System.Description`,
      `/fields/Microsoft.VSTS.Scheduling.StoryPoints`).

### `update_work_item`

Actualiza campos, estado o asignación de un Work Item existente mediante JSON Patch.

* **Parámetros:**
    - `id` *(integer, requerido)*: ID del Work Item.
    - `operations` *(array de JsonPatchOperation, requerido)*: Parche de operaciones a aplicar.

### `query_by_wiql`

Ejecuta una consulta en lenguaje WIQL (Work Item Query Language) contra el motor de búsqueda de
Azure DevOps.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `query` *(string, requerido)*: Sentencia WIQL (ej.
      `SELECT [System.Id] FROM WorkItems WHERE [System.WorkItemType] = 'User Story' AND [System.State] = 'Active'`).

### `get_team_iterations`

Lista las iteraciones y ventanas temporales (fechas de inicio y fin) asociadas a un equipo de
desarrollo.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `team` *(string, requerido)*: Nombre del equipo.
    - `timeframe` *(string, opcional)*: Filtro temporal (`current`, `past`, `future`).

---

## 3. Herramientas de Repositorios Git y Pull Requests

### `get_pull_request`

Recupera el estado, rama origen, rama destino, revisores y descripción de un Pull Request.

* **Parámetros:**
    - `repositoryId` *(string, requerido)*: Nombre o UUID del repositorio Git.
    - `pullRequestId` *(integer, requerido)*: Número del Pull Request.

### `get_pull_request_changes`

Lista los archivos modificados, añadidos o eliminados en el Pull Request.

* **Parámetros:**
    - `repositoryId` *(string, requerido)*: Repositorio.
    - `pullRequestId` *(integer, requerido)*: Número del PR.

### `create_pull_request_comment`

Publica un hilo de comentarios o feedback de revisión de código en un Pull Request.

* **Parámetros:**
    - `repositoryId` *(string, requerido)*: Repositorio.
    - `pullRequestId` *(integer, requerido)*: Número del PR.
    - `content` *(string, requerido)*: Texto Markdown del comentario o sugerencia.

---

## 4. Herramientas de Pipelines y CI/CD

### `list_pipelines`

Lista los pipelines de compilación y entrega continua configurados en el proyecto.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.

### `get_pipeline_run`

Consulta el estado de ejecución, resultado (`succeeded`, `failed`) y métricas de una corrida de
pipeline.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `pipelineId` *(integer, requerido)*: ID del pipeline.
    - `runId` *(integer, requerido)*: ID de la ejecución.

### `trigger_pipeline_run`

Desencadena una nueva ejecución de un pipeline sobre una rama específica.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `pipelineId` *(integer, requerido)*: ID del pipeline.
    - `branch` *(string, opcional)*: Rama (default: `main`).

### `get_pipeline_run_logs`

Descarga los logs y trazas de salida de una ejecución de pipeline para auditoría de errores.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `pipelineId` *(integer, requerido)*: ID del pipeline.
    - `runId` *(integer, requerido)*: ID de la ejecución.

---

## 5. Herramientas de Documentación (Wikis)

### `search_wiki`

Realiza búsquedas textuales de documentación y guías en las wikis del proyecto.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `searchText` *(string, requerido)*: Término de búsqueda.

### `get_wiki_page`

Recupera el contenido Markdown y metadatos de una página wiki por su ruta.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `wikiIdentifier` *(string, requerido)*: Identificador o nombre de la wiki.
    - `path` *(string, requerido)*: Ruta de la página (ej. `/Arquitectura/Lineamientos`).

### `create_or_update_wiki_page`

Crea o sobrescribe una página en la wiki de Azure DevOps.

* **Parámetros:**
    - `project` *(string, requerido)*: Proyecto.
    - `wikiIdentifier` *(string, requerido)*: Identificador de la wiki.
    - `path` *(string, requerido)*: Ruta de la página.
    - `content` *(string, requerido)*: Contenido Markdown.

---

## 6. Herramientas de Visión General (Overview)

### `get_project_overview`

Provee una síntesis ejecutiva del proyecto en Azure DevOps, incluyendo repositorios activos, últimos
builds y estado de los tableros de trabajo.

* **Parámetros:**
    - `project` *(string, requerido)*: Nombre del proyecto.
