# PROMPT PARA EJECUCIÓN — FASE 02: Casos de Uso de Consulta de PRs y Cambios

> Utiliza este prompt para detonar la ejecución de la Fase 02 en `azure-devops-mcp`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture Bancolombia, programación reactiva (Project Reactor) y Spring Boot.

Vamos a ejecutar la FASE 02 de la iniciativa "Evaluación de Pull Requests con Azure DevOps MCP":
1. Lee obligatoriamente:
   - azure-devops-mcp/docs/plan/ESTADO.md
   - azure-devops-mcp/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-mcp/docs/fases/FASE-02-casos-uso-pull-request.md
   - AGENTS.md (Reglas Fase 2)
   - azure-devops-mcp/COMMIT_RULES.md

2. Ejecuta las tareas técnicas T-01 a T-04 especificadas en azure-devops-mcp/docs/fases/FASE-02-casos-uso-pull-request.md:
   - Crear GetPullRequestUseCase.java en azure-devops-mcp/domain/usecase/src/main/java/co/com/bancolombia/usecase/pullrequest/
   - Crear GetPullRequestChangesUseCase.java en azure-devops-mcp/domain/usecase/src/main/java/co/com/bancolombia/usecase/pullrequest/
   - Crear GetPullRequestUseCaseTest.java en azure-devops-mcp/domain/usecase/src/test/java/co/com/bancolombia/usecase/pullrequest/
   - Crear GetPullRequestChangesUseCaseTest.java en azure-devops-mcp/domain/usecase/src/test/java/co/com/bancolombia/usecase/pullrequest/

3. Asegúrate de cumplir con todas las restricciones de arquitectura:
   - Casos de uso 100% puros: Cero anotaciones de Spring (@Service, @Component) o frameworks.
   - Manejo de flujo reactivo (Mono/Flux) con Project Reactor.
   - Exactamente 4 archivos creados.

4. Ejecuta y valida:
   - En la raíz de azure-devops-mcp: ./gradlew :usecase:test

5. Genera el entregable de trazabilidad:
   - azure-devops-mcp/docs/resultados/RESULTADO-FASE-02.md
   - Actualiza azure-devops-mcp/docs/plan/ESTADO.md con la Fase 02 en 🟢 COMPLETADA.
   - Genera azure-devops-mcp/docs/fases/FASE-03-adaptador-rest-git.md y azure-devops-mcp/docs/prompts/PROMPT-FASE-03.md para la continuidad.

6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```
