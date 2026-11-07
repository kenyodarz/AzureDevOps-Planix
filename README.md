# Azure DevOps Agent Harness

> Plataforma desacoplada de IA orientada a gobernar y acelerar el SDLC en Azure DevOps mediante
> agentes autónomos (A2A), contratos Model Context Protocol (MCP) y planeación normativa de historias
> de usuario.

---

## ⚡ Quickstart (Menos de 30 Segundos)

Levanta y prueba el ecosistema completo en tu máquina local:

### Requisitos Previos

* **Java:** JDK 21 LTS (`java -version`).
* **Node.js:** v22 LTS o superior (`node -v`).
* **Docker:** *(Opcional)* Para PostgreSQL + pgvector local.

### Puesta en Marcha Inmediata

```powershell
# 1. Configurar variables de entorno
Copy-Item .env.example .env

# 2. Levantar el Motor del Agente (Java 21 / Spring WebFlux :8080)
cd azure-devops-agent; .\gradlew.bat :app-service:bootRun

# 3. Levantar el Servidor MCP de Azure DevOps (Spring Boot :8082 - en otra terminal)
cd azure-devops-mcp; .\gradlew.bat :app-service:bootRun

# 4. Levantar el BFF Reactivo (Spring WebFlux :8081 - en otra terminal)
cd azure-devops-backend; .\gradlew.bat :app-service:bootRun

# 5. Levantar la Interfaz Web (Angular 21/22 Signals :4200 - en otra terminal)
cd azure-devops-frontend; npm install; npm start
```

Una vez levantado, accede a la interfaz gráfica en: **`http://localhost:4200`**  
*(Healthcheck del Agente: `http://localhost:8080/.well-known/agent-card.json`)*.

---

## 📂 Estructura del Monorepo

```text
AzureDevOps/
├── azure-devops-agent/       # Motor central: Clean Architecture, A2A (JSON-RPC), 7 intenciones, Program Planning
├── azure-devops-backend/     # BFF Reactivo: WebFlux, SSE para tareas en vivo, ingesta de specs Markdown
├── azure-devops-frontend/    # UI Web: Angular Standalone, Signals, Wizard de Program Planning y Dashboard
├── azure-devops-mcp/         # Servidor MCP: Herramientas para Azure DevOps (Git, PRs, Pipelines, Wikis, Work Items)
├── docs/                     # Portal de documentación unificado bajo el estándar Diátaxis
│   ├── diagrams/             # Diagramas interactivos HTML compilados con Archify (Showcase)
│   ├── tutorials/            # Tutoriales guiados para onboarding
│   ├── how-to/               # Guías prácticas orientadas a tareas
│   ├── reference/            # Especificación técnica de contratos, APIs y variables
│   ├── explanation/          # Arquitectura conceptual y filosofía del Harness
│   └── adr/                  # Architecture Decision Records inmutables (0001 a 0006)
├── mkdocs.yml                # Configuración corporativa MkDocs (Backstage TechDocs)
└── README.md                 # Este documento
```

---

## 📊 Diagramas Interactivos de Arquitectura (Archify)

Compilados con calidad **Showcase** (0 errores, 0 advertencias) en HTML autocontenido:

| Diagrama                     | Archivo                                                | Descripción                                                                                     |
|------------------------------|--------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| **Arquitectura de Software** | [architecture.html](docs/diagrams/architecture.html)   | Topología multicapa del monorepo, integración con LiteLLM y Azure DevOps.                       |
| **Secuencia Principal**      | [sequence_main.html](docs/diagrams/sequence_main.html) | Traza cronológica de extremo a extremo de la transacción de Program Planning.                   |
| **Workflow de Intenciones**  | [workflow_main.html](docs/diagrams/workflow_main.html) | Máquina de estados, compuertas de decisión, directiva HyMS (**DP-PL-02**) y supervisión humana. |

---

## 🔐 Variables de Entorno Clave

Configura `.env` con tus credenciales locales:

| Variable            | Descripción                           | Valor por Defecto                   |       Módulo       |
|:--------------------|:--------------------------------------|:------------------------------------|:------------------:|
| `AI_BASE_URL`       | URL del gateway compatible con OpenAI | `http://localhost:4141` *(LiteLLM)* |      `agent`       |
| `AI_API_KEY`        | Clave de API para inferencia          | `dummy-local-key`                   |      `agent`       |
| `AI_MODEL`          | Modelo de lenguaje para chat          | `gpt-4o-mini`                       |      `agent`       |
| `AZURE_DEVOPS_PAT`  | Personal Access Token de Azure DevOps | *Vacío*                             |       `mcp`        |
| `SPEC_STORAGE_PATH` | Carpeta de persistencia documental    | `docs/sdd/specs`                    | `agent`, `backend` |

Consulta el catálogo exhaustivo en [
`docs/reference/environment-variables.md`](docs/reference/environment-variables.md).

---

## 🧪 Pruebas Automatizadas y Calidad

```powershell
# Pruebas unitarias y de arquitectura del agente (407 pruebas en verde)
cd azure-devops-agent; .\gradlew.bat test

# Pruebas del servidor MCP
cd azure-devops-mcp; .\gradlew.bat test

# Pruebas del BFF
cd azure-devops-backend; .\gradlew.bat test

# Pruebas unitarias de frontend con Vitest
cd azure-devops-frontend; npm test
```

* **Cobertura en el Dominio:** $\ge 94\%$ en `domain/model` y `domain/usecase`.
* **ArchUnit:** 0 violaciones de aislamiento de capas.
* **PIT Mutation Testing:** Fuerza de mutación $\ge 95\%$ en adaptadores críticos.

---

## 📖 Portal de Documentación (MkDocs / TechDocs)

El repositorio incluye soporte nativo para **Backstage TechDocs** con la paleta oficial de
Bancolombia.

Para previsualizar la documentación completa localmente:

```powershell
pip install mkdocs-material mkdocs-techdocs-core
mkdocs serve
```

Abre en tu navegador: **`http://127.0.0.1:8000`**.
