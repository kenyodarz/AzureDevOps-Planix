# FASE 01 — Modelos de Dominio y Puertos de Git / Pull Request

> **Estado:** 🟡 PENDIENTE · **Depende de:** Ninguna (Fase inicial) · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(mcp): definir modelos de dominio y puertos para pull requests`  
> **Siguiente fase:** `FASE-02-casos-uso-pull-request.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

El módulo `azure-devops-mcp` implementa Clean Architecture con soporte probado para Work Items
(`domain/model/src/main/java/co/com/bancolombia/model/workitem`). El dominio es 100% puro (cero
dependencias de Spring, Jackson o frameworks externos), tras el cierre exitoso del plan maestro de
refactorización archivado en `docs/historico/plan_refactorizacion_mcp/`.

### 1.2 Problema que resuelve esta fase

Actualmente no existen modelos de dominio ni puertos gateway que representen el concepto de Pull
Requests, iteraciones de código o cambios en archivos de repositorios Git de Azure DevOps.

### 1.3 Estado esperado al terminar

Existirá el paquete `co.com.bancolombia.model.pullrequest` con:

1. Las entidades inmutables `PullRequest` y `GitChange`.
2. La interfaz de puerto reactivo `PullRequestPort` (`Mono`/`Flux`).
3. Pruebas unitarias de dominio que validen la inmutabilidad y construcción correcta mediante
   builders y records.

### 1.4 Archivos involucrados

| Ruta                                                                                                             | Acción |
|------------------------------------------------------------------------------------------------------------------|--------|
| `azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/PullRequest.java`              | CREAR  |
| `azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/GitChange.java`                | CREAR  |
| `azure-devops-mcp/domain/model/src/main/java/co/com/bancolombia/model/pullrequest/gateways/PullRequestPort.java` | CREAR  |
| `azure-devops-mcp/domain/model/src/test/java/co/com/bancolombia/model/pullrequest/PullRequestTest.java`          | CREAR  |

### 1.5 Reglas aplicables

- Reglas de Fase 2 / Fase 4 (`AGENTS.md`): Dominio 100% puro, sin anotaciones de Spring, Jackson o
  serialización técnica.
- Inmutabilidad con Lombok (`@Getter`, `@Builder(toBuilder = true)`).
- Regla Anti-Monolito: Exactamente 4 archivos tocados en esta fase.

### 1.6 Decisiones pendientes que bloquean

| ID         | Estado      | Acción si sigue ABIERTA |
|------------|-------------|-------------------------|
| `DP-PR-01` | 🟢 RESUELTA | N/A                     |
| `DP-PR-02` | 🟢 RESUELTA | N/A                     |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

* **T-01: Crear la entidad inmutable `GitChange.java`**
    * Ubicación: `co.com.bancolombia.model.pullrequest.GitChange`
    * Atributos: `itemPath` (String), `changeType` (String, ej. "edit", "add", "delete"),
      `originalObjectId` (String), `newObjectId` (String).
* **T-02: Crear la entidad inmutable `PullRequest.java`**
    * Ubicación: `co.com.bancolombia.model.pullrequest.PullRequest`
    * Atributos: `pullRequestId` (int), `title` (String), `description` (String), `status` (String),
      `sourceRefName` (String), `targetRefName` (String), `repositoryId` (String), `createdBy`
      (String), `creationDate` (String), `workItemIds` (List<Integer>).
* **T-03: Crear la interfaz gateway `PullRequestPort.java`**
    * Ubicación: `co.com.bancolombia.model.pullrequest.gateways.PullRequestPort`
    * Métodos del contrato:
        *
        `Mono<PullRequest> getPullRequestById(String organization, String project, String repositoryId, int pullRequestId, String apiVersion);`
        *
        `Flux<GitChange> getPullRequestChanges(String organization, String project, String repositoryId, int pullRequestId, String apiVersion);`
* **T-04: Crear pruebas unitarias `PullRequestTest.java`**
    * Ubicación: `co.com.bancolombia.model.pullrequest.PullRequestTest`
    * Validar instanciación con builder, inmutabilidad y valores por defecto.

### 2.2 Qué NO hacer

- NO agregar anotaciones de Jackson (`@JsonProperty`), Spring (`@Component`) ni librerías de
  infraestructura en las clases de dominio.
- NO modificar el módulo `azure-devops-agent` en esta fase.
- NO tocar `AzureDevOpsTools.java` ni el paquete `workitem`.

### 2.3 Criterios de aceptación

- [ ] `PullRequest`, `GitChange` y `PullRequestPort` creados en el paquete
  `co.com.bancolombia.model.pullrequest`.
- [ ] Cero dependencias de Spring o librerías de transporte en `domain/model`.
- [ ] 100% de tests unitarios pasando en `domain/model`.
- [ ] `./gradlew :model:test` ejecutado exitosamente sin fallos.

### 2.4 Checklist de calidad

- [ ] Estructura Clean Architecture respetada.
- [ ] Clases documentadas con Javadoc claro en español o inglés técnico.
- [ ] Inmutabilidad garantizada.
- [ ] Métodos con tipado explícito.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                    | Resultado esperado                 |
|-----:|-------------------------------------------------------------------------------------------|------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                         | Confirmar que no hay bloqueos      |
| P-02 | Crear `GitChange.java` y `PullRequest.java`                                               | Modelos de dominio listos          |
| P-03 | Crear `PullRequestPort.java`                                                              | Contrato de puerto listo           |
| P-04 | Crear y ejecutar `PullRequestTest.java`                                                   | Tests en verde                     |
| P-05 | Ejecutar `./gradlew :model:test`                                                          | Build sin fallos                   |
| P-06 | Escribir `docs/resultados/RESULTADO-FASE-01.md`                                           | Trazabilidad del resultado medido  |
| P-07 | Actualizar `docs/plan/ESTADO.md` a 🟢 COMPLETADA                                          | Tablero al día                     |
| P-08 | Generar `docs/fases/FASE-02-casos-uso-pull-request.md` y `docs/prompts/PROMPT-FASE-02.md` | Siguiente fase lista para ejecutar |
