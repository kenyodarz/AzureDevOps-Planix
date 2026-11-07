# Referencia: Diccionario de Variables de Entorno

> **Cuadrante Diátaxis: Referencia (Information-Oriented)**  
> Catálogo maestro de configuración y variables de entorno para los cuatro módulos del monorepo.

---

## 1. Variables de Modelos de Lenguaje (LiteLLM / OpenAI Gateway)

| Variable             | Descripción                                               | Valor Local por Defecto             | Módulo               | Obligatoria en Prod |
|----------------------|-----------------------------------------------------------|-------------------------------------|----------------------|:-------------------:|
| `AI_API_KEY`         | Clave de API para el gateway de inferencia.               | `dummy-local-key`                   | `azure-devops-agent` |       **Sí**        |
| `AI_BASE_URL`        | URL base del endpoint compatible con OpenAI.              | `http://localhost:4141` *(LiteLLM)* | `azure-devops-agent` |       **Sí**        |
| `AI_MODEL`           | Nombre del modelo para chat completion.                   | `z-ai/glm-5.3-flash` o `gpt-5-mini` | `azure-devops-agent` |       **Sí**        |
| `AI_EMBEDDING_MODEL` | Nombre del modelo para cálculo de embeddings vectoriales. | `text-embedding-ada-002`            | `azure-devops-agent` |       **Sí**        |

---

## 2. Variables de Azure DevOps

| Variable                            | Descripción                                                        | Valor Local por Defecto                   | Módulo             | Obligatoria en Prod |
|-------------------------------------|--------------------------------------------------------------------|-------------------------------------------|--------------------|:-------------------:|
| `AZURE_DEVOPS_PAT`                  | Personal Access Token con permisos en Work Items, Git y Pipelines. | *Vacío*                                   | `azure-devops-mcp` |       **Sí**        |
| `AZURE_DEVOPS_DEFAULT_ORGANIZATION` | Nombre de la organización en Azure DevOps Cloud.                   | `grupobancolombia`                        | `agent`, `mcp`     |       **Sí**        |
| `AZURE_DEVOPS_DEFAULT_PROJECT`      | Nombre o identificador del proyecto principal.                     | `Vicepresidencia Servicios de Tecnología` | `agent`, `mcp`     |       **Sí**        |

---

## 3. Puertos de Servicios e Integración Inter-Módulos

| Variable                | Descripción                                            | Valor Local por Defecto          | Módulo                  |
|-------------------------|--------------------------------------------------------|----------------------------------|-------------------------|
| `SERVER_PORT` (Agent)   | Puerto HTTP/JSON-RPC del agente.                       | `8080` (o `8082` en app-service) | `azure-devops-agent`    |
| `SERVER_PORT` (Backend) | Puerto HTTP REST/SSE del BFF.                          | `8081`                           | `azure-devops-backend`  |
| `SERVER_PORT` (MCP)     | Puerto del servidor MCP de Azure DevOps.               | `8082`                           | `azure-devops-mcp`      |
| `MCP_AZURE_DEVOPS_URL`  | URL de conexión del cliente MCP hacia el servidor MCP. | `http://localhost:8080`          | `azure-devops-agent`    |
| `AGENT_BASE_URL`        | URL para que el BFF invoque al agente por JSON-RPC.    | `http://localhost:8080`          | `azure-devops-backend`  |
| `API_BASE_URL`          | URL del BFF consumida por la SPA de Angular.           | `http://localhost:8081`          | `azure-devops-frontend` |

---

## 4. Persistencia y Bases de Datos

| Variable                     | Descripción                                                    | Valor Local por Defecto                               | Módulo               |
|------------------------------|----------------------------------------------------------------|-------------------------------------------------------|----------------------|
| `SPRING_DATASOURCE_URL`      | URL JDBC de conexión a PostgreSQL (con extensión pgvector).    | `jdbc:postgresql://localhost:5432/agent_azure_devops` | `azure-devops-agent` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base de datos.                                      | `postgres`                                            | `azure-devops-agent` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de base de datos.                                   | `postgres`                                            | `azure-devops-agent` |
| `SPEC_STORAGE_PATH`          | Ruta del directorio local para persistencia de specs Markdown. | `docs/sdd/specs`                                      | `agent`, `backend`   |

---

## 5. Flags de Transición y Legacy A2A

| Variable                          | Descripción                                                          | Valor por Defecto |
|-----------------------------------|----------------------------------------------------------------------|-------------------|
| `A2A_LEGACY_MESSAGE_SEND_ENABLED` | Habilita el endpoint REST obsoleto `/message:send`.                  | `true`            |
| `A2A_LEGACY_SUNSET_DATE`          | Fecha ISO-8601 de retiro definitivo de la ruta legacy.               | `2026-06-30`      |
| `A2A_DISABLE_LEGACY_AFTER_SUNSET` | Apaga automáticamente la ruta legacy tras vencer la fecha de sunset. | `false`           |
