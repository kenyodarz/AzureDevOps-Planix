# Guía Práctica: Cómo Registrar Nuevas Herramientas en el Servidor MCP

> **Cuadrante Diátaxis: Guías Prácticas / How-To (Problem-Oriented)**  
> En esta guía aprenderás cómo extender las capacidades de `azure-devops-mcp` añadiendo una nueva
> herramienta anotada con `@McpTool` bajo los principios de Clean Architecture.

---

## 🎯 Caso de Uso: Añadir Herramienta para Consultar Commits de un Repositorio

Queremos exponer una nueva tool llamada `get_repository_commits` para que los agentes puedan auditar
la historia de commits de Azure DevOps.

---

## Paso 1: Definir el Caso de Uso en `domain/usecase`

Crea la clase `GetRepositoryCommitsUseCase.java` en
`domain/usecase/src/main/java/co/com/bancolombia/usecase/git/`:

```java
package co.com.bancolombia.usecase.git;

import co.com.bancolombia.model.git.gateways.GitRepositoryGateway;
import co.com.bancolombia.model.git.GitCommit;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class GetRepositoryCommitsUseCase {
    private final GitRepositoryGateway gitGateway;

    public Flux<GitCommit> execute(String repositoryId, int top) {
        return gitGateway.findCommits(repositoryId, top);
    }
}
```

---

## Paso 2: Exponer la Herramienta en el Entry Point MCP

En
`infrastructure/entry-points/mcp-server/src/main/java/co/com/bancolombia/mcp/tools/AzureDevOpsGitTools.java`:

Añade el método anotado con `@McpTool`:

```java
@McpTool(
    name = "get_repository_commits",
    description = "Consulta la lista de commits recientes de un repositorio en Azure DevOps Git."
)
public List<GitCommitResponse> getRepositoryCommits(
        @McpParameter(name = "repositoryId", description = "Identificador o nombre del repositorio Git", required = true)
        String repositoryId,
        @McpParameter(name = "top", description = "Cantidad máxima de commits a retornar (default: 10)", required = false)
        Integer top) {
    
    int limit = (top != null && top > 0) ? top : 10;
    return getRepositoryCommitsUseCase.execute(repositoryId, limit)
            .map(mcpGitResponseMapper::toResponse)
            .collectList()
            .block(); // MCP ejecuta síncronamente sobre el protocolo
}
```

---

## Paso 3: Cablear el Bean en `applications/app-service`

En `UseCasesConfig.java`:

```java
@Bean
public GetRepositoryCommitsUseCase getRepositoryCommitsUseCase(GitRepositoryGateway gitGateway) {
    return new GetRepositoryCommitsUseCase(gitGateway);
}
```

---

## Paso 4: Validar la Compilación y Pruebas

Ejecuta en la terminal de `azure-devops-mcp`:

```powershell
.\gradlew.bat test
```

Cuando inicies el servidor MCP (`:app-service:bootRun`), el catálogo de herramientas expondrá
automáticamente `get_repository_commits` con su esquema JSON Schema para cualquier LLM o cliente
MCP.
