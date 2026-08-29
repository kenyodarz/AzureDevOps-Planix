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

## DP-04 — Ver la sección «DP-04 ✅ RESUELTA» más abajo

Entrada original archivada. La decisión final (aprobada) está registrada al final de este documento.

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

## DP-07 — Ver la sección «DP-07 ✅ RESUELTA» más abajo

Entrada original archivada. La decisión final (eliminar) está registrada al final de este documento.

---

## DP-06 — Estimación por horas ✅ RESUELTA

- **Estado:** 🟢 RESUELTA
- **Resuelta por:** Usuario · **Fecha:** 2026-08-28
- **Se aplica en:** Fase 01 (contenido de los prompts), Fase 02 (validaciones)

**Tabla oficial de equivalencia aportada por el usuario:**

| Story Point | Esfuerzo (Horas) | Complejidad |
|---:|---|---|
| 1 | < 1 hora | Muy baja |
| 2 | 1 - 4 horas | Baja |
| 3 | 4 - 8 horas (1 día) | Baja |
| 5 | 8 - 24 horas (1-3 días) | Media |
| 8 | 24 - 30 horas (1 semana) | Media - Alta |
| 13 | > 30 horas (> 1 semana) | Alta |

Esta tabla es la **única fuente de verdad** para estimar. Se inyecta literalmente en los prompts que
estiman o auditan estimaciones: `fase2-historia-estructurada.md`, `division-historias.md`,
`refinamiento-historia.md` y `auditoria-calidad.md`.

**Coherencia con DP-02:** la tabla confirma el umbral. 8 SP = hasta 30 horas (1 semana) es el máximo
admisible; 13 SP = más de una semana, que es exactamente el «>8 Requiere Dividir» de la guía
corporativa. Ambas decisiones apuntan al mismo punto de corte.

### Sub-decisión aplicada: eliminación de la «Regla de Mínimos de 5 puntos»

**Contexto:** `AgentChatUseCase.java:93` obliga a estimar en **≥ 5 puntos** cualquier desarrollo con
múltiples endpoints, base de datos, seguridad o integraciones.

**Por qué se elimina:** con la tabla de horas, 5 SP equivale a **8-24 horas**. La regla afirmaría
entonces que «publicar un CRUD nunca puede costar menos de 8 horas», lo cual contradice
directamente el criterio medible por horas que el usuario adopta y el ejemplo de la guía corporativa
(`HISTORIA_USUARIO.md:73`: *«2 = He realizado esto antes… Ej. Publicar un nuevo endpoint»*).

Además **elimina una contradicción interna real**: el prompt de auditoría (`:182`) ya afirmaba lo
opuesto («la guía corporativa NO establece mínimos obligatorios»). Hoy el agente **crea historias con
una regla y las audita con la contraria**.

**Resolución:** la «Regla de Mínimos» se sustituye íntegramente por la tabla de horas como criterio
único. El prompt de auditoría queda automáticamente coherente con el de creación.

> ⚠️ Sub-decisión derivada por implicación lógica directa de la tabla aportada, no confirmada de
> forma explícita por el usuario. **Señalada al usuario para su ratificación o reversión.** Revertir
> es trivial: reintroducir el párrafo en `fase2-historia-estructurada.md`.

---

## DP-04 — Nombres de recursos de prompts ✅ RESUELTA

- **Estado:** 🟢 RESUELTA — **aprobada la propuesta**
- **Resuelta por:** Usuario · **Fecha:** 2026-08-28
- **Se aplica en:** Fase 01

```
applications/app-service/src/main/resources/prompts/
├── fase1-propuesta-inicial.md
├── fase2-historia-estructurada.md
├── division-historias.md
├── refinamiento-historia.md
└── auditoria-calidad.md
```

**Nota:** el archivo `creacion-estructurada.md` de la propuesta original **queda descartado** por la
resolución de DP-07. Son **5** plantillas, no 6.

---

## DP-07 — `buildPrompt()` y la plantilla de creación estructurada ✅ RESUELTA

- **Estado:** 🟢 RESUELTA — **Opción B: ELIMINAR**
- **Resuelta por:** Usuario · **Fecha:** 2026-08-28
- **Se aplica en:** Fase 01

**Decisión del usuario:** *«Eliminémoslo. Cuando dejemos este back/agente al 100% y saltemos al
front, allá determinamos si lo necesitamos y se reconstruye correctamente. Puede que sea dead code,
pero ahora mismo no nos detengamos en eso.»*

**Alcance de la eliminación:**

1. Se elimina la rama `CREATE_STRUCTURED_USER_STORY` de `buildPrompt():569-586`, con su plantilla
   inline de creación de Work Item y tareas hijas.
2. `buildPrompt()` desaparece: su rama restante se limita a devolver el texto del usuario, y la
   validación de contenido vacío ya ocurre antes en `executeChat():352-354`. La rama `else` de
   `handleSpecialCommands()` pasa a invocar el gateway con `userText` directamente.
3. **No** se crea `PromptTemplateId.STRUCTURED_CREATION` ni `creacion-estructurada.md`.

**Impacto en los tests de la Fase 00:** ninguno funcional. El test
`givenStructuredCreationMarker_whenChatAndRespond_thenPlanningFlowWinsAndTemplateIsNeverUsed`
**sigue en verde** porque el marcador ya se enrutaba a Planificación. Se conserva como prueba de
regresión y se actualiza su Javadoc para reflejar que DP-07 quedó resuelta por eliminación.

**Reconstrucción futura:** si el frontend llega a necesitar creación estructurada, se implementará
como intención de primer nivel (`STRUCTURED_CREATION`) detectada por marcador exacto antes de
cualquier heurística de longitud, tal como exige DP-01.

---

## Registro de decisiones resueltas

| ID | Decisión tomada | Resuelta por | Fecha |
|---|---|---|---|
| DP-01 | Corregir la precedencia: `(ID: n)` + verbo gana sobre palabras clave genéricas; keywords por palabra completa | Usuario | 2026-08-28 |
| DP-02 | Umbral de división `> 8` (`MAX_STORY_POINTS_PER_STORY = 8`), según tabla oficial de `HISTORIA_USUARIO.md` | Usuario | 2026-08-28 |
| DP-04 | Aprobada la ruta `resources/prompts/` y los nombres de archivo (5 plantillas) | Usuario | 2026-08-28 |
| DP-06 | Estimación por horas según tabla oficial; se elimina la «Regla de Mínimos de 5 puntos» | Usuario | 2026-08-28 |
| DP-07 | Eliminar `buildPrompt()` y la plantilla de creación estructurada (dead code) | Usuario | 2026-08-28 |

## Decisiones aún abiertas

| ID | Descripción | Bloquea |
|---|---|---|
| DP-03 | Qué hacer si el LLM no devuelve estimación (recomendación: Opción C, aviso explícito) | Fase 02 |
| DP-05 | Camino muerto `chat()` async contra `NoOpAgentResponseAdapter` | Fase 07 |

