# Plan de Ejecución: Portal Centralizado de Documentación y Diagramas Archify

> **Fecha:** 2026-11-03  
> **Documento de Diseño Rector:** [
> `docs/superpowers/specs/2026-11-03-agent-harness-centralized-docs-design.md`](file:///C:/Users/minaj/Work/GitHub/Labs/AzureDevOps/docs/superpowers/specs/2026-11-03-agent-harness-centralized-docs-design.md)  
> **Estado:** Listo para Ejecución

---

## Resumen Ejecutivo de Fases

|    Fase    | Título                                          | Entregables Principales                                                                                 |
|:----------:|:------------------------------------------------|:--------------------------------------------------------------------------------------------------------|
| **Fase 1** | Configuración Base MkDocs & Estilos Bancolombia | `mkdocs.yml`, `docs/stylesheets/bancolombia.css`, `docs/index.md`                                       |
| **Fase 2** | Modelado y Compilación de Diagramas Archify     | `docs/diagrams/` (`architecture.{json,html}`, `sequence_main.{json,html}`, `workflow_main.{json,html}`) |
| **Fase 3** | Cuadrante Explicación & ADRs                    | `docs/explanation/` (Arquitectura & Filosofía) y `docs/adr/0001` a `0006`                               |
| **Fase 4** | Cuadrante Referencia Técnica                    | `docs/reference/` (A2A, BFF REST/SSE, MCP Tools, Environment Variables)                                 |
| **Fase 5** | Cuadrantes Tutoriales & Guías How-To            | `docs/tutorials/quickstart-local.md`, `docs/how-to/` (Planning, MCP Tools, LiteLLM)                     |
| **Fase 6** | README del Narrador & Auditoría de Dependencias | `README.md` raíz (< 30s Quickstart), `docs/DEPENDENCY_SCAN_AND_IMPROVEMENTS.md`                         |

---

## Fase 1: Configuración Base MkDocs & Estilos Bancolombia

- **Objetivo:** Establecer la infraestructura del portal de documentación conforme a Backstage
  TechDocs y la identidad corporativa oficial.
- **Tareas:**
    1. Copiar y adaptar `assets/templates/mkdocs.template.yml` en `mkdocs.yml` en la raíz.
    2. Crear `docs/stylesheets/bancolombia.css` a partir de `assets/templates/bancolombia.css` con
       los tokens `primaryYellow: #FDDA24`, `primaryGreen: #00C389`, etc.
    3. Crear `docs/index.md` como página de inicio y portal de navegación del Harness.
- **Criterio de Aceptación:** Archivos creados y sintaxis YAML válida.

---

## Fase 2: Modelado y Compilación de Diagramas Archify (Showcase)

- **Objetivo:** Producir los 3 diagramas interactivos HTML con calidad `showcase` verificada por el
  CLI de Archify.
- **Tareas:**
    1. Diseñar `docs/diagrams/architecture.json` (schema v1) modelando los 4 módulos (Frontend
       Angular, BFF WebFlux, Agent Core A2A, MCP Server) e integraciones con Azure DevOps y LiteLLM.
    2. Diseñar `docs/diagrams/sequence_main.json` (schema v1) modelando la transacción principal de
       Program Planning y refinamiento de historias.
    3. Diseñar `docs/diagrams/workflow_main.json` (schema v2) modelando las compuertas de decisión
       de intenciones del agente, supervisión humana y validación normativa HyMS.
    4. Ejecutar el compilador `archify deliver` para cada diagrama y verificar 0 errores y 0
       advertencias (`status: pass`).
- **Criterio de Aceptación:** Los 3 archivos `.html` generados y validados con `quality: showcase`.

---

## Fase 3: Cuadrante de Explicación y ADRs

- **Objetivo:** Documentar la arquitectura transversal y consagrar las 6 decisiones de diseño del
  repositorio.
- **Tareas:**
    1. Redactar `docs/explanation/ecosystem-architecture.md` con mapa conceptual y diagramas Mermaid
       estáticos de respaldo.
    2. Redactar `docs/explanation/agent-harness-philosophy.md` con el análisis de desacoplamiento y
       comparativa contra Wynxx.
    3. Redactar los 6 ADRs bajo el formato canónico Nygard en `docs/adr/`:
        - `0001-clean-architecture-scaffold.md`
        - `0002-intent-resolution-and-flows.md`
        - `0003-externalized-prompts-and-hyms.md`
        - `0004-a2a-dual-transport.md`
        - `0005-spec-storage-filesystem-port.md`
        - `0006-program-planning-story-engine.md`
- **Criterio de Aceptación:** Documentos redactados explicando el *por qué* de las decisiones de
  diseño.

---

## Fase 4: Cuadrante de Referencia Técnica

- **Objetivo:** Documentar de forma neutral y exhaustiva las interfaces públicas, APIs y contratos.
- **Tareas:**
    1. Redactar `docs/reference/agent-a2a-contracts.md` con las especificaciones de JSON-RPC 2.0
       (`message/send`, `tasks/get`, `tasks/cancel`) y Agent Card.
    2. Redactar `docs/reference/bff-api.md` con los endpoints REST y flujos reactivos SSE de
       `azure-devops-backend`.
    3. Redactar `docs/reference/mcp-tools-catalog.md` con las herramientas `@McpTool` expuestas por
       `azure-devops-mcp`.
    4. Redactar `docs/reference/environment-variables.md` con el catálogo completo de variables de
       entorno y defaults locales.
- **Criterio de Aceptación:** Contratos reflejados fielmente a partir del código fuente.

---

## Fase 5: Cuadrantes de Tutoriales y Guías How-To

- **Objetivo:** Proveer guías orientadas a la acción y aprendizaje práctico para nuevos
  desarrolladores.
- **Tareas:**
    1. Redactar `docs/tutorials/quickstart-local-environment.md` con instrucciones paso a paso para
       levantar los 4 módulos localmente en < 5 minutos.
    2. Redactar `docs/how-to/run-program-planning.md` detallando la ejecución del wizard de
       planeación en el frontend y el comando `/plan`.
    3. Redactar `docs/how-to/register-mcp-tools.md` explicando cómo agregar una nueva herramienta
       MCP en Clean Architecture.
    4. Redactar `docs/how-to/configure-environment-and-llm.md` para configuración de LiteLLM,
       credenciales y Azure DevOps PAT.
- **Criterio de Aceptación:** Comandos copy-paste ejecutables y guías didácticas sin supuestos
  tácitos.

---

## Fase 6: README del Narrador y Auditoría de Dependencias

- **Objetivo:** Publicar el README raíz orientado a onboarding de 30 segundos y generar el informe
  de salud técnica.
- **Tareas:**
    1. Actualizar `README.md` de la raíz con la plantilla del Narrador
       (`README.narrator.template.md`).
    2. Generar `docs/DEPENDENCY_SCAN_AND_IMPROVEMENTS.md` con el diagnóstico de dependencias de
       Gradle y npm, observabilidad y resiliencia.
- **Criterio de Aceptación:** `README.md` completo y reporte de auditoría generado sin advertencias.
