# RESULTADO — FASE 06: Orquestación y Prompt de Evaluación en el Agente

> **Ejecutada:** 2026-09-08 · **Estado final:** 🟢 COMPLETADA  
> **Instrucciones:** `azure-devops-mcp/docs/fases/FASE-06-orquestacion-evaluacion-agente.md`  
> **Commit sugerido:**
> `feat(agent): integrar herramientas mcp de git y orquestacion de evaluacion de pull requests`  
> **Archivos nuevos:** 11 archivos (6 producción, 5 pruebas/recursos) · **Archivos modificados:** 5
> archivos

---

## 1. Objetivo de la fase

Completar el ciclo de evaluación automatizada de Pull Requests integrando el agente
(`azure-devops-agent`) con el servidor MCP (`azure-devops-mcp`). Para lograrlo, el agente debía:

1. Definir los modelos de dominio y puertos de salida necesarios para interactuar con las
   capacidades Git de MCP.
2. Implementar el caso de uso `EvaluatePullRequestUseCase` que orquesta la consulta de metadatos y
   cambios, la generación de veredictos asistida por LLM y la publicación opcional de
   retroalimentación en Azure DevOps.
3. Diseñar la plantilla de prompt especializada `evaluate-pull-request.st` con directrices de Clean
   Architecture Bancolombia, estándares Java 25, seguridad y reactividad no bloqueante.
4. Implementar el adaptador de infraestructura `McpPullRequestAdapter` sobre el cliente MCP.
5. Desarrollar pruebas unitarias completas y validar el 100% de éxito en ambos repositorios.

---

## 2. Qué se construyó / modificó

### 2.1 En `azure-devops-agent`

| Artefacto                                                                                     | Acción     | Detalle                                                                                                                                                                        |
|-----------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/model/.../pullrequest/EvaluationVerdict.java`                                         | CREADO     | Enum puro con veredictos: `APPROVED`, `CHANGES_REQUESTED`, `REJECTED`.                                                                                                         |
| `domain/model/.../pullrequest/EvaluationFinding.java`                                         | CREADO     | Entidad inmutable para hallazgos arquitecturales o de calidad (`filePath`, `severity`, `category`, `message`, `suggestion`).                                                   |
| `domain/model/.../pullrequest/PullRequestInfo.java`                                           | CREADO     | Entidad inmutable con metadatos del PR (`pullRequestId`, `repositoryId`, `title`, `description`, `sourceRefName`, `targetRefName`, `status`, `createdBy`).                     |
| `domain/model/.../pullrequest/PullRequestChangeInfo.java`                                     | CREADO     | Entidad inmutable representando archivos afectados y tipo de cambio (`path`, `changeType`).                                                                                    |
| `domain/model/.../pullrequest/PullRequestEvaluationRequest.java`                              | CREADO     | Objeto inmutable de solicitud de evaluación con flags (`publishComment`).                                                                                                      |
| `domain/model/.../pullrequest/PullRequestEvaluation.java`                                     | CREADO     | Entidad inmutable del resultado global de evaluación (`verdict`, `summary`, `findings`, `recommendations`, `commentPublished`, `publishedCommentId`).                          |
| `domain/model/.../pullrequest/gateways/PullRequestMcpPort.java`                               | CREADO     | Puerto reactivo puro de salida: `getPullRequest`, `getPullRequestChanges`, `createPullRequestComment`.                                                                         |
| `domain/model/.../pullrequest/PullRequestEvaluationTest.java`                                 | CREADO     | Pruebas unitarias de modelos inmutables en `:model`.                                                                                                                           |
| `domain/model/.../prompt/PromptTemplateId.java`                                               | MODIFICADO | Agregada constante tipada `EVALUATE_PULL_REQUEST`.                                                                                                                             |
| `domain/usecase/.../pullrequest/EvaluatePullRequestUseCase.java`                              | CREADO     | Caso de uso puro de orquestación (consulta concurrente de PR y cambios, renderizado de prompt, llamada al LLM vía `ChatGateway`, parseo y publicación de comentario opcional). |
| `domain/usecase/.../pullrequest/EvaluatePullRequestUseCaseTest.java`                          | CREADO     | Pruebas unitarias exhaustivas con Mockito y StepVerifier cubriendo todos los caminos y fallbacks.                                                                              |
| `infrastructure/.../prompt-template/src/main/resources/prompts/evaluate-pull-request.st`      | CREADO     | Plantilla estructurada con directrices Bancolombia, seguridad, reactividad y marcadores delimitadores `EVALUATION_JSON_START/END`.                                             |
| `infrastructure/.../prompt-template/src/test/resources/test-prompts/evaluate-pull-request.st` | CREADO     | Recurso sintético para pruebas de resolución de variables en `ClasspathPromptTemplateAdapterTest`.                                                                             |
| `infrastructure/.../prompttemplate/ClasspathPromptTemplateAdapter.java`                       | MODIFICADO | Mapeo de `PromptTemplateId.EVALUATE_PULL_REQUEST` a `"evaluate-pull-request.st"`.                                                                                              |
| `infrastructure/.../prompttemplate/ClasspathPromptTemplateAdapterTest.java`                   | MODIFICADO | Pruebas unitarias actualizadas verificando renderizado de la nueva plantilla.                                                                                                  |
| `infrastructure/.../mcpclient/adapter/McpPullRequestAdapter.java`                             | CREADO     | Adaptador reactivo que traduce `PullRequestMcpPort` hacia `McpSyncClient.callTool` sobre `Schedulers.boundedElastic()`.                                                        |
| `infrastructure/.../mcpclient/adapter/McpPullRequestAdapterTest.java`                         | CREADO     | Pruebas unitarias del adaptador MCP validando mapeo de herramientas `getPullRequest`, `getPullRequestChanges` y `createPullRequestComment`.                                    |
| `applications/.../resources/prompts/evaluate-pull-request.st`                                 | CREADO     | Plantilla de evaluación desplegada en el classpath de `app-service`.                                                                                                           |
| `applications/.../prompt/PromptTemplateContentTest.java`                                      | MODIFICADO | Pruebas de integración de contenido real validando presencia de directrices y marcadores estructurados.                                                                        |
| `applications/.../config/UseCasesConfigWiringTest.java`                                       | MODIFICADO | Verificación de cableado Spring con mock de `PullRequestMcpPort` y aserción de `EvaluatePullRequestUseCase`.                                                                   |

---

## 3. Métricas Obtenidas

| Métrica                                                | Estado Previo | Resultado Fase 06 | Meta del Proyecto |
|:-------------------------------------------------------|:-------------:|:-----------------:|:-----------------:|
| **Tests Unitarios `azure-devops-agent`**               |      105      |      **118**      |   100% pasando    |
| **Mutaciones Eliminadas `azure-devops-agent:usecase`** |      74%      |      **75%**      |    $\ge 60\%$     |
| **Mutaciones Eliminadas `azure-devops-agent:model`**   |      98%      |      **98%**      |    $\ge 60\%$     |
| **Mutaciones Eliminadas `azure-devops-agent:prompt`**  |     100%      |     **100%**      |    $\ge 60\%$     |
| **Cobertura de Línea `azure-devops-agent:usecase`**    |      94%      |      **95%**      |    $\ge 80\%$     |
| **Tests Unitarios `azure-devops-mcp`**                 |      229      |      **229**      |   100% pasando    |
| **Violaciones de Arquitectura (ArchUnit)**             |       0       |       **0**       |         0         |
| **Estado del Build (`agent` y `mcp`)**                 |      OK       |   🟢 **Limpio**   |        OK         |

---

## 4. Criterios de Aceptación Cumplidos

- [x] Agente configurado para invocar herramientas de Git desde `azure-devops-mcp` vía
  `McpPullRequestAdapter`.
- [x] Prompt de evaluación de PR (`evaluate-pull-request.st`) probado, estructurado y desplegado.
- [x] Flujo de evaluación reproducible mediante pruebas unitarias exhaustivas en
  `EvaluatePullRequestUseCaseTest`.
- [x] 0 violaciones de Clean Architecture (dominio puro, dependencias invertidas).
- [x] 100% de pruebas pasando en `azure-devops-agent` y `azure-devops-mcp`.
- [x] Tablero de estado actualizado y cierre formal de la iniciativa.
