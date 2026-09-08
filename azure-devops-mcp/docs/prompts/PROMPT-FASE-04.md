# PROMPT PARA EJECUCIÓN — FASE 04: Casos de Uso y Adaptador para Feedback de PR

> Utiliza este prompt para detonar la ejecución de la Fase 04 en `azure-devops-mcp`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture Bancolombia, Spring WebFlux y Project Reactor.

Vamos a ejecutar la FASE 04 de la iniciativa "Evaluación de Pull Requests con Azure DevOps MCP":
1. Lee obligatoriamente:
   - azure-devops-mcp/docs/plan/ESTADO.md
   - azure-devops-mcp/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-mcp/docs/fases/FASE-04-feedback-comentarios-pr.md
   - AGENTS.md (Reglas Fase 2 - Backend e Infraestructura)
   - azure-devops-mcp/COMMIT_RULES.md

2. Ejecuta las tareas técnicas especificadas en azure-devops-mcp/docs/fases/FASE-04-feedback-comentarios-pr.md:
   - Crear modelo de dominio PullRequestComment en domain/model/src/main/java/co/com/bancolombia/model/pullrequest/
   - Extender PullRequestPort con el método createComment(...) en domain/model/src/main/java/co/com/bancolombia/model/pullrequest/gateways/
   - Implementar CreatePullRequestCommentUseCase en domain/usecase/src/main/java/co/com/bancolombia/usecase/pullrequest/
   - Crear pruebas unitarias completas en domain/model y domain/usecase.
   - Crear DTOs de thread y comentario en infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/dto/
   - Implementar createComment en GitPullRequestAdapter invocando POST /threads en Azure DevOps Git API.
   - Crear pruebas unitarias con MockWebServer en GitPullRequestAdapterTest.java.

3. Asegúrate de cumplir con todas las restricciones de arquitectura:
   - Dominio puro sin Spring ni Jackson.
   - Traducción de errores mediante AzureDevOpsErrorTranslator.
   - Reactividad con Project Reactor (Mono).

4. Ejecuta y valida:
   - ./gradlew test

5. Genera el entregable de trazabilidad:
   - azure-devops-mcp/docs/resultados/RESULTADO-FASE-04.md
   - Actualiza azure-devops-mcp/docs/plan/ESTADO.md con la Fase 04 en 🟢 COMPLETADA.
   - Genera azure-devops-mcp/docs/fases/FASE-05-mcp-tools-git.md y azure-devops-mcp/docs/prompts/PROMPT-FASE-05.md para la continuidad.

6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```
