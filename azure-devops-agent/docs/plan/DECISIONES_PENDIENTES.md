# Decisiones Pendientes — Política de No-Asunción

> `rules/spring-rules.md` §6 y `.github/copilot-instructions.md`:
> **Nunca asumas información que no esté explícita. Ante cualquier duda, detente y pregunta.**

Toda entrada con estado `ABIERTA` **bloquea** las fases listadas en su columna «Bloquea».
Ningún agente puede resolverla por su cuenta.

---

## DP-01 — El primer `if` de `executeChat()` secuestra flujos (BUG POTENCIAL)

- **Estado:** 🔴 ABIERTA
- **Bloquea:** Fase 03, Fase 04
- **Ubicación:** `AgentChatUseCase.java:356-366`

**Hallazgo:** `isGeneralFlow` se evalúa **antes** que la auditoría y el refinamiento, y se activa con
`userText.toLowerCase().contains("lista"|"busca"|"consulta"|"reporte")`.

Consecuencia real observable hoy:

```text
"Genera un reporte de calidad de la historia (ID: 12345)"
  -> contiene "reporte" -> entra a runGeneralFlow()
  -> NUNCA llega a runQualityAuditFlow()
```

Lo mismo aplica a `"consulta"` dentro de un texto largo de planeación, que secuestra el flujo RAG.

**Pregunta al usuario:**
> ¿El comportamiento actual es intencional o es un defecto?
> **Opción A (por defecto):** Se preserva tal cual. El refactor replica la precedencia actual.
> **Opción B:** Se corrige y las intenciones específicas con `(ID: n)` pasan a evaluarse **antes**
> que la detección genérica por palabras clave.

**Impacto si se elige B:** cambio funcional visible; requiere ajustar los tests de caracterización
creados en la Fase 00 y validación funcional con usuarios del agente.

- **Resolución:** _(pendiente)_
- **Fecha de resolución:** —

---

## DP-02 — Umbral de complejidad inconsistente: `13` en código vs `8` en el texto

- **Estado:** 🔴 ABIERTA
- **Bloquea:** Fase 02
- **Ubicación:** `AgentChatUseCase.java:447` y `:518` (condición `puntos >= 13`) contra
  `HIGH_COMPLEXITY_ALERT_TEMPLATE:57` («supera el estándar de la célula de 8 puntos»)

**Hallazgo:** el código dispara la alerta de división a partir de 13 puntos, pero el mensaje que lee
el usuario afirma que el estándar son 8. Una historia de 8 o 13 puntos recibe tratamiento distinto
al anunciado.

**Pregunta al usuario:**
> ¿Cuál es el umbral correcto para sugerir la división de una historia?
> **Opción A (por defecto):** Preservar `>= 13` y dejar el texto como está.
> **Opción B:** Preservar `>= 13` y corregir únicamente el texto del mensaje.
> **Opción C:** Cambiar el umbral a `> 8` para alinearlo con la guía corporativa.

- **Resolución:** _(pendiente)_
- **Fecha de resolución:** —

---

## DP-03 — Default silencioso de `1` punto cuando el LLM no devuelve estimación

- **Estado:** 🔴 ABIERTA
- **Bloquea:** Fase 02
- **Ubicación:** `AgentChatUseCase.extractPuntosFromLlmResponse():291`

**Hallazgo:** si el LLM omite el bloque JSON, el método retorna `1` sin avisar. El usuario ve
«📊 Estimación de Complejidad (Story Points): 1» aunque el modelo nunca estimó nada. Esto viola el
espíritu de S2259 (enmascarar ausencia de dato como dato válido).

**Pregunta al usuario:**
> ¿Qué debe ocurrir cuando el LLM no devuelve el bloque JSON de estimación?
> **Opción A (por defecto):** Mantener el `1` silencioso.
> **Opción B:** Omitir el bloque informativo de estimación por completo.
> **Opción C:** Mostrar un aviso explícito del tipo «⚠️ El modelo no devolvió una estimación».

- **Resolución:** _(pendiente)_
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

## Registro de decisiones resueltas

| ID | Decisión tomada | Resuelta por | Fecha |
|---|---|---|---|
| — | — | — | — |

