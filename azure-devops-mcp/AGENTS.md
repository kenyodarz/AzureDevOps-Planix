# Guía del Agente en el Servidor MCP (AGENTS.md)

Este repositorio contiene un proyecto implementado en **Java Spring Boot** estructurado bajo el patrón de arquitectura hexagonal utilizando el **Scaffold Clean Architecture de Bancolombia**. Su propósito específico es actuar como un **Servidor del Protocolo de Contexto de Modelo (MCP - Model Context Protocol)** para automatizar e interactuar de forma segura con recursos de Azure DevOps.

Cualquier agente de IA, Copilot o desarrollador humano que trabaje en este subproyecto **debe** cumplir obligatoriamente y sin excepciones con las reglas y directrices de este documento.

---

## 1. Stack Tecnológico y Propósito del Proyecto

El proyecto está diseñado exclusivamente como una aplicación de backend reactiva orientada a MCP:
- **Tecnología**: Java Spring Boot, WebFlux Reactivo y Clean Architecture de Bancolombia.
- **Protocolo de Entrada**: MCP (Model Context Protocol). El módulo de entrada expone herramientas (`@McpTool`) como `HealthTool` y `AzureDevOpsTools` para realizar consultas, crear o editar Work Items y consultar campos de equipo en Azure DevOps.
- **Arquitectura Limpia**:
  - `domain/model`: Núcleo puro del negocio (modelos de WorkItem, WIQL, configuraciones de equipo). Cero anotaciones de Spring/Jackson/JPA.
  - `domain/usecase`: Casos de uso de negocio puros (orquestadores). Inyección nativa por constructor.
  - `infrastructure/driven-adapters`: Adaptadores que implementan los puertos de dominio para comunicarse con la API de Azure DevOps, servicios externos u otros proveedores técnico-físicos.
  - `infrastructure/entry-points/mcp-server`: Gateway MCP que implementa y expone las herramientas a modelos de lenguaje (LLM).

---

## 2. Reglas del Servidor MCP y Arquitectura Hexagonal

Para garantizar un código mantenible, se deben respetar las reglas establecidas en `rules/spring-rules.md` adaptadas al protocolo MCP:

* 📗 **Dominio Limpio de Infraestructura**:
  - Prohibido el acoplamiento técnico en `domain/model` y `domain/usecase`.
  - Cero dependencias de persistencia o frameworks en la lógica core.
  - Todos los DTOs de las herramientas MCP se mapean a entidades puras usando mappers en la capa de adaptadores o en el entry-point si son específicos de la API del protocolo.
* 🛠️ **Invariantes del Protocolo MCP (mcp-server)**:
  - Mantener las herramientas MCP (`McpTool`) bien especificadas bajo la carpeta `entry-points/mcp-server/`.
  - Toda nueva herramienta expuesta al protocolo debe contar con una descripción detallada en sus parámetros para que los LLM puedan entender perfectamente cuándo y cómo invocarla.
  - No incluyas lógica de negocio pesada dentro de las clases de herramientas MCP (`AzureDevOpsTools`, etc.). Las herramientas deben delegar inmediatamente su ejecución a un caso de uso del dominio.

---

## 3. Mensajes de Commit y Estilo de Código

Todos los commits de este repositorio deben cumplir con el estándar definido de manera obligatoria en `COMMIT_RULES.md`:

Formato: `COMMIT_TYPE(SCOPE): DESCRIPTION`

- **COMMIT_TYPE**: Minúscula (`feat`, `fix`, `docs`, `refactor`, `test`, `security`, `chore`, etc.).
- **SCOPE**: Entre paréntesis, en `snake_case` (ej. `mcp_tools`, `work_items`, `azure_client`).
- **DESCRIPTION**: En español, comenzando con minúscula, claro y conciso.

*Ejemplo*: `feat(mcp_tools): agregar herramienta para consultar campos del equipo`

---

## 4. Política de No-Asunción (Principio Transversal)

**Nunca asumas información que no esté explícita**. Ante cualquier duda, ambigüedad o falta de requisitos en una tarea, el agente **debe detenerse y preguntar** al usuario.

### Ejemplos comunes que requieren detenerse:
- Cambios en las firmas, esquemas, opcionalidad de entradas o salidas de las herramientas MCP.
- Nombres de endpoints, rutas de la API de Azure DevOps que se deban integrar o parámetros nuevos configurables.
- Secretos, autenticaciones o configuraciones delicadas del sistema de DevOps.

---

## 5. Pruebas Unitarias y Calidad (Testing & Quality)

- **GIVEN / WHEN / THEN**: Todos los archivos de pruebas (`*Test.java`) deben estructurar sus casos bajo esta convención semántica clara.
- **Sin Secretos Hardcodeados (S2068)**: Bajo ningún motivo se usarán tokens de Azure DevOps (PAT), passwords o llaves privadas embebidas en el código o archivos `application.yml`. Utilizar configuración reactiva y variables de entorno del sistema.
- **Null Safety (S2259)**: Protegerse de NullPointerExceptions utilizando `Optional<T>` en retornos opcionales de consultas de cliente y validaciones explícitas de nulos.
- **Complejidad Cognitiva ≤ 15**: Métodos cortos y con Early Returns. Extraer el código complejo a funciones privadas explicativas.
- **Bloques con Llaves Obligatorios (S1117)**: Todo condicional o bucle (`if`, `else`, `for`) debe tener llaves de forma estrictamente obligatoria, incluso si es de una línea.

---

## 6. Checklist de Cumplimiento de Agente

Antes de dar una tarea por finalizada, comprueba:
- [ ] ¿El dominio y los casos de uso están completamente libres de anotaciones o importaciones de Spring?
- [ ] ¿Las herramientas de MCP se exponen de forma adecuada sin lógica de negocio, delegando al dominio?
- [ ] ¿Los parámetros de las herramientas MCP incluyen explicaciones claras en código para que la IA los use correctamente?
- [ ] ¿Se crearon los tests unitarios con JUnit 5 usando la convención GIVEN/WHEN/THEN?
- [ ] ¿El commit cumple con el formato exacto de `COMMIT_RULES.md`?
- [ ] ¿Se comprobó que no hay llaves ausentes en sentencias condicionales (`if`/`else`)?
- [ ] ¿Se verificó la no-exposición de credenciales o secretos en el código?
- [ ] Si la tarea requiere configuraciones o pruebas en un contexto real de Azure DevOps, ¿se documentaron las instrucciones de instalación y se delegó la confirmación de la prueba final al usuario?

