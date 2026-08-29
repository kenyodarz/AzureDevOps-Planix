# Tablero de Estado — Refactorización `azure-devops-agent`

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**
> Debe actualizarse al cerrar cada fase, antes de hacer commit.

**Última actualización:** 2026-08-29 · **Fase activa:** _ninguna — **PLAN CERRADO**_

> 🏁 **Plan completado.** Las 8 fases están cerradas y no queda ninguna decisión abierta.
> Informe final: **`docs/resultados/CIERRE-DEL-PLAN.md`**.

---

## Progreso

| # | Fase | Instrucciones | Resultado | Estado |
|---|---|---|---|---|
| 00 | Baseline y caracterización | `fases/FASE-00-...md` | `resultados/RESULTADO-FASE-00.md` | 🟢 COMPLETADA |
| 01 | Externalización de prompts | `fases/FASE-01-...md` | `resultados/RESULTADO-FASE-01.md` | 🟢 COMPLETADA |
| 02 | Value Objects de estimación | `fases/FASE-02-...md` | `resultados/RESULTADO-FASE-02.md` | 🟢 COMPLETADA |
| 03 | Resolución de intención | `fases/FASE-03-resolucion-de-intencion.md` | `resultados/RESULTADO-FASE-03.md` | 🟢 COMPLETADA |
| 04 | Separación de flujos (Strategy) | `fases/FASE-04-separacion-de-flujos.md` | `resultados/RESULTADO-FASE-04.md` | 🟢 COMPLETADA |
| 05 | Value Objects de configuración | `fases/FASE-05-value-objects-de-configuracion.md` | `resultados/RESULTADO-FASE-05.md` | 🟢 COMPLETADA |
| 06 | Separación del entry-point | `fases/FASE-06-separacion-del-entry-point.md` | `resultados/RESULTADO-FASE-06.md` | 🟢 COMPLETADA |
| 07 | Endurecimiento y cierre | `fases/FASE-07-endurecimiento-y-cierre.md` | `resultados/RESULTADO-FASE-07.md` | 🟢 COMPLETADA |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

> ✅ **Ninguna decisión abierta.** DP-01 a DP-07 fueron resueltas por el usuario.

---

## Métricas

| Métrica | Baseline | Fase 04 | Fase 05 | Fase 06 | Fase 07 | Objetivo |
|---|---:|---:|---:|---:|---:|---:|
| Líneas `AgentChatUseCase` | 644 | **89** | 89 | 89 | 89 | ≤ 100 ✅ |
| Líneas `Handler` | 495 | 495 | 495 | **112** | 112 | ≤ 150 ✅ |
| Clases > 300 líneas | 2 | **1** | 1 | **0** | 0 | 0 ✅ |
| Tests totales | 23 | 186 | 207 | 251 | **305** | — |
| Cobertura `domain/model` | 0% | 95,8% | 96,1% | 96,1% | **99,7%** | ≥ 90% ✅ |
| Cobertura `domain/usecase` | 0% | **95,4%** | 95,4% | 95,4% | **99,2%** | ≥ 90% ✅ |
| Cobertura `prompt-template` | n/a | 96,6% | 96,6% | 96,6% | 96,6% | ≥ 90% ✅ |
| Cobertura `AgentChatUseCase` | 0% | 75,9% | 75,9% | 75,9% | **97,4%** | ≥ 90% ✅ |
| Cobertura `Handler` | 0% | — | — | 86,1% | **100%** | ≥ 90% ✅ |
| Cobertura modelos `a2a` | 0% | ~0% | ~0% | ~0% | **100%** | ≥ 90% ✅ |
| Ramas `if` de enrutamiento en `executeChat()` | 4 | 0 | 0 | 0 | 0 | 0 ✅ |
| Bloques `onErrorResume` duplicados | 6 | **1** | 1 | 1 | 1 | 1 ✅ |
| Argumentos del constructor | 9 | **4** | 4 | 4 | 4 | ≤ 5 ✅ |
| `String` de configuración sueltos en constructores | 5 | 10 | **0** | 0 | 0 | 0 ✅ |
| Responsabilidades en `Handler` | 4 | 4 | 4 | **1** | 1 | 1 ✅ |
| Códigos de error JSON-RPC como literales sueltos | 6 | 6 | 6 | **0** | 0 | 0 ✅ |
| **Violaciones de ArchUnit** | 6 | 6 | 6 | 6 | **0** | 0 ✅ |
| **Decisiones abiertas** | 7 | 1 | 1 | 1 | **0** | 0 ✅ |

> El repunte a 10 `String` sueltos en la Fase 04 fue transitorio: al repartir los flujos, la
> configuración se distribuyó entre los handlers antes de tiparse en la Fase 05.
>
> 🏁 **Todas las métricas de éxito del plan maestro §5 quedaron alcanzadas.** La clase productiva
> más larga es `PgVectorPlanningAdapter`, con 240 líneas.

---

## Bitácora de sesiones

| Fecha | Fase | Qué se hizo | Bloqueos |
|---|---|---|---|
| 2026-08-28 | — | Plan maestro, decisiones pendientes y Fase 00 | DP-01 a DP-05 abiertas |
| 2026-08-28 | 00 | Usuario resolvió DP-01 y DP-02. 23 pruebas de caracterización. Cobertura `usecase` 0% → 81,7%. Código productivo intacto | **DP-07 detectada**; DP-06 abierta |
| 2026-08-28 | 01 | Usuario resolvió DP-04, DP-06 y DP-07. Módulo `prompt-template` con puerto y adaptador. 5 plantillas externalizadas. Eliminada la «Regla de Mínimos». 644 → 505 líneas. 69 tests | DP-03 pendiente |
| 2026-08-28 | 02 | Usuario resolvió DP-03. Creados `ComplexityEstimation`, `UncertaintyLevel` y `EstimationSummary`. Umbral `> 8`; aviso explícito si falta estimación. 505 → 446 líneas. 118 tests | Ninguno |
| 2026-08-28 | 03 | DP-01 aplicada: `AgentIntent`, `IntentResolution` e `IntentResolver` (100% cobertura). `executeChat()` valida + despacha. Eliminados `handleSpecialCommands()` y `buildPrompt()`. 446 → 347 líneas. 152 tests | **Ratificación pendiente:** regla del pronombre relativo |
| 2026-08-28 | 04 | D-02 saldada: seis `ChatFlowHandler` + `ChatFlowDispatcher` con fail-fast y **un único** `onErrorResume`. Extraída `A2AResponseFactory`. Constructor 11 → 4. 347 → **89 líneas**. 186 tests | Ninguno |
| 2026-08-29 | 05 | D-03 saldada: `AzureDevOpsScope` y `CorporateKnowledge` (100% cobertura). Cero `String` sueltos en handlers; recursos leídos una sola vez. **Descubierto que ningún test validaba el cableado de Spring**: añadido `UseCasesConfigWiringTest`. 207 tests | Ninguno |
| 2026-08-29 | 06 | D-10 y D-11 saldadas: `JsonRpcPayloadMapper`, `JsonRpcResponseFactory`, `JsonRpcDispatcher` y `AgentCardProvider`, las cuatro al **100%** de cobertura. `Handler` 495 → **112 líneas** y una sola responsabilidad. **Cero clases > 300 líneas.** Contrato JSON-RPC intacto: `git diff` vacío sobre las 4 suites. Eliminado el `JsonMapper` muerto del constructor. 251 tests | Ninguno |
| 2026-08-29 | 07 | **Usuario resolvió DP-05 (Opción A)**: A2A corre sobre Kafka en el Banco, así que `chat()` se conserva como punto de extensión, documentado y **cubierto**. D-13 saldada. **ArchUnit a cero** (5×`Rule_2.7` + 1×`Rule_2.2`); eliminado `ClientRequest` (dead code). `UseCasesConfigTest` reescrito: la señal falsa pasa a ser un `assertThatThrownBy` real. 37 pruebas para los modelos A2A. Cobertura `model` **99,7%**, `usecase` **99,2%**, `Handler` **100%**. 305 tests. **PLAN CERRADO** | Ninguno |

---

## Decisiones

### Resueltas

| ID | Decisión | Aplicada en |
|---|---|---|
| DP-01 | Corregir precedencia de intención: `(ID: n)` gana sobre palabras clave | ✅ Fase 03 |
| DP-02 | Umbral de división `> 8` | ✅ Fase 02 |
| DP-03 | Aviso explícito si el modelo no estima | ✅ Fase 02 |
| DP-04 | Ruta y nombres de las plantillas | ✅ Fase 01 |
| DP-06 | Estimación por horas; se elimina la «Regla de Mínimos de 5 puntos» | ✅ Fase 01 |
| DP-07 | Eliminar `buildPrompt()` y su plantilla (dead code) | ✅ Fase 01 (plantilla) + Fase 03 (método) |
| DP-05 | Conservar `chat()` async como punto de extensión A2A/Kafka (Opción A) | ✅ Fase 07 |

### Abiertas

**Ninguna.** Las siete decisiones del plan fueron resueltas por el usuario.

### Ratificación pendiente

| Asunto | Detalle |
|---|---|
| Eliminación de la «Regla de Mínimos de 5 puntos» | Derivada por implicación de la tabla de horas de DP-06, no confirmada palabra por palabra. Revertir es reintroducir el párrafo en `prompts/fase2-historia-estructurada.md` |
| Regla del pronombre relativo en las palabras clave genéricas | Fase 03. Una palabra clave precedida de `que`/`quien`/`cual` no enruta a General, porque describe el sistema a construir y no una petición al agente. Sin ella no es posible cumplir a la vez las dos pruebas obligatorias de la fase. Revertir es borrar los tres lookbehind de `GENERAL_KEYWORD_PATTERN` |

---

## Hallazgos técnicos

### Saldados

| Hallazgo | Detectado en | Saldado en |
|---|---|---|
| ArchUnit `Rule_2.7` violada 5 veces (campos `@Value` no finales) | Fase 00 | ✅ Fase 07 |
| ArchUnit `Rule_2.2` violada 1 vez (`ClientRequest`, dead code) | Fase 00 | ✅ Fase 07 |
| Los ~15 modelos A2A sin pruebas lastraban la cobertura del módulo | Fase 02 | ✅ Fase 07 — **100%** |
| `AgentChatUseCase` al 75,9%: faltaba el camino `chat()` asíncrono | Fase 04 | ✅ Fase 07 — **97,4%** |
| **`UseCasesConfigTest` descartaba `UnsatisfiedDependencyException`: un cableado roto pasaba verde** | Fase 05 | ✅ Fase 07 — reescrito |
| `Handler.java` (495 líneas), única clase de más de 300 líneas | Fase 04 | ✅ Fase 06 |
| `Handler` recibía un `JsonMapper` por constructor que nunca usaba | Fase 06 | ✅ Fase 06 |
| `Handler` al 86,1%: sin cubrir `handleListTasks` ni el error del legacy | Fase 06 | ✅ Fase 07 — **100%** |

### Vivos, para un plan futuro

| Hallazgo | Detectado en |
|---|---|
| **`build/issues.json` se genera siempre vacío** aunque haya violaciones: el mapeo de ficheros de `ArchitectureTest` no resuelve. Solo se ven con `-i` | Fase 07 |
| **El workaround `--no-configuration-cache` no es de JaCoCo**: falla `pitestReportAggregate` del plugin `info.solidsoft.gradle.pitest` al serializar `__additionalClasspath__`. Requiere corrección aguas arriba | Fases 00 y 07 |
| La Agent Card sigue compilada en Java (D-11 parcial); ya aislada, externalizarla es un cambio pequeño | Fase 06 |
| Las 4 suites del entry-point fijan `Handler` como único bean del contexto: introducir colaboradores `@Component` obligará a renegociarlas | Fase 06 |
| La guarda `getTask() == null` de `chat()` es inalcanzable: `A2AResponseFactory` siempre crea `Task` | Fase 07 |
| Verificar las eliminaciones de código con `grep`, no solo con el build | Fase 03 — **práctica permanente** |





