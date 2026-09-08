# FASE 04 — Casos de Uso y Adaptador para Feedback de PR

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01, FASE 02, FASE 03 · **Riesgo:** Medio  
> **Commit al cerrar:**
> `feat(pullrequest): agregar soporte de comentarios e hilos de feedback en pull requests`  
> **Siguiente fase:** `FASE-05-mcp-tools-git.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- La Fase 01 definió `PullRequest`, `GitChange` y `PullRequestPort` en `domain/model`.
- La Fase 02 implementó los casos de uso puros `GetPullRequestUseCase` y
  `GetPullRequestChangesUseCase` en `domain/usecase`.
- La Fase 03 implementó el adaptador reactivo `GitPullRequestAdapter`, DTOs y mapeadores en
  `infrastructure/driven-adapters/rest-consumer`.

### 1.2 Problema que resuelve esta fase

Actualmente el sistema solo permite operaciones de lectura sobre Pull Requests. Conforme a la
decisión de diseño `DP-PR-01`, se requiere que el agente MCP tenga la capacidad activa de publicar
comentarios técnicos y feedback de arquitectura directamente en hilos (*threads*) del PR en Azure
DevOps.

### 1.3 Estado esperado al terminar

Existirá:

1. En `domain/model`:
    - Modelo o record inmutable `PullRequestComment` (o `PullRequestThread`) con contenido, estado
      del hilo y autor.
    - Contrato de puerto en `PullRequestPort`:
      `Mono<PullRequestComment> createComment(String organization, String project, String repositoryId, int pullRequestId, PullRequestComment comment, String apiVersion);`.
2. En `domain/usecase`:
    - Caso de uso puro `CreatePullRequestCommentUseCase` con validación estricta de parámetros y del
      contenido del comentario.
    - Pruebas unitarias completas con 100% éxito y mutaciones eliminadas en Pitest.
3. En `infrastructure/driven-adapters/rest-consumer`:
    - DTOs de petición/respuesta de hilos (`GitPullRequestCommentRequest`,
      `GitPullRequestCommentResponse`).
    - Mapeo en `PullRequestMapper`.
    - Implementación en `GitPullRequestAdapter` invocando
      `POST .../pullRequests/{pullRequestId}/threads`.
    - Pruebas con `MockWebServer` verificando payload JSON, código 200/201 y traducción de errores
      401/403/404/500.

### 1.4 Archivos involucrados

| Ruta                                                                                                                                             | Acción    |
|--------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/PullRequestComment.java`                                       | CREAR     |
| `azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/gateways/PullRequestPort.java`                                 | MODIFICAR |
| `azure-devops-mcp/domain/model/src/test/java/co/com/bancolombia/model/pullrequest/PullRequestCommentTest.java`                                   | CREAR     |
| `azure-devops-mcp/domain/usecase/src/main/java/co/com/bancolombia/usecase/pullrequest/CreatePullRequestCommentUseCase.java`                      | CREAR     |
| `azure-devops-mcp/domain/usecase/src/test/java/co/com/bancolombia/usecase/pullrequest/CreatePullRequestCommentUseCaseTest.java`                  | CREAR     |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/dto/GitPullRequestCommentRequest.java`  | CREAR     |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/dto/GitPullRequestCommentResponse.java` | CREAR     |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/mapper/PullRequestMapper.java`          | MODIFICAR |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/GitPullRequestAdapter.java`             | MODIFICAR |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/test/java/co/com/bancolombia/consumer/GitPullRequestAdapterTest.java`         | MODIFICAR |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Modelos de Dominio:**
    - Crear `PullRequestComment` con campos `id`, `content`, `status` y `author`.
    - Extender `PullRequestPort` con el método `createComment`.
2. **Casos de Uso:**
    - Crear `CreatePullRequestCommentUseCase`, validando organización, proyecto, repositorio, ID del
      PR y contenido no vacío.
    - Escribir pruebas unitarias exhaustivas en `:usecase`.
3. **Adaptador REST:**
    - Crear DTOs para el payload de threads de Azure DevOps API (`/threads`).
    - Implementar la llamada HTTP `POST` en `GitPullRequestAdapter` con `timeout` reactivo y
      `AzureDevOpsErrorTranslator`.
    - Actualizar pruebas en `GitPullRequestAdapterTest`.
4. **Validación:**
    - Ejecutar `./gradlew test` en todo el proyecto.

### 2.2 Criterios de aceptación

- [ ] Dominio 100% puro sin librerías externas.
- [ ] 100% de pruebas unitarias pasando en `:model`, `:usecase` y `:rest-consumer`.
- [ ] `./gradlew test` exitoso.
