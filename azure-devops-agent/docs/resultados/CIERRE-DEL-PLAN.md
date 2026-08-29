# INFORME DE CIERRE — Plan de Refactorización `azure-devops-agent`

> **Plan:** `docs/plan/PLAN_MAESTRO.md` v1.0 · **Iniciado:** 2026-08-28 · **Cerrado:** 2026-08-29
> **Fases ejecutadas:** 8 (00 a 07), todas 🟢 COMPLETADAS
> **Resultado:** todas las métricas de éxito alcanzadas; ninguna decisión abierta.

---

## 1. Métricas de éxito (`PLAN_MAESTRO.md` §5)

| Métrica | Baseline | Objetivo | **Final real** | |
|---|---:|---:|---:|:--|
| Líneas de `AgentChatUseCase` | 644 | ≤ 100 | **89** | ✅ |
| Líneas de `Handler` | 495 | ≤ 150 | **112** | ✅ |
| Clases > 300 líneas | 2 | 0 | **0** | ✅ |
| Complejidad cognitiva máx. por método | > 15 | ≤ 15 | **≤ 15** | ✅ |
| Cobertura `domain/model` | 0% | ≥ 90% | **99,7%** | ✅ |
| Cobertura `domain/usecase` | 0% | ≥ 90% | **99,2%** | ✅ |
| Constructores con > 5 parámetros | 1 | 0 | **0** | ✅ |
| Bloques `onErrorResume` duplicados | 6 | 1 | **1** | ✅ |
| Números mágicos en dominio | ≥ 4 | 0 | **0** | ✅ |

### Métricas adicionales

| Métrica | Baseline | Final |
|---|---:|---:|
| Tests totales | 23 | **305** |
| Violaciones de ArchUnit | 6 | **0** |
| Decisiones pendientes abiertas | 7 | **0** |
| Clase productiva más larga | `AgentChatUseCase` (644) | `PgVectorPlanningAdapter` (240) |

---

## 2. Estado de las deudas técnicas D-01 a D-14

| ID | Deuda | Estado | Dónde se saldó |
|---|---|---|---|
| **D-01** | Routing por cascada de `if` con `contains()`; el primer `if` gana siempre | 🟢 **SALDADA** | Fase 03 — `IntentResolver` en dominio puro, con precedencia explícita (DP-01) |
| **D-02** | 6 bloques `onErrorResume` duplicados | 🟢 **SALDADA** | Fase 04 — un único `onErrorResume` en `executeChat()` |
| **D-03** | ~180 líneas de prompts como constantes en el dominio | 🟢 **SALDADA** | Fase 01 — `PromptTemplatePort` + 5 plantillas en `resources/prompts/` |
| **D-04** | Post-procesado de respuesta LLM duplicado entre 2 flujos | 🟢 **SALDADA** | Fase 02 |
| **D-05** | Parseo con regex y defaults silenciosos (`return 1`) | 🟢 **SALDADA** | Fase 02 — `ComplexityEstimation.parseFrom()` devuelve `Optional` y avisa (DP-03) |
| **D-06** | Números mágicos `13`, `8`, `25`, `3` | 🟢 **SALDADA** | Fase 02 — constantes con nombre; umbral `> 8` (DP-02) |
| **D-07** | `Pattern.compile()` en caliente por request | 🟢 **SALDADA** | Fase 03 — patrones precompilados como constantes |
| **D-08** | Imports FQN inline | 🟢 **SALDADA** | Fase 03 |
| **D-09** | Constructor de 9 args con 5 `String` posicionales | 🟢 **SALDADA** | Fases 04 y 05 — 4 argumentos tipados; `AzureDevOpsScope` y `CorporateKnowledge` |
| **D-10** | `Handler` mezcla 4 responsabilidades | 🟢 **SALDADA** | Fase 06 — `JsonRpcPayloadMapper`, `JsonRpcResponseFactory`, `JsonRpcDispatcher` |
| **D-11** | Agent Card construida a mano en Java | 🟡 **PARCIAL** | Fase 06 — aislada en `AgentCardProvider`. **Sigue compilada**: externalizarla a JSON quedó como mejora opcional |
| **D-12** | Lista de comandos duplicada en dos métodos | 🟢 **SALDADA** | Fase 03 |
| **D-13** | `chat()` async publica a un adapter no-op: camino muerto no documentado | 🟢 **SALDADA** | Fase 07 — **por documentación y cobertura**, no por eliminación (DP-05, Opción A) |
| **D-14** | Cobertura de `domain/usecase` muy por debajo del 90% | 🟢 **SALDADA** | Fases 00→07 — de 0% a **99,2%** |

**13 de 14 saldadas por completo. D-11 queda parcial y documentada**: el objetivo de SRP se cumplió
—la tarjeta ya no vive en el `Handler`—, pero publicar una capacidad nueva sigue exigiendo
recompilar.

---

## 3. Decisiones (DP-01 a DP-07)

| ID | Decisión tomada | Resuelta por | Fecha |
|---|---|---|---|
| DP-01 | Corregir la precedencia: `(ID: n)` + verbo gana sobre palabras clave genéricas | Usuario | 2026-08-28 |
| DP-02 | Umbral de división `> 8` según la tabla oficial de `HISTORIA_USUARIO.md` | Usuario | 2026-08-28 |
| DP-03 | Aviso explícito «no se pudo obtener la estimación» en lugar de inventar 1 punto | Usuario | 2026-08-28 |
| DP-04 | Ruta `resources/prompts/` y nombres de archivo (5 plantillas) | Usuario | 2026-08-28 |
| DP-05 | Conservar `chat()` async como punto de extensión A2A/Kafka (Opción A) | Usuario | 2026-08-29 |
| DP-06 | Estimación por horas; se elimina la «Regla de Mínimos de 5 puntos» | Usuario | 2026-08-28 |
| DP-07 | Eliminar `buildPrompt()` y la plantilla de creación estructurada (dead code) | Usuario | 2026-08-28 |

**Ninguna decisión fue tomada por un agente.** Las siete las resolvió el usuario, conforme a la
Política de No-Asunción (`rules/spring-rules.md` §6).

---

## 4. Pendiente de ratificación del usuario

Ninguna bloquea nada ni afecta al build. Ambas se derivaron **por implicación** de una decisión del
usuario, no de forma literal, y se señalan por transparencia.

| Asunto | Fase | Por qué se derivó | Cómo revertir |
|---|---|---|---|
| **Eliminación de la «Regla de Mínimos de 5 puntos»** | 01 | La tabla de horas de DP-06 hace que 5 SP = 8-24 h; la regla afirmaría que «publicar un CRUD nunca cuesta menos de 8 horas», en contra de la guía corporativa. Además el prompt de auditoría ya decía lo contrario que el de creación | Reintroducir el párrafo en `prompts/fase2-historia-estructurada.md` |
| **Regla del pronombre relativo en las palabras clave genéricas** | 03 | Sin ella es imposible cumplir a la vez los dos casos obligatorios de la fase: «Dame la **lista** de historias» → General y «un servicio **que consulta** el saldo» → Planificación | Borrar los tres *lookbehind* de `IntentResolver.GENERAL_KEYWORD_PATTERN` |

---

## 5. Lo que el plan no atacó

### Fuera de alcance declarado (`PLAN_MAESTRO.md` §9)

- Migración de `NoOpAgentResponseAdapter` a un productor Kafka real.
- Cambios en el esquema de base de datos (`agent_tasks`, `planning_chunks`).
- Modificación del contrato público A2A / JSON-RPC 2.0 — **respetado íntegramente**: las cuatro
  suites del entry-point siguen intactas y verdes desde la Fase 00.
- Cambios en `azure-devops-backend`, `azure-devops-frontend` o `azure-devops-mcp`.

### Trabajo para un plan futuro

| # | Tema | Origen |
|---|---|---|
| 1 | **Activar A2A sobre Kafka**: sustituir `NoOpAgentResponseAdapter` por el adaptador productor. El punto de extensión ya está preparado y cubierto por pruebas | DP-05 |
| 2 | **Externalizar la Agent Card** a `resources/agent-card.json` para publicar capacidades sin recompilar | D-11 |
| 3 | **`build/issues.json` se genera vacío** aunque haya violaciones de ArchUnit: el mapeo de ficheros no resuelve. Hoy solo se ven con `-i` | Fase 07 |
| 4 | **`pitestReportAggregate` rompe la configuration cache** (`error writing value of type DefaultConfigurableFileCollection`). Defecto del plugin `info.solidsoft.gradle.pitest`; requiere corrección aguas arriba | Fase 00 y 07 |
| 5 | Regeneración automática de la historia si falla la estimación | DP-03, descartado explícitamente por el usuario |
| 6 | Reconstruir la creación estructurada como intención de primer nivel si el frontend la necesita | DP-07 |
| 7 | Las 4 suites del entry-point fijan `Handler` como único bean del contexto: introducir colaboradores `@Component` obligará a renegociarlas | Fase 06 |
| 8 | `PgVectorPlanningAdapter` (240 líneas) es ahora la clase más larga; por debajo del umbral, pero es la siguiente candidata | Fase 07 |

---

## 6. Cronología

| Fase | Fecha | Hito | Tests |
|---|---|---|---:|
| 00 | 2026-08-28 | Baseline medido; 23 pruebas de caracterización blindan el comportamiento | 23 |
| 01 | 2026-08-28 | Prompts externalizados; 644 → 505 líneas | 69 |
| 02 | 2026-08-28 | Value Objects de estimación; 505 → 446 | 118 |
| 03 | 2026-08-28 | `IntentResolver` en dominio puro; 446 → 347 | 152 |
| 04 | 2026-08-28 | Strategy: 6 handlers + dispatcher; 347 → **89** | 186 |
| 05 | 2026-08-29 | Configuración tipada; descubierto que nada validaba el wiring de Spring | 207 |
| 06 | 2026-08-29 | Entry-point partido; **cero clases > 300 líneas** | 251 |
| 07 | 2026-08-29 | DP-05 resuelta; ArchUnit a cero; cobertura ≥ 99% | **305** |

---

## 7. Lecciones registradas

1. **Las pruebas de caracterización de la Fase 00 fueron la red que permitió todo lo demás.** Sin
   congelar el comportamiento antes de tocar nada, ninguna de las seis refactorizaciones siguientes
   habría sido verificable.
2. **Un test puede mentir.** `UseCasesConfigTest` capturaba `UnsatisfiedDependencyException` y
   respondía `assertTrue(true)`: durante dos fases que reescribieron el wiring por completo, un
   cableado roto habría pasado como verde. Se detectó en la Fase 05 y se corrigió en la 07.
3. **El código muerto no lo detecta el compilador.** `buildPrompt()` sobrevivió dos fases; el campo
   `JsonMapper` de `Handler` sobrevivió seis, inyectado y jamás leído; `ClientRequest` sobrevivió el
   plan entero. Verificar las eliminaciones con `grep`, no con el build, quedó como práctica
   permanente.
4. **Preguntar en lugar de asumir cambió el resultado técnico.** DP-05 parecía un camino muerto
   eliminable; la respuesta del usuario —A2A corre sobre Kafka en el Banco— convirtió una supuesta
   limpieza en un punto de extensión que había que conservar y cubrir.
5. **Un *Null Object* puede sustituir a un *feature flag*.** Reconocerlo evitó añadir configuración,
   cableado condicional y una segunda ruta de arranque para lograr un apagado que ya existía.

