# FASE 02 — Value Objects de Estimación

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01 (🟢 completada) · **Riesgo:** Bajo
> **Commit al cerrar:** `refactor(agent_chat): extraer objeto de valor de estimacion de complejidad`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La **Fase 01 quedó cerrada**:

- Las 5 plantillas de prompt viven en `applications/app-service/src/main/resources/prompts/`.
- La tabla oficial de estimación por horas (DP-06) está en un único fragmento compartido,
  `prompts/fragmentos/tabla-estimacion.md`, inyectado automáticamente por el adaptador.
- `AgentChatUseCase` bajó de 644 a **505 líneas**.
- `buildPrompt()` fue eliminado (DP-07).
- **69 tests** en verde en todo el proyecto.

### 1.2 Problema que resuelve esta fase

Tres deudas concentradas en el post-procesado de la respuesta del LLM:

| ID | Deuda | Ubicación actual (tras Fase 01) |
|---|---|---|
| **D-04** | El bloque «limpiar JSON + añadir bloque informativo + alerta de complejidad» está **duplicado literalmente** entre el flujo de Refinamiento y el de Aprobación | `runRefinementFlow()` y la rama `isApproval` de `handleSpecialCommands()` |
| **D-05** | Parseo del JSON con expresiones regulares y **defaults silenciosos**: si el LLM no estima, se inventa `1` punto e incertidumbre «Media» | `extractPuntosFromLlmResponse()`, `extractJustificacion()`, `extractIncertidumbreNivel()` |
| **D-06** | Números mágicos: `13` (umbral de alerta), `25` (longitud mínima para planeación), `3` (resultados RAG) | `handleSpecialCommands()`, `shouldSearchPlanning()`, `runPlanningSimilarityFlow()` |

Además, **D-07** (`Pattern.compile()` ejecutado en cada petición) y **D-08** (imports
`java.util.regex.*` escritos con nombre completamente cualificado dentro del cuerpo de los métodos).

### 1.3 Estado esperado al terminar

1. Existe el enum `UncertaintyLevel` y el record `ComplexityEstimation` en `domain/model`, con
   comportamiento de negocio propio (**modelo rico, no anémico**, `spring-rules.md` §1).
2. `ComplexityEstimation.parseFrom(...)` devuelve `Optional<ComplexityEstimation>`: **desaparecen
   los defaults inventados**.
3. El umbral de división es `> 8` mediante la constante `MAX_STORY_POINTS_PER_STORY` (**DP-02
   aplicada**).
4. Un único componente compone la respuesta final: la duplicación D-04 desaparece.
5. Todos los `Pattern` son `static final`; no quedan imports FQN inline.
6. `AgentChatUseCase` baja de 505 a **~420 líneas**.
7. Cobertura de `domain/model` deja de ser 0%.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `domain/model/src/main/java/co/com/bancolombia/model/agent/UncertaintyLevel.java` | CREAR |
| `domain/model/src/main/java/co/com/bancolombia/model/agent/ComplexityEstimation.java` | CREAR |
| `domain/model/src/test/java/co/com/bancolombia/model/agent/ComplexityEstimationTest.java` | CREAR |
| `domain/model/src/test/java/co/com/bancolombia/model/agent/UncertaintyLevelTest.java` | CREAR |
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/AgentChatUseCase.java` | MODIFICAR |
| `domain/usecase/src/test/.../AgentChatUseCaseParsingTest.java` | MODIFICAR (solo los tests indicados en §2.1 Paso F) |
| `domain/usecase/src/test/.../AgentChatUseCaseCharacterizationTest.java` | **NO TOCAR** |

> `domain/model` no tiene hoy carpeta `src/test`. Créala. No requiere cambios en `build.gradle`:
> `spring-boot-starter-test` y `reactor-test` se heredan de `main.gradle`.

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§1 `domain/model`:** «Clases ricas en comportamiento e invariantes de negocio. **Prohibido el
  modelo anémico**.» Por eso `requiresSplit()` vive en `ComplexityEstimation` y no en el caso de uso.
- **§1:** Value Objects inmutables. Usar `record`.
- **§4 S2259:** «Siempre envolver los retornos opcionales en `Optional<T>`.» Es exactamente el caso
  de `parseFrom`.
- **§4 S109:** sin literales numéricos sueltos.
- **§4 Cognitive Complexity ≤ 15.**
- **§5:** tests GIVEN/WHEN/THEN; cobertura de `domain/model` y `domain/usecase` ≥ 90%.

### 1.6 Decisiones pendientes

| ID | Estado | Acción |
|---|---|---|
| **DP-03** | 🟡 PROPUESTA | **Confirmar con el usuario antes del Paso B.** Recomendación: Opción C — mostrar «⚠️ El modelo no devolvió una estimación válida» en lugar de inventar 1 punto. |

**Decisiones ya resueltas que se aplican aquí:**

| ID | Resolución | Efecto |
|---|---|---|
| **DP-02** | Umbral `> 8` | `MAX_STORY_POINTS_PER_STORY = 8`; `requiresSplit()` devuelve `points > 8`. Invertir el test de 9 puntos (§2.1 Paso F). |
| **DP-06** | Tabla por horas | Ya aplicada en los prompts. Aquí solo se usa el umbral coherente con ella (8 SP = 30 h máximo). |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — `UncertaintyLevel` (dominio puro)

```java
package co.com.bancolombia.model.agent;

public enum UncertaintyLevel {
    NULA, BAJA, MEDIA, ALTA, CRITICA;

    /**
     * Parseo tolerante del texto devuelto por el modelo: ignora mayúsculas, espacios y tildes.
     * Devuelve Optional.empty() si el valor no es reconocible.
     */
    public static Optional<UncertaintyLevel> fromText(String raw) { ... }
}
```

Normalizar tildes con `Normalizer.normalize(raw, Form.NFD).replaceAll("\\p{M}", "")` en lugar de
encadenar `replace()`. Debe reconocer «Crítica», «critica» y «CRITICA» como el mismo valor.

#### Paso B — `ComplexityEstimation` (Value Object rico)

```java
package co.com.bancolombia.model.agent;

public record ComplexityEstimation(int points, UncertaintyLevel level, String rationale) {

    private static final int MAX_STORY_POINTS_PER_STORY = 8;   // DP-02

    public ComplexityEstimation { /* validar invariantes: points > 0, level no nulo */ }

    /** DP-02: la guía corporativa exige dividir toda historia por encima de 8 puntos. */
    public boolean requiresSplit() {
        return points > MAX_STORY_POINTS_PER_STORY;
    }

    /** Devuelve Optional.empty() si el modelo no entregó un bloque de estimación válido. */
    public static Optional<ComplexityEstimation> parseFrom(String llmResponse) { ... }
}
```

Los `Pattern` de parseo deben ser **`private static final`**, compilados una sola vez.

`parseFrom` devuelve `Optional.empty()` si falta el campo `puntos` o no es numérico. **Está
prohibido devolver un valor por defecto inventado** (D-05, DP-03).

#### Paso C — Componente de composición de la respuesta

Crear en `domain/usecase` (o `domain/model` si se considera servicio de dominio) un componente que
concentre lo hoy duplicado:

1. Eliminar del texto el bloque JSON de estimación (ambas variantes: con y sin cerca ```json```).
2. Añadir el bloque informativo con puntos, nivel y justificación.
3. Añadir la alerta de complejidad **si** `estimation.requiresSplit()`, o el mensaje de
   confirmación en caso contrario.
4. Manejar el caso `Optional.empty()` según la resolución de DP-03.

El mensaje de confirmación difiere entre flujos («¿Deseas actualizar el Work Item…» en Refinamiento,
«¿Deseas registrar esta Historia…» en Aprobación): pásalo como parámetro, no lo dupliques.

#### Paso D — Constantes en lugar de números mágicos

| Literal actual | Constante | Ubicación |
|---|---|---|
| `13` | (desaparece; sustituido por `requiresSplit()`) | — |
| `25` | `MIN_PLANNING_TEXT_LENGTH` | `AgentChatUseCase` |
| `3` | `RAG_MAX_RESULTS` | `AgentChatUseCase` |

#### Paso E — Limpieza de regex e imports

- Mover todos los `Pattern.compile()` a constantes `private static final Pattern`.
- Sustituir los usos FQN inline (`java.util.regex.Pattern`, `java.util.logging.Level`) por imports.
- Sustituir la cadena de `replace()` de `normalizeText()` por normalización Unicode NFD.

#### Paso F — Ajustar los tests afectados

En `AgentChatUseCaseParsingTest`, **solo** estos cambios:

1. `givenLlmResponseWithNinePoints_whenApprovalFlow_thenDoesNotAppendAlertYet`
   → renombrar a `givenLlmResponseWithNinePoints_whenApprovalFlow_thenAppendsComplexityAlert`,
   **invertir la aserción** (`contains` en vez de `doesNotContain`) y actualizar el Javadoc:
   DP-02 aplicada.
2. `givenLlmResponseWithoutJson_whenApprovalFlow_thenInfoBlockShowsDefaultValues`
   → adaptar al comportamiento que decida DP-03 y actualizar el Javadoc.
3. Los 5 tests restantes **no se tocan**.

#### Paso G — Tests unitarios puros de los Value Objects

Cobertura ≥ 90% de `domain/model`. Casos mínimos:

- `parseFrom` con JSON válido, con JSON dentro de cerca ```json```, sin JSON, con `puntos` no
  numérico, con JSON parcial (sin nivel ni justificación).
- `requiresSplit()` en la frontera: **8 → false**, **9 → true**, 13 → true, 5 → false.
- `UncertaintyLevel.fromText` con «Crítica», «critica», «MEDIA», valor desconocido y `null`.
- Invariantes del constructor compacto.

### 2.2 Qué NO hacer

- ❌ **No tocar `AgentChatUseCaseCharacterizationTest`.** Esta fase no altera el enrutamiento.
- ❌ No corregir DP-01 (es la Fase 03).
- ❌ No modificar las plantillas `.md` de la Fase 01.
- ❌ No introducir Spring en `domain/model` ni en `domain/usecase`.
- ❌ No tocar `Handler.java` (Fase 06).
- ❌ No dejar ningún valor por defecto silencioso en el parseo.

### 2.3 Criterios de aceptación

- [ ] DP-03 resuelta y registrada en `DECISIONES_PENDIENTES.md`.
- [ ] `UncertaintyLevel` y `ComplexityEstimation` creados, sin imports de Spring.
- [ ] `parseFrom` devuelve `Optional<ComplexityEstimation>`.
- [ ] `requiresSplit()` implementa `points > 8`; **9 puntos disparan la alerta**.
- [ ] El post-procesado ya no está duplicado (D-04 resuelta).
- [ ] Cero `Pattern.compile()` fuera de constantes `static final`.
- [ ] Cero imports FQN inline en `AgentChatUseCase`.
- [ ] Cero literales numéricos sueltos salvo `-1`, `0`, `1`.
- [ ] `AgentChatUseCase` ≤ 430 líneas.
- [ ] Cobertura `domain/model` ≥ 90%.
- [ ] `git diff` sobre `AgentChatUseCaseCharacterizationTest.java` **vacío**.
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL` (69 tests + los nuevos).

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] Sin secretos hardcodeados (S2068)
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109)
- [ ] Tests GIVEN/WHEN/THEN con Mockito
- [ ] `.\gradlew.bat build` verde

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar estado de DP-03 |
| 2 | **Si DP-03 sigue abierta → preguntar al usuario** | Decisión registrada |
| 3 | Crear `UncertaintyLevel` + su test (Paso A, G) | Tests en verde |
| 4 | Crear `ComplexityEstimation` + su test (Paso B, G) | Tests en verde; frontera 8/9 cubierta |
| 5 | Crear el componente de composición de respuesta (Paso C) | Duplicación D-04 eliminada |
| 6 | Sustituir en `AgentChatUseCase` y aplicar Pasos D y E | Use case ≤ 430 líneas |
| 7 | Ajustar los 2 tests indicados en el Paso F | Aserción de 9 puntos invertida |
| 8 | Ejecutar `.\gradlew.bat test` | Todas las suites en verde |
| 9 | Verificar `git diff` sobre `AgentChatUseCaseCharacterizationTest.java` | Salida **vacía** |
| 10 | Ejecutar `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| 11 | Medir cobertura (`jacocoMergedReport --no-configuration-cache`) y actualizar `ESTADO.md` | Métricas al día |
| 12 | Marcar Fase 02 🟢 COMPLETADA + entrada en la bitácora | Tablero al día |
| 13 | **Generar `docs/fases/FASE-03-resolucion-de-intencion.md`** con `_PLANTILLA_FASE.md` | Checkpoint creado |
| 14 | Commit: `refactor(agent_chat): extraer objeto de valor de estimacion de complejidad` | Cambios versionados |

> ⚠️ **El paso 13 es innegociable.** Sin el MD de la Fase 03, la Fase 02 se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la Fase 03

**CONTEXTO**
- Venimos de la Fase 02: la estimación es un Value Object rico y el post-procesado está unificado.
- Deudas a resolver: **D-01** (routing por cascada de `if` con `contains()`), **D-12** (lista de
  comandos duplicada entre `shouldSearchPlanning()` y `handleSpecialCommands()`).
- Decisión a aplicar: **DP-01** (🟢 resuelta) — nueva precedencia de intención.

**INSTRUCCIONES**
1. Crear en `domain/model/agent`: enum `AgentIntent` (`GENERAL`, `QUALITY_AUDIT`, `REFINEMENT`,
   `PLANNING_DRAFT`, `APPROVAL`, `DIVISION`) y record `IntentResolution(AgentIntent intent,
   Map<String,String> params)`.
2. Crear `IntentResolver` como **servicio de dominio puro** (sin gateways, 100% testeable sin
   mocks) que implemente la tabla de precedencia de DP-01:

   | Prioridad | Intención | Criterio |
   |---:|---|---|
   | 1 | `QUALITY_AUDIT` | verbo de auditoría **+** `(ID: n)` |
   | 2 | `REFINEMENT` | `analicemos y refinemos` **+** `(ID: n)` |
   | 3 | `APPROVAL` / `DIVISION` | comando corto exacto tras normalización |
   | 4 | `GENERAL` | `contextId` `general*`/`dashboard*`, **o** palabra clave genérica |
   | 5 | `PLANNING_DRAFT` | texto libre > 25 caracteres |

3. Las palabras clave genéricas (`lista`, `busca`, `consulta`, `reporte`) deben coincidir como
   **palabra completa** (`\b`), no con `contains()`.
4. Centralizar la lista de comandos en un único lugar (D-12).
5. `executeChat()` pasa a: resolver intención → despachar. Sin cascada de `if`.
6. **Invertir las aserciones** de los 2 tests de DP-01 en
   `AgentChatUseCaseCharacterizationTest`:
   - `...thenGeneralFlowWinsByPrecedence` (reporte) → ahora debe resolver `QUALITY_AUDIT`.
   - `...thenGeneralFlowWinsByPrecedence` (consulta) → ahora debe resolver `PLANNING_DRAFT`.
   Renombrarlos y actualizar el Javadoc indicando que DP-01 quedó aplicada.
7. Añadir tests de tabla de `IntentResolver` cubriendo cada prioridad y sus fronteras.

**NO HACER:** no separar aún los flujos en handlers (es la Fase 04); no tocar `Handler.java`.

**ORDEN DE EJECUCIÓN:** crear `AgentIntent`/`IntentResolution` → crear `IntentResolver` con tests de
tabla → sustituir la cascada en `executeChat()` → invertir los 2 tests de DP-01 → build verde →
actualizar `ESTADO.md` → **generar `FASE-04-separacion-de-flujos.md`** → commit.

**Commit de cierre:** `refactor(agent_intent): introducir resolutor de intencion en el dominio`

