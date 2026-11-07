# Diseño Técnico: Portal Centralizado de Documentación y Diagramas Archify para el Monorepo Agent Harness

> **Fecha:** 2026-11-03  
> **Estado:** Documento de Diseño Aprobado para Planificación  
> **Alcance:** Monorepo Azure DevOps (`azure-devops-agent`, `azure-devops-backend`,
> `azure-devops-frontend`, `azure-devops-mcp`)  
> **Metodologías rectoras:** Diátaxis (TechDocs / MkDocs), El Narrador (README < 30s Quickstart) y
> Archify (Diagramas interactivos Showcase).

---

## 1. Contexto y Propósito (*Intent & The Why*)

El monorepo Azure DevOps aloja un ecosistema integral de **Harness de Agentes** diseñado para
automatizar y gobernar el ciclo de vida de desarrollo de software (SDLC) sobre Azure DevOps y
modelos de lenguaje corporativos:

* **`azure-devops-agent`**: Motor central del Harness en Java 21 / Spring WebFlux bajo Clean
  Architecture Bancolombia, implementando 7 intenciones de flujo conversacional y A2A (JSON-RPC 2.0
  y soporte Kafka), Program Planning, persistencia documental (`SpecStoragePort`), y reglas
  normativas HyMS/QA (**DP-PL-02**). 407 pruebas en verde.
* **`azure-devops-backend`**: BFF reactivo en Spring Boot que conecta el frontend con el agente,
  exponiendo endpoints REST y Server-Sent Events (SSE) para ingesta de especificaciones y
  sincronización de tareas.
* **`azure-devops-frontend`**: SPA en Angular 21/22 Standalone con arquitectura reactiva basada en
  Signals, OnPush y Tailwind CSS (`ProgramPlanningWizard`, visor de specs y tablero de métricas de
  calidad).
* **`azure-devops-mcp`**: Servidor de herramientas MCP (Model Context Protocol) en Spring Boot que
  expone capacidades sobre Azure DevOps (Git, Pull Requests, Work Items, WIQL, Pipelines y Wikis)
  para consumo por LLMs y agentes autónomos.

Actualmente, la documentación técnica está fragmentada: cada submódulo cuenta con plantillas base o
documentación histórica desacoplada, sin una visión unificada de sistema ni diagramas interactivos
para nuevos desarrolladores que lleguen al repositorio (*Peer Onboarding*).

El propósito de este proyecto es centralizar toda la documentación técnica en un **portal único
gobernado bajo el estándar Diátaxis y Backstage TechDocs**, acompañado de **diagramas interactivos
HTML de alta fidelidad compilados con Archify**, un **README del Narrador (< 30s Quickstart)** y un
**escaneo de dependencias y auditoría de salud técnica**.

---

## 2. Objetivos, No-Objetivos y Anti-Objetivos

### Objetivos

1. Centralizar la documentación en la raíz del repositorio en la carpeta `docs/`, gobernada por un
   archivo de configuración canónico `mkdocs.yml` compatible con Backstage TechDocs y MkDocs
   Material.
2. Aplicar la paleta visual corporativa oficial de Bancolombia (`#FDDA24`, `#00C389`, `#9063CD`,
   `#FF7F41`, `#F5B6CD`, `#59CBE8`, `#2c2a29`) en la hoja de estilos
   `docs/stylesheets/bancolombia.css`.
3. Organizar los contenidos en los cuatro cuadrantes canónicos de Diátaxis:
    - **Tutoriales (`docs/tutorials/`)**: Guía de arranque guiado paso a paso para levantar y probar
      todo el ecosistema en local en menos de 5 minutos.
    - **Guías Prácticas / How-To (`docs/how-to/`)**: Procedimientos puntuales (ejecutar un ciclo de
      Program Planning, registrar una herramienta MCP, depurar el flujo A2A, configurar variables de
      entorno y LiteLLM).
    - **Referencia (`docs/reference/`)**: Catálogo exhaustivo de contratos públicos (JSON-RPC 2.0
      del agente, REST/SSE del BFF, herramientas del servidor MCP) y diccionario de variables de
      entorno.
    - **Explicación y ADRs (`docs/explanation/`, `docs/adr/`)**: Fundamentos de arquitectura (Clean
      Architecture, A2A, MCP, Signals) y registro de decisiones de diseño consolidadas (DP-01,
      DP-02, DP-03, DP-05, DP-PL-01, DP-PL-02).
4. Diseñar y compilar tres diagramas interactivos Archify con calidad `showcase` (0 errores, 0
   advertencias) dentro de `docs/diagrams/`:
    - `architecture.html`: Arquitectura global multicapa del ecosistema monorepo.
    - `sequence_main.html`: Secuencia cronológica de Program Planning y refinamiento de historias de
      usuario.
    - `workflow_main.html`: Flujo de decisión y compuertas de enrutamiento del Harness (intenciones,
      reglas HyMS y supervisión humana).
5. Redactar el `README.md` maestro de la raíz bajo el patrón **El Narrador**, garantizando la regla
   de los 30 segundos de quickstart.
6. Generar el reporte de auditoría técnica y observabilidad en
   `docs/DEPENDENCY_SCAN_AND_IMPROVEMENTS.md`.

### No-Objetivos

- No se modificará el código fuente de producción en `src/` o `lib/` de ninguno de los cuatro
  módulos.
- No se crearán réplicas redundantes de la documentación en cada subproyecto.
- No se alterarán las ramas ni la configuración del sistema de control de versiones Git.

### Anti-Objetivos

- **Anti-patrón del monolito visual:** No se crearán diagramas masivos ilegibles; los diagramas
  Archify modelan con precisión los flujos principales, con inventario modular para demandas
  futuras.
- **Anti-patrón de documentación zombi:** Prohibido inventar datos, endpoints, métricas o comandos
  que no existan en el código actual. Todo está respaldado por la inspección real del repositorio.
- **Contaminación de estilos Diátaxis:** No mezclar teoría con pasos de tutoriales ni opiniones en
  la documentación de referencia.

---

## 3. Restricciones Técnicas

1. **Tokens de Color Oficiales Bancolombia:**
    - Marca: `primaryYellow: #FDDA24`, `primaryGreen: #00C389`, `primaryPurple: #9063CD`,
      `primaryOrange: #FF7F41`, `primaryPink: #F5B6CD`, `primaryBlue: #59CBE8`, `textDark: #2c2a29`,
      `textLight: #ffffff`, `textMuted: #b0b0b0`.
    - Semántica: `ok: #00C389`, `info: #59CBE8`, `warning: #FF7F41`, `error: #F5B6CD`,
      `secondary: #b0b0b0`.
2. **Archify CLI:**
    - Compilación portable mediante
      `node C:\Users\minaj\.gemini\config\skills\archify\bin\archify.mjs deliver <type> <input.json> <output.html> --quality showcase --json`.
    - Criterio de validación: Status `pass` (9/9 checks sin errores ni advertencias).
3. **Preservación de Artefactos Previos:**
    - La carpeta `docs/sdd/` existente que contiene el framework de Spec-Driven Development y
      plantillas se mantiene intacta como gobernanza del repositorio.

---

## 4. Enfoques Considerados y Selección

### Enfoque 1: Portal Unificado Diátaxis (TechDocs/MkDocs) con Archify Embebido (SELECCIONADO)

* **Descripción:** Concentrar toda la documentación del monorepo en `docs/` con `mkdocs.yml`
  corporativo, estructurando tutoriales, how-tos, referencias de los 4 componentes y ADRs
  transversales, integrando los diagramas HTML interactivos de Archify.
* **Por qué se eligió:** Aprobado por el usuario. Proporciona una experiencia homogénea en Backstage
  TechDocs y local, eliminando silos de información.

### Enfoque 2: Portal Central con Referencias a Subproyectos (DESCARTADO)

* **Descripción:** Documentación macro en la raíz y documentación técnica segregada en cada módulo.
* **Por qué se descartó:** Fragmenta la búsqueda de información y no aprovecha la ventaja de un
  monorepo cohesionado.

---

## 5. Arquitectura de la Solución Documental y Decisiones del Constructor (*Builder Calls*)

A partir de la revisión técnica del diseño, se fijan las siguientes decisiones operativas:

1. **Compatibilidad Dual MkDocs / TechDocs:**
    - `mkdocs.yml` empleará la configuración base de la plantilla oficial de la skill
      (`theme: material` con soporte para plugins `search` y `techdocs-core`). Esto permite servir
      la documentación localmente con `mkdocs serve` y compilarla de forma transparente en el
      pipeline de Backstage TechDocs.
2. **Estrategia de Visualización de Diagramas Archify (Resiliencia CSP):**
    - Los diagramas se compilarán como HTMLs autocontenidos en `docs/diagrams/`
      (`architecture.html`, `sequence_main.html`, `workflow_main.html`).
    - Para garantizar legibilidad inmediata sin depender de ejecución de scripts o iframes
      restringidos por políticas CSP en Backstage, cada página conceptual de explicación incluirá
      además un bloque nativo Mermaid complementario como vista estática de respaldo, acompañado del
      enlace directo al visor HTML interactivo de Archify.
3. **Mapeo Taxativo de ADRs (1:1 con Decisiones Reales):**
    - Se crearán 6 registros de decisión de arquitectura en `docs/adr/`:
        - `0001-clean-architecture-scaffold.md`: Estructura base multicapa y aislamiento de
          detalles.
        - `0002-intent-resolution-and-flows.md`: Servicio `IntentResolver`, umbral de complejidad >
          8 y fail-fast en despacho (DP-01, DP-02, DP-03).
        - `0003-externalized-prompts-and-hyms.md`: Externalización de plantillas y delimitación
          normativa HU (QA) vs HA (HyMS) (DP-PL-02).
        - `0004-a2a-dual-transport.md`: Transporte síncrono (JSON-RPC) y asíncrono para eventos
          Kafka con Null Object pattern (DP-05).
        - `0005-spec-storage-filesystem-port.md`: Almacenamiento documental y versionamiento de
          artefactos vía `SpecStoragePort` (DP-PL-01).
        - `0006-program-planning-story-engine.md`: Motor de desglose de épicas, capacidad y
          generación downstream estructurada (DP-PL-04).
4. **Metodología de `DEPENDENCY_SCAN_AND_IMPROVEMENTS.md`:**
    - Auditoría estática integral de configuración de construcción (`build.gradle`, `package.json`),
      alineación de frameworks (Java 21, Spring Boot 4.x / Clean Architecture 5.0.1, Angular 21/22
      Standalone), análisis de observabilidad (Actuator, Prometheus, Micrometer) y patrones de
      resiliencia (timeouts reactivos y circuit breakers).

### Estructura de Entregables en el Repositorio

```text
AzureDevOps/
├── README.md                                  # README Narrador raíz (< 30s Quickstart)
├── mkdocs.yml                                 # Configuración TechDocs con paleta Bancolombia
└── docs/
    ├── index.md                               # Portada y visión general del Harness
    ├── stylesheets/
    │   └── bancolombia.css                    # Tokens oficiales corporativos
    ├── diagrams/                              # Especificaciones y HTMLs compilados Archify
    │   ├── architecture.json                  # Spec Archify: Arquitectura Monorepo
    │   ├── architecture.html                  # Compilado interactivo Showcase
    │   ├── sequence_main.json                 # Spec Archify: Secuencia Program Planning
    │   ├── sequence_main.html                 # Compilado interactivo Showcase
    │   ├── workflow_main.json                 # Spec Archify: Flujo de Decisión e Intenciones
    │   └── workflow_main.html                 # Compilado interactivo Showcase
    ├── tutorials/
    │   └── quickstart-local-environment.md    # Despliegue de los 4 módulos en < 5 min
    ├── how-to/
    │   ├── run-program-planning.md            # Ejecución del wizard y comando /plan
    │   ├── register-mcp-tools.md              # Agregar nuevas herramientas al servidor MCP
    │   └── configure-environment-and-llm.md   # Setup de variables de entorno y LiteLLM
    ├── reference/
    │   ├── agent-a2a-contracts.md             # Especificación JSON-RPC 2.0 y Agent Card
    │   ├── bff-api.md                         # Endpoints REST y SSE del Backend
    │   ├── mcp-tools-catalog.md               # Herramientas expuestas por azure-devops-mcp
    │   └── environment-variables.md           # Tabla maestra de configuración
    ├── explanation/
    │   ├── ecosystem-architecture.md          # Visión transversal Clean Architecture & Signals
    │   └── agent-harness-philosophy.md        # Orquestación, desacoplamiento y Wynxx comparison
    ├── adr/
    │   ├── 0001-clean-architecture-scaffold.md
    │   ├── 0002-intent-resolution-and-flows.md
    │   ├── 0003-externalized-prompts-and-hyms.md
    │   ├── 0004-a2a-dual-transport.md
    │   ├── 0005-spec-storage-filesystem-port.md
    │   └── 0006-program-planning-story-engine.md
    └── DEPENDENCY_SCAN_AND_IMPROVEMENTS.md    # Reporte de auditoría y modernización técnica
```

---

## 6. Lo que Queda a Criterio del Constructor (*Left to the Builder*)

1. Extracción y formateo exacto de los ejemplos JSON en la referencia de API basándose en los
   modelos DTO del proyecto.
2. Definición precisa de coordenadas de nodos en los diagramas Archify garantizando balance visual y
   cumplimiento de contraste AAA.
3. Redacción didáctica orientada a *developer peer* sin tecnicismos innecesarios.
