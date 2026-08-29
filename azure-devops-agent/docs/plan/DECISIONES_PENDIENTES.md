# Decisiones Pendientes — Política de No-Asunción

> `rules/spring-rules.md` §6 y `.github/copilot-instructions.md`:
> **Nunca asumas información que no esté explícita. Ante cualquier duda, detente y pregunta.**

Toda entrada con estado `ABIERTA` **bloquea** las fases listadas en su columna «Bloquea».
Ningún agente puede resolverla por su cuenta.

---

## DP-01 — El primer `if` de `executeChat()` secuestra flujos ✅ RESUELTA

- **Estado:** 🟢 RESUELTA — **Opción B: CORREGIR**
- **Resuelta por:** Usuario · **Fecha:** 2026-08-28
- **Se aplica en:** Fase 03 (`IntentResolver`)
- **Ubicación del defecto:** `AgentChatUseCase.java:356-366`

**Hallazgo:** `isGeneralFlow` se evaluaba **antes** que la auditoría y el refinamiento, activándose
con `userText.toLowerCase().contains("lista"|"busca"|"consulta"|"reporte")`.

```text
"Genera un reporte de calidad de la historia (ID: 12345)"
  -> contiene "reporte" -> entra a runGeneralFlow()
  -> NUNCA llega a runQualityAuditFlow()   [DEFECTO]
```

**Decisión del usuario:** *«Si lo ves mal es mejor corregirlo, hagamos la app anti dummies.»*

**Resolución — nueva precedencia de intención (de más específica a más genérica):**

| Prioridad | Intención | Criterio de detección |
|---:|---|---|
| 1 | `QUALITY_AUDIT` | verbo de auditoría (`audita`/`evalúa`/`revisa`/`calidad`) **+** `(ID: n)` |
| 2 | `REFINEMENT` | `analicemos y refinemos` **+** `(ID: n)` |
| 3 | `STRUCTURED_CREATION` | marcador explícito `CREATE_STRUCTURED_USER_STORY` |
| 4 | `APPROVAL` / `DIVISION` | comando corto exacto tras normalización |
| 5 | `GENERAL` | `contextId` empieza por `general`/`dashboard`, **o** palabra clave genérica |
| 6 | `PLANNING_DRAFT` | texto libre > 25 caracteres que no encaje en lo anterior |

**Reglas derivadas obligatorias:**

1. La presencia de `(ID: n)` junto a un verbo de intención **siempre** gana sobre las palabras clave
   genéricas. Un `(ID: n)` explícito es una señal fuerte; `"reporte"` es una señal débil.
2. Las palabras clave genéricas dejan de evaluarse con `contains()` sobre la frase completa: deben
   coincidir como **palabra completa** (límite de palabra `\b`), para que `"consultar"` dentro de una
   idea larga no secuestre el flujo de planeación.
3. El `contextId` (`general*`/`dashboard*`) mantiene su fuerza actual: es una señal explícita del
   cliente, no una inferencia sobre texto libre.

**Impacto en Fase 00:** los tests 15 y 16 se escriben capturando el comportamiento **actual
(defectuoso)**, con Javadoc `@see DP-01` indicando que sus aserciones **se invertirán en la Fase 03**.
Así el cambio de comportamiento queda explícito y versionado en el diff, no oculto.

---

## DP-02 — Umbral de división: `13` en código vs `8` en el texto ✅ RESUELTA

- **Estado:** 🟢 RESUELTA — **Opción C: umbral `> 8`**
- **Resuelta por:** Usuario · **Fecha:** 2026-08-28
- **Se aplica en:** Fase 02
- **Fuente normativa:** `applications/app-service/src/main/resources/HISTORIA_USUARIO.md:71-76`

**Evidencia documental (tabla oficial de la guía corporativa):**

| ESTIMACIÓN | 1 | 2 | 3 | 5 | 8 | **>8 Requiere Dividir** |
|---|---|---|---|---|---|---|

La guía es inequívoca: **cualquier estimación superior a 8 requiere dividir**.

**Resolución:**

```java
private static final int MAX_STORY_POINTS_PER_STORY = 8;
// Dispara alerta de división cuando: points > MAX_STORY_POINTS_PER_STORY
```

**Por qué `> 8` y no `>= 13`:** en una escala Fibonacci pura ambos coinciden (el valor siguiente a 8
es 13), pero el LLM **no siempre respeta la escala**. Con la condición actual, una estimación de 9,
10 o 12 puntos **no dispara ninguna alerta** y la historia se registra sin dividir. Con `> 8` el
comportamiento es correcto para cualquier valor que devuelva el modelo. Es exactamente el tipo de
defecto que cubre el criterio «anti dummies» de DP-01.

**Efecto secundario:** el texto de `HIGH_COMPLEXITY_ALERT_TEMPLATE` («supera el estándar de la célula
de 8 puntos») deja de ser mentira y pasa a describir el comportamiento real. No requiere cambios.

---

## DP-06 — Migrar el criterio de estimación de «días» a «horas» 🔴 ABIERTA

- **Estado:** 🔴 ABIERTA — **requiere dato del usuario**
- **Bloquea:** Fase 01 (redacción de prompts), Fase 02 (validaciones)
- **No bloquea:** Fase 00

**Contexto aportado por el usuario:** *«Para crear esta regla me basé en `HISTORIA_USUARIO.md`, solo
que después bajé el estándar por una célula donde se trabajaba 1 día = 1 punto, pero es mejor usar el
estándar por horas pues es mucho más medible.»*

**Situación actual en el código:** los prompts **no mencionan ninguna unidad temporal**. Definen la
estimación de forma cualitativa y con una regla de mínimos discutible:

> `AgentChatUseCase.java:93` — «Cualquier desarrollo técnico que involucre múltiples endpoints
> (CRUD/BFF), base de datos, seguridad (JWT/RBAC) o integraciones de APIs no puede ser estimado en
> menos de **5 puntos**.»

Esa regla de mínimos **contradice** el prompt de auditoría, que afirma lo contrario:

> `AgentChatUseCase.java:182` — «la guía corporativa NO establece mínimos obligatorios de Story
> Points para tareas de bases de datos, integraciones o APIs.»

Es decir: **el mismo agente crea historias con una regla y las audita con la regla opuesta.**

**Lo que NO puedo asumir** (`spring-rules.md` §6.1 y §6.5): la tabla exacta de equivalencia
puntos → horas. Inventarla desalinearía todas las estimaciones del equipo.

**Pregunta al usuario — necesito la equivalencia concreta:**

| Story Points | Horas (¿?) |
|---:|---|
| 1 | _¿?_ |
| 2 | _¿?_ |
| 3 | _¿?_ |
| 5 | _¿?_ |
| 8 | _¿?_ |

Y adicionalmente:

1. ¿Las horas son de **una persona** o de la célula completa?
2. ¿Incluyen pruebas, documentación y pipeline, o solo desarrollo?
3. ¿Se **elimina** la «Regla de Mínimos de 5 puntos» para dejar solo el criterio de horas?
   (Recomiendo eliminarla: hoy contradice al prompt de auditoría.)

- **Resolución:** _(pendiente)_

---

## DP-03 — Default silencioso de `1` punto cuando el LLM no devuelve estimación

- **Estado:** 🟡 PROPUESTA (recomendación pendiente de confirmar)
- **Bloquea:** Fase 02
- **Ubicación:** `AgentChatUseCase.extractPuntosFromLlmResponse():291`

**Hallazgo:** si el LLM omite el bloque JSON, el método retorna `1` sin avisar. El usuario ve
«📊 Estimación de Complejidad (Story Points): 1» aunque el modelo **nunca estimó nada**. Enmascara
ausencia de dato como dato válido (espíritu de S2259).

**Recomendación (Opción C), coherente con el criterio «anti dummies» de DP-01:** modelar el
resultado como `Optional<ComplexityEstimation>` y, cuando esté vacío, mostrar
«⚠️ El modelo no devolvió una estimación válida» en lugar de un número inventado.

- **Resolución:** _(pendiente de confirmación explícita)_
- **Fecha de resolución:** —

---

## DP-04 — Nombres de recursos de prompts

- **Estado:** 🟡 PROPUESTA (a confirmar en Fase 01)
- **Bloquea:** Fase 01
- **Contexto:** `spring-rules.md` §6.1 prohíbe asumir «paths de recursos».

**Propuesta a validar:**

```
applications/app-service/src/main/resources/prompts/
├── fase1-propuesta-inicial.md
├── fase2-historia-estructurada.md
├── division-historias.md
├── refinamiento-historia.md
├── auditoria-calidad.md
└── creacion-estructurada.md
```

**Pregunta al usuario:**
> ¿Se aceptan estos nombres y esta ruta, o prefieres otra convención?

- **Resolución:** _(pendiente)_
- **Fecha de resolución:** —

---

## DP-05 — Camino muerto: `chat()` asíncrono contra un adapter no-op

- **Estado:** 🟡 ABIERTA (no bloqueante hasta Fase 07)
- **Bloquea:** Fase 07
- **Ubicación:** `AgentChatUseCase.chat():248` → `NoOpAgentResponseAdapter`

**Hallazgo:** el método `chat()` persiste la tarea y publica la respuesta en un gateway cuya única
implementación retorna `Mono.empty()`. Es un camino muerto en producción.

**Pregunta al usuario:**
> **Opción A (por defecto):** Conservarlo y documentarlo como punto de extensión para Kafka.
> **Opción B:** Eliminar `chat()`, `AgentResponseGateway` y `NoOpAgentResponseAdapter`.
> **Opción C:** Conservarlo tras un feature flag explícito en `application.yaml`.

- **Resolución:** _(pendiente)_
- **Fecha de resolución:** —

---

## DP-07 — `buildPrompt()` y la plantilla de creación estructurada son código inalcanzable 🔴 ABIERTA

- **Estado:** 🔴 ABIERTA — **hallazgo nuevo detectado durante la Fase 00**
- **Bloquea:** Fase 03, Fase 04
- **No bloquea:** Fase 00
- **Ubicación:** `AgentChatUseCase.buildPrompt():561-588` y `shouldSearchPlanning():320-333`

**Hallazgo:** `buildPrompt()` contiene una rama que construye el prompt de creación estructurada de
Historia de Usuario (con la plantilla corporativa y el desglose de tareas hijas) cuando el texto es
`CREATE_STRUCTURED_USER_STORY`. **Esa rama nunca puede ejecutarse.**

Traza del enrutamiento:

```text
userText = "CREATE_STRUCTURED_USER_STORY"   (28 caracteres)

executeChat()
 └─> shouldSearchPlanning(userText)
      ├─ trimmed.length() = 28  ->  NO es < 25          -> sigue
      ├─ normalized = "create_structured_user_story"
      ├─ ¿está en la lista de comandos cortos?  ->  NO
      └─ return TRUE
 └─> runPlanningSimilarityFlow()     [gana siempre]

handleSpecialCommands() -> NUNCA se alcanza
buildPrompt()           -> NUNCA se alcanza
```

`buildPrompt()` solo se invoca desde la rama `else` de `handleSpecialCommands()`, y llegar allí
exige `shouldSearchPlanning() == false`, es decir: texto de **menos de 25 caracteres** o comando
corto exacto. El marcador tiene 28 caracteres, y la variante JSON
(`{"intent":"CREATE_STRUCTURED_USER_STORY"}`) es aún más larga.

**Consecuencia:** la funcionalidad de creación estructurada con tareas hijas vinculadas
(`createWorkItem` + `updateWorkItem` con jerarquía `System.LinkTypes.Hierarchy-Reverse`) **está
implementada pero es inalcanzable**. Cualquier cliente que envíe ese marcador recibe una propuesta
de planeación en prosa en lugar de la creación estructurada.

**Pregunta al usuario:**
> **Opción A:** `CREATE_STRUCTURED_USER_STORY` es un marcador que el frontend **sí** envía y debe
> funcionar. En ese caso pasa a ser una intención de primer nivel (`STRUCTURED_CREATION`), detectada
> por marcador exacto **antes** de cualquier heurística de longitud. *(Recomendada, coherente con
> DP-01.)*
> **Opción B:** Es funcionalidad obsoleta. Se elimina `buildPrompt()` y su plantilla.

**Nota:** confirmar si `azure-devops-frontend` envía este marcador determina la respuesta. No lo
asumo (`spring-rules.md` §6.2).

- **Resolución:** _(pendiente)_

---

## Registro de decisiones resueltas

| ID | Decisión tomada | Resuelta por | Fecha |
|---|---|---|---|
| DP-01 | Corregir la precedencia: `(ID: n)` + verbo gana sobre palabras clave genéricas; keywords por palabra completa | Usuario | 2026-08-28 |
| DP-02 | Umbral de división `> 8` (`MAX_STORY_POINTS_PER_STORY = 8`), según tabla oficial de `HISTORIA_USUARIO.md` | Usuario | 2026-08-28 |

