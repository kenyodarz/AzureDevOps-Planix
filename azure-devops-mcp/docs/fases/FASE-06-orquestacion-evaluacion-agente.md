# FASE 06 — Orquestación y Prompt de Evaluación en el Agente

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 05 · **Riesgo:** Medio  
> **Commit al cerrar:**
> `feat(agent): integrar herramientas mcp de git y orquestacion de evaluacion de pull requests`  
> **Siguiente fase:** N/A (Fase final de la iniciativa)

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- Las Fases 01 a 05 implementaron en `azure-devops-mcp` el soporte integral para Pull Requests:
    - Modelos de dominio (`PullRequest`, `GitChange`, `PullRequestComment`).
    - Casos de uso de consulta y publicación de feedback (`GetPullRequestUseCase`,
      `GetPullRequestChangesUseCase`, `CreatePullRequestCommentUseCase`).
    - Adaptador reactivo contra Azure DevOps Git REST API (`GitPullRequestAdapter`).
    - Herramientas MCP expuestas y securizadas con RBAC en `AzureDevOpsGitTools` (`getPullRequest`,
      `getPullRequestChanges`, `createPullRequestComment`).

### 1.2 Problema que resuelve esta fase

El agente (`azure-devops-agent`) necesita ser capaz de:

1. Consumir las nuevas herramientas MCP de Git (`getPullRequest`, `getPullRequestChanges`,
   `createPullRequestComment`) a través del cliente MCP (`mcp-client`).
2. Contar con un caso de uso o flujo de orquestación para la revisión/evaluación automatizada de un
   Pull Request:
    - Consultar metadatos del PR y cambios realizados.
    - Analizar código respecto a estándares, Clean Architecture y seguridad.
    - Generar un veredicto técnico y opcionalmente publicar feedback en el PR.
3. Disponer de un prompt especializado de evaluación de PR (`evaluate-pull-request.st`) con
   instrucciones claras para el LLM sobre criterios de aceptación, detección de deuda técnica y
   formato de comentarios.

### 1.3 Estado esperado al terminar

1. En `azure-devops-agent`:
    - Configuración del cliente MCP actualizada para reconocer las herramientas de Git.
    - Plantilla de prompt `evaluate-pull-request.st` en `prompt-template` orientada a revisión de
      arquitectura y calidad.
    - Caso de uso o endpoint para solicitar la evaluación de un Pull Request específico.
    - Pruebas unitarias completas de la orquestación y del renderizado de prompts.
2. Cierre formal de la iniciativa de Evaluación de Pull Requests en el tablero de estado.

### 1.4 Archivos involucrados

| Ruta                                                                                                 | Módulo               | Acción |
|------------------------------------------------------------------------------------------------------|----------------------|--------|
| `domain/model/.../pullrequest/PullRequestEvaluation.java`                                            | `azure-devops-agent` | CREAR  |
| `domain/usecase/.../EvaluatePullRequestUseCase.java`                                                 | `azure-devops-agent` | CREAR  |
| `infrastructure/driven-adapters/prompt-template/src/main/resources/prompts/evaluate-pull-request.st` | `azure-devops-agent` | CREAR  |
| `domain/usecase/.../EvaluatePullRequestUseCaseTest.java`                                             | `azure-devops-agent` | CREAR  |
| `azure-devops-mcp/docs/resultados/RESULTADO-FASE-06.md`                                              | `azure-devops-mcp`   | CREAR  |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Definición de modelos y contratos en el Agente:**
    - Crear el modelo de evaluación `PullRequestEvaluation` (veredicto, hallazgos, recomendaciones).
    - Crear el caso de uso `EvaluatePullRequestUseCase` que interactúa con el cliente MCP y el
      modelo de lenguaje.
2. **Plantilla de Evaluación (Prompt):**
    - Diseñar `evaluate-pull-request.st` con instrucciones de Clean Architecture Bancolombia,
      convenciones de nombres, seguridad y pruebas.
3. **Pruebas y Validación:**
    - Pruebas unitarias de casos de uso y de renderizado de prompts.
    - Ejecución de pruebas y validación de arquitectura `./gradlew test`.

### 2.2 Criterios de aceptación

- [ ] Agente configurado para invocar herramientas de Git desde `azure-devops-mcp`.
- [ ] Prompt de evaluación de PR probado y estructurado.
- [ ] Flujo de evaluación reproducible mediante pruebas unitarias.
- [ ] 0 violaciones de Clean Architecture y 100% de pruebas pasando.
