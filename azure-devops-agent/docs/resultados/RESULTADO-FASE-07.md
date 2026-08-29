# RESULTADO — FASE 07: Endurecimiento y Cierre

> **Ejecutada:** 2026-08-29 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(agent): endurecer arquitectura y cerrar deudas del plan`
> **Instrucciones:** `docs/fases/FASE-07-endurecimiento-y-cierre.md`

---

## 1. Objetivo de la fase

Fase de **cierre**: resolver la última decisión abierta (DP-05), saldar las deudas acumuladas
—violaciones de ArchUnit, test con señal falsa, modelos sin pruebas— y dejar el plan documentado.

---

## 2. Qué se construyó

### Código modificado

```
domain/model/src/main/java/co/com/bancolombia/model/chat/
└── ClientRequest.java                          ELIMINADO   dead code (Rule_2.2)

domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/
└── AgentChatUseCase.java                       MODIFICADO  Javadoc de los dos transportes

applications/app-service/src/main/java/co/com/bancolombia/config/
└── UseCasesConfig.java                         MODIFICADO  2 campos @Value → parámetros (Rule_2.7)

infrastructure/driven-adapters/pgvector-store/.../
├── PgVectorPlanningAdapter.java                MODIFICADO  2 campos @Value → constructor (Rule_2.7)
└── config/PgVectorConfig.java                  MODIFICADO  1 campo @Value → parámetro (Rule_2.7)

infrastructure/driven-adapters/mcp-client/.../adapter/
└── NoOpAgentResponseAdapter.java               MODIFICADO  documentado como Null Object (DP-05)
```

### Pruebas creadas

```
domain/model/src/test/java/co/com/bancolombia/model/a2a/
├── MessageTest.java                    9 pruebas   factorías + extractText degenerado
├── PartTest.java                       5 pruebas   las tres factorías de contenido
├── TaskStateTest.java                 10 pruebas   valores serializados del protocolo
└── A2AModelsTest.java                 13 pruebas   los 12 modelos restantes

domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/
└── AgentChatUseCaseAsyncTransportTest.java   5 pruebas   transporte asíncrono (DP-05)

infrastructure/entry-points/reactive-web/src/test/java/co/com/bancolombia/api/
└── HandlerTaskListingTest.java               4 pruebas   GET /api/tasks y Agent Card

applications/app-service/src/test/java/co/com/bancolombia/config/
└── UseCasesConfigTest.java            REESCRITO   2 pruebas honestas de fail-fast
```

---

## 3. Decisiones aplicadas

### DP-05 — Resuelta por el usuario: **Opción A (conservar y documentar)**

El usuario descartó la Opción B con una razón de negocio explícita: *«Ahí está clara la razón por
la que se devuelve un `Mono.empty()`, por ende la opción B se descarta»*, precisando que **en el
Banco A2A corre sobre Kafka y no sobre el protocolo oficial de transporte**. El camino asíncrono no
es un experimento abandonado: es el transporte real de la plataforma, hoy sin cablear.

Entre A y C delegó la elección con el criterio de **la menos traumática**. Se eligió **A**:

1. `NoOpAgentResponseAdapter` **ya es el interruptor**: es un *Null Object*, publicar en
   `Mono.empty()` no tiene efecto lateral.
2. La Opción C **duplicaría el mecanismo de apagado** sobre uno que ya funciona: propiedad nueva,
   cableado condicional y una segunda ruta de arranque que probar.
3. Activar Kafka con A es **sustituir la implementación del puerto**, sin tocar dominio ni
   configuración. Con C habría además que acordarse de encender el flag: un modo de fallo nuevo.

**Cómo se materializó:** cero cambios funcionales. Javadoc explícito en `AgentChatUseCase.chat()`
y en `NoOpAgentResponseAdapter` —incluyendo el procedimiento de activación—, sección dedicada en el
`README.md`, y **cobertura del camino** con `AgentChatUseCaseAsyncTransportTest`.

> **D-13 queda saldada por documentación y cobertura, no por eliminación**: deja de ser un camino
> muerto no documentado para ser un punto de extensión explícito y verificado.

---

## 4. Métricas obtenidas

| Métrica | Antes (Fase 06) | Después |
|---|---:|---:|
| Violaciones ArchUnit `Rule_2.7` | 5 | **0** |
| Violaciones ArchUnit `Rule_2.2` | 1 | **0** |
| Cobertura `domain/model` | 96,1% | **99,7%** (869/872) |
| Cobertura `domain/usecase` | 95,4% | **99,2%** (649/654) |
| Cobertura `model/a2a` (los 15 modelos) | ~0% | **100%** (132/132) |
| Cobertura `AgentChatUseCase` | 75,9% | **97,4%** (113/116) |
| Cobertura `Handler` | 86,1% | **100%** (173/173) |
| Cobertura paquete `api` | 98,7%¹ | **98,7%** (592/600) |
| Tests totales del proyecto | 251 | **305** |
| Decisiones abiertas | 1 (DP-05) | **0** |
| Clases > 300 líneas | 0 | **0** |

¹ El paquete `api` incluye los DTO `JsonRpcRequest`/`JsonRpcResponse`, cuyo código no generado por
Lombok es mínimo.

### Las 6 violaciones de ArchUnit, identificadas con evidencia

Se obtuvieron ejecutando el test con `-i`, **no por inspección**, porque `build/issues.json` venía
vacío:

| Regla | Elemento |
|---|---|
| `Rule_2.7` | `UseCasesConfig.defaultOrg` |
| `Rule_2.7` | `UseCasesConfig.defaultProject` |
| `Rule_2.7` | `PgVectorPlanningAdapter.similarityThreshold` |
| `Rule_2.7` | `PgVectorPlanningAdapter.tableName` |
| `Rule_2.7` | `PgVectorConfig.tableName` |
| `Rule_2.2` | `co.com.bancolombia.model.chat.ClientRequest` |

**Patrón común de las cinco de `Rule_2.7`:** `@Value` sobre el campo en lugar de sobre el
parámetro. La corrección es la misma en los tres archivos —inyectar por parámetro de método `@Bean`
o por constructor— y deja los beans sin estado mutable.

---

## 5. Validación de los criterios de aceptación

| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | DP-05 resuelta **por el usuario**, registrada y aplicada | `DECISIONES_PENDIENTES.md` + Javadoc + tests | ✅ Opción A |
| 2 | 0 violaciones de `Rule_2.7` y `Rule_2.2` | `build` sin línea `ARCHITECTURE_RULE_VIOLATED` | ✅ |
| 3 | `UseCasesConfigTest` ya no descarta la excepción | Reescrito con `assertThatThrownBy` | ✅ |
| 4 | `domain/model` ≥ 90% y ningún modelo A2A al 0% | `jacocoMergedReport` | ✅ 99,7%; `model/a2a` **100%** |
| 5 | `domain/usecase` ≥ 90% | `jacocoMergedReport` | ✅ 99,2% |
| 6 | `Handler` ≥ 90% | `jacocoMergedReport` | ✅ **100%** |
| 7 | Las 4 suites del entry-point sin modificar | `git diff` sobre las 4 rutas | ✅ **salida vacía** |
| 8 | `ArchitectureTest.java` sin modificar | `git diff` | ✅ **salida vacía** |
| 9 | `.\gradlew.bat build` verde | Ejecutado | ✅ `BUILD SUCCESSFUL` |
| 10 | `README.md` actualizado | Sección de arquitectura del agente añadida | ✅ |
| 11 | `CIERRE-DEL-PLAN.md` escrito | Creado con métricas reales | ✅ |

---

## 6. Comandos ejecutados

```powershell
.\gradlew.bat :app-service:test --tests "*ArchitectureTest*" --rerun-tasks -i   # identificar las 6 violaciones
.\gradlew.bat compileJava                                        # tras eliminar ClientRequest
.\gradlew.bat :app-service:test --tests "*ArchitectureTest*" --rerun-tasks      # 0 advertencias
.\gradlew.bat :model:test                                        # 37 pruebas nuevas verdes
.\gradlew.bat :usecase:test                                      # transporte asíncrono verde
.\gradlew.bat build                                              # BUILD SUCCESSFUL
.\gradlew.bat jacocoMergedReport --no-configuration-cache
.\gradlew.bat jacocoMergedReport                                 # reproduce el fallo del plugin pitest
git --no-pager diff --stat HEAD -- <4 suites> ArchitectureTest.java   # salida vacía
```

**Resultado:** `BUILD SUCCESSFUL` · **305/305** pruebas en verde · **0** advertencias de ArchUnit.

---

## 7. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **`ClientRequest` se eliminó en vez de renombrarse** | La instrucción contemplaba renombrar. Se verificó con `grep` en todo el módulo que **no tiene una sola referencia**: la única coincidencia era `org.springframework.web.reactive.function.client.ClientRequest` en `OAuth2WebClientConfig`, una clase homónima de Spring. Renombrar habría conservado dead code con otro nombre. Reversible desde git |
| **`UseCasesConfigTest` se reescribió, no se eliminó** | La instrucción permitía «sustituir o corregir». Se corrigió: el `catch` que devolvía `assertTrue(true)` pasó a ser un `assertThatThrownBy` explícito. Ahora afirma algo real —el fail-fast ante puertos ausentes— en vez de tragarse el fallo |
| **La guarda `response.getTask() == null` de `chat()` se conservó sin cubrir** | Es **inalcanzable**: `A2AResponseFactory.success()` y `failure()` siempre construyen un `Task`. Cubrirla exigiría mockear un método privado o alterar la fábrica. Se conserva como guarda defensiva barata; son las 3 instrucciones que separan a `AgentChatUseCase` del 100%. Anotado en §8 |
| **La Agent Card no se externalizó a `resources/agent-card.json`** | Estaba marcada como *opcional*. Sigue anotada como mejora futura en el informe de cierre |
| **Se añadió una prueba de `handleAgentCard()` en `HandlerTaskListingTest`** | No estaba prevista, pero completaba el 100% de `Handler` en la misma suite y sin tocar las del contrato |

---

## 8. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| Estructura de paquetes de Clean Architecture | ✅ |
| `domain/model` sin Spring, persistencia ni serialización | ✅ |
| `domain/usecase` sin anotaciones de Spring | ✅ el wiring sigue solo en `app-service` |
| Los mappers viven en la capa correcta | ✅ |
| Sin secretos hardcodeados (S2068) | ✅ |
| `Optional<T>` en retornos opcionales (S2259) | ✅ |
| Complejidad cognitiva ≤ 15 | ✅ |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ |
| **Campos finales en los beans (Rule_2.7)** | ✅ **0 violaciones** |
| **Nombres de dominio sin sufijo tecnológico (Rule_2.2)** | ✅ **0 violaciones** |
| Tests GIVEN/WHEN/THEN con Mockito | ✅ 54 pruebas nuevas |
| `.\gradlew.bat build` verde (ArchUnit + BlockHound) | ✅ |
| Ningún contrato asumido sin validación (§6) | ✅ DP-05 la resolvió el usuario |
| Commit en formato `tipo(scope): descripción` | ✅ |

---

## 9. Hallazgos

| Hallazgo | Atender en |
|---|---|
| **`build/issues.json` se genera siempre vacío**, aunque haya violaciones. El mapeo de `ArchitectureTest` no resuelve los ficheros. Las violaciones solo se ven en el log con `-i` | Plan futuro |
| **El workaround `--no-configuration-cache` no es culpa de JaCoCo.** Reproducido: falla `pitestReportAggregate` de `info.solidsoft.gradle.pitest` al serializar `__additionalClasspath__`. Defecto del plugin, ajeno al proyecto. Documentado en el `README.md` | Plan futuro (aguas arriba) |
| **Las cinco violaciones de `Rule_2.7` compartían causa**: `@Value` sobre campo en lugar de sobre parámetro. Conviene vigilarlo en revisiones, porque el IDE lo sugiere así por defecto | Práctica permanente |
| La guarda `getTask() == null` de `chat()` es inalcanzable desde el código actual | Plan futuro (opcional) |
| La Agent Card sigue compilada en Java; ya aislada en una clase, externalizarla es un cambio pequeño | Plan futuro (opcional) |
| Las 4 suites del entry-point fijan `Handler` como único bean del contexto: introducir un colaborador `@Component` obligará a renegociarlas | Plan futuro |

---

## 10. Estado al cerrar

- **Siguiente fase generada:** **ninguna.** La Fase 07 era la última del plan; en su lugar se
  escribió `docs/resultados/CIERRE-DEL-PLAN.md`.
- **Decisiones abiertas:** **ninguna.** DP-01 a DP-07 resueltas por el usuario.
- **Pendiente de ratificación del usuario** (ambas triviales de revertir, sin bloquear nada):
  1. Eliminación de la «Regla de Mínimos de 5 puntos» (Fase 01).
  2. Regla del pronombre relativo en las palabras clave genéricas (Fase 03).
- **Plan maestro:** todas las métricas de éxito de §5 alcanzadas.

