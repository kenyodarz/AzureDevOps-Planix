# ADR-0004: Transporte Dual A2A y Patrón Null Object para Kafka (DP-05)

* **Fecha:** 2026-08-28
* **Estado:** Aceptado
* **Autor(es):** Integration Architecture Team
* **Aprobadores:** Lead Architect

---

## 1. Contexto y Planteamiento del Problema

El protocolo de interacción entre agentes (A2A - Agent-to-Agent) en entornos corporativos debe
soportar dos modos operativos:

1. **Síncrono:** Invocación inmediata solicitud-respuesta mediante JSON-RPC 2.0 sobre HTTP (usado
   por interfaces interactivas y asistentes de usuario).
2. **Asíncrono:** Publicación orientada a eventos desacoplada sobre brokers de mensajería (Kafka en
   la infraestructura del Banco).

Si se elimina el soporte asíncrono para simplificar la API actual, se genera una deuda técnica mayor
cuando la plataforma active el bus corporativo de agentes. Si se impone Kafka desde el día uno, se
introduce una dependencia dura de infraestructura local que bloquea el desarrollo ágil de la POC.

---

## 2. Opciones Evaluadas

| Opción                                                                             | Ventajas                                                                                                                                                                                | Desventajas                                                                                      |
|------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| **Opción A:** Mantener solo transporte síncrono HTTP                               | Menor número de clases y puertos.                                                                                                                                                       | Regresión de diseño cuando se requiera orquestación asíncrona entre agentes de fondo.            |
| **Opción B:** Requerir broker Kafka obligatorio local y en CI                      | Trazabilidad de mensajería desde el inicio.                                                                                                                                             | Complejidad de arranque local; necesidad de contenedores adicionales; lentitud en pruebas.       |
| **Opción C:** Transporte dual con Null Object Pattern (`NoOpAgentResponseAdapter`) | El caso de uso preserva ambos caminos; el camino asíncrono opera con `Mono.empty()` sin efectos secundarios ni necesidad de feature flags; migración transparente a Kafka en el futuro. | Requiere mantener pruebas unitarias de ambos transportes (`AgentChatUseCaseAsyncTransportTest`). |

---

## 3. Decisión Adoptada

**Adoptamos la Opción C (DP-05):**

* El caso de uso `AgentChatUseCase` expone dos métodos claros:
    - `chatAndRespond(...)`: Camino síncrono que retorna la respuesta directamente al cliente HTTP
      JSON-RPC 2.0.
    - `chat(...)`: Camino asíncrono que persiste la tarea y emite el resultado mediante el puerto
      `AgentResponseGateway`.
* Hoy, el bean por defecto de `AgentResponseGateway` es `NoOpAgentResponseAdapter` (un *Null Object*
  que retorna `Mono.empty()`).
* **Para activar A2A sobre Kafka:** Basta con sustituir el bean de infraestructura por un adaptador
  productor de Kafka, sin necesidad de modificar el dominio ni la configuración del caso de uso.

---

## 4. Consecuencias y Compromisos

### Positivas:

* Arquitectura preparada para el futuro (*future-proof*) sin sobrecoste de infraestructura local.
* 100% de cobertura en `AgentChatUseCaseAsyncTransportTest` para evitar regresiones antes de
  encender mensajería.
* Cero consumo de recursos en llamadas asíncronas no suscritas.

### Negativas / Riesgos Aceptados:

* El desarrollador par debe entender que invocar `chat(...)` en local no producirá eventos externos
  mientras no se reemplace el adaptador nulo.

---

## 5. Referencias y Enlaces Relacionados

* `azure-devops-agent/README.md` (Sección Transporte A2A)
*
`azure-devops-agent/docs/plan_refactorizacion_god_classes/fases/FASE-06-separacion-del-entry-point.md`
