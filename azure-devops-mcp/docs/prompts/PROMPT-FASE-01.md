# PROMPT PARA EJECUCIÓN — FASE 01: Modelos de Dominio y Puertos de Git / Pull Request

> Utiliza este prompt para detonar la ejecución de la Fase 01 con cualquier sesión o agente de IA en
> `azure-devops-mcp`.

---

```text
Actúa como un desarrollador experto en Java 17, Clean Architecture Bancolombia, programación reactiva (Project Reactor) y Spring Boot.

Vamos a ejecutar la FASE 01 de la iniciativa "Evaluación de Pull Requests con Azure DevOps MCP":
1. Lee obligatoriamente:
   - azure-devops-mcp/docs/plan/ESTADO.md
   - azure-devops-mcp/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-mcp/docs/fases/FASE-01-modelos-dominio-pull-request.md
   - AGENTS.md (Reglas Fase 2 y Fase 4)
   - azure-devops-mcp/COMMIT_RULES.md

2. Ejecuta todas las tareas técnicas T-01 a T-04 especificadas en azure-devops-mcp/docs/fases/FASE-01-modelos-dominio-pull-request.md sin desviaciones:
   - Crear GitChange.java en azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/
   - Crear PullRequest.java en azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/
   - Crear PullRequestPort.java en azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/gateways/
   - Crear PullRequestTest.java en azure-devops-mcp/domain/model/src/test/java/co/com/bancolombia/model/pullrequest/

3. Asegúrate de cumplir con todas las restricciones de arquitectura:
   - Dominio 100% puro: Cero anotaciones de Jackson, Spring o serialización técnica.
   - Modelos inmutables con Lombok (@Getter, @Builder).
   - Exactamente 4 archivos tocados.

4. Ejecuta y valida:
   - En la raíz de azure-devops-mcp: ./gradlew :model:test

5. Genera el entregable de trazabilidad:
   - azure-devops-mcp/docs/resultados/RESULTADO-FASE-01.md (siguiendo docs/resultados/_PLANTILLA_RESULTADO.md)
   - Actualiza azure-devops-mcp/docs/plan/ESTADO.md con la Fase 01 en 🟢 COMPLETADA.
   - Genera azure-devops-mcp/docs/fases/FASE-02-casos-uso-pull-request.md y azure-devops-mcp/docs/prompts/PROMPT-FASE-02.md para la continuidad.

6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```
