# PROMPT PARA EJECUCIÓN — FASE 05: Entry Points MCP Server (Tools @McpTool para Git)

> Utiliza este prompt para detonar la ejecución de la Fase 05 en `azure-devops-mcp`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture Bancolombia, Spring WebFlux, Spring AI MCP y Project Reactor.

Vamos a ejecutar la FASE 05 de la iniciativa "Evaluación de Pull Requests con Azure DevOps MCP":
1. Lee obligatoriamente:
   - azure-devops-mcp/docs/plan/ESTADO.md
   - azure-devops-mcp/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-mcp/docs/fases/FASE-05-mcp-tools-git.md
   - AGENTS.md (Reglas Fase 4 - MCP Server)
   - azure-devops-mcp/COMMIT_RULES.md

2. Ejecuta las tareas técnicas especificadas en azure-devops-mcp/docs/fases/FASE-05-mcp-tools-git.md:
   - Crear componente AzureDevOpsGitTools en infrastructure/entry-points/mcp-server/src/main/java/co/com/bancolombia/mcp/tools/.
   - Exponer las herramientas MCP getPullRequest, getPullRequestChanges y createPullRequestComment delegando en los casos de uso respectivos.
   - Aplicar @PreAuthorize con roles RBAC de lectura y escritura (McpRoles).
   - Aplicar McpErrorTranslator para traducción de errores.
   - Crear pruebas unitarias completas en AzureDevOpsGitToolsTest.java y actualizar pruebas de autorización en McpToolsAuthorizationTest.java.

3. Asegúrate de cumplir con todas las restricciones de arquitectura:
   - Capa entry-point pura: solo validación de entrada, delegación y traducción de respuestas/errores.
   - Sin lógica de negocio en el entry-point.
   - No exponer entidades de dominio directamente si se requiere aislamiento del contrato MCP.

4. Ejecuta y valida:
   - ./gradlew test

5. Genera el entregable de trazabilidad:
   - azure-devops-mcp/docs/resultados/RESULTADO-FASE-05.md
   - Actualiza azure-devops-mcp/docs/plan/ESTADO.md con la Fase 05 en 🟢 COMPLETADA.
   - Genera azure-devops-mcp/docs/fases/FASE-06-orquestacion-evaluacion-agente.md y azure-devops-mcp/docs/prompts/PROMPT-FASE-06.md para la continuidad.

6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```
