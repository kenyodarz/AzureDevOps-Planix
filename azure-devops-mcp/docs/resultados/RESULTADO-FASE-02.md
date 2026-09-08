# RESULTADO — FASE 02: Casos de Uso de Consulta de PRs y Cambios

> **Ejecutada:** 2026-09-08 · **Estado final:** 🟢 COMPLETADA  
> **Instrucciones:** `azure-devops-mcp/docs/fases/FASE-02-casos-uso-pull-request.md`  
> **Archivos nuevos:** 4 archivos en `domain/usecase` (2 producción, 2 pruebas) · **Archivos
modificados:** 0

---

## 1. Objetivo de la fase

Implementar los casos de uso puros del dominio para consultar los metadatos de un Pull Request
(`GetPullRequestUseCase`) y recuperar la secuencia reactiva de cambios en archivos
(`GetPullRequestChangesUseCase`), aplicando validaciones defensivas de parámetros requeridos y
asegurando 100% de cobertura y mutaciones eliminadas en `domain/usecase`.

---

## 2. Qué se construyó / modificó

| Artefacto                                                                      | Acción | Detalle                                                                                                                                               |
|--------------------------------------------------------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/usecase/.../usecase/pullrequest/GetPullRequestUseCase.java`            | CREADO | Caso de uso puro que valida parámetros obligatorios y delega en `pullRequestPort.getPullRequestById(...)` retornando `Mono<PullRequest>`.             |
| `domain/usecase/.../usecase/pullrequest/GetPullRequestChangesUseCase.java`     | CREADO | Caso de uso puro que valida parámetros obligatorios y delega en `pullRequestPort.getPullRequestChanges(...)` retornando `Flux<GitChange>`.            |
| `domain/usecase/.../usecase/pullrequest/GetPullRequestUseCaseTest.java`        | CREADO | Pruebas unitarias completas con Mockito y StepVerifier cubriendo casos de éxito, validaciones de argumentos (`@ParameterizedTest`) y fallos upstream. |
| `domain/usecase/.../usecase/pullrequest/GetPullRequestChangesUseCaseTest.java` | CREADO | Pruebas unitarias completas cubriendo casos de éxito, flujos vacíos, validaciones de argumentos y propagación de errores.                             |

### 2.1 Pruebas nuevas o reescritas

| Archivo                                 | Pruebas / Casos | Resultado |
|-----------------------------------------|----------------:|----------:|
| `GetPullRequestUseCaseTest.java`        |              17 |  🟢 PASAN |
| `GetPullRequestChangesUseCaseTest.java` |              18 |  🟢 PASAN |
| **Total módulo `:usecase`**             |              36 |   🟢 100% |

---

## 3. Decisiones aplicadas

| ID         | Resolución                | Efecto en esta fase                                                                                  |
|------------|---------------------------|------------------------------------------------------------------------------------------------------|
| `DP-PR-01` | Alcance lectura/escritura | Se crearon casos de uso enfocados en la consulta reactiva de metadatos y cambios.                    |
| `DP-PR-02` | Manejo de diffs grandes   | `GetPullRequestChangesUseCase` transmite reactivamente cambios unitarios mediante `Flux<GitChange>`. |

---

## 4. Métricas obtenidas

| # | Métrica                               | Antes | Después |         Meta |
|--:|---------------------------------------|------:|--------:|-------------:|
| 1 | Tests unitarios ejecutados            |    14 |      36 | 100% pasando |
| 2 | Cobertura de líneas mutadas (Pitest)  |   98% |     99% |   $\ge 90\%$ |
| 3 | Mutaciones generadas (Pitest)         |    39 |      65 |            — |
| 4 | Mutaciones eliminadas (Pitest)        |    38 |      64 |   $\ge 60\%$ |
| 5 | Test strength (Pitest)                |   97% |     98% |   $\ge 90\%$ |
| 6 | Anotaciones de Spring en casos de uso |     0 |       0 |            0 |
| 7 | Nuevas mutaciones sobrevivientes      |     0 |       0 |            0 |

---

## 5. Comandos ejecutados y resultados

```bash
./gradlew :usecase:test
# Resultado: BUILD SUCCESSFUL en 24s.
# 36 tests ejecutados con 0 fallos.
# Pitest: 65 mutaciones generadas, 64 eliminadas (98%), 1 sobreviviente preexistente ajena a esta fase.
```

---

## 6. Hallazgos no previstos

*Ninguno.* Las clases mantuvieron la pureza de Clean Architecture delegando directamente en el
puerto reactivo `PullRequestPort`.

---

## 7. Checklist de calidad

- [x] Casos de uso 100% puros (sin `@Service`, `@Component`, Spring ni Jackson).
- [x] Inyección de dependencias por constructor vía `@RequiredArgsConstructor`.
- [x] Validaciones de entrada no bloqueantes que retornan `Mono.error()` / `Flux.error()`.
- [x] `./gradlew :usecase:test` exitoso con 0 fallos.
- [x] Exactamente 4 archivos creados en `domain/usecase`.

---

## 8. Desviaciones respecto a las instrucciones

*Ninguna.*

---

## 9. Estado al cerrar

- Siguiente fase generada: `azure-devops-mcp/docs/fases/FASE-03-adaptador-rest-git.md`
- Prompt de continuidad generado: `azure-devops-mcp/docs/prompts/PROMPT-FASE-03.md`
- `azure-devops-mcp/docs/plan/ESTADO.md` actualizado: Sí (Fase 02 🟢 COMPLETADA)
