# FASE 00 — Baseline y Caracterización

> **Estado:** PENDIENTE · **Depende de:** — · **Riesgo:** Nulo (no se modifica código productivo)
> **Commit al cerrar:** `test(agent_chat): agregar pruebas de caracterizacion del enrutamiento de flujos`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

Es la primera fase. El módulo `azure-devops-agent` implementa un agente A2A sobre Clean
Architecture de Bancolombia. La estructura de capas es correcta, pero **toda la lógica de negocio
está colapsada en dos god-classes**:

| Archivo | Líneas |
|---|---:|
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/AgentChatUseCase.java` | 644 |
| `infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/Handler.java` | 495 |

`AgentChatUseCase` concentra 6 flujos de negocio distintos (General, Auditoría de Calidad,
Refinamiento, Planificación RAG, Aprobación y División), sus 6 plantillas de prompt, el parseo de
la respuesta del LLM y el enrutamiento entre flujos.

**Cobertura de test del dominio: prácticamente nula.** No existe ningún archivo de test para
`AgentChatUseCase`. Los tests actuales cubren solo `RouterRest`, contrato JSON-RPC,
`ChatGatewayAdapter`, métricas y reglas ArchUnit.

### 1.2 Problema que resuelve esta fase

Las fases 01 a 07 refactorizan `AgentChatUseCase` de forma agresiva. **Refactorizar sin red de
seguridad sobre una clase de 644 líneas sin tests es inaceptable.**

Esta fase no corrige ninguna deuda: **congela el comportamiento actual** en tests de
caracterización (*characterization tests*) que actúan como contrato de regresión. Cualquier fase
posterior que altere el flujo elegido para un texto dado romperá el build de inmediato.

Además establece las **métricas baseline** exigidas por `docs/plan/PLAN_MAESTRO.md` §5.

### 1.3 Estado esperado al terminar

1. Existe `AgentChatUseCaseCharacterizationTest` que documenta, para cada entrada representativa,
   **qué flujo se ejecuta hoy** y **qué prompt recibe el `ChatGateway`**.
2. Existe `AgentChatUseCaseParsingTest` que documenta el comportamiento actual de los tres
   extractores de JSON, incluidos sus valores por defecto.
3. `docs/plan/ESTADO.md` contiene las métricas baseline reales de cobertura.
4. El código productivo está **intacto**: cero líneas modificadas fuera de `src/test`.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/AgentChatUseCase.java` | **LEER** (no modificar) |
| `domain/usecase/build.gradle` | MODIFICAR (solo si faltan dependencias de test) |
| `domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/AgentChatUseCaseCharacterizationTest.java` | CREAR |
| `domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/AgentChatUseCaseParsingTest.java` | CREAR |
| `docs/plan/ESTADO.md` | MODIFICAR |
| `docs/fases/FASE-01-externalizacion-de-prompts.md` | CREAR |

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§5 Testing:** patrón GIVEN/WHEN/THEN en el nombre del método
  (`givenX_whenY_thenZ`), `@ExtendWith(MockitoExtension.class)`, `@Mock` para gateways,
  `@InjectMocks` para la clase bajo prueba. Cobertura objetivo de `domain/usecase` ≥ 90%.
- **§1 `domain/usecase`:** cero anotaciones de Spring. Los tests **no** deben levantar contexto de
  Spring; son unitarios puros con Mockito.
- **§4 S109:** sin números mágicos en el código de test que se extraiga a constantes.

Stack disponible confirmado: Java 25, Spring Boot 4.1.0, Reactor (`reactor-test` ya declarado en
`main.gradle`), Mockito y AssertJ vía `spring-boot-starter-test`, JaCoCo 0.8.15.

> `AgentChatUseCase` se construye por constructor con 9 argumentos. **`@InjectMocks` no funciona
> de forma fiable con 4 parámetros `String` consecutivos**: instancia manualmente el caso de uso en
> un método `@BeforeEach` pasando los mocks y valores literales. Esto es una excepción justificada
> a §5 y debe documentarse con un comentario en el test.

### 1.6 Decisiones pendientes que bloquean

| ID | Estado | Acción |
|---|---|---|
| — | — | **Ninguna.** Esta fase documenta el comportamiento existente sin juzgarlo ni cambiarlo. |

> ⚠️ Si al escribir los tests descubres comportamientos extraños adicionales (más allá de DP-01 a
> DP-05 ya registrados), **no los corrijas**: añádelos como nueva entrada `DP-XX` en
> `docs/plan/DECISIONES_PENDIENTES.md` y escribe el test reflejando el comportamiento **actual**.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — Verificar dependencias de test de `domain/usecase`

Revisa `domain/usecase/build.gradle`. Hoy solo declara `project(':model')`. Confirma que hereda de
`main.gradle` las dependencias `reactor-test`, `spring-boot-starter-test` y `lombok`. Si al ejecutar
los tests falta alguna, añádela **únicamente al bloque de test** de ese módulo:

```gradle
dependencies {
    implementation project(':model')
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'org.mockito:mockito-junit-jupiter'
}
```

No añadas ninguna dependencia de Spring al módulo.

#### Paso B — Crear `AgentChatUseCaseCharacterizationTest`

Ubicación exacta:
`domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/AgentChatUseCaseCharacterizationTest.java`

Objetivo: para cada entrada, capturar con `ArgumentCaptor<String>` el prompt que recibe
`chatGateway.sendMessage(...)` y afirmar **qué flujo se activó**, identificándolo por un fragmento
único e inequívoco del prompt.

Fragmentos identificadores de cada flujo (verificados contra el código actual):

| Flujo | Fragmento único del prompt capturado |
|---|---|
| General | El prompt es **exactamente igual** al texto del usuario |
| Auditoría de Calidad | `"Eres un Agile Coach y Quality Analyst de Bancolombia"` |
| Refinamiento | `"Tu objetivo es analizar y proponer mejoras de refinamiento"` |
| Planificación RAG (Fase 1) | `"Analiza la siguiente idea de desarrollo y genera una propuesta"` |
| Aprobación (Fase 2) | `"Genera el borrador estructurado en Markdown"` |
| División | `"Divide esta idea de desarrollo en múltiples Historias"` |
| Creación estructurada | `"PASO 1 - Crear Historia de Usuario"` |

Casos de prueba **mínimos obligatorios** (uno por método `@Test`):

| # | Nombre del test | Entrada (`userText` / `contextId`) | Flujo esperado |
|---:|---|---|---|
| 1 | `givenContextIdGeneral_whenChatAndRespond_thenExecutesGeneralFlow` | texto libre / `general-123` | General |
| 2 | `givenContextIdDashboard_whenChatAndRespond_thenExecutesGeneralFlow` | texto libre / `dashboard-1` | General |
| 3 | `givenTextWithKeywordLista_whenChatAndRespond_thenExecutesGeneralFlow` | `"Dame la lista de historias"` | General |
| 4 | `givenAuditRequestWithWorkItemId_whenChatAndRespond_thenExecutesQualityAuditFlow` | `"Audita la historia (ID: 12345)"` | Auditoría |
| 5 | `givenRefinementRequestWithWorkItemId_whenChatAndRespond_thenExecutesRefinementFlow` | `"Analicemos y refinemos la historia (ID: 999)"` | Refinamiento |
| 6 | `givenLongIdeaText_whenChatAndRespond_thenExecutesPlanningSimilarityFlow` | texto > 25 caracteres no comando | Planificación RAG |
| 7 | `givenApprovalCommand_whenChatAndRespond_thenExecutesApprovalFlow` | `"Aprobado"` | Aprobación |
| 8 | `givenApprovalCommandWithAccent_whenChatAndRespond_thenExecutesApprovalFlow` | `"Sí"` | Aprobación |
| 9 | `givenDivisionCommand_whenChatAndRespond_thenExecutesDivisionFlow` | `"Dividir"` | División |
| 10 | `givenStructuredCreationIntent_whenChatAndRespond_thenBuildsTemplatePrompt` | `"CREATE_STRUCTURED_USER_STORY"` | Creación estructurada |
| 11 | `givenEmptyText_whenChatAndRespond_thenReturnsNoContentWithoutCallingGateway` | `""` | Ninguno (`verify(chatGateway, never())`) |
| 12 | `givenNullMessage_whenChatAndRespond_thenReturnsNoContent` | request con `message` nulo | Ninguno |
| 13 | `givenGatewayError_whenChatAndRespond_thenReturnsFailedTask` | gateway lanza excepción | `TaskState.FAILED` |
| 14 | `givenVectorStoreError_whenPlanningFlow_thenContinuesWithoutContext` | `searchSimilarity` falla | Planificación RAG igualmente ejecutada |

**Casos que documentan el comportamiento anómalo de DP-01 (obligatorios):**

| # | Nombre del test | Entrada | Flujo esperado HOY |
|---:|---|---|---|
| 15 | `givenAuditRequestContainingKeywordReporte_whenChatAndRespond_thenGeneralFlowWinsByPrecedence` | `"Genera un reporte de calidad de la historia (ID: 12345)"` | **General** (no Auditoría) |
| 16 | `givenLongIdeaContainingKeywordConsulta_whenChatAndRespond_thenGeneralFlowWinsByPrecedence` | idea larga que contiene `"consulta"` | **General** (no Planificación) |

Los tests 15 y 16 deben llevar un comentario Javadoc que enlace explícitamente a **DP-01** y aclare
que documentan el comportamiento actual, no el deseado.

Usa `StepVerifier` de `reactor-test` para verificar los `Mono` y `ArgumentCaptor` para el prompt.

#### Paso C — Crear `AgentChatUseCaseParsingTest`

Ubicación exacta:
`domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/AgentChatUseCaseParsingTest.java`

Los tres extractores son `private`. **No uses reflexión ni cambies su visibilidad.** Verifícalos
indirectamente a través del flujo de Aprobación, afirmando sobre el bloque informativo que aparece
en el texto de la respuesta final.

Casos mínimos:

| # | Nombre del test | Respuesta simulada del LLM | Aserción sobre la salida |
|---:|---|---|---|
| 1 | `givenLlmResponseWithValidJson_whenApprovalFlow_thenInfoBlockShowsParsedValues` | JSON con `"puntos": 5`, nivel `"Baja"`, justificación | Contiene `5`, `Baja` y la justificación |
| 2 | `givenLlmResponseWithoutJson_whenApprovalFlow_thenInfoBlockShowsDefaultValues` | sin bloque JSON | Contiene `1` y `Media` → documenta **DP-03** |
| 3 | `givenLlmResponseWithHighPoints_whenApprovalFlow_thenAppendsComplexityAlert` | `"puntos": 13` | Contiene `Alerta de Complejidad` |
| 4 | `givenLlmResponseWithEightPoints_whenApprovalFlow_thenDoesNotAppendAlert` | `"puntos": 8` | **No** contiene la alerta → documenta **DP-02** |
| 5 | `givenLlmResponseWithJsonBlock_whenApprovalFlow_thenJsonIsStrippedFromOutput` | JSON en bloque ```json | La salida no contiene `"puntos"` crudo |
| 6 | `givenAuditResponseWithMarkers_whenAuditFlow_thenAuditJsonBlockIsStripped` | respuesta con `AUDIT_JSON_START/END` | La salida no contiene los marcadores |

Los tests 2 y 4 llevan Javadoc enlazando a **DP-03** y **DP-02** respectivamente.

#### Paso D — Medir y registrar el baseline

Ejecuta:

```
.\gradlew.bat clean build jacocoMergedReport
```

De `build/reports/jacocoMergedReport/jacocoMergedReport.xml` extrae el porcentaje de cobertura de
instrucciones para `domain/model` y `domain/usecase`, y regístralo en la tabla de métricas de
`docs/plan/ESTADO.md`.

### 2.2 Qué NO hacer

- ❌ **No modifiques ni una línea** de `AgentChatUseCase.java` ni de ningún archivo bajo `src/main`
  de `domain`, `infrastructure` o `applications`. La única excepción autorizada es añadir
  dependencias de test a `domain/usecase/build.gradle`.
- ❌ No corrijas los comportamientos anómalos de DP-01, DP-02 ni DP-03. Los tests deben **fallar**
  si alguien los "arregla" sin aprobación.
- ❌ No cambies la visibilidad de métodos privados para hacerlos testeables.
- ❌ No uses reflexión, `PowerMock` ni acceso a campos privados.
- ❌ No toques los tests existentes (`RouterRestTest`, `JsonRpcErrorContractTest`,
  `ArchitectureTest`, `ChatGatewayAdapterTest`, `MicrometerMetricPublisherTest`).
- ❌ No añadas dependencias de Spring al módulo `domain/usecase`.

### 2.3 Criterios de aceptación

- [ ] Existen los 2 archivos de test en las rutas exactas indicadas.
- [ ] Los 16 casos del Paso B están implementados y pasan.
- [ ] Los 6 casos del Paso C están implementados y pasan.
- [ ] Los tests 15 y 16 (Paso B) y 2 y 4 (Paso C) tienen Javadoc enlazando a su `DP-XX`.
- [ ] `git diff --stat` no muestra cambios en `src/main` salvo, opcionalmente,
      `domain/usecase/build.gradle`.
- [ ] `.\gradlew.bat build` termina en `BUILD SUCCESSFUL`.
- [ ] La cobertura real de `domain/usecase` está registrada en `ESTADO.md`.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring (tampoco en los tests)
- [ ] Sin secretos hardcodeados (S2068) — usar IDs ficticios como `12345`
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15 en todo método de test
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109) — extraer a `private static final` en el test
- [ ] Nombres de test en formato `givenX_whenY_thenZ`
- [ ] `.\gradlew.bat build` verde

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmado: Fase 00 no está bloqueada |
| 2 | Leer íntegro `AgentChatUseCase.java` (644 líneas) | Comprensión completa del enrutamiento actual |
| 3 | Verificar dependencias de test en `domain/usecase/build.gradle` (Paso A) | Módulo listo para tests unitarios |
| 4 | Crear `AgentChatUseCaseCharacterizationTest` con los 16 casos (Paso B) | Archivo creado |
| 5 | Ejecutar `.\gradlew.bat :usecase:test` | Los 16 tests en verde |
| 6 | Crear `AgentChatUseCaseParsingTest` con los 6 casos (Paso C) | Archivo creado |
| 7 | Ejecutar `.\gradlew.bat :usecase:test` | Los 22 tests en verde |
| 8 | Ejecutar `.\gradlew.bat clean build jacocoMergedReport` (Paso D) | `BUILD SUCCESSFUL` + reporte generado |
| 9 | Verificar con `git diff --stat` que `src/main` está intacto | Sin cambios productivos |
| 10 | Registrar métricas baseline de cobertura en `docs/plan/ESTADO.md` | Tabla de métricas completa |
| 11 | Marcar Fase 00 como 🟢 COMPLETADA en `ESTADO.md` + añadir entrada a la bitácora | Tablero al día |
| 12 | **Generar `docs/fases/FASE-01-externalizacion-de-prompts.md`** usando `_PLANTILLA_FASE.md` | Checkpoint de continuidad creado |
| 13 | Commit: `test(agent_chat): agregar pruebas de caracterizacion del enrutamiento de flujos` | Cambios versionados |

> ⚠️ **El paso 12 es innegociable.** Sin el MD de la Fase 01, el trabajo no es reanudable tras una
> desconexión y la Fase 00 se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la Fase 01

El archivo `docs/fases/FASE-01-externalizacion-de-prompts.md` debe contener, con la estructura de
tres bloques de `_PLANTILLA_FASE.md`:

**CONTEXTO**
- Venimos de la Fase 00: existe una red de 22 tests de caracterización que blinda el comportamiento.
- Deuda a resolver: **D-03** — ~180 líneas de plantillas de prompt viven como constantes `String`
  dentro de `AgentChatUseCase` (líneas 46-217), lo que viola SRP y obliga a recompilar para cambiar
  contenido editorial.
- Problema adicional: `String.format` posicional frágil. `AUDIT_QUALITY_PROMPT_TEMPLATE` recibe
  `workItemId` **tres veces** en posiciones distintas (líneas 405-409) y la tabla de porcentajes
  obliga a escapar `%%` (líneas 194-199).
- Bloqueo: **DP-04** (nombres y ruta de los recursos de prompt) debe estar resuelta antes de crear
  los archivos. Si sigue ABIERTA → detenerse y preguntar.

**INSTRUCCIONES**
1. Crear en `domain/model` el puerto puro `co.com.bancolombia.model.prompt.gateways.PromptTemplatePort`
   con `String render(PromptTemplateId id, Map<String, Object> variables)` y el enum
   `PromptTemplateId` con las 6 plantillas.
2. Crear el driven-adapter `ClasspathPromptTemplateAdapter` que carga los `.md` desde
   `applications/app-service/src/main/resources/prompts/` y sustituye variables **por nombre**
   (`{{workItemId}}`), eliminando el `String.format` posicional y el escapado `%%`.
3. Mover el contenido literal de las 6 constantes a sus archivos `.md`, **sin alterar ni un
   carácter** del texto.
4. Cachear las plantillas al arranque (no leer del classpath en cada request).
5. Sustituir en `AgentChatUseCase` cada `String.format(CONSTANTE, ...)` por
   `promptTemplatePort.render(ID, Map.of(...))` y borrar las constantes.
6. Añadir un test que compare el prompt renderizado contra el `String.format` original
   **byte a byte** para garantizar equivalencia exacta.
7. Registrar el nuevo bean en `UseCasesConfig` (el constructor sube temporalmente a 10 args; se
   corrige en la Fase 05).

**NO HACER:** no reescribir, mejorar ni traducir el texto de los prompts; no cambiar el orden de
las secciones; no tocar los 22 tests de la Fase 00.

**ORDEN DE EJECUCIÓN:** verificar DP-04 → crear puerto y enum → crear los 6 `.md` → crear adapter →
test de equivalencia byte a byte → sustituir en el use case → build verde → los 22 tests de Fase 00
siguen verdes → actualizar `ESTADO.md` → **generar `FASE-02-value-objects-de-estimacion.md`** →
commit `refactor(agent_prompts): externalizar plantillas de prompt a recursos`.

**Commit de cierre:** `refactor(agent_prompts): externalizar plantillas de prompt a recursos`

