# FASE 05 — Entry Points MCP Server (Tools @McpTool para Git y Pull Requests)

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01, FASE 02, FASE 03, FASE 04 · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(mcp): exponer herramientas mcp de git y evaluacion de pull requests`  
> **Siguiente fase:** `FASE-06-orquestacion-evaluacion-agente.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- Las Fases 01 a 04 implementaron el modelo de dominio (`PullRequest`, `GitChange`,
  `PullRequestComment`), los casos de uso (`GetPullRequestUseCase`, `GetPullRequestChangesUseCase`,
  `CreatePullRequestCommentUseCase`) y el adaptador reactivo de infraestructura
  `GitPullRequestAdapter`.
- El sistema cuenta con capacidades completas de consulta y publicación de feedback en Pull Requests
  contra Azure DevOps Git API con 100% de pruebas y mutaciones validadas.

### 1.2 Problema que resuelve esta fase

El LLM o agente de IA interactúa a través del protocolo Model Context Protocol (MCP). Actualmente
las capacidades de Git y PR no están expuestas como herramientas MCP anotadas con `@McpTool`.
Conforme a la decisión de diseño `DP-PR-03`, se requiere crear una clase dedicada
`AzureDevOpsGitTools.java` en el entry point `mcp-server` para exponer las tres operaciones de PR
con seguridad RBAC y traducción de respuestas/errores.

### 1.3 Estado esperado al terminar

1. En `infrastructure/entry-points/mcp-server`:
    - DTOs de respuesta MCP o mapeadores si aplica para no exponer directamente el dominio hacia el
      cliente MCP.
    - Componente `AzureDevOpsGitTools.java` con anotaciones `@Component`,
      `@RequiredArgsConstructor`, `@McpTool` y `@PreAuthorize`:
        - `getPullRequest`: anotada con `@McpTool(name = "getPullRequest", description = "...")` y
          `@PreAuthorize(McpRoles.HAS_READ_ROLE)`.
        - `getPullRequestChanges`: anotada con
          `@McpTool(name = "getPullRequestChanges", description = "...")` y
          `@PreAuthorize(McpRoles.HAS_READ_ROLE)`.
        - `createPullRequestComment`: anotada con
          `@McpTool(name = "createPullRequestComment", description = "...")` y
          `@PreAuthorize(McpRoles.HAS_WRITE_ROLE)`.
    - Manejo y traducción de errores mediante `McpErrorTranslator`.
    - Pruebas unitarias completas en `AzureDevOpsGitToolsTest.java` verificando invocación de casos
      de uso, parámetros opcionales y manejo de errores.
    - Pruebas de autorización en `McpToolsAuthorizationTest.java` verificando los permisos `READ` y
      `WRITE` sobre las nuevas herramientas.

### 1.4 Archivos involucrados

| Ruta                                                                                                                              | Acción    |
|-----------------------------------------------------------------------------------------------------------------------------------|-----------|
| `azure-devops-mcp/infrastructure/entry-points/mcp-server/src/main/java/co/com/bancolombia/mcp/tools/AzureDevOpsGitTools.java`     | CREAR     |
| `azure-devops-mcp/infrastructure/entry-points/mcp-server/src/test/java/co/com/bancolombia/mcp/tools/AzureDevOpsGitToolsTest.java` | CREAR     |
| `azure-devops-mcp/applications/app-service/src/test/java/co/com/bancolombia/config/McpToolsAuthorizationTest.java`                | MODIFICAR |
| `azure-devops-mcp/docs/resultados/RESULTADO-FASE-05.md`                                                                           | CREAR     |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Creación de `AzureDevOpsGitTools.java`:**
    - Inyectar los casos de uso: `GetPullRequestUseCase`, `GetPullRequestChangesUseCase` y
      `CreatePullRequestCommentUseCase`.
    - Implementar las tres herramientas MCP con descripciones claras para el LLM en `@McpTool` y
      parámetros documentados con `@McpToolParam`.
    - Aplicar traducción con `McpErrorTranslator` en caso de excepciones.
2. **Pruebas unitarias:**
    - Crear `AzureDevOpsGitToolsTest.java` cubriendo ejecución exitosa y escenarios de error.
    - Actualizar pruebas de seguridad RBAC (`McpToolsAuthorizationTest`).
3. **Validación:**
    - Ejecutar `./gradlew test`.

### 2.2 Criterios de aceptación

- [ ] Herramientas MCP expuestas y accesibles en el protocolo Spring AI MCP.
- [ ] Roles RBAC configurados (`READ` para consultas, `WRITE` para publicación de comentarios).
- [ ] 100% pruebas unitarias pasando y 0 violaciones de arquitectura.
