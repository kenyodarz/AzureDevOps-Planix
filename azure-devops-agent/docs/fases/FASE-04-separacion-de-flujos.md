# FASE 04 — Separación de Flujos (Strategy)

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 03 (🟢 completada) · **Riesgo:** Medio
> **Commit al cerrar:** `refactor(agent_chat): separar flujos de conversacion con patron strategy`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La **Fase 03 quedó cerrada**:

- `AgentIntent`, `IntentResolution` e `IntentResolver` viven en `domain/model/agent`, con **100%**
  de cobertura y sin un solo mock en sus 33 pruebas.
- `executeChat()` ya no enruta: valida el texto, pide la intención al dominio y despacha con un
  `switch` exhaustivo sobre `AgentIntent`.
- `handleSpecialCommands()` y `buildPrompt()` eliminados.
- `AgentChatUseCase` bajó de 446 a **347 líneas**. **152 tests** en verde.

### 1.2 Problema que resuelve esta fase

**Deuda D-02.** `AgentChatUseCase` sigue conteniendo **seis métodos `run*Flow`** —General,
Auditoría, Refinamiento, Aprobación, División y Planificación— cada uno con su propio bloque
`onErrorResume` **literalmente idéntico**:

```java
.onErrorResume(error -> {
    log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
    return Mono.just(createErrorResponse(request, startTime, error.getMessage()));
});
```

Seis copias del mismo manejo de error y seis responsabilidades distintas en una sola clase:
violación directa de SRP (`spring-rules.md` §3-S). Cambiar el formato del mensaje de fallo obliga
hoy a tocar seis sitios.

**Consecuencia medible:** la clase no puede bajar de ~340 líneas mientras los seis flujos vivan
dentro de ella. El objetivo del plan maestro es **≤ 100 líneas**.

### 1.3 Estado esperado al terminar

1. Existe la interfaz `ChatFlowHandler` y el record `ChatFlowContext` en
   `domain/usecase/chat/handler`.
2. Existen **seis** handlers, uno por valor de `AgentIntent`, cada uno con su prueba unitaria.
3. Existe `ChatFlowDispatcher`, que indexa los handlers en un `EnumMap<AgentIntent, …>` y **falla
   al construirse** si falta alguna intención (fail-fast, no en tiempo de petición).
4. El manejo de error queda en **un único** `onErrorResume` dentro del caso de uso.
5. `AgentChatUseCase` queda en **≤ 120 líneas**: validar → resolver → despachar → envolver →
   manejar error.
6. Las 17 pruebas de caracterización del enrutamiento y las 7 del parseo siguen verdes **sin
   modificarse** (salvo el `setUp()`, si cambia el constructor).

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/ChatFlowHandler.java` | CREAR |
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/ChatFlowContext.java` | CREAR |
| `.../handler/GeneralFlowHandler.java` | CREAR |
| `.../handler/QualityAuditFlowHandler.java` | CREAR |
| `.../handler/RefinementFlowHandler.java` | CREAR |
| `.../handler/ApprovalFlowHandler.java` | CREAR |
| `.../handler/DivisionFlowHandler.java` | CREAR |
| `.../handler/PlanningDraftFlowHandler.java` | CREAR |
| `.../handler/ChatFlowDispatcher.java` | CREAR |
| `domain/usecase/src/test/java/.../handler/*Test.java` | CREAR (uno por handler + dispatcher) |
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/AgentChatUseCase.java` | MODIFICAR |
| `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java` | MODIFICAR |
| `.../AgentChatUseCaseCharacterizationTest.java` | MODIFICAR **solo** el `setUp()` |
| `.../AgentChatUseCaseParsingTest.java` | MODIFICAR **solo** el `setUp()` |
| `domain/model/.../agent/IntentResolver.java` | **NO TOCAR** |
| `infrastructure/entry-points/.../Handler.java` | **NO TOCAR** (Fase 06) |

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§3-S Single Responsibility:** «Cada clase debe tener una única razón para cambiar.» Un flujo,
  una clase.
- **§3-O Open/Closed:** añadir una intención debe consistir en crear un handler y registrarlo, sin
  editar el caso de uso.
- **§3-D Dependency Inversion:** los handlers dependen de puertos (`ChatGateway`,
  `PromptTemplatePort`, `PlanningVectorStorePort`), nunca de adaptadores.
- **§2 `domain/usecase`:** sin anotaciones de Spring; el wiring vive en `UseCasesConfig`.
- **§4:** complejidad cognitiva ≤ 15; sin números mágicos (S109); llaves en todo control de flujo.
- **§5:** cobertura ≥ 90% en las clases nuevas.

### 1.6 Decisiones pendientes que bloquean

| ID | Estado | Acción si sigue ABIERTA |
|---|---|---|
| — | — | **Ninguna bloquea esta fase.** DP-05 solo afecta a la Fase 07 |

> ⚠️ Antes de empezar, comprobar en `ESTADO.md` si el usuario **ratificó o revirtió** la regla del
> pronombre relativo introducida en la Fase 03. Si la revirtió, ajustar primero `IntentResolver` y
> su prueba, y **luego** ejecutar esta fase.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — Contrato del flujo

```java
package co.com.bancolombia.usecase.chat.handler;

public interface ChatFlowHandler {

    AgentIntent supports();

    Mono<String> handle(ChatFlowContext context);
}
```

Los handlers devuelven **el texto final para el usuario**, no un `SendMessageResponse`: construir
la respuesta A2A y medir el tiempo siguen siendo responsabilidad del caso de uso.

#### Paso B — Contexto del flujo

```java
public record ChatFlowContext(String userText, String contextId, IntentResolution resolution) {

    public String requireWorkItemId() { ... }   // mueve aquí workItemIdOf() del caso de uso
}
```

#### Paso C — Los seis handlers

Mover **tal cual** el cuerpo de cada `run*Flow`, quitándole el `.map(...)` de construcción de
respuesta y el `.onErrorResume(...)`. Cada handler recibe por constructor solo los puertos y
`String` de configuración que realmente usa:

| Handler | `supports()` | Dependencias |
|---|---|---|
| `GeneralFlowHandler` | `GENERAL` | `ChatGateway` |
| `QualityAuditFlowHandler` | `QUALITY_AUDIT` | `ChatGateway`, `PromptTemplatePort`, org, proyecto, guía, estándares |
| `RefinementFlowHandler` | `REFINEMENT` | `ChatGateway`, `PromptTemplatePort`, org, proyecto, guía |
| `ApprovalFlowHandler` | `APPROVAL` | `ChatGateway`, `PromptTemplatePort`, plantilla, guía |
| `DivisionFlowHandler` | `DIVISION` | `ChatGateway`, `PromptTemplatePort`, guía |
| `PlanningDraftFlowHandler` | `PLANNING_DRAFT` | `ChatGateway`, `PromptTemplatePort`, `PlanningVectorStorePort` |

`RAG_MAX_RESULTS`, `AUDIT_JSON_BLOCK` y las tres constantes `CONFIRM_*` se mudan al handler que las
usa. **Cada una debe quedar en un solo sitio.**

> El `onErrorResume` que degrada el fallo del vector store a lista vacía **se queda** en
> `PlanningDraftFlowHandler`: no es manejo genérico de error, es una regla de negocio del flujo.

#### Paso D — Dispatcher

```java
public final class ChatFlowDispatcher {

    private final Map<AgentIntent, ChatFlowHandler> handlers;

    public ChatFlowDispatcher(List<ChatFlowHandler> handlers) {
        // EnumMap; lanza IllegalStateException si falta alguna intención o hay duplicados
    }

    public Mono<String> dispatch(ChatFlowContext context) { ... }
}
```

La validación de exhaustividad se hace **en el constructor**, comparando contra
`AgentIntent.values()`.

#### Paso E — Adelgazar el caso de uso

```java
private Mono<SendMessageResponse> executeChat(SendMessageRequest request) {
    long startTime = System.currentTimeMillis();
    String contextId = extractContextId(request);
    String userText = ...;

    if (userText == null || userText.trim().isEmpty()) {
        return Mono.just(createSuccessResponse(request, NO_CONTENT, 0));
    }

    IntentResolution resolution = intentResolver.resolve(userText, contextId);
    return chatFlowDispatcher.dispatch(new ChatFlowContext(userText, contextId, resolution))
            .map(text -> createSuccessResponse(request, text, elapsed(startTime)))
            .onErrorResume(error -> {          // ← ÚNICO onErrorResume genérico (D-02)
                log.log(SEVERE, ERROR_EXECUTING_CHAT, error);
                return Mono.just(createErrorResponse(request, startTime, error.getMessage()));
            });
}
```

El constructor pasa de 11 argumentos a **4**: `chatGateway` deja de usarse aquí,
`agentResponseGateway`, `taskStoreGateway`, `intentResolver` y `chatFlowDispatcher`. La Fase 05
queda con menos trabajo del previsto: **verificarlo y anotarlo en el resultado**.

#### Paso F — Wiring

Registrar en `UseCasesConfig` los seis handlers y el dispatcher. Mantener `IntentResolver` como
`@Bean`. Los `String` de configuración se leen igual que hoy.

#### Paso G — Pruebas

- Una clase de prueba por handler: verifica el `PromptTemplateId` solicitado, las variables de la
  plantilla y el texto compuesto. Con Mockito, GIVEN/WHEN/THEN.
- `ChatFlowDispatcherTest`: despacho correcto por intención **y** fallo al construir si falta un
  handler o hay dos para la misma intención.
- Las 17 + 7 pruebas de caracterización: solo cambia el `setUp()`.

### 2.2 Qué NO hacer

- ❌ No tocar `IntentResolver` ni sus pruebas.
- ❌ No cambiar ninguna aserción de las pruebas de caracterización.
- ❌ No tocar `Handler.java` (Fase 06).
- ❌ No cambiar el contenido de las plantillas de prompt.
- ❌ No introducir Spring en `domain/usecase`.
- ❌ No convertir los handlers en `@Component`: se registran explícitamente en `UseCasesConfig`.

### 2.3 Criterios de aceptación

- [ ] `ChatFlowHandler`, `ChatFlowContext` y `ChatFlowDispatcher` creados.
- [ ] Seis handlers, uno por valor de `AgentIntent`.
- [ ] El dispatcher falla al construirse si falta una intención.
- [ ] **Un solo** `onErrorResume` genérico en todo el flujo de chat (D-02 saldada).
- [ ] `AgentChatUseCase` ≤ 120 líneas.
- [ ] Argumentos del constructor ≤ 5.
- [ ] Las 24 pruebas de caracterización verdes con el único cambio del `setUp()`.
- [ ] Cobertura de las clases nuevas ≥ 90%.
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL`.

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
| 1 | Leer `ESTADO.md`, `DECISIONES_PENDIENTES.md` y `RESULTADO-FASE-03.md` | Contexto al día; comprobar la ratificación pendiente de la Fase 03 |
| 2 | Crear `ChatFlowHandler` y `ChatFlowContext` (Pasos A y B) | Contrato del flujo |
| 3 | Crear los seis handlers moviendo el cuerpo de cada `run*Flow` (Paso C) | Un flujo, una clase |
| 4 | Escribir una prueba por handler (Paso G) | Verdes antes de tocar el caso de uso |
| 5 | Crear `ChatFlowDispatcher` con validación de exhaustividad (Paso D) | Fail-fast en el arranque |
| 6 | Adelgazar `AgentChatUseCase` y unificar el `onErrorResume` (Paso E) | ≤ 120 líneas, D-02 saldada |
| 7 | Registrar handlers y dispatcher en `UseCasesConfig` (Paso F) | El contexto de Spring arranca |
| 8 | Ejecutar `.\gradlew.bat :usecase:test` | 24 de caracterización verdes; **ninguna aserción modificada** |
| 9 | Ejecutar `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| 10 | Medir cobertura (`jacocoMergedReport --no-configuration-cache`) | Métricas reales |
| 11 | **Escribir `docs/resultados/RESULTADO-FASE-04.md`** con `_PLANTILLA_RESULTADO.md` | Trazabilidad |
| 12 | Actualizar `docs/plan/ESTADO.md`: 🟢 COMPLETADA + métricas + bitácora | Tablero al día |
| 13 | **Generar `docs/fases/FASE-05-value-objects-de-configuracion.md`** con `_PLANTILLA_FASE.md` | Checkpoint creado |
| 14 | Commit: `refactor(agent_chat): separar flujos de conversacion con patron strategy` | Cambios versionados |

> ⚠️ **Los pasos 11 y 13 son innegociables.** Sin ellos la fase se considera **incompleta**.
>
> 💡 **Control de sanidad:** si alguna prueba de caracterización se pone roja, el cuerpo de un flujo
> se movió mal. Corrige el handler, **nunca la prueba**: esta fase cambia *dónde* vive el código,
> no *qué* hace.

### 3.1 Contenido mínimo del MD de la Fase 05

**CONTEXTO**
- Venimos de la Fase 04: seis handlers independientes y un único manejo de error.
- Deuda a resolver: **D-03** — los `String` de configuración (`defaultOrg`, `defaultProject`,
  `templateMarkdown`, `agileGuideContent`, `qualityAuditContent`) viajan sueltos por constructores
  de varios handlers, sin tipo ni validación.
- Objetivo: agruparlos en Value Objects y **retirar el `// TODO Fase 05`** del código.

**INSTRUCCIONES**
1. Crear `AzureDevOpsScope` (organización + proyecto) con validación de no-vacío.
2. Crear `CorporateKnowledge` (plantilla HU/HA + guía de agilidad + estándares de auditoría).
3. Sustituir los `String` sueltos en handlers y caso de uso por esos Value Objects.
4. `UseCasesConfig` los construye al leer los recursos del classpath.
5. Simplificar el `setUp()` de las pruebas y valorar si `@InjectMocks` vuelve a ser viable (la
   excepción documentada en el Javadoc de `AgentChatUseCaseCharacterizationTest`).

**NO HACER:** no cambiar el contenido de los recursos; no tocar `Handler.java`.

**ORDEN DE EJECUCIÓN:** crear los Value Objects con sus pruebas → sustituir en handlers →
actualizar el wiring → simplificar los `setUp()` → build verde → `RESULTADO-FASE-05.md` →
**generar `FASE-06-separacion-del-entry-point.md`** → commit.

**Commit de cierre:** `refactor(agent_config): agrupar configuracion en objetos de valor`

