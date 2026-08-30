# Protocolo de fases incrementales

Este directorio contiene el desglose ejecutable del plan `../plan/plan-maestro.md`.

## Regla de continuidad

1. Cada fase vive en un único archivo `fase-NN.md` con **tres secciones obligatorias**:
    1. **Contexto** — estado del sistema, dependencias resueltas de fases previas y alcance.
    2. **Instrucciones** — tareas técnicas, archivos a crear/modificar y validaciones.
    3. **Orden de Ejecución** — checklist secuencial.
2. **Al concluir una fase se genera automáticamente `fase-NN+1.md`**, redactado con el estado *real*
   alcanzado, no con el previsto.
3. El checklist de la sección 3 **es** el punto de reanudación: ante un reinicio de sesión o una
   pérdida de contexto, se abre el último `fase-NN.md` sin cerrar y se continúa por el primer paso
   sin marcar.
4. Ninguna fase arranca con el build en rojo heredado de la anterior (la Fase 01 es la única
   excepción, declarada en su §1.4).
5. Ninguna decisión marcada como `DP-nn` en el plan maestro se resuelve por cuenta propia: la fase
   se detiene y se consulta al usuario (`rules/spring-rules.md` §6).

6. **Si dos instrucciones de una misma fase se contradicen, no se elige en silencio**: se detiene la
   ejecución, se propone la salida y se deja escrita en el propio `fase-NN.md` y en el resultado.
   Precedente: T-04 vs T-07 en la Fase 04.

## Índice

| Fase                                            | Archivo      | Estado                                                                                                   |
|-------------------------------------------------|--------------|----------------------------------------------------------------------------------------------------------|
| 01 · Baseline y red de seguridad                | `fase-01.md` | 🟢 COMPLETADA · [resultado](../resultados/RESULTADO-FASE-01.md)                                          |
| 02 · Cliente A2A del agente y control de tareas | `fase-02.md` | 🟢 COMPLETADA · [resultado](../resultados/RESULTADO-FASE-02.md)                                          |
| 03 · API de tareas del BFF hacia el front       | `fase-03.md` | 🟢 COMPLETADA · [resultado](../resultados/RESULTADO-FASE-03.md)                                          |
| 04 · Prompts y datos fuera del dominio          | `fase-04.md` | 🟢 COMPLETADA · [resultado](../resultados/RESULTADO-FASE-04.md)                                          |
| 05 · Dominio del dashboard                      | `fase-05.md` | 🔴 **BLOQUEADA** · [bloqueantes](../plan/BLOQUEANTES-FASE-05.md) · [handoff](../plan/HANDOFF-FASE-05.md) |
| 06 · Desacople del flujo SSE                    | —            | ⚪                                                                                                       |
| 07 · Adaptadores y contrato HTTP                | —            | ⚪                                                                                                       |
| 08 · Configuración, cierre y verificación       | —            | ⚪                                                                                                       |

## Arranque en una sesión nueva

Cuando una fase se ejecuta en una sesión distinta a la que la planificó, se genera un
`HANDOFF-FASE-NN.md` en `../plan/` con: el prompt de arranque listo para copiar, los hechos ya
verificados en código —para no redescubrirlos—, las trampas conocidas y las reglas de economía de
contexto. Precedente: [`HANDOFF-FASE-05.md`](../plan/HANDOFF-FASE-05.md).

