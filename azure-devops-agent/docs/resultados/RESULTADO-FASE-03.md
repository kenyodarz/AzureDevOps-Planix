# RESULTADO — FASE 03: Resolución de Intención

> **Ejecutada:** 2026-08-28 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(agent_intent): introducir resolutor de intencion en el dominio`
> **Instrucciones:** `docs/fases/FASE-03-resolucion-de-intencion.md`

---

## 1. Objetivo de la fase

Eliminar la cascada de `if` de enrutamiento de `executeChat()` (**D-01**) llevando la decisión «qué
flujo atiende este mensaje» a un servicio de dominio puro, y cerrar la dispersión de las listas de
comandos (**D-12**). Es la primera fase que **cambia el comportamiento visible**: aplica **DP-01**.

---

## 2. Qué se construyó

### Dominio (puro, sin Spring)

```
domain/model/src/main/java/co/com/bancolombia/model/agent/
├── AgentIntent.java         (enum: 6 intenciones, ordenadas por precedencia)
├── IntentResolution.java    (record inmutable: intención + params, workItemId() -> Optional)
└── IntentResolver.java      (servicio de dominio: tabla de precedencia DP-01)

domain/model/src/test/java/co/com/bancolombia/model/agent/
└── IntentResolverTest.java  (33 pruebas, sin un solo mock)
```

### Caso de uso

`AgentChatUseCase` pierde el enrutamiento completo: `normalizeText()`, `shouldSearchPlanning()`,
`containsGeneralKeyword()`, `handleSpecialCommands()`, 4 `Pattern` y 4 listas de comandos.
Gana `runApprovalFlow()`, `runDivisionFlow()` y un `switch` exhaustivo sobre `AgentIntent`.

### Wiring

`UseCasesConfig` publica `IntentResolver` como `@Bean` singleton (sin estado ni dependencias) y lo
inyecta como argumento 11 del caso de uso.

---

## 3. Decisiones aplicadas

### DP-01 — Precedencia de intención (Opción B: CORREGIR)

Materializada como tabla explícita en `IntentResolver.resolve()`, con *early returns* encadenados
mediante `Optional.or(...)`:

| Prioridad | Intención | Criterio |
|---:|---|---|
| 1 | `QUALITY_AUDIT` | verbo de auditoría **+** `(ID: n)` |
| 2 | `REFINEMENT` | `analicemos y refinemos` **+** `(ID: n)` |
| 3 | `APPROVAL` / `DIVISION` | comando corto exacto tras normalización |
| 4 | `GENERAL` | `contextId` `general*`/`dashboard*`, **o** palabra clave genérica |
| 5 | `PLANNING_DRAFT` | texto libre ≥ 25 caracteres que no sea comando |

**Efecto observable (comportamiento corregido):**

| Entrada | Antes | Ahora |
|---|---|---|
| `"Genera un reporte de calidad de la historia (ID: 12345)"` | General | **Auditoría** (`workItemId=12345`) |
| `"Necesito un servicio que consulta el saldo…"` | General | **Planificación** (con RAG) |

**Mejora colateral:** el reconocimiento de verbos de auditoría se hace sobre texto sin diacríticos,
por lo que `"Evalúa la calidad (ID: 7)"` ya se detecta por el verbo y no solo por la palabra
«calidad».

### DP-07 — cierre efectivo

Se encontró `buildPrompt()` **todavía presente** en el caso de uso, inalcanzable desde la Fase 01
(ningún llamador en todo el repositorio). Se eliminó, cerrando de verdad la decisión.

---

## 4. Deudas saldadas

| ID | Deuda | Cómo se resolvió |
|---|---|---|
| **D-01** | Cascada de `if` donde el primer `if` gana siempre | `IntentResolver` + `switch` exhaustivo; `executeChat()` solo valida y despacha |
| **D-12** | «Qué es un comando» decidido en dos sitios | Todas las constantes de enrutamiento viven únicamente en `IntentResolver` |
| **DP-07 (resto)** | `buildPrompt()` dead code | Eliminado |

---

## 5. Métricas obtenidas

| Métrica | Antes (Fase 02) | Después |
|---|---:|---:|
| Líneas `AgentChatUseCase` | 446 | **347** (−99) |
| Métodos de enrutamiento en el caso de uso | 4 | **0** |
| `Pattern` de enrutamiento en el caso de uso | 4 | **0** (solo queda `AUDIT_JSON_BLOCK`, de post-proceso) |
| Listas de comandos en el caso de uso | 4 | **0** |
| Ramas `if` de enrutamiento en `executeChat()` | 4 | **0** |
| Tests totales del proyecto | 118 | **152** |
| Cobertura `IntentResolver` | n/a | **100%** (218/218) |
| Cobertura `IntentResolution` | n/a | **100%** (56/56) |
| Cobertura `AgentIntent` | n/a | **100%** (39/39) |
| Cobertura `AgentChatUseCase` | 79,9% (561/702) | **84,5%** (561/664) |
| Cobertura `domain/model` | 95,8%* | **95,8%** (770/804) |
| Cobertura `domain/usecase` | 79,9% | **84,5%** (561/664) |
| Argumentos del constructor | 10 | 11 (se corrige en la Fase 05) |

> \* La métrica «60,3%» registrada en la Fase 02 se obtuvo sumando los *instructions* cubiertos de
> `model/agent` sobre el total de **todo** el módulo. Recalculada con el mismo criterio para ambos
> momentos, la cobertura real de `domain/model` era 93,1% antes y es 95,8% ahora. Ver Hallazgos.

### Suites nuevas o afectadas

| Suite | Tests | Fallos |
|---|---:|---:|
| `IntentResolver` (5 clases anidadas) | 33 | 0 |
| `AgentChatUseCase - Caracterización del enrutamiento` | 17 (16 + 1 nueva) | 0 |
| `AgentChatUseCase - Caracterización del parseo` | 7 | 0 |
| **Total del proyecto** | **152** | **0** |

---

## 6. Comandos ejecutados

```powershell
.\gradlew.bat :model:test          # 82 pruebas verdes, incluidas las 33 nuevas
.\gradlew.bat :usecase:test        # CONTROL DE SANIDAD: 23 pruebas, 2 rojas (las de DP-01)
.\gradlew.bat :usecase:test        # tras invertir las 2 pruebas: verde
.\gradlew.bat build                # BUILD SUCCESSFUL
.\gradlew.bat jacocoMergedReport --no-configuration-cache
```

**Control de sanidad del paso 7 — superado.** Fallaron **exactamente** las dos pruebas de DP-01:

```
DEFECTO DP-01: 'reporte' secuestra una peticion de auditoria con (ID: n) FAILED
DEFECTO DP-01: 'consulta' secuestra el flujo de Planificacion FAILED
23 tests completed, 2 failed
```

Ninguna de las otras 21 se movió: el refactor no introdujo regresiones y sí cambió el
comportamiento previsto.

---

## 7. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **El patrón de palabras clave no incluye las formas de infinitivo** (`listar`, `buscar`, `consultar`) pese a aparecer en el ejemplo del MD de la fase | El ejemplo contradice al propio caso 11 de su tabla y a la regla 2 de DP-01, cuyo texto literal justifica el límite `\b` precisamente «para que *consultar* dentro de una idea larga no secuestre el flujo de planeación». Si `consultar` fuese palabra clave, «un microservicio para **consultar** el saldo» volvería a irse a General. Se mantiene la lista original de DP-01: `lista`, `busca`, `consulta`, `reporte` (con sus plurales, para no perder la cobertura que daba el antiguo `contains()`) |
| **Regla añadida: se descartan las palabras clave gobernadas por un pronombre relativo** (`que`/`quien`/`cual`) | Sin ella es **imposible** satisfacer a la vez los dos requisitos obligatorios de la fase: `"Dame la lista de historias del sprint actual"` → General (prueba intocable) y `"Necesito un servicio que consulta el saldo…"` → Planificación (paso E). Ambas frases son largas y contienen una palabra clave como palabra completa, así que `\b` por sí solo no las distingue. El criterio es lingüísticamente sólido: tras un relativo, el verbo describe **el sistema que se quiere construir**, no una acción que se le pide al agente. **⚠️ Señalada al usuario para ratificación** |
| **Se tocó `AgentChatUseCaseParsingTest`** («NO TOCAR») | Únicamente el `setUp()`, para pasar el argumento 11 del constructor. Ninguna aserción, entrada ni nombre de prueba se modificó. Era imprescindible para compilar |
| **Se añadió una prueba a `AgentChatUseCaseCharacterizationTest`** | La exige el propio MD de la fase: «un texto de menos de 25 caracteres que no sea comando ni contenga palabra clave debe resolverse como `GENERAL`… Añade una prueba que lo verifique» |
| **Se eliminó `buildPrompt()`**, que no figuraba en la lista de archivos de la fase | Dead code confirmado (sin ningún llamador) que DP-07 ordenó eliminar y la Fase 01 dejó a medias. Al desaparecer `handleSpecialCommands()` quedaba como aviso permanente de Sonar S1144 |
| **`MIN_PLANNING_TEXT_LENGTH` es `public` en `IntentResolver`** | Permite que la prueba de frontera del caso 15 se escriba contra la constante y no contra un `25` mágico (S109) |

---

## 8. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| `domain/model` sin Spring, persistencia ni serialización | ✅ solo `java.text`, `java.util`, `java.util.regex` |
| `domain/usecase` sin anotaciones de Spring | ✅ |
| Modelo rico, no anémico | ✅ la decisión de intención vive en el dominio |
| Value Objects inmutables | ✅ `record` con copia defensiva del mapa |
| Open/Closed (§3-O) | ✅ añadir una intención toca el resolutor y el `switch`, nunca la lógica de decisión dispersa |
| Sin secretos hardcodeados (S2068) | ✅ |
| `Optional<T>` en retornos opcionales (S2259) | ✅ `resolve` internamente, `workItemId()` |
| Complejidad cognitiva ≤ 15 | ✅ `resolve()` = 1 expresión encadenada; `executeChat()` = 1 `if` + 1 `switch` |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ |
| Tests GIVEN/WHEN/THEN | ✅ 33 pruebas, `@Nested` por prioridad, parametrizadas para los comandos |
| Cobertura de clases nuevas ≥ 90% | ✅ **100%** en las tres |
| `AgentChatUseCase` ≤ 390 líneas | ✅ 347 |
| `.\gradlew.bat build` verde | ✅ |

---

## 9. Hallazgos

| Hallazgo | Atender en |
|---|---|
| **La métrica de cobertura de `domain/model` de la Fase 02 (60,3%) está mal calculada**: sumaba los *instructions* cubiertos de un solo paquete sobre el total del módulo. El valor real de entonces era 93,1% | Corregido aquí; vigilar en Fase 07 |
| `buildPrompt()` seguía vivo pese a que la Fase 01 lo daba por eliminado: conviene verificar las eliminaciones con `grep` y no solo con el build | Práctica para el resto de fases |
| `ArchitectureTest` mantiene sus 2 advertencias preexistentes (`Rule_2.7` ×5, `Rule_2.2` ×1). Las clases nuevas **no** añaden violaciones | Fase 07 |
| El constructor de `AgentChatUseCase` sube a 11 argumentos, como estaba previsto | Fase 05 |
| Siguen los 6 bloques `onErrorResume` idénticos (D-02); ahora son 6 métodos `run*Flow` en la misma clase | Fase 04 |
| Los ~15 modelos A2A de `domain/model` siguen sin pruebas propias | Fase 07 |

---

## 10. Estado al cerrar

- **Siguiente fase generada:** `FASE-04-separacion-de-flujos.md`
- **Decisiones abiertas:** DP-05 (bloquea Fase 07)
- **Pendiente de ratificación:**
  1. Eliminación de la «Regla de Mínimos de 5 puntos» (Fase 01).
  2. **Regla del pronombre relativo** en la detección de palabras clave genéricas (esta fase).
     Revertirla es borrar los tres lookbehind de `GENERAL_KEYWORD_PATTERN`; a cambio, la prueba
     `..._thenExecutesPlanningSimilarityFlow` volvería a exigir flujo General.
- **Verificación clave:** el control de sanidad del paso 7 rompió **exactamente** las 2 pruebas
  previstas y ninguna más.

