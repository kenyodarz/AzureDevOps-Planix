# RESULTADO — FASE 02: Value Objects de Estimación

> **Ejecutada:** 2026-08-28 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(agent_chat): extraer objeto de valor de estimacion de complejidad`
> **Instrucciones:** `docs/fases/FASE-02-value-objects-de-estimacion.md`

---

## 1. Objetivo de la fase

Eliminar la duplicación del post-procesado de la respuesta del LLM (**D-04**), sustituir el parseo
con defaults silenciosos por un Value Object rico con `Optional` (**D-05**), y erradicar los números
mágicos y las regex compiladas en caliente (**D-06**, **D-07**, **D-08**).

---

## 2. Qué se construyó

### Dominio (puro, sin Spring)

```
domain/model/src/main/java/co/com/bancolombia/model/agent/
├── UncertaintyLevel.java        (enum con parseo tolerante a tildes vía Unicode NFD)
├── ComplexityEstimation.java    (record con requiresSplit() y parseFrom() -> Optional)
└── EstimationSummary.java       (servicio de dominio: compone la respuesta final)

domain/model/src/test/java/co/com/bancolombia/model/agent/
├── UncertaintyLevelTest.java       (16 pruebas)
├── ComplexityEstimationTest.java   (25 pruebas)
└── EstimationSummaryTest.java      (8 pruebas)
```

> `domain/model` no tenía carpeta `src/test`. Se creó en esta fase; **no hizo falta tocar
> `build.gradle`**: las dependencias de test se heredan de `main.gradle`.

### Caso de uso

`AgentChatUseCase` pierde los tres extractores privados, las dos constantes de presentación y la
lógica duplicada de composición. Gana un bloque de constantes con nombre.

---

## 3. Decisiones aplicadas

### DP-02 — Umbral de división `> 8`

Materializado como comportamiento del propio Value Object, no como un `if` en el caso de uso
(`spring-rules.md` §1: «Prohibido el modelo anémico»):

```java
public static final int MAX_STORY_POINTS_PER_STORY = 8;

public boolean requiresSplit() {
    return points > MAX_STORY_POINTS_PER_STORY;
}
```

**Efecto observable:** una estimación de **9, 10 o 12 puntos ahora sí exige dividir**. Antes, con
`>= 13`, pasaba desapercibida. Es un caso real porque el modelo no siempre respeta Fibonacci.

### DP-03 — Aviso explícito de estimación ausente

`parseFrom` devuelve `Optional<ComplexityEstimation>`. Cuando viene vacío, el usuario ve:

```
⚠️ **No se pudo obtener la estimación:** el modelo no devolvió una estimación válida.
Por favor, estima manualmente esta historia en Azure DevOps.
```

No se muestra ningún número inventado ni la alerta de complejidad (no se puede evaluar
`requiresSplit()` sin puntos). Sí se conserva la llamada a la acción del flujo.

---

## 4. Deudas saldadas

| ID | Deuda | Cómo se resolvió |
|---|---|---|
| **D-04** | Post-procesado duplicado literal entre Refinamiento y Aprobación | `EstimationSummary.compose(...)`, con el mensaje de confirmación como parámetro |
| **D-05** | Parseo con defaults silenciosos (`return 1`) | `Optional<ComplexityEstimation>`; los defaults inventados desaparecen |
| **D-06** | Números mágicos `13`, `25`, `3` | `MAX_STORY_POINTS_PER_STORY`, `MIN_PLANNING_TEXT_LENGTH`, `RAG_MAX_RESULTS` |
| **D-07** | `Pattern.compile()` en cada petición | 5 constantes `private static final Pattern` |
| **D-08** | Imports FQN inline (`java.util.regex.*`, `java.util.logging.Level`) | Imports normales; cero usos cualificados |
| **D-12 (parcial)** | Lista de comandos duplicada | `SHORT_COMMANDS`, `APPROVAL_COMMANDS`, `DIVISION_COMMANDS` como constantes; el resto se cierra en la Fase 03 |

### Mejora colateral: normalización Unicode

`normalizeText()` sustituye la cadena de cinco `replace()` de vocales acentuadas por descomposición
canónica:

```java
Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
```

Ahora cubre **todos** los diacríticos (ñ, ü, à…), no solo las cinco vocales previstas.

---

## 5. Métricas obtenidas

| Métrica | Antes (Fase 01) | Después |
|---|---:|---:|
| Líneas `AgentChatUseCase` | 505 | **446** (−59) |
| Métodos de parseo en el caso de uso | 3 | **0** |
| Bloques de post-procesado duplicados | 2 | **1** |
| `Pattern.compile()` en caliente | 4 | **0** |
| Números mágicos en el caso de uso | 3 | **0** |
| Tests totales del proyecto | 69 | **118** |
| Tests en `domain/model` | 0 | **49** |
| Cobertura `domain/model` | 0% | **60,3%** (296/491) |
| Cobertura `domain/usecase` | 81,5% | 79,6% (681/856) |

### Suites tras la fase

| Suite | Tests | Fallos |
|---|---:|---:|
| `ComplexityEstimation` | 25 | 0 |
| `UncertaintyLevel` | 16 | 0 |
| `EstimationSummary` | 8 | 0 |
| `AgentChatUseCase - Caracterización del enrutamiento` | 16 | 0 |
| `AgentChatUseCase - Caracterización del parseo` | 7 | 0 |
| `ClasspathPromptTemplateAdapter` | 10 | 0 |
| `Contenido real de las plantillas de prompt` | 7 | 0 |
| `RouterRestTest` | 10 | 0 |
| `ArchitectureTest` | 6 | 0 |
| `JsonRpcErrorContractTest` | 5 | 0 |
| Resto | 8 | 0 |
| **Total** | **118** | **0** |

---

## 6. Comandos ejecutados

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat jacocoMergedReport --no-configuration-cache
git --no-pager diff --stat -- .../AgentChatUseCaseCharacterizationTest.java   # salida vacía
```

**Resultado:** `BUILD SUCCESSFUL` · 118/118 pruebas en verde.

---

## 7. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **`AgentChatUseCase` quedó en 446 líneas, no en ≤430** | Se añadió un bloque de constantes con nombre (comandos, palabras clave, llamadas a la acción, patrones) que sustituye a literales dispersos. Son más líneas pero eliminan duplicación y números mágicos. El objetivo real (≤100) se alcanza en la Fase 04, al separar los flujos |
| **Cobertura de `domain/model` es 60,3%, no ≥90%** | El módulo contiene además los ~15 modelos A2A heredados (`Message`, `Task`, `AgentCard`, `Part`…) sin ninguna prueba. Las tres clases creadas en esta fase sí están cubiertas por 49 pruebas. Elevar el conjunto al 90% corresponde a la Fase 07 |
| **`EstimationSummary` se ubicó en `domain/model`, no en `domain/usecase`** | Las instrucciones lo permitían explícitamente. Compone texto a partir de reglas de negocio (umbral, ausencia de estimación) y no depende de ningún gateway, por lo que encaja como servicio de dominio |
| **Se extrajo `containsGeneralKeyword()` conservando `contains()`** | Se aisló la detección de palabras clave para preparar la Fase 03, **sin cambiar su semántica**: pasar a coincidencia por palabra completa es parte de DP-01 y alteraría el enrutamiento, fuera del alcance de esta fase |

---

## 8. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| `domain/model` sin Spring, persistencia ni serialización | ✅ solo `java.text`, `java.util`, `java.util.regex` |
| `domain/usecase` sin anotaciones de Spring | ✅ |
| Modelo rico, no anémico | ✅ `requiresSplit()` vive en la estimación |
| Value Objects inmutables | ✅ `record` con constructor compacto validador |
| Sin secretos hardcodeados (S2068) | ✅ |
| `Optional<T>` en retornos opcionales (S2259) | ✅ `parseFrom`, `UncertaintyLevel.fromText` |
| Complejidad cognitiva ≤ 15 | ✅ |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ |
| Tests GIVEN/WHEN/THEN | ✅ incluye pruebas parametrizadas de frontera |
| Build verde | ✅ |

---

## 9. Hallazgos

| Hallazgo | Atender en |
|---|---|
| `ArchitectureTest` sigue emitiendo las 2 advertencias preexistentes (`Rule_2.7` ×5, `Rule_2.2` ×1). Las clases nuevas **no** añaden violaciones | Fase 07 |
| Los ~15 modelos A2A de `domain/model` siguen sin pruebas y lastran la cobertura del módulo | Fase 07 |
| El constructor de `AgentChatUseCase` sigue en 10 argumentos | Fase 05 |

---

## 10. Estado al cerrar

- **Siguiente fase generada:** `FASE-03-resolucion-de-intencion.md`
- **Decisiones abiertas:** DP-05 (bloquea Fase 07)
- **Pendiente de ratificación:** eliminación de la «Regla de Mínimos de 5 puntos» (Fase 01)
- **Verificación clave:** `git diff` sobre `AgentChatUseCaseCharacterizationTest.java` quedó
  **vacío**: el enrutamiento no se tocó, tal como exigían las instrucciones

