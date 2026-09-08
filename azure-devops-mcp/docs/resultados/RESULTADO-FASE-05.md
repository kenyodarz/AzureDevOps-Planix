# RESULTADO — FASE 05: Entry Points MCP Server (Tools @McpTool para Git y Pull Requests)

> **Ejecutada:** 2026-09-08 · **Estado final:** 🟢 COMPLETADA  
> **Instrucciones:** `azure-devops-mcp/docs/fases/FASE-05-mcp-tools-git.md`  
> **Archivos nuevos:** 6 archivos (4 producción, 2 pruebas) · **Archivos modificados:** 2  
> (`McpResponseMapper.java`, `McpToolsAuthorizationTest.java`)

---

## 1. Objetivo de la fase

Exponer las operaciones de consulta y retroalimentación de Pull Requests hacia clientes MCP y
modelos de lenguaje (LLMs) mediante Spring AI MCP. Conforme a la decisión de diseño `DP-PR-03`, se
implementa el componente desacoplado `AzureDevOpsGitTools`, protegiendo los métodos con roles RBAC
(`McpRoles.HAS_READ` y `McpRoles.HAS_WRITE`), aislando el contrato público con DTOs de respuesta
dedicados y canalizando los errores mediante `McpErrorTranslator`.

---

## 2. Qué se construyó / modificó

| Artefacto                                                    | Acción     | Detalle                                                                                                                                                                                                        |
|--------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `infrastructure/.../mcp/dto/PullRequestResponse.java`        | CREADO     | DTO de respuesta inmutable (`record`) que encapsula la vista MCP de un `PullRequest` sin acoplar el cable al modelo de dominio.                                                                                |
| `infrastructure/.../mcp/dto/GitChangeResponse.java`          | CREADO     | DTO de respuesta inmutable (`record`) que representa un archivo modificado, agregado o eliminado dentro del PR.                                                                                                |
| `infrastructure/.../mcp/dto/PullRequestCommentResponse.java` | CREADO     | DTO de respuesta inmutable (`record`) que representa el hilo/comentario publicado en el PR.                                                                                                                    |
| `infrastructure/.../mcp/dto/McpResponseMapper.java`          | MODIFICADO | Incorporados métodos de mapeo estáticos puros hacia los nuevos DTOs (`toResponse(PullRequest)`, `toResponse(GitChange)`, `toGitChangeResponses(List<GitChange>)`, `toResponse(PullRequestComment)`).           |
| `infrastructure/.../mcp/tools/AzureDevOpsGitTools.java`      | CREADO     | Entry point MCP anotado con `@Component`, `@RequiredArgsConstructor`, `@McpTool` y `@PreAuthorize`, exponiendo `getPullRequest`, `getPullRequestChanges` y `createPullRequestComment`. Cero lógica de negocio. |
| `infrastructure/.../mcp/tools/AzureDevOpsGitToolsTest.java`  | CREADO     | Pruebas unitarias completas con Mockito y StepVerifier verificando delegación a casos de uso, defaults de parámetros y traducción de excepciones con `McpErrorTranslator`.                                     |
| `infrastructure/.../mcp/dto/McpGitResponseMapperTest.java`   | CREADO     | Pruebas unitarias de mapeo de DTOs comprobando asignación fiel de campos, listas y manejo de nulos.                                                                                                            |
| `applications/.../config/McpToolsAuthorizationTest.java`     | MODIFICADO | Actualizada la suite reactiva de seguridad para validar que las 3 nuevas herramientas exigen roles `READ` y `WRITE`, denegando acceso anónimo o con roles invertidos.                                          |

---

## 3. Decisiones aplicadas

| ID               | Resolución                                       | Efecto en esta fase                                                                                                       |
|------------------|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `DP-PR-01`       | Alcance de operaciones de lectura y escritura    | Se exponen `getPullRequest` y `getPullRequestChanges` con rol `READ`, y `createPullRequestComment` con rol `WRITE`.       |
| `DP-PR-03`       | Ubicación desacoplada del Entry Point MCP de Git | Se creó `AzureDevOpsGitTools.java` como componente independiente de `AzureDevOpsTools` en la capa de entry points.        |
| `B-08`           | Traducción estandarizada de errores MCP          | Cada tool encadena `.onErrorMap(McpErrorTranslator.forTool(...))` retornando códigos estables al cliente.                 |
| `DP-07` / `D-16` | Aislamiento de contrato público MCP              | Se crearon DTOs específicos de salida mapeados con `McpResponseMapper`, impidiendo fuga de detalles internos del dominio. |

---

## 4. Métricas obtenidas

| # | Métrica                                         | Antes | Después |         Meta |
|--:|-------------------------------------------------|------:|--------:|-------------:|
| 1 | Tests unitarios `:model`                        |    50 |      50 | 100% pasando |
| 2 | Tests unitarios `:usecase`                      |    44 |      44 | 100% pasando |
| 3 | Tests unitarios `:rest-consumer`                |    68 |      68 | 100% pasando |
| 4 | Tests unitarios `:mcp-server`                   |    27 |      41 | 100% pasando |
| 5 | Tests unitarios `:app-service`                  |    21 |      26 | 100% pasando |
| 6 | Mutaciones eliminadas en `:mcp-server` (Pitest) |   60% |     62% |   $\ge 60\%$ |
| 7 | Violaciones de Arquitectura                     |     0 |       0 |            0 |
| 8 | Estado del Build                                |    OK |      OK |    🟢 Limpio |

---

## 5. Comandos ejecutados y resultados

```bash
./gradlew :mcp-server:test
# Resultado: BUILD SUCCESSFUL. 41 tests ejecutados, 0 fallos. Pitest: 62% mutaciones eliminadas.

./gradlew :app-service:test
# Resultado: BUILD SUCCESSFUL. 26 tests ejecutados, 0 fallos. Pruebas de autorización RBAC al 100%.

./gradlew test
# Resultado: BUILD SUCCESSFUL. Todos los módulos en verde y 0 violaciones de Clean Architecture.
```

---

## 6. Checklist de calidad

- [x] Herramientas MCP expuestas mediante `@McpTool` y parámetros documentados con `@McpToolParam`.
- [x] Roles RBAC aplicados con `@PreAuthorize(McpRoles.HAS_READ)` y
  `@PreAuthorize(McpRoles.HAS_WRITE)`.
- [x] Contratos MCP desacoplados del dominio mediante DTOs y `McpResponseMapper`.
- [x] Traducción estandarizada de errores reactivos mediante `McpErrorTranslator`.
- [x] Pruebas unitarias completas en `AzureDevOpsGitToolsTest` y `McpGitResponseMapperTest`.
- [x] Pruebas de seguridad de métodos en `McpToolsAuthorizationTest` para las herramientas Git.
- [x] `./gradlew test` ejecutado exitosamente sin fallos ni violaciones de Clean Architecture.

---

## 7. Estado al cerrar

- Siguiente fase: `azure-devops-mcp/docs/fases/FASE-06-orquestacion-evaluacion-agente.md`
- Prompt de continuidad: `azure-devops-mcp/docs/prompts/PROMPT-FASE-06.md`
- Tablero de progreso: `azure-devops-mcp/docs/plan/ESTADO.md` actualizado a 🟢 COMPLETADA.
