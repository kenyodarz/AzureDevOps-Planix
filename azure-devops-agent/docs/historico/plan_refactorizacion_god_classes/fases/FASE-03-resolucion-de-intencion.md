# FASE 03 — Resolución de Intención

> **Estado:** 🟢 COMPLETADA · **Depende de:** FASE 02 (🟢 completada) · **Riesgo:** Medio
> **Commit al cerrar:** `refactor(agent_intent): introducir resolutor de intencion en el dominio`
> **Resultado de la ejecución:** `docs/resultados/RESULTADO-FASE-03.md`

> ⚠️ **Esta es la primera fase que cambia el comportamiento visible para el usuario.**
> Aplica DP-01: peticiones que hoy se atienden mal pasarán a atenderse bien.

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La **Fase 02 quedó cerrada**:

- `ComplexityEstimation` y `UncertaintyLevel` viven en `domain/model/agent`, con
  `requiresSplit()` implementando el umbral `> 8` (DP-02).
- `EstimationSummary` unifica el post-procesado; la duplicación D-04 desapareció.
- `parseFrom` devuelve `Optional`: sin estimación se avisa explícitamente (DP-03).
- `AgentChatUseCase` bajó de 505 a **446 líneas**.
- **118 tests** en verde. Cobertura `domain/model` 0% → 60,3%.

### 1.2 Problema que resuelve esta fase

**Deuda D-01.** El enrutamiento vive en una cascada de `if` dentro de `executeChat()`:

```text
isGeneralFlow (contextId  O  containsGeneralKeyword)
  └─> regex de auditoría
       └─> marcador de refinamiento
            └─> shouldSearchPlanning() -> handleSpecialCommands()
                 └─> runPlanningSimilarityFlow()
```

**El primer `if` gana siempre.** Consecuencias reales, capturadas por dos pruebas de la Fase 00:

| Entrada del usuario | Flujo hoy | Flujo correcto |
|---|---|---|
| `"Genera un reporte de calidad de la historia (ID: 12345)"` | General | **Auditoría** |
| Idea larga que contiene la palabra `"consulta"` | General | **Planificación** |

**Deuda D-12.** Las listas de comandos, aunque ya son constantes tras la Fase 02, siguen repartidas
entre `shouldSearchPlanning()` y `handleSpecialCommands()`: la decisión de «qué es un comando» se
toma en dos sitios.

### 1.3 Estado esperado al terminar

1. Existe `AgentIntent`, `IntentResolution` e `IntentResolver` en `domain/model/agent`.
2. `IntentResolver` es un **servicio de dominio puro**: sin gateways, sin Spring, testeable sin un
   solo mock.
3. `executeChat()` se reduce a **resolver la intención y despachar**. Sin cascada de `if`.
4. Las dos pruebas de DP-01 tienen sus aserciones **invertidas** y renombradas.
5. `AgentChatUseCase` baja de 446 a **~380 líneas**.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `domain/model/src/main/java/co/com/bancolombia/model/agent/AgentIntent.java` | CREAR |
| `domain/model/src/main/java/co/com/bancolombia/model/agent/IntentResolution.java` | CREAR |
| `domain/model/src/main/java/co/com/bancolombia/model/agent/IntentResolver.java` | CREAR |
| `domain/model/src/test/java/co/com/bancolombia/model/agent/IntentResolverTest.java` | CREAR |
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/AgentChatUseCase.java` | MODIFICAR |
| `domain/usecase/src/test/.../AgentChatUseCaseCharacterizationTest.java` | MODIFICAR (**solo** las 2 pruebas de DP-01) |
| `domain/usecase/src/test/.../AgentChatUseCaseParsingTest.java` | **NO TOCAR** |

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§1 `domain/model` — Domain Services:** «Lógica de dominio cross-entidad que no encaja en una
  sola clase de entidad.» `IntentResolver` encaja exactamente ahí.
- **§3-O Open/Closed:** «Usar polimorfismo o patrones de estrategia para soportar nuevos
  comportamientos sin modificar el orquestador principal.» Añadir una intención debe requerir
  tocar el resolutor, no el caso de uso.
- **§4 Cognitive Complexity ≤ 15:** `executeChat()` la supera hoy. Debe quedar muy por debajo.
- **§4 S109:** sin números mágicos.
- **§5:** cobertura de `domain/model` ≥ 90% para las clases nuevas.

### 1.6 Decisiones pendientes

| ID | Estado | Acción |
|---|---|---|
| — | — | **Ninguna bloquea esta fase.** DP-01 está resuelta (corregir). |

**Decisión que se aplica aquí:**

**DP-01 — Tabla de precedencia obligatoria** (de más específica a más genérica):

| Prioridad | Intención | Criterio de detección |
|---:|---|---|
| 1 | `QUALITY_AUDIT` | verbo de auditoría (`audita`/`evalúa`/`revisa`/`calidad`) **+** `(ID: n)` |
| 2 | `REFINEMENT` | `analicemos y refinemos` **+** `(ID: n)` |
| 3 | `APPROVAL` / `DIVISION` | comando corto exacto tras normalización |
| 4 | `GENERAL` | `contextId` empieza por `general`/`dashboard`, **o** palabra clave genérica |
| 5 | `PLANNING_DRAFT` | texto libre de más de 25 caracteres |

**Reglas derivadas obligatorias:**

1. Un `(ID: n)` explícito junto a un verbo de intención **siempre** gana sobre las palabras clave
   genéricas. `(ID: n)` es una señal fuerte; `"reporte"` es una señal débil.
2. Las palabras clave genéricas se evalúan como **palabra completa** (límite `\b`), no con
   `contains()`. Así `"consultar"` dentro de una idea larga deja de secuestrar la planificación.
3. El `contextId` (`general*`/`dashboard*`) conserva su fuerza: es una señal explícita del cliente,
   no una inferencia sobre texto libre.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — `AgentIntent` e `IntentResolution`

```java
package co.com.bancolombia.model.agent;

public enum AgentIntent {
    QUALITY_AUDIT, REFINEMENT, APPROVAL, DIVISION, GENERAL, PLANNING_DRAFT
}
```

```java
public record IntentResolution(AgentIntent intent, Map<String, String> params) {

    public static final String PARAM_WORK_ITEM_ID = "workItemId";

    public IntentResolution {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public static IntentResolution of(AgentIntent intent) { ... }
    public static IntentResolution withWorkItem(AgentIntent intent, String workItemId) { ... }

    public Optional<String> workItemId() { ... }
}
```

#### Paso B — `IntentResolver` (servicio de dominio puro)

```java
public final class IntentResolver {

    public IntentResolution resolve(String userText, String contextId) { ... }
}
```

Implementa la tabla de precedencia **en ese orden exacto**, con *early returns* para mantener la
complejidad cognitiva baja. Un método privado por prioridad:

```java
private Optional<IntentResolution> asQualityAudit(String userText) { ... }
private Optional<IntentResolution> asRefinement(String userText) { ... }
private Optional<IntentResolution> asShortCommand(String normalized) { ... }
private boolean isGeneral(String userText, String contextId) { ... }
```

**Detalle crítico — palabras clave como palabra completa:**

```java
private static final Pattern GENERAL_KEYWORDS_PATTERN = Pattern.compile(
        "\\b(lista|listar|busca|buscar|consulta|consultar|reporte|reportes)\\b",
        Pattern.CASE_INSENSITIVE);
```

> ⚠️ Verifica el efecto sobre la prueba existente `givenTextWithKeywordLista_...`, cuya entrada es
> `"Dame la lista de historias del sprint actual"`. La palabra `lista` aparece completa, así que la
> prueba **debe seguir en verde sin modificarse**. Si se pone roja, el patrón está mal.

Traslada al resolutor las constantes que hoy viven en `AgentChatUseCase`:
`SHORT_COMMANDS`, `APPROVAL_COMMANDS`, `DIVISION_COMMANDS`, `GENERAL_KEYWORDS`,
`MIN_PLANNING_TEXT_LENGTH`, `REFINEMENT_MARKER`, `AUDIT_REQUEST_PATTERN`, `WORK_ITEM_ID_PATTERN`,
y la normalización de texto. **Deben quedar en un único lugar** (D-12).

#### Paso C — Simplificar `executeChat()`

```java
private Mono<SendMessageResponse> executeChat(SendMessageRequest request) {
    // ... validación de texto vacío (sin cambios)

    IntentResolution resolution = intentResolver.resolve(userText, contextId);
    return switch (resolution.intent()) {
        case QUALITY_AUDIT -> runQualityAuditFlow(request, resolution.workItemId().orElseThrow(), contextId, startTime);
        case REFINEMENT    -> runRefinementFlow(request, resolution.workItemId().orElseThrow(), contextId, startTime);
        case APPROVAL      -> runApprovalFlow(request, contextId, startTime);
        case DIVISION      -> runDivisionFlow(request, contextId, startTime);
        case GENERAL       -> runGeneralFlow(request, userText, contextId, startTime);
        case PLANNING_DRAFT -> runPlanningSimilarityFlow(request, userText, contextId, startTime);
    };
}
```

`handleSpecialCommands()` desaparece: sus dos ramas se convierten en `runApprovalFlow()` y
`runDivisionFlow()`. Su rama `else` deja de existir, porque el resolutor nunca devuelve una
intención sin flujo asociado (los textos cortos que no son comando caen en `GENERAL`).

> **Comprobar:** con la nueva tabla, un texto de menos de 25 caracteres que no sea comando ni
> contenga palabra clave debe resolverse como `GENERAL`, replicando el comportamiento de la antigua
> rama `else`. Añade una prueba que lo verifique.

#### Paso D — Inyectar el resolutor

`IntentResolver` no tiene estado ni dependencias. Añádelo como campo `final` del caso de uso y
regístralo como `@Bean` en `UseCasesConfig`. El constructor sube a 11 argumentos; se corrige en la
**Fase 05**, que ya está prevista.

#### Paso E — Invertir las dos pruebas de DP-01

En `AgentChatUseCaseCharacterizationTest`, **solo** estas dos:

| Prueba actual | Cambio |
|---|---|
| `givenAuditRequestContainingKeywordReporte_..._thenGeneralFlowWinsByPrecedence` | Renombrar a `..._thenExecutesQualityAuditFlow`. Ahora debe resolver `QUALITY_AUDIT` con `workItemId = 12345`. Actualizar Javadoc: DP-01 aplicada |
| `givenLongIdeaContainingKeywordConsulta_..._thenGeneralFlowWinsByPrecedence` | Renombrar a `..._thenExecutesPlanningSimilarityFlow`. Ahora debe resolver `PLANNING_DRAFT` y consultar el vector store. Actualizar Javadoc: DP-01 aplicada |

**Las otras 14 pruebas no se tocan.** Si alguna se pone roja, el resolutor está mal: corrige el
resolutor, no la prueba.

#### Paso F — Pruebas de tabla del resolutor

`IntentResolverTest`, sin mocks. Cobertura ≥ 90%. Casos mínimos:

| # | Entrada | `contextId` | Intención esperada |
|---:|---|---|---|
| 1 | `"Audita la historia (ID: 123)"` | `null` | `QUALITY_AUDIT` |
| 2 | `"Genera un reporte de calidad de la historia (ID: 123)"` | `null` | `QUALITY_AUDIT` ← **DP-01** |
| 3 | `"Evalúa la calidad (ID: 7)"` | `null` | `QUALITY_AUDIT` |
| 4 | `"Analicemos y refinemos la historia (ID: 99)"` | `null` | `REFINEMENT` |
| 5 | `"Aprobado"` / `"Sí"` / `"De acuerdo"` | `null` | `APPROVAL` |
| 6 | `"Dividir"` / `"Divídela"` | `null` | `DIVISION` |
| 7 | `"Dame la lista de historias"` | `null` | `GENERAL` |
| 8 | cualquier texto | `"general-1"` | `GENERAL` |
| 9 | cualquier texto | `"dashboard-9"` | `GENERAL` |
| 10 | idea larga con la palabra `"consulta"` | `null` | `PLANNING_DRAFT` ← **DP-01** |
| 11 | idea larga con `"consultar"` (derivada) | `null` | `PLANNING_DRAFT` |
| 12 | idea libre de más de 25 caracteres | `null` | `PLANNING_DRAFT` |
| 13 | texto corto que no es comando | `null` | `GENERAL` |
| 14 | `"audita esto"` (sin `(ID: n)`) | `null` | `GENERAL` o `PLANNING_DRAFT` según longitud — **documentar el resultado elegido** |
| 15 | texto exactamente de 25 caracteres | `null` | frontera de `MIN_PLANNING_TEXT_LENGTH` |

Añade además pruebas de que `workItemId` se extrae correctamente en los casos 1-4.

### 2.2 Qué NO hacer

- ❌ **No separar los flujos en handlers.** Eso es la Fase 04. Aquí solo cambia **cómo se decide**
  qué flujo se ejecuta, no cómo se ejecuta.
- ❌ No tocar `AgentChatUseCaseParsingTest`.
- ❌ No modificar ninguna prueba de caracterización salvo las dos de DP-01.
- ❌ No tocar `Handler.java` (Fase 06).
- ❌ No cambiar las plantillas de prompt.
- ❌ No introducir Spring en `domain/model`.

### 2.3 Criterios de aceptación

- [ ] `AgentIntent`, `IntentResolution` e `IntentResolver` creados en `domain/model/agent`.
- [ ] `IntentResolver` no depende de ningún gateway y se prueba sin mocks.
- [ ] `executeChat()` no contiene ninguna cascada de `if` de enrutamiento.
- [ ] `handleSpecialCommands()` eliminado.
- [ ] Las palabras clave genéricas se evalúan con límite de palabra `\b`.
- [ ] Las 2 pruebas de DP-01 invertidas, renombradas y con Javadoc actualizado.
- [ ] Las otras 14 pruebas de caracterización **sin modificar** y en verde.
- [ ] `IntentResolverTest` cubre los 15 casos de la tabla.
- [ ] Cobertura de las clases nuevas ≥ 90%.
- [ ] `AgentChatUseCase` ≤ 390 líneas.
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL`.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] Sin secretos hardcodeados (S2068)
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15 en `executeChat()` y en `resolve()`
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109)
- [ ] Tests GIVEN/WHEN/THEN
- [ ] `.\gradlew.bat build` verde

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `ESTADO.md`, `DECISIONES_PENDIENTES.md` y `RESULTADO-FASE-02.md` | Contexto al día; DP-01 confirmada como resuelta |
| 2 | Crear `AgentIntent` e `IntentResolution` (Paso A) | Contratos en el dominio |
| 3 | Crear `IntentResolver` con la tabla de precedencia (Paso B) | Servicio de dominio puro |
| 4 | Escribir `IntentResolverTest` con los 15 casos (Paso F) | Todos en verde **antes** de tocar el caso de uso |
| 5 | Simplificar `executeChat()` y eliminar `handleSpecialCommands()` (Paso C) | Sin cascada de `if` |
| 6 | Registrar el bean en `UseCasesConfig` (Paso D) | El contexto de Spring arranca |
| 7 | Ejecutar `.\gradlew.bat :usecase:test` | 14 pruebas verdes; **2 rojas, las de DP-01** |
| 8 | Invertir y renombrar esas 2 pruebas (Paso E) | 16 pruebas en verde |
| 9 | Ejecutar `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| 10 | Medir cobertura (`jacocoMergedReport --no-configuration-cache`) | Métricas reales |
| 11 | **Escribir `docs/resultados/RESULTADO-FASE-03.md`** con `_PLANTILLA_RESULTADO.md` | Trazabilidad |
| 12 | Actualizar `docs/plan/ESTADO.md`: 🟢 COMPLETADA + métricas + bitácora | Tablero al día |
| 13 | **Generar `docs/fases/FASE-04-separacion-de-flujos.md`** con `_PLANTILLA_FASE.md` | Checkpoint creado |
| 14 | Commit: `refactor(agent_intent): introducir resolutor de intencion en el dominio` | Cambios versionados |

> ⚠️ **Los pasos 11 y 13 son innegociables.** Sin ellos la fase se considera **incompleta**.
>
> 💡 **El paso 7 es un control de sanidad:** si esas 2 pruebas **no** se ponen rojas, DP-01 no se
> aplicó y el refactor no cambió nada. Si se pone roja alguna de las otras 14, hay una regresión:
> corrige el resolutor, nunca la prueba.

### 3.1 Contenido mínimo del MD de la Fase 04

**CONTEXTO**
- Venimos de la Fase 03: la intención se resuelve en el dominio y `executeChat()` solo despacha.
- Deuda a resolver: **D-02** — seis bloques `onErrorResume` con cuerpo idéntico, y seis métodos
  `run*Flow` dentro de la misma clase (violación de SRP).
- Objetivo: `AgentChatUseCase` ≤ 100 líneas.

**INSTRUCCIONES**
1. Crear la interfaz `ChatFlowHandler` en `domain/usecase/chat/handler`:
   `AgentIntent supports()` + `Mono<String> handle(ChatFlowContext context)`.
2. Crear el record `ChatFlowContext` con `userText`, `contextId`, `params` y lo que cada flujo
   necesite.
3. Crear seis handlers, uno por intención, moviendo el cuerpo de cada `run*Flow`.
4. Crear `ChatFlowDispatcher` que indexe los handlers por `AgentIntent` en un `EnumMap` y falle
   rápido si falta alguno.
5. **Centralizar el manejo de errores**: los handlers devuelven `Mono<String>`; el caso de uso
   aplica un único `onErrorResume` que construye la respuesta de fallo. Los 6 bloques duplicados
   se reducen a 1 (D-02).
6. `AgentChatUseCase` queda como: validar → resolver intención → despachar → envolver en
   `SendMessageResponse` → manejar error.
7. Registrar los handlers y el dispatcher en `UseCasesConfig`.
8. Un test unitario por handler; los 16 de caracterización deben seguir verdes **sin modificarse**.

**NO HACER:** no cambiar el resolutor de intención; no tocar `Handler.java`.

**ORDEN DE EJECUCIÓN:** crear contrato y contexto → crear los 6 handlers con sus tests → crear el
dispatcher → adelgazar el caso de uso → unificar el manejo de errores → wiring → build verde →
`RESULTADO-FASE-04.md` → **generar `FASE-05-value-objects-de-configuracion.md`** → commit.

**Commit de cierre:** `refactor(agent_chat): separar flujos de conversacion con patron strategy`

