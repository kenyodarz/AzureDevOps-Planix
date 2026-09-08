# RESULTADO — FASE 03: Adaptador REST para API Git de Azure DevOps

> **Ejecutada:** 2026-09-08 · **Estado final:** 🟢 COMPLETADA  
> **Instrucciones:** `azure-devops-mcp/docs/fases/FASE-03-adaptador-rest-git.md`  
> **Archivos nuevos:** 6 archivos (4 producción en `rest-consumer`, 2 pruebas) · **Archivos
modificados:** 4 (`AzureDevOpsAdapterProperties.java`, `application.yaml`,
> `UseCasesConfigWiringTest.java`, `UseCasesConfigTest.java`)

---

## 1. Objetivo de la fase

Implementar la capa de infraestructura reactiva no bloqueante (`WebClient`, Project Reactor) en
`infrastructure/driven-adapters/rest-consumer` para consultar la API Git de Azure DevOps (metadatos
de Pull Requests y lista de cambios de archivos), cumpliendo el contrato de `PullRequestPort`,
mapeando DTOs desacoplados a las entidades inmutables del dominio, gestionando la resiliencia
mediante Resilience4j y traduciendo errores técnicos con `AzureDevOpsErrorTranslator`.

---

## 2. Qué se construyó / modificó

| Artefacto                                                                                       | Acción     | Detalle                                                                                                                                                                                         |
|-------------------------------------------------------------------------------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `infrastructure/.../consumer/dto/GitPullRequestResponse.java`                                   | CREADO     | DTO inmutable con soporte Jackson/Lombok para deserializar metadatos de Pull Requests y referencias de repositorios, creadores y work items (`workItemRefs`).                                   |
| `infrastructure/.../consumer/dto/GitPullRequestChangesResponse.java`                            | CREADO     | DTO inmutable para deserializar cambios en archivos (`changeEntries` / `changes`), con tracking IDs, tipos de cambio e items Git (`objectId`, `originalObjectId`, `path`).                      |
| `infrastructure/.../consumer/mapper/PullRequestMapper.java`                                     | CREADO     | Mapper estático desacoplado que transforma `GitPullRequestResponse` a `PullRequest` y `ChangeEntryDTO` a `GitChange`, incluyendo parseo seguro y defensivo de `workItemRefs`.                   |
| `infrastructure/.../consumer/GitPullRequestAdapter.java`                                        | CREADO     | Adaptador reactivo que implementa `PullRequestPort`, protegido por `@CircuitBreaker(name = "gitPullRequest")`, timeout reactivo y traducción con `AzureDevOpsErrorTranslator`.                  |
| `infrastructure/.../consumer/mapper/PullRequestMapperTest.java`                                 | CREADO     | Pruebas unitarias exhaustivas del mapper (nulos, campos opcionales, fallbacks de nombres y parseos defensivos de IDs).                                                                          |
| `infrastructure/.../consumer/GitPullRequestAdapterTest.java`                                    | CREADO     | Pruebas unitarias con `MockWebServer` cubriendo respuestas 200 OK, traducción de 401 Unauthorized, 404 Not Found y 500 Internal Server Error.                                                   |
| `infrastructure/.../consumer/config/AzureDevOpsAdapterProperties.java`                          | MODIFICADO | Añadido soporte para `git` en `ApiVersion` con fallback por defecto `"7.1"`, conservando compatibilidad binaria hacia atrás.                                                                    |
| `applications/app-service/src/main/resources/application.yaml`                                  | MODIFICADO | Registrada la versión de API `git: "7.1"` y la instancia del cortacircuito `gitPullRequest` en `resilience4j.circuitbreaker.instances`.                                                         |
| `applications/app-service/.../config/UseCasesConfigWiringTest.java` y `UseCasesConfigTest.java` | MODIFICADO | Actualizados los tests de wiring de la aplicación para registrar el mock de `PullRequestPort` y validar que `GetPullRequestUseCase` y `GetPullRequestChangesUseCase` resuelvan a un bean único. |

### 2.1 Pruebas del módulo `:rest-consumer`

| Módulo / Archivo                 | Tests Ejecutados | Fallos | Resultado |
|----------------------------------|-----------------:|-------:|----------:|
| `PullRequestMapperTest.java`     |                5 |      0 |  🟢 PASAN |
| `GitPullRequestAdapterTest.java` |                7 |      0 |  🟢 PASAN |
| **Total `:rest-consumer`**       |               58 |      0 |   🟢 100% |

---

## 3. Decisiones aplicadas

| ID         | Resolución                               | Efecto en esta fase                                                                                      |
|------------|------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `DP-PR-01` | Alcance lectura/escritura                | Se implementaron los endpoints de consulta de PR y consulta de cambios de archivos.                      |
| `DP-PR-02` | Manejo de diffs y cambios                | Deserialización y mapeo streaming reactivo de entradas de cambios a `Flux<GitChange>`.                   |
| `B-08`     | Traducción de errores de infraestructura | Se utiliza `AzureDevOpsErrorTranslator.forOperation(...)` en todas las cadenas reactivas (`onErrorMap`). |
| `B-09`     | Versiones de API desde configuración     | Se parametriza la versión de Git en `AzureDevOpsAdapterProperties.ApiVersion` con fallback `"7.1"`.      |

---

## 4. Métricas obtenidas

| # | Métrica                              | Antes | Después |         Meta |
|--:|--------------------------------------|------:|--------:|-------------:|
| 1 | Tests unitarios `:rest-consumer`     |    40 |      58 | 100% pasando |
| 2 | Cobertura de líneas mutadas (Pitest) |   88% |     90% |   $\ge 80\%$ |
| 3 | Mutaciones generadas (Pitest)        |    87 |     111 |            — |
| 4 | Mutaciones eliminadas (Pitest)       |    73 |      99 |   $\ge 60\%$ |
| 5 | Porcentaje de mutaciones eliminadas  |   84% |     89% |   $\ge 60\%$ |
| 6 | Test strength (Pitest)               |   86% |     91% |   $\ge 80\%$ |
| 7 | Violaciones de Arquitectura          |     0 |       0 |            0 |

---

## 5. Comandos ejecutados y resultados

```bash
./gradlew :rest-consumer:test
# Resultado: BUILD SUCCESSFUL en 38s.
# 58 tests completados con 0 fallos.
# Pitest: 111 mutaciones generadas, 99 eliminadas (89%).

./gradlew test
# Resultado: BUILD SUCCESSFUL en 1s (todas las tareas al día y validadas).
# Todos los módulos (:model, :usecase, :rest-consumer, :mcp-server, :app-service) en verde.
```

---

## 6. Hallazgos no previstos y mitigaciones

1. **Aislamiento en `MockWebServer`:** Al mezclar pruebas asíncronas de `WebClient` que verificaban
   `takeRequest()` con pruebas de traducción de errores que no consumían la cola de peticiones, las
   peticiones acumuladas desfasaban las aserciones. Se solucionó configurando un ciclo de vida
   limpio con `@BeforeEach` y `@AfterEach` instanciando un `MockWebServer` aislado por método de
   prueba.
2. **Wiring en `app-service`:** Al haberse introducido los casos de uso `GetPullRequestUseCase` y
   `GetPullRequestChangesUseCase` en la Fase 02, el escaneo automático de `UseCasesConfig` requería
   la provisión del mock de `PullRequestPort` en las pruebas de wiring (`UseCasesConfigWiringTest` y
   `UseCasesConfigTest`). Se añadieron los dobles y se verificó el registro exitoso a un solo bean
   por caso de uso.

---

## 7. Checklist de calidad

- [x] Adaptador reactivo implementa `PullRequestPort` sin fugas técnicas al dominio.
- [x] DTOs aislados en `dto/` y mapeo concentrado en `PullRequestMapper`.
- [x] Traducción de excepciones mediante `AzureDevOpsErrorTranslator`.
- [x] Cortacircuito configurado en `application.yaml` e integrado con anotación `@CircuitBreaker`.
- [x] `./gradlew :rest-consumer:test` y `./gradlew test` ejecutados exitosamente con 0 fallos.

---

## 8. Estado al cerrar

- Siguiente fase especificada: `azure-devops-mcp/docs/fases/FASE-04-feedback-comentarios-pr.md`
- Prompt de continuidad generado: `azure-devops-mcp/docs/prompts/PROMPT-FASE-04.md`
- Tablero de progreso: `azure-devops-mcp/docs/plan/ESTADO.md` actualizado a 🟢 COMPLETADA.
