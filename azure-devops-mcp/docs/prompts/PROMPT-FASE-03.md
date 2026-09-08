# PROMPT PARA EJECUCIÓN — FASE 03: Adaptador REST para API Git de Azure DevOps

> Utiliza este prompt para detonar la ejecución de la Fase 03 en `azure-devops-mcp`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture Bancolombia, Spring WebFlux y Project Reactor.

Vamos a ejecutar la FASE 03 de la iniciativa "Evaluación de Pull Requests con Azure DevOps MCP":
1. Lee obligatoriamente:
   - azure-devops-mcp/docs/plan/ESTADO.md
   - azure-devops-mcp/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-mcp/docs/fases/FASE-03-adaptador-rest-git.md
   - AGENTS.md (Reglas Fase 2 - Backend e Infraestructura)
   - azure-devops-mcp/COMMIT_RULES.md

2. Ejecuta las tareas técnicas T-01 a T-05 especificadas en azure-devops-mcp/docs/fases/FASE-03-adaptador-rest-git.md:
   - Crear DTOs GitPullRequestResponse y GitPullRequestChangesResponse en infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/dto/
   - Crear PullRequestMapper en infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/mapper/
   - Crear GitPullRequestAdapter implementando PullRequestPort en infrastructure/driven-adapters/rest-consumer/src/main/java/co/com/bancolombia/consumer/
   - Crear pruebas unitarias con MockWebServer o WebClient mocks en infrastructure/driven-adapters/rest-consumer/src/test/java/co/com/bancolombia/consumer/GitPullRequestAdapterTest.java
   - Realizar wiring en applications/app-service si aplica.

3. Asegúrate de cumplir con todas las restricciones de arquitectura:
   - Mapeo estricto de DTOs técnicos a entidades del dominio (`PullRequest`, `GitChange`).
   - Manejo de excepciones técnicas mediante AzureDevOpsErrorTranslator (traducción a excepciones de dominio).
   - Reactividad con WebClient y Project Reactor.

4. Ejecuta y valida:
   - En la raíz de azure-devops-mcp: ./gradlew :rest-consumer:test

5. Genera el entregable de trazabilidad:
   - azure-devops-mcp/docs/resultados/RESULTADO-FASE-03.md
   - Actualiza azure-devops-mcp/docs/plan/ESTADO.md con la Fase 03 en 🟢 COMPLETADA.
   - Genera azure-devops-mcp/docs/fases/FASE-04-feedback-comentarios-pr.md y azure-devops-mcp/docs/prompts/PROMPT-FASE-04.md para la continuidad.

6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```
