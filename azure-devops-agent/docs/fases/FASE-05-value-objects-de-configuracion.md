# FASE 05 — Value Objects de Configuración

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 04 (🟢 completada) · **Riesgo:** Bajo
> **Commit al cerrar:** `refactor(agent_config): agrupar configuracion en objetos de valor`

> ℹ️ **Esta fase se replanteó al cerrar la Fase 04.** El objetivo original —bajar el constructor de
> `AgentChatUseCase` de 11 argumentos— **ya se cumplió** como efecto colateral de mover los flujos a
> los handlers: el constructor quedó en 4 y el `// TODO Fase 05` desapareció. Lo que queda por
> resolver es distinto y está descrito abajo.

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La **Fase 04 quedó cerrada**:

- Seis `ChatFlowHandler`, un `ChatFlowDispatcher` con validación de exhaustividad al construirse, y
  `A2AResponseFactory` para el protocolo A2A.
- **Un único** `onErrorResume` genérico (D-02 saldada).
- `AgentChatUseCase`: 347 → **89 líneas**, constructor de 4 argumentos.
- **186 tests** en verde. Cobertura `domain/usecase` 84,5% → **95,4%**; los 6 handlers al 100%.

### 1.2 Problema que resuelve esta fase

**Deuda D-03 (replanteada).** Los `String` de configuración ya no están en el caso de uso, pero
siguen viajando sueltos por los constructores de cinco handlers:

| Handler | `String` que recibe |
|---|---|
| `QualityAuditFlowHandler` | `defaultOrg`, `defaultProject`, `agileGuideContent`, `qualityAuditContent` |
| `RefinementFlowHandler` | `defaultOrg`, `defaultProject`, `agileGuideContent` |
| `ApprovalFlowHandler` | `templateMarkdown`, `agileGuideContent` |
| `DivisionFlowHandler` | `agileGuideContent` |

Problemas concretos:

1. **Sin tipo ni validación.** Nada impide construir `QualityAuditFlowHandler` con la organización y
   el proyecto **intercambiados**: ambos son `String` y el compilador calla. Es exactamente el tipo
   de defecto silencioso que motivó el criterio «anti dummies» de DP-01.
2. **I/O repetido en el arranque.** `UseCasesConfig` lee `HISTORIA_USUARIO.md` **cuatro veces**, una
   por handler que la necesita (hallazgo registrado en la Fase 04).
3. **Cambiar la configuración obliga a tocar N constructores** en lugar de uno.

### 1.3 Estado esperado al terminar

1. Existen `AzureDevOpsScope` y `CorporateKnowledge` en `domain/model/agent`, como *records*
   inmutables con validación en el constructor compacto.
2. Los cinco handlers reciben esos Value Objects en lugar de `String` sueltos.
3. `UseCasesConfig` lee **cada recurso una sola vez**.
4. Un `String` en la posición equivocada deja de compilar.
5. Los `setUp()` de las pruebas se simplifican; se documenta si `@InjectMocks` vuelve a ser viable.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `domain/model/src/main/java/co/com/bancolombia/model/agent/AzureDevOpsScope.java` | CREAR |
| `domain/model/src/main/java/co/com/bancolombia/model/agent/CorporateKnowledge.java` | CREAR |
| `domain/model/src/test/java/co/com/bancolombia/model/agent/AzureDevOpsScopeTest.java` | CREAR |
| `domain/model/src/test/java/co/com/bancolombia/model/agent/CorporateKnowledgeTest.java` | CREAR |
| `.../handler/QualityAuditFlowHandler.java` | MODIFICAR |
| `.../handler/RefinementFlowHandler.java` | MODIFICAR |
| `.../handler/ApprovalFlowHandler.java` | MODIFICAR |
| `.../handler/DivisionFlowHandler.java` | MODIFICAR |
| `applications/app-service/.../config/UseCasesConfig.java` | MODIFICAR |
| `.../handler/ChatFlowHandlerTest.java` | MODIFICAR (solo la construcción de los handlers) |
| `.../AgentChatUseCaseCharacterizationTest.java` | MODIFICAR **solo** `buildDispatcher()` |
| `.../AgentChatUseCaseParsingTest.java` | MODIFICAR **solo** el `setUp()` |
| `.../model/agent/IntentResolver.java` | **NO TOCAR** |
| `infrastructure/entry-points/.../Handler.java` | **NO TOCAR** (Fase 06) |

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§1 `domain/model`:** Value Objects inmutables, con validación propia. «Prohibido el modelo
  anémico»: si el objeto sabe validarse, que se valide él.
- **§1:** `domain/model` no puede depender de Spring ni de nada técnico.
- **§4 S107:** listas largas de parámetros del mismo tipo son un olor de diseño.
- **§5:** cobertura ≥ 90% en las clases nuevas.

### 1.6 Decisiones pendientes que bloquean

| ID | Estado | Acción si sigue ABIERTA |
|---|---|---|
| — | — | **Ninguna bloquea esta fase.** DP-05 solo afecta a la Fase 07 |

> ⚠️ Antes de empezar, comprobar en `ESTADO.md` si el usuario ratificó o revirtió la **regla del
> pronombre relativo** (Fase 03). Si la revirtió, ajustar primero `IntentResolver` y su prueba.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — `AzureDevOpsScope`

```java
package co.com.bancolombia.model.agent;

/** Dónde vive el Work Item: organización y proyecto de Azure DevOps. */
public record AzureDevOpsScope(String organization, String project) {

    public AzureDevOpsScope {
        // Ambos obligatorios y no en blanco; mensaje de error que nombre el campo concreto
    }
}
```

#### Paso B — `CorporateKnowledge`

```java
/** Documentación corporativa que alimenta los prompts. */
public record CorporateKnowledge(String storyTemplate, String agileGuide, String qualityStandards) {

    public CorporateKnowledge {
        // Los tres obligatorios y no en blanco
    }

    /** Guía de agilidad y estándares de auditoría concatenados, tal como los espera la plantilla. */
    public String auditStandards() {
        return agileGuide + "\n\n" + qualityStandards;
    }
}
```

> `auditStandards()` traslada al dominio la concatenación que hoy hace
> `QualityAuditFlowHandler` en línea. Es conocimiento de negocio, no de flujo.

#### Paso C — Sustituir en los handlers

| Handler | Antes | Después |
|---|---|---|
| `QualityAuditFlowHandler` | 4 `String` | `AzureDevOpsScope` + `CorporateKnowledge` |
| `RefinementFlowHandler` | 3 `String` | `AzureDevOpsScope` + `CorporateKnowledge` |
| `ApprovalFlowHandler` | 2 `String` | `CorporateKnowledge` |
| `DivisionFlowHandler` | 1 `String` | `CorporateKnowledge` |

`GeneralFlowHandler` y `PlanningDraftFlowHandler` **no cambian**: no reciben configuración.

#### Paso D — `UseCasesConfig`

Crear **dos beans**, `AzureDevOpsScope` y `CorporateKnowledge`, leyendo cada recurso del classpath
**una sola vez**. Los handlers los reciben ya construidos. El método `readResource(...)` se invoca
tres veces en total, no seis.

#### Paso E — Pruebas

- `AzureDevOpsScopeTest` y `CorporateKnowledgeTest`: construcción válida, rechazo de nulos y
  blancos, y `auditStandards()`.
- Actualizar la construcción de los handlers en `ChatFlowHandlerTest` y los `setUp()` de las dos
  pruebas de caracterización. **Ninguna aserción cambia.**
- Anotar en el Javadoc de `AgentChatUseCaseCharacterizationTest` si `@InjectMocks` vuelve a ser
  viable (la excepción documentada desde la Fase 00) o por qué sigue sin serlo.

### 2.2 Qué NO hacer

- ❌ No tocar `IntentResolver`, `ChatFlowDispatcher` ni `A2AResponseFactory`.
- ❌ No cambiar ninguna aserción de las pruebas de caracterización.
- ❌ No tocar `Handler.java` (Fase 06).
- ❌ No cambiar el contenido de los recursos `.md` ni de las plantillas de prompt.
- ❌ No introducir Spring en `domain/model`.

### 2.3 Criterios de aceptación

- [ ] `AzureDevOpsScope` y `CorporateKnowledge` creados en `domain/model/agent`, con validación.
- [ ] Ningún handler recibe ya `String` de configuración sueltos.
- [ ] `UseCasesConfig` lee cada recurso del classpath **una sola vez**.
- [ ] `auditStandards()` vive en el dominio, no en el handler.
- [ ] Las 24 pruebas de caracterización verdes, sin cambiar aserciones.
- [ ] Cobertura de las clases nuevas ≥ 90%.
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL`.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] Value Objects inmutables con validación en el constructor compacto
- [ ] Sin secretos hardcodeados (S2068)
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109)
- [ ] Tests GIVEN/WHEN/THEN
- [ ] `.\gradlew.bat build` verde

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `ESTADO.md`, `DECISIONES_PENDIENTES.md` y `RESULTADO-FASE-04.md` | Contexto al día; comprobar la ratificación pendiente de la Fase 03 |
| 2 | Crear `AzureDevOpsScope` y `CorporateKnowledge` (Pasos A y B) | Value Objects con validación |
| 3 | Escribir sus pruebas (Paso E, primera parte) | Verdes antes de tocar los handlers |
| 4 | Sustituir los `String` en los cuatro handlers (Paso C) | Compilador como red de seguridad |
| 5 | Ajustar `UseCasesConfig` para leer cada recurso una vez (Paso D) | Sin I/O repetido en el arranque |
| 6 | Ajustar la construcción de handlers en las pruebas (Paso E, segunda parte) | Sin cambiar aserciones |
| 7 | Ejecutar `.\gradlew.bat :usecase:test` y `:model:test` | Todo verde |
| 8 | Ejecutar `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| 9 | Medir cobertura (`jacocoMergedReport --no-configuration-cache`) | Métricas reales |
| 10 | **Escribir `docs/resultados/RESULTADO-FASE-05.md`** con `_PLANTILLA_RESULTADO.md` | Trazabilidad |
| 11 | Actualizar `docs/plan/ESTADO.md`: 🟢 COMPLETADA + métricas + bitácora | Tablero al día |
| 12 | **Generar `docs/fases/FASE-06-separacion-del-entry-point.md`** con `_PLANTILLA_FASE.md` | Checkpoint creado |
| 13 | Commit: `refactor(agent_config): agrupar configuracion en objetos de valor` | Cambios versionados |

> ⚠️ **Los pasos 10 y 12 son innegociables.** Sin ellos la fase se considera **incompleta**.
>
> 💡 **Control de sanidad:** esta fase **no debe cambiar ningún comportamiento**. Si una prueba de
> caracterización se pone roja, hay un `String` intercambiado. Corrige el wiring, nunca la prueba.

### 3.1 Contenido mínimo del MD de la Fase 06

**CONTEXTO**
- Venimos de la Fase 05: la configuración viaja tipada y validada.
- Deuda a resolver: **D-09/D-10** — `Handler.java` (495 líneas) es la **única** clase de más de 300
  líneas que queda. Mezcla enrutamiento HTTP, validación del contrato JSON-RPC, mapeo de errores y
  orquestación de la respuesta.
- Objetivo: `Handler` ≤ 150 líneas y **cero** clases de más de 300 líneas en el proyecto.

**INSTRUCCIONES**
1. Extraer la validación del contrato JSON-RPC a un colaborador dedicado.
2. Extraer el mapeo de errores a códigos JSON-RPC a su propia clase, con tabla de equivalencia.
3. Dejar en `Handler` únicamente el enrutamiento y la delegación al caso de uso.
4. Conservar verdes `RouterRestTest` (10 pruebas) y `JsonRpcErrorContractTest` (5 pruebas)
   **sin modificarlas**: son el contrato público de la API.
5. Revisar de paso las 5 violaciones de `Rule_2.7` (campos no finales en beans), por si alguna vive
   en el entry-point y se corrige de forma natural al separar.

**NO HACER:** no cambiar el contrato JSON-RPC, ni los códigos de error, ni las rutas.

**ORDEN DE EJECUCIÓN:** extraer validador con sus pruebas → extraer mapeador de errores con las
suyas → adelgazar `Handler` → build verde → `RESULTADO-FASE-06.md` → **generar
`FASE-07-endurecimiento-y-cierre.md`** → commit.

**Commit de cierre:** `refactor(agent_api): separar validacion y mapeo de errores del entry point`

