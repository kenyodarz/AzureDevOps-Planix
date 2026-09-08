# RESULTADO — FASE 01: Modelos de Dominio y Puertos de Git / Pull Request

> **Ejecutada:** 2026-09-08 · **Estado final:** 🟢 COMPLETADA  
> **Instrucciones:** `azure-devops-mcp/docs/fases/FASE-01-modelos-dominio-pull-request.md`  
> **Archivos nuevos:** 4 archivos en `domain/model` · **Archivos modificados:** 0

---

## 1. Objetivo de la fase

Definir las entidades de dominio puras e inmutables (`PullRequest`, `GitChange`) y el contrato
gateway reactivo (`PullRequestPort`) para soportar la integración con repositorios Git de Azure
DevOps en el servidor MCP bajo Java 25 y Clean Architecture.

---

## 2. Qué se construyó / modificó

| Artefacto                                                          | Acción | Detalle                                                                                                                                        |
|--------------------------------------------------------------------|--------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/model/.../model/pullrequest/GitChange.java`                | CREADO | Objeto de valor inmutable (`record` con `@Builder(toBuilder = true)`) que modela los cambios por archivo.                                      |
| `domain/model/.../model/pullrequest/PullRequest.java`              | CREADO | Entidad inmutable (`record` con `@Builder(toBuilder = true)`) con copia defensiva de `workItemIds` mediante `DomainCollections.immutableCopy`. |
| `domain/model/.../model/pullrequest/gateways/PullRequestPort.java` | CREADO | Interfaz de puerto reactivo (`Mono<PullRequest>`, `Flux<GitChange>`) sin dependencias de frameworks.                                           |
| `domain/model/.../model/pullrequest/PullRequestTest.java`          | CREADO | Suite de 5 pruebas unitarias cubriendo invariantes, accesores, copias defensivas, nulos y `toBuilder()`.                                       |

### 2.1 Pruebas nuevas o reescritas

| Archivo                   | Casos antes | Casos después |
|---------------------------|------------:|--------------:|
| `PullRequestTest.java`    |           0 |             5 |
| **Total módulo `:model`** |          42 |            47 |

---

## 3. Decisiones aplicadas

| ID         | Resolución                | Efecto en esta fase                                                                        |
|------------|---------------------------|--------------------------------------------------------------------------------------------|
| `DP-PR-01` | Alcance lectura/escritura | Se definió `PullRequestPort` con operaciones de lectura de metadatos y cambios.            |
| `DP-PR-02` | Manejo de diffs grandes   | Se modeló `GitChange` a nivel de archivo individual con stream reactivo `Flux<GitChange>`. |

---

## 4. Métricas obtenidas

| # | Métrica                              | Antes | Después |         Meta |
|--:|--------------------------------------|------:|--------:|-------------:|
| 1 | Tests unitarios `:model`             |    42 |      47 | 100% pasando |
| 2 | Cobertura de líneas mutadas          |   94% |     94% |   $\ge 90\%$ |
| 3 | Mutaciones eliminadas (Pitest)       |   98% |     98% |   $\ge 60\%$ |
| 4 | Test strength (Pitest)               |  100% |    100% |         100% |
| 5 | Dependencias de framework en dominio |     0 |       0 |            0 |

---

## 5. Comandos ejecutados y resultados

```bash
./gradlew :model:test
# Resultado: BUILD SUCCESSFUL en 13s, 47 tests ejecutados, 0 fallos.
```

---

## 6. Hallazgos no previstos

*Ninguno.* La implementación encajó limpiamente con las convenciones existentes de
`DomainCollections` y records inmutables.

---

## 7. Checklist de calidad

- [x] Dominio 100% puro (sin Jackson, Spring ni WebClient).
- [x] Entidades inmutables (`record` + `@Builder`).
- [x] Tipado explícito y contrato reactivo (`Mono`/`Flux`).
- [x] `./gradlew :model:test` OK con 0 fallos.
- [x] Exactamente 4 archivos tocados en `domain/model`.

---

## 8. Desviaciones respecto a las instrucciones

*Ninguna.*

---

## 9. Estado al cerrar

- Siguiente fase generada: `azure-devops-mcp/docs/fases/FASE-02-casos-uso-pull-request.md`
- `azure-devops-mcp/docs/plan/ESTADO.md` actualizado: Sí (Fase 01 🟢 COMPLETADA)
