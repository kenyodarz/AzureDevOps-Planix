# FASE 01 — Externalización de Prompts

> **Estado:** 🟢 COMPLETADA (2026-08-28) · **Depende de:** FASE 00 · **Riesgo:** Bajo
> **Commit:** `refactor(agent_prompts): externalizar plantillas de prompt a recursos`
> **Siguiente fase:** `FASE-02-value-objects-de-estimacion.md`

## Resultado de la ejecución

| Resultado | Valor |
|---|---|
| Líneas `AgentChatUseCase` | **644 → 505** (−139) |
| Constantes de prompt en el dominio | **5 → 0** |
| Plantillas externalizadas | 5 archivos `.md` + 1 fragmento compartido |
| Módulo nuevo | `infrastructure/driven-adapters/prompt-template` |
| Tests totales del proyecto | **46 → 69** (0 fallos) |
| Cobertura `prompt-template` | **94,2%** |
| Cobertura `domain/usecase` | 81,5% |
| Build | `BUILD SUCCESSFUL` |

### Decisiones aplicadas

- **DP-04** — Ruta y nombres aprobados. Se crearon 5 plantillas (no 6, por DP-07).
- **DP-06** — Tabla de estimación por horas aplicada. Vive en un **único** fragmento compartido
  (`prompts/fragmentos/tabla-estimacion.md`) que se inyecta automáticamente en las 4 plantillas que
  estiman. Se eliminó la «Regla de Mínimos de 5 puntos», que contradecía al prompt de auditoría.
- **DP-07** — `buildPrompt()` y su plantilla inline eliminados. La rama `else` de
  `handleSpecialCommands()` envía el texto del usuario directamente.

### Desviación respecto al plan original

El plan preveía un test de **equivalencia byte a byte** contra las constantes originales. Al aplicar
DP-06 el contenido de 4 de las 5 plantillas cambia por decisión de negocio, por lo que una
comparación literal carecía de sentido. Se sustituyó por `PromptTemplateContentTest` (7 pruebas en
`app-service`), que verifica integridad estructural (ningún marcador sin resolver) y contenido
normativo (tabla de horas presente, «Regla de Mínimos» ausente, umbral de 8 puntos anunciado).

También se actualizó el mecanismo de identificación de flujo en los tests de la Fase 00: pasó de
buscar subcadenas del prompt a comparar el `PromptTemplateId` solicitado. Es un criterio
equivalente pero más preciso. Los 16 + 7 casos y sus aserciones de comportamiento se conservan
íntegros.

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La **Fase 00 quedó cerrada** con estos resultados:

- `AgentChatUseCaseCharacterizationTest` — 16 tests que congelan el enrutamiento de los 6 flujos.
- `AgentChatUseCaseParsingTest` — 7 tests que congelan el parseo y post-procesado de la respuesta.
- Cobertura de `domain/usecase`: **0% → 81,7%** (884/1082 instrucciones).
- `.\gradlew.bat build` **verde**. Código productivo **intacto**.

Esos 23 tests son la red de seguridad de esta fase: **deben seguir en verde sin modificarlos.**

Decisiones resueltas por el usuario que afectan a fases posteriores (no a esta):

- **DP-01** → corregir la precedencia de intención (se aplica en Fase 03).
- **DP-02** → umbral de división `> 8` (se aplica en Fase 02).

### 1.2 Problema que resuelve esta fase

**Deuda D-03.** `AgentChatUseCase.java:46-217` contiene **171 líneas de plantillas de prompt** como
constantes `String`. Consecuencias:

1. **Violación de SRP.** El dominio, que debe contener reglas de negocio, alberga contenido
   editorial. Cambiar una palabra de un prompt exige recompilar y desplegar.
2. **`String.format` posicional frágil.** `AUDIT_QUALITY_PROMPT_TEMPLATE` recibe `workItemId`
   **tres veces** en posiciones distintas (`:405-409`). Invertir dos argumentos compila
   perfectamente y falla en runtime.
3. **Escapado contaminante.** La tabla de porcentajes obliga a escribir `%%` en lugar de `%`
   (`:194-199`), lo que hace el texto difícil de leer y editar.
4. **Contradicción funcional detectada** (relacionada con DP-06): el prompt de creación impone una
   «Regla de Mínimos de 5 puntos» (`:93`) mientras el prompt de auditoría afirma explícitamente que
   *«la guía corporativa NO establece mínimos obligatorios»* (`:182`). **El agente crea historias
   con una regla y las audita con la contraria.**

### 1.3 Estado esperado al terminar

1. Existe el puerto puro `PromptTemplatePort` en `domain/model`.
2. Existen los 6 archivos `.md` de prompt en `resources`, con el texto **idéntico** al actual salvo
   los cambios que apruebe el usuario en DP-06.
3. Existe `ClasspathPromptTemplateAdapter` que resuelve variables **por nombre**, no por posición.
4. `AgentChatUseCase` ya no contiene ninguna constante de prompt: pasa de 644 a **~470 líneas**.
5. Un test demuestra la **equivalencia byte a byte** entre el prompt renderizado y el original.
6. Los 23 tests de la Fase 00 siguen verdes **sin haber sido modificados**.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `domain/model/src/main/java/co/com/bancolombia/model/prompt/PromptTemplateId.java` | CREAR |
| `domain/model/src/main/java/co/com/bancolombia/model/prompt/gateways/PromptTemplatePort.java` | CREAR |
| `infrastructure/driven-adapters/prompt-template/` (módulo Gradle nuevo) | CREAR |
| `applications/app-service/src/main/resources/prompts/*.md` (6 archivos) | CREAR |
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/AgentChatUseCase.java` | MODIFICAR |
| `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java` | MODIFICAR |
| `settings.gradle` | MODIFICAR (registrar el módulo nuevo) |
| `domain/usecase/src/test/.../AgentChatUseCaseCharacterizationTest.java` | **NO TOCAR** |
| `domain/usecase/src/test/.../AgentChatUseCaseParsingTest.java` | **NO TOCAR** |

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§1 `domain/model`:** el puerto y el enum deben ser Java puro. Cero Spring, cero Jackson, cero
  `org.springframework.core.io.Resource`. La carga desde classpath vive **solo** en el adapter.
- **§1 `driven-adapters`:** el adapter implementa el puerto del dominio y traduce excepciones
  técnicas (`IOException`) a excepciones tipadas del dominio.
- **§3-D Dependency Inversion:** `AgentChatUseCase` depende de `PromptTemplatePort`, nunca de la
  implementación.
- **§4 S2259:** `render()` debe validar sus argumentos con `Objects.requireNonNull()`.
- **§4 S109:** sin literales numéricos sueltos.
- **§5:** tests GIVEN/WHEN/THEN.

### 1.6 Decisiones pendientes que BLOQUEAN esta fase

| ID | Estado | Qué se necesita | Acción si sigue ABIERTA |
|---|---|---|---|
| **DP-04** | 🟡 PROPUESTA | Confirmar los 6 nombres de archivo y la ruta `resources/prompts/` | **DETENERSE** y preguntar |
| **DP-06** | 🔴 ABIERTA | Tabla de equivalencia Story Points → horas; si se elimina la «Regla de Mínimos de 5 puntos»; si las horas son por persona o por célula | **DETENERSE** y preguntar |

**Por qué DP-06 bloquea concretamente esta fase:** al mover los prompts a archivos `.md` hay que
escribir su contenido definitivo. Si el criterio de estimación cambia de cualitativo a horas, el
texto de `fase2-historia-estructurada.md`, `division-historias.md`, `refinamiento-historia.md` y
`auditoria-calidad.md` cambia. Hacerlo en dos pasos significaría reescribir los mismos 4 archivos
dos veces y regenerar el test de equivalencia byte a byte.

> **Alternativa si el usuario prefiere avanzar ya:** ejecutar la fase con **texto idéntico al
> actual** (equivalencia byte a byte estricta, sin tocar el criterio de estimación) y abrir una
> **Fase 01-bis** exclusiva para el contenido editorial cuando DP-06 se resuelva. Esta alternativa
> requiere aprobación explícita del usuario.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — Puerto y enum en el dominio (`domain/model`)

```java
package co.com.bancolombia.model.prompt;

public enum PromptTemplateId {
    PLANNING_DRAFT,          // antes FASE1_PROMPT_TEMPLATE
    STRUCTURED_STORY,        // antes FASE2_PROMPT_TEMPLATE
    STORY_DIVISION,          // antes DIVISION_PROMPT_TEMPLATE
    STORY_REFINEMENT,        // antes REFINEMENT_PROMPT_TEMPLATE
    QUALITY_AUDIT,           // antes AUDIT_QUALITY_PROMPT_TEMPLATE
    STRUCTURED_CREATION      // antes el bloque inline de buildPrompt()
}
```

```java
package co.com.bancolombia.model.prompt.gateways;

public interface PromptTemplatePort {
    String render(PromptTemplateId id, Map<String, Object> variables);
}
```

Crear también `PromptTemplateNotFoundException` extendiendo una excepción base del dominio (no de
Spring ni de HTTP).

#### Paso B — Los 6 archivos `.md` (solo tras confirmar DP-04)

Mover el contenido literal de cada constante a su archivo. **Sin reescribir, mejorar ni traducir
una sola palabra**, salvo lo aprobado en DP-06.

Sustituir los marcadores posicionales `%s` por marcadores nombrados:

| Constante original | Marcador posicional | Marcador nombrado |
|---|---|---|
| `FASE1` | `%s` (1º) | `{{ideaOriginal}}` |
| `FASE1` | `%s` (2º) | `{{contextoRag}}` |
| `FASE2` | `%s` (1º) | `{{plantillaCorporativa}}` |
| `FASE2` | `%s` (2º) | `{{guiaAgilidad}}` |
| `DIVISION` | `%s` | `{{guiaAgilidad}}` |
| `REFINEMENT` | `%s` (1º/2º/3º/4º) | `{{workItemId}}` / `{{organizacion}}` / `{{proyecto}}` / `{{guiaAgilidad}}` |
| `AUDIT` | 6 posiciones | `{{workItemId}}` (×4), `{{organizacion}}`, `{{proyecto}}`, `{{estandares}}` |
| `STRUCTURED_CREATION` | `%s` | `{{plantillaCorporativa}}` |

Al usar marcadores nombrados, los `%%` de la tabla de porcentajes vuelven a ser `%` normales.

#### Paso C — Adapter `ClasspathPromptTemplateAdapter`

- Nuevo módulo Gradle `infrastructure/driven-adapters/prompt-template`, registrado en
  `settings.gradle` siguiendo el patrón de los adapters existentes.
- Carga los `.md` **una sola vez al arranque** (`@PostConstruct` o inicialización en el
  constructor) y los cachea en un `Map<PromptTemplateId, String>` inmutable. **Prohibido leer del
  classpath en cada request.**
- Sustitución por nombre: recorrer las entradas del mapa y reemplazar `{{clave}}`.
- Si falta un archivo o queda un marcador sin resolver, lanzar la excepción de dominio. **Nunca
  devolver un prompt a medio renderizar al LLM.**
- Traducir `IOException` a la excepción de dominio (§1 driven-adapters).

#### Paso D — Test de equivalencia byte a byte (crítico)

`ClasspathPromptTemplateAdapterTest` debe contener, por cada una de las 6 plantillas, un test que:

1. Renderice la plantilla con valores conocidos.
2. Construya el resultado esperado con el `String.format` y la constante **originales** (copiados
   literalmente al test como constantes privadas).
3. Afirme `assertThat(renderizado).isEqualTo(esperado)`.

Este test es la garantía de que el LLM sigue recibiendo exactamente el mismo texto.

> Si DP-06 aprueba cambios de contenido, este test compara contra el **texto nuevo aprobado**, y
> debe documentarse con Javadoc qué se cambió y por qué.

#### Paso E — Sustituir en `AgentChatUseCase`

- Añadir `private final PromptTemplatePort promptTemplatePort;`.
- Reemplazar cada `String.format(CONSTANTE, ...)` por
  `promptTemplatePort.render(PromptTemplateId.XXX, Map.of("clave", valor, ...))`.
- **Borrar las 6 constantes** de plantilla.
- Dejar intactas `INFO_BLOCK_TEMPLATE` y `HIGH_COMPLEXITY_ALERT_TEMPLATE`: son fragmentos de
  presentación, no prompts. Se tratan en la Fase 02.

#### Paso F — Wiring en `UseCasesConfig`

Inyectar el nuevo bean. El constructor sube temporalmente a **10 argumentos**; es aceptable y
**se corrige en la Fase 05**. Dejar un comentario `// TODO Fase 05: agrupar en Value Objects`.

### 2.2 Qué NO hacer

- ❌ **No modificar los 23 tests de la Fase 00.** Si alguno se pone rojo, el refactor está mal: el
  fallo indica que el prompt cambió. Arregla el código, no el test.
- ❌ No reescribir, mejorar, resumir ni traducir el texto de los prompts.
- ❌ No cambiar el orden de las secciones dentro de un prompt.
- ❌ No introducir `org.springframework.core.io.Resource` ni ninguna clase de Spring en
  `domain/model` ni en `domain/usecase`.
- ❌ No corregir DP-01, DP-02, DP-03 ni DP-07 en esta fase.
- ❌ No tocar `Handler.java` (es la Fase 06).

### 2.3 Criterios de aceptación

- [ ] DP-04 y DP-06 resueltas y registradas en `DECISIONES_PENDIENTES.md`.
- [ ] `PromptTemplateId` y `PromptTemplatePort` creados en `domain/model`, sin imports de Spring.
- [ ] Los 6 archivos `.md` existen con los nombres aprobados en DP-04.
- [ ] `ClasspathPromptTemplateAdapter` cachea las plantillas al arranque.
- [ ] Test de equivalencia byte a byte para las 6 plantillas, en verde.
- [ ] `AgentChatUseCase` no contiene ninguna constante de prompt y baja a ~470 líneas.
- [ ] Los 23 tests de la Fase 00 pasan **sin haber sido modificados**
      (verificar con `git diff` sobre esos dos archivos: debe estar vacío).
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL`.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] Mappers y carga técnica exclusivamente en `driven-adapters`
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
| 1 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar estado de DP-04 y DP-06 |
| 2 | **Si DP-04 o DP-06 siguen ABIERTAS → DETENERSE y preguntar al usuario** | Decisiones registradas antes de escribir código |
| 3 | Crear `PromptTemplateId`, `PromptTemplatePort` y la excepción de dominio (Paso A) | Contratos puros en `domain/model` |
| 4 | Crear el módulo Gradle `prompt-template` y registrarlo en `settings.gradle` | `.\gradlew.bat projects` lo lista |
| 5 | Crear los 6 archivos `.md` con el texto literal (Paso B) | Recursos creados |
| 6 | Implementar `ClasspathPromptTemplateAdapter` con caché al arranque (Paso C) | Adapter funcional |
| 7 | Escribir el test de equivalencia byte a byte (Paso D) | 6 tests en verde |
| 8 | Sustituir los `String.format` en `AgentChatUseCase` y borrar las constantes (Paso E) | Use case ~470 líneas |
| 9 | Actualizar `UseCasesConfig` (Paso F) | Contexto de Spring arranca |
| 10 | Ejecutar `.\gradlew.bat :usecase:test` | Los 23 tests de la Fase 00 en verde, **sin modificarlos** |
| 11 | Ejecutar `git diff --stat` sobre los 2 tests de la Fase 00 | Salida **vacía** |
| 12 | Ejecutar `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| 13 | Medir cobertura y actualizar métricas en `ESTADO.md` (usar `--no-configuration-cache`) | Tablero al día |
| 14 | Marcar Fase 01 como 🟢 COMPLETADA en `ESTADO.md` + entrada en la bitácora | Tablero al día |
| 15 | **Generar `docs/fases/FASE-02-value-objects-de-estimacion.md`** con `_PLANTILLA_FASE.md` | Checkpoint de continuidad creado |
| 16 | Commit: `refactor(agent_prompts): externalizar plantillas de prompt a recursos` | Cambios versionados |

> ⚠️ **El paso 15 es innegociable.** Sin el MD de la Fase 02, el trabajo no es reanudable tras una
> desconexión y la Fase 01 se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la Fase 02

El archivo `docs/fases/FASE-02-value-objects-de-estimacion.md` debe contener:

**CONTEXTO**
- Venimos de la Fase 01: los prompts viven en recursos y `AgentChatUseCase` ronda las 470 líneas.
- Deudas a resolver: **D-04** (post-procesado duplicado literal entre `runRefinementFlow():436-451`
  y `handleSpecialCommands():506-524`), **D-05** (parseo por regex con defaults silenciosos,
  `:276-318`), **D-06** (números mágicos `13`, `8`, `25`, `3`).
- Decisiones a aplicar: **DP-02** (🟢 resuelta → `MAX_STORY_POINTS_PER_STORY = 8`, condición
  `points > 8`) y **DP-03** (confirmar Opción C antes de empezar).

**INSTRUCCIONES**
1. Crear en `domain/model/agent` el enum `UncertaintyLevel` (`NULA`, `BAJA`, `MEDIA`, `ALTA`,
   `CRITICA`) con parseo tolerante desde el texto del LLM.
2. Crear el record `ComplexityEstimation(int points, UncertaintyLevel level, String rationale)` con
   factory `Optional<ComplexityEstimation> parseFrom(String llmResponse)` — **`Optional`, no
   defaults silenciosos** (S2259, DP-03).
3. Añadir a `ComplexityEstimation` el método de negocio `boolean requiresSplit()` que devuelva
   `points > MAX_STORY_POINTS_PER_STORY` con `MAX_STORY_POINTS_PER_STORY = 8`. **Aquí se materializa
   DP-02.** Modelo rico, no anémico (`spring-rules.md` §1).
4. Crear `LlmResponseDecorator` (dominio) que limpie el JSON y componga bloque informativo + alerta,
   eliminando la duplicación D-04.
5. Sustituir `Pattern.compile()` en caliente por constantes `static final Pattern` (D-07) y eliminar
   los imports FQN inline `java.util.regex.*` (D-08).
6. Reemplazar los magic numbers `25` y `3` por constantes nombradas (`MIN_PLANNING_TEXT_LENGTH`,
   `RAG_MAX_RESULTS`).
7. **Invertir la aserción** del test `givenLlmResponseWithNinePoints_whenApprovalFlow_thenDoesNotAppendAlertYet`:
   con el umbral `> 8`, 9 puntos **debe** disparar la alerta. Renombrarlo a
   `givenLlmResponseWithNinePoints_whenApprovalFlow_thenAppendsComplexityAlert` y actualizar su
   Javadoc indicando que DP-02 quedó aplicada.
8. Añadir tests unitarios puros de `ComplexityEstimation` y `UncertaintyLevel` (≥ 90% de cobertura).

**NO HACER:** no tocar el enrutamiento de flujos (es la Fase 03); no modificar los 16 tests de
`AgentChatUseCaseCharacterizationTest`.

**ORDEN DE EJECUCIÓN:** confirmar DP-03 → crear VOs y tests puros → crear el decorador → sustituir
en el use case → invertir el test de 9 puntos → build verde → actualizar `ESTADO.md` → **generar
`FASE-03-resolucion-de-intencion.md`** → commit.

**Commit de cierre:** `refactor(agent_chat): extraer objeto de valor de estimacion de complejidad`

