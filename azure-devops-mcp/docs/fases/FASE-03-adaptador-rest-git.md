# FASE 03 — Adaptador REST para API Git de Azure DevOps

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01, FASE 02 · **Riesgo:** Medio  
> **Commit al cerrar:**
>
`feat(rest_consumer): implementar adaptador reactivo para API Git y Pull Requests de Azure DevOps`  
> **Siguiente fase:** `FASE-04-feedback-comentarios-pr.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 01 definió las entidades `PullRequest`, `GitChange` y el puerto reactivo `PullRequestPort`
en `domain/model`.
La Fase 02 implementó los casos de uso `GetPullRequestUseCase` y `GetPullRequestChangesUseCase` con
100% de tests unitarios y mutaciones eliminadas en `domain/usecase`.

### 1.2 Problema que resuelve esta fase

El puerto reactivo `PullRequestPort` carece de una implementación de infraestructura que se
comunique mediante HTTP no bloqueante (`WebClient`) con la API REST de Azure DevOps para consultar
detalles de Pull Requests (`_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}`) y
sus cambios asociados
(`_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/iterations/{iterationId}/changes`
o commits/diffs). Se requiere traducir las respuestas JSON a modelos de dominio y capturar errores
HTTP mediante `AzureDevOpsErrorTranslator`.

### 1.3 Estado esperado al terminar

Existirá en `infrastructure/driven-adapters/rest-consumer`:

1. DTOs inmutables para deserializar respuestas de la API Git de Azure DevOps
   (`GitPullRequestResponse`, `GitPullRequestChangesResponse`).
2. Mapper desacoplado (`PullRequestMapper`) para transformar DTOs a entidades de dominio
   `PullRequest` y `GitChange`.
3. Adaptador reactivo `GitPullRequestAdapter` que implemente `PullRequestPort` usando `WebClient`.
4. Manejo de excepciones y códigos de error con `AzureDevOpsErrorTranslator`.
5. Pruebas unitarias completas con `MockWebServer` o mocks de `WebClient` verificando
   deserialización, headers de autorización, traducción de errores 401/404/500 y emisión reactiva.

### 1.4 Archivos involucrados

| Ruta                                                                                                                                             | Acción                |
|--------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/dto/GitPullRequestResponse.java`        | CREAR                 |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/dto/GitPullRequestChangesResponse.java` | CREAR                 |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/mapper/PullRequestMapper.java`          | CREAR                 |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/GitPullRequestAdapter.java`             | CREAR                 |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/test/java/co/com/bancolombia/consumer/GitPullRequestAdapterTest.java`         | CREAR                 |
| `azure-devops-mcp/infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/ApiVersions.java`                       | MODIFICAR (si aplica) |

### 1.5 Reglas aplicables

- Reglas de Fase 2 (`AGENTS.md`): Mapear DTOs técnicos a entidades de dominio en la infraestructura.
- Los adaptadores capturan excepciones técnicas y las traducen con `AzureDevOpsErrorTranslator`.
- Sin fugas de detalles HTTP o de Azure DevOps hacia el dominio.
- Reactividad pura con `WebClient` de Spring WebFlux.

### 1.6 Decisiones pendientes que bloquean

| ID         | Estado      | Acción si sigue ABIERTA |
|------------|-------------|-------------------------|
| `DP-PR-01` | 🟢 RESUELTA | N/A                     |
| `DP-PR-02` | 🟢 RESUELTA | N/A                     |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

* **T-01: Diseñar e implementar DTOs de respuesta de Azure DevOps**
    - Modelar `GitPullRequestResponse` mapeando campos: `pullRequestId`, `title`, `description`,
      `status`, `sourceRefName`, `targetRefName`, `repository`, `createdBy`, `creationDate`,
      `workItemRefs`.
    - Modelar `GitPullRequestChangesResponse` y sus elementos `changeTrackingId`, `item`,
      `changeType`.
* **T-02: Crear `PullRequestMapper.java`**
    - Mapear de `GitPullRequestResponse` a `PullRequest`.
    - Mapear cambios a `GitChange`.
* **T-03: Implementar `GitPullRequestAdapter.java`**
    - Implementar `PullRequestPort`.
    - Inyectar `WebClient` y `AzureDevOpsErrorTranslator`.
    - Invocar endpoints REST de Azure DevOps con versionamiento seguro (`apiVersion`).
* **T-04: Implementar pruebas unitarias en `GitPullRequestAdapterTest.java`**
    - Validar casos 200 OK para metadatos de PR y lista de cambios.
    - Validar traducción de errores 401 Unauthorized, 404 Not Found y 500 Server Error.
* **T-05: Registrar el bean en `applications/app-service` si se requiere wiring de configuración**

### 2.2 Qué NO hacer

- NO acoplar el adaptador con clases del entry-point MCP.
- NO propagar excepciones crudas de `WebClientResponseException` al dominio.
- NO omitir las pruebas unitarias del adaptador.

### 2.3 Criterios de aceptación

- [ ] Adaptador implementa `PullRequestPort` de forma 100% reactiva.
- [ ] DTOs desacoplados mediante `PullRequestMapper`.
- [ ] 100% de tests unitarios pasando en `:rest-consumer`.
- [ ] `./gradlew :rest-consumer:test` exitoso.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                            | Resultado esperado                      |
|-----:|-------------------------------------------------------------------|-----------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar estado activo de Fase 03      |
| P-02 | Implementar DTOs y Mapper                                         | Deserialización y transformación listas |
| P-03 | Implementar `GitPullRequestAdapter.java`                          | Adaptador REST reactivo listo           |
| P-04 | Implementar pruebas unitarias en `GitPullRequestAdapterTest.java` | Tests unitarios pasando                 |
| P-05 | Ejecutar `./gradlew :rest-consumer:test`                          | Verde                                   |
| P-06 | Escribir `docs/resultados/RESULTADO-FASE-03.md`                   | Registro de trazabilidad                |
| P-07 | Actualizar `docs/plan/ESTADO.md` a 🟢 COMPLETADA                  | Tablero al día                          |
| P-08 | Generar `docs/fases/FASE-04-feedback-comentarios-pr.md`           | Siguiente fase lista                    |
