# ADR-0002: Resolución Pura de Intenciones y Despacho Fail-Fast

* **Fecha:** 2026-08-20
* **Estado:** Aceptado
* **Autor(es):** Agent Core Team
* **Aprobadores:** Lead Architect

---

## 1. Contexto y Planteamiento del Problema

El agente conversacional debe clasificar las solicitudes de los desarrolladores (auditoría de
calidad, refinamiento, planeación, división de historias, aprobación, etc.) y enrutarlas al
manejador adecuado. Anteriormente existía una "God Class" (`AgentChatUseCase`) con condicionales
anidados, dependencias circulares y heurísticas ambiguas para detectar comandos cuando un usuario
especificaba un ID de historia.

Decisiones técnicas asociadas:

- **DP-01:** Precedencia de resolución de intención cuando conviven IDs explícitos y palabras clave.
- **DP-02:** Umbral estricto para forzar división de historias complejas.
- **DP-03:** Comportamiento ante fallos de estimación del modelo de lenguaje.

---

## 2. Opciones Evaluadas

| Opción                                                                                                | Ventajas                                                                                                                                           | Desventajas                                                                            |
|-------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| **Opción A:** Clasificación heurística dentro de `AgentChatUseCase`                                   | Todo en un solo archivo.                                                                                                                           | Violación de SRP; clases de más de 600 líneas; pruebas complejas.                      |
| **Opción B:** Clasificación puramente basada en embeddings / LLM en cada turno                        | Máxima flexibilidad semántica.                                                                                                                     | Alta latencia, coste en tokens y comportamiento impredecible ante comandos explícitos. |
| **Opción C:** Servicio puro `IntentResolver` en `domain/model` + Patrón Strategy `ChatFlowDispatcher` | Lógica determinista testeable en milisegundos; fail-fast en arranque de Spring si falta un handler; precedencia clara de ID explícito (**DP-01**). | Requiere registrar y tipar explícitamente cada handler por intención.                  |

---

## 3. Decisión Adoptada

**Adoptamos la Opción C:**

1. **`IntentResolver` como Servicio Puro en `domain/model`:**  
   Un `(ID: n)` explícito junto a un verbo de intención **gana** sobre palabras clave genéricas
   (**DP-01**).
2. **Umbral de División (**DP-02**):**  
   Si una historia de usuario se estima en **$> 8$ puntos de historia**, el flujo dispara
   automáticamente una recomendación de división hacia `DivisionFlowHandler`.
3. **No-Invención de Estimaciones (**DP-03**):**  
   Si el modelo de lenguaje no puede estimar con certeza, el sistema emite un aviso explícito de
   incertidumbre en lugar de inventar un puntaje ficticio.
4. **Exhaustividad Estricta en `ChatFlowDispatcher`:**  
   Al arrancar, el despachador valida que los 7 valores del enum `AgentIntent` cuenten con un
   `ChatFlowHandler` único. Si falta uno o hay duplicados, la aplicación falla inmediatamente
   (*fail-fast*).

---

## 4. Consecuencias y Compromisos

### Positivas:

* `AgentChatUseCase` se redujo a menos de 90 líneas con responsabilidad única de orquestación.
* 100% de cobertura en `IntentResolver` y `ChatFlowDispatcher`.
* Eliminación de regresiones de enrutamiento al introducir nuevas intenciones.

### Negativas / Riesgos Aceptados:

* Incorporar una nueva intención al enum `AgentIntent` exige implementar su handler correspondiente
  antes de que la aplicación pueda compilar e iniciar.

---

## 5. Referencias y Enlaces Relacionados

*
`azure-devops-agent/docs/plan_refactorizacion_god_classes/fases/FASE-03-resolucion-de-intencion.md`
* `azure-devops-agent/docs/plan_refactorizacion_god_classes/fases/FASE-04-separacion-de-flujos.md`
