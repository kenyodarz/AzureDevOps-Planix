# Arquitectura General del Ecosistema Agent Harness

> **Cuadrante Diátaxis: Explicación (Understanding-Oriented)**  
> Esta sección detalla la organización de alto nivel del sistema, las responsabilidades de cada
> componente y la interacción entre capas bajo el patrón Clean Architecture.

---

## 1. Topología de Componentes

El **Azure DevOps Agent Harness** es un monorepo distribuido que resuelve la automatización del SDLC
(Software Development Life Cycle) mediante una arquitectura por capas desacoplada.

Para una exploración visual de alta fidelidad, consulta el
**[Diagrama Interactivo de Arquitectura en Archify](../diagrams/architecture.html)** (con zoom,
paneo, temas claro/oscuro y exportación).

```mermaid
graph TB
    subgraph UX["Capa de Experiencia (Frontend)"]
        User["Usuario / Desarrollador"]
        UI["azure-devops-frontend<br/>(Angular 21 · Signals · OnPush)"]
        User -->|Interacción Web / Wizard| UI
    end

    subgraph Core["Capa de Orquestación y Negocio (Harness Core)"]
        BFF["azure-devops-backend<br/>(Spring WebFlux :8081)"]
        Agent["azure-devops-agent<br/>(Clean Architecture :8080)"]
        MCP["azure-devops-mcp<br/>(Spring Boot MCP :8082)"]

        UI -->|HTTP REST / SSE| BFF
        BFF -->|JSON-RPC 2.0 (A2A)| Agent
        Agent -->|Invoca Tools| MCP
    end

    subgraph Storage["Servicios Externos y Almacenamiento"]
        LiteLLM["LiteLLM Gateway<br/>(OpenAI Protocol :4141)"]
        ADO["Azure DevOps Cloud<br/>(REST API v7)"]
        SpecFS["Spec Storage<br/>(FileSystem Markdown)"]
        PgVector["Vector Store<br/>(PostgreSQL PgVector)"]

        Agent -->|Chat Completion| LiteLLM
        Agent -->|SpecStoragePort| SpecFS
        Agent -->|PlanningVectorPort| PgVector
        MCP -->|REST API| ADO
    end

    classDef brand fill:#FDDA24,stroke:#2c2a29,stroke-width:2px,color:#2c2a29;
    classDef comp fill:#9063CD,stroke:#2c2a29,stroke-width:2px,color:#ffffff;
    classDef adapt fill:#59CBE8,stroke:#2c2a29,stroke-width:1px,color:#2c2a29;
    classDef ext fill:#00C389,stroke:#2c2a29,stroke-width:1px,color:#ffffff;

    class User,UI brand;
    class BFF,Agent,MCP comp;
    class SpecFS,PgVector adapt;
    class LiteLLM,ADO ext;
```

---

## 2. Responsabilidades por Componente

### 1. `azure-devops-agent` (Núcleo de Inteligencia y Flujos)

Implementado en **Java 21** y **Spring WebFlux** bajo el Scaffold oficial de **Clean Architecture
Bancolombia**:

* **`domain/model` (Puro):** Inmutable, sin frameworks ni dependencias técnicas. Contiene las
  entidades del agente, Value Objects de estimación (`ComplexityEstimation`), el enum exhaustivo
  `AgentIntent` y el servicio de dominio puro `IntentResolver`.
* **`domain/usecase`:** Orquestador de flujos. `AgentChatUseCase` delega a `ChatFlowDispatcher`, el
  cual enruta fail-fast hacia 7 handlers especializados (`GeneralFlowHandler`,
  `QualityAuditFlowHandler`, `RefinementFlowHandler`, `PlanningDraftFlowHandler`,
  `ApprovalFlowHandler`, `DivisionFlowHandler` y `ProgramPlanningFlowHandler`).
* **`infrastructure/driven-adapters`:**
    - `prompt-template`: Carga y renderizado de plantillas externalizadas
      (`ClasspathPromptTemplateAdapter`).
    - `spec-storage`: Persistencia documental y versionamiento en Markdown
      (`FileSystemSpecAdapter`).
    - `pgvector-store`: Almacenamiento de embeddings para búsqueda contextual y memoria de
      planeaciones.
    - `mcp-client`: Cliente para invocación de herramientas expuestas por `azure-devops-mcp`.
* **`infrastructure/entry-points`:** `reactive-web` expone el protocolo JSON-RPC 2.0 y la Agent Card
  A2A (`/.well-known/agent-card.json`).

### 2. `azure-devops-backend` (BFF Orquestador)

* Actúa como Backend-for-Frontend reactivo en **Spring WebFlux**.
* Agrega y simplifica la interacción para el frontend web, exponiendo endpoints REST y flujos
  reactivos SSE (Server-Sent Events) para actualizaciones de tareas y planeación en tiempo real.
* Gestiona la ingesta de documentos de especificación mediante `IngestPlanningSpecUseCase` y genera
  resúmenes para el dashboard con `DevOpsDashboardUseCase`.

### 3. `azure-devops-frontend` (SPA Reactiva)

* Desarrollada con **Angular 21/22 Standalone Components**, reactividad basada en **Signals** y
  estrategia **ChangeDetectionStrategy.OnPush**.
* Módulos funcionales:
    - `program-planning-wizard`: Asistente interactivo multicomponente para diagnóstico de capacidad
      de equipo y generación guiada de planeaciones.
    - `planning-specs-explorer`: Visor interactivo y explorador documental de Markdown generado por
      el agente.
    - `planning-dashboard`: Gráficas y tablas de auditoría de historias con cálculo de puntajes de
      calidad.
    - `chat-messages`: Interfaz conversacional A2A conectada al agente.

### 4. `azure-devops-mcp` (Servidor Model Context Protocol)

* Microservicio en **Spring Boot** que toolifica la API REST v7 de Azure DevOps.
* Expone herramientas estandarizadas (`@McpTool`) para:
    - Listado y creación de Work Items (`CreateWorkItemUseCase`, `GetWorkItemUseCase`).
    - Consultas analíticas en lenguaje WIQL (`QueryByWiqlUseCase`).
    - Trazabilidad y logs de Pipelines (`ListPipelinesUseCase`, `GetPipelineRunLogsUseCase`).
    - Búsqueda y actualización de páginas en Azure DevOps Wikis (`GetWikiPageUseCase`,
      `CreateOrUpdateWikiPageUseCase`).
    - Inspección de Pull Requests y comentarios automáticos (`EvaluatePullRequestUseCase`).

---

## 3. Principios de Diseño Rectores

1. **Aislamiento Total del Dominio:**  
   El núcleo de negocio de `azure-devops-agent` no importa paquetes de Spring, Jackson, AWS ni
   dependencias externas. Toda interacción se modela mediante puertos (`gateways` / `ports`).
2. **Fail-Fast en Despacho de Intenciones:**  
   `ChatFlowDispatcher` verifica durante el arranque de Spring que exista exactamente un handler
   registrado para cada uno de los 7 valores de `AgentIntent`. Si falta uno, la aplicación falla de
   inmediato, previniendo excepciones en tiempo de ejecución.
3. **Transporte A2A Desacoplado:**  
   El agente implementa el patrón Null Object (`NoOpAgentResponseAdapter`) para el camino asíncrono,
   permitiendo operar síncronamente vía HTTP JSON-RPC hoy y migrar transparentemente a Kafka sin
   tocar el dominio.
4. **Delimitación Normativa de Historias (DP-PL-02):**  
   Los prompts externalizados y el caso de uso de planeación fuerzan la regla de negocio
   corporativa: las Historias de Usuario (HU) cubren el ciclo hasta certificación en QA; el
   despliegue a producción y habilitación HyMS se aísla en Historias de Acompañamiento (HA).
