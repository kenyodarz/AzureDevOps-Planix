# PROMPT PARA EJECUCIÓN — FASE 06: Orquestación y Prompt de Evaluación en el Agente

> Utiliza este prompt para detonar la ejecución de la Fase 06 en `azure-devops-agent` y
> `azure-devops-mcp`.

---

```text
Actúa como un desarrollador experto en Java 25, Clean Architecture Bancolombia, Spring AI, Spring WebFlux y Project Reactor.

Vamos a ejecutar la FASE 06 de la iniciativa "Evaluación de Pull Requests con Azure DevOps MCP":
1. Lee obligatoriamente:
   - azure-devops-mcp/docs/plan/ESTADO.md
   - azure-devops-mcp/docs/plan/DECISIONES_PENDIENTES.md
   - azure-devops-mcp/docs/fases/FASE-06-orquestacion-evaluacion-agente.md
   - AGENTS.md (Reglas Fase 3 - Agente)
   - azure-devops-agent/COMMIT_RULES.md

2. Ejecuta las tareas técnicas especificadas en azure-devops-mcp/docs/fases/FASE-06-orquestacion-evaluacion-agente.md:
   - Definir modelos y puertos para la evaluación automatizada de PRs en azure-devops-agent.
   - Implementar EvaluatePullRequestUseCase orquestando la consulta de metadatos, cambios vía MCP y LLM.
   - Crear plantilla de evaluación estructurada en evaluate-pull-request.st con directrices de Clean Architecture Bancolombia.
   - Desarrollar pruebas unitarias completas en el agente.

3. Asegúrate de cumplir con todas las restricciones de arquitectura:
   - Dominio puro sin dependencias de frameworks ni librerías técnicas.
   - Comunicación con MCP a través de puertos y adaptadores reactivos.
   - No exponer secretos ni tokens.

4. Ejecuta y valida:
   - ./gradlew test (en azure-devops-agent y azure-devops-mcp)

5. Genera el entregable de trazabilidad:
   - azure-devops-mcp/docs/resultados/RESULTADO-FASE-06.md
   - Actualiza azure-devops-mcp/docs/plan/ESTADO.md con la Fase 06 en 🟢 COMPLETADA.

6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```
