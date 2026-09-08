# FASE 02 — Casos de Uso de Consulta de PRs y Cambios

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(usecase): implementar casos de uso para consulta de pull requests y cambios`  
> **Siguiente fase:** `FASE-03-adaptador-rest-git.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 01 definió los modelos inmutables `PullRequest` y `GitChange` junto al puerto reactivo
`PullRequestPort` en `domain/model`. El módulo compiló limpiamente en Java 25 con 100% de tests
verdes.

### 1.2 Problema que resuelve esta fase

La capa de casos de uso (`domain/usecase`) carece de la lógica de negocio para coordinar la consulta
de metadatos de un Pull Request y la recuperación reactiva de los cambios por archivo, aplicando
validaciones de parámetros de entrada sin contaminarse de detalles técnicos de transporte o
frameworks.

### 1.3 Estado esperado al terminar

Existirá el paquete `co.com.bancolombia.usecase.pullrequest` con:

1. `GetPullRequestUseCase`: Caso de uso para obtener un PR validando parámetros obligatorios.
2. `GetPullRequestChangesUseCase`: Caso de uso para recuperar la secuencia reactiva de `GitChange`.
3. Pruebas unitarias para ambos casos de uso usando mocks de `PullRequestPort`, validando casos de
   éxito y validaciones.

### 1.4 Archivos involucrados

| Ruta                                                                                                                         | Acción |
|------------------------------------------------------------------------------------------------------------------------------|--------|
| `azure-devops-mcp/domain/usecase/src/main/java/co/com/bancolombia/usecase/pullrequest/GetPullRequestUseCase.java`            | CREAR  |
| `azure-devops-mcp/domain/usecase/src/main/java/co/com/bancolombia/usecase/pullrequest/GetPullRequestChangesUseCase.java`     | CREAR  |
| `azure-devops-mcp/domain/usecase/src/test/java/co/com/bancolombia/usecase/pullrequest/GetPullRequestUseCaseTest.java`        | CREAR  |
| `azure-devops-mcp/domain/usecase/src/test/java/co/com/bancolombia/usecase/pullrequest/GetPullRequestChangesUseCaseTest.java` | CREAR  |

### 1.5 Reglas aplicables

- Reglas de Fase 2 (`AGENTS.md`): `domain/usecase` puro: sin anotaciones de Spring (`@Service`,
  `@Component`), Jackson ni dependencias de frameworks. Inyección de dependencias por constructor.
- Regla Anti-Monolito: Exactamente 4 archivos.

### 1.6 Decisiones pendientes que bloquean

| ID         | Estado      | Acción si sigue ABIERTA |
|------------|-------------|-------------------------|
| `DP-PR-01` | 🟢 RESUELTA | N/A                     |
| `DP-PR-02` | 🟢 RESUELTA | N/A                     |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

* **T-01: Crear `GetPullRequestUseCase.java`**
    * Inyectar `PullRequestPort` por constructor (con `@RequiredArgsConstructor` de Lombok).
    * Implementar método
      `getPullRequest(String organization, String project, String repositoryId, int pullRequestId, String apiVersion): Mono<PullRequest>`.
    * Validar parámetros básicos (`pullRequestId > 0`, cadenas no nulas/en blanco).
* **T-02: Crear `GetPullRequestChangesUseCase.java`**
    * Inyectar `PullRequestPort` por constructor.
    * Implementar método
      `getChanges(String organization, String project, String repositoryId, int pullRequestId, String apiVersion): Flux<GitChange>`.
* **T-03: Crear pruebas unitarias `GetPullRequestUseCaseTest.java`**
    * Validar delegación al puerto y manejo de errores con Mockito y StepVerifier.
* **T-04: Crear pruebas unitarias `GetPullRequestChangesUseCaseTest.java`**
    * Validar emisión reactiva de cambios y flujos vacíos.

### 2.2 Qué NO hacer

- NO usar anotaciones de Spring (`@Service`, `@Autowired`, `@Component`) en los casos de uso.
- NO tocar módulos fuera de `domain/usecase`.

### 2.3 Criterios de aceptación

- [ ] Casos de uso creados en `domain/usecase`.
- [ ] 100% de tests unitarios pasando en `:usecase`.
- [ ] `./gradlew :usecase:test` exitoso.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                 | Resultado esperado                 |
|-----:|----------------------------------------------------------------------------------------|------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                      | Confirmar estado activo de Fase 02 |
| P-02 | Implementar `GetPullRequestUseCase.java` y `GetPullRequestChangesUseCase.java`         | Lógica de negocio lista            |
| P-03 | Implementar `GetPullRequestUseCaseTest.java` y `GetPullRequestChangesUseCaseTest.java` | Pruebas unitarias listas           |
| P-04 | Ejecutar `./gradlew :usecase:test`                                                     | Verde                              |
| P-05 | Escribir `docs/resultados/RESULTADO-FASE-02.md`                                        | Registro de trazabilidad           |
| P-06 | Actualizar `docs/plan/ESTADO.md` a 🟢 COMPLETADA                                       | Tablero al día                     |
| P-07 | Generar `docs/fases/FASE-03-adaptador-rest-git.md` y `docs/prompts/PROMPT-FASE-03.md`  | Siguiente fase lista               |
