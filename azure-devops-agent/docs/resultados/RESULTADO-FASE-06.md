# RESULTADO — FASE 06: Separación del Entry-Point

> **Ejecutada:** 2026-08-29 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(agent_api): separar validacion y mapeo de errores del entry point`
> **Instrucciones:** `docs/fases/FASE-06-separacion-del-entry-point.md`

---

## 1. Objetivo de la fase

Saldar **D-10** y **D-11**: `Handler.java` concentraba cuatro responsabilidades sin relación
—mapeo de payloads, construcción de sobres JSON-RPC, Agent Card estática y enrutamiento HTTP— en
**495 líneas**, la única clase de más de 300 líneas que quedaba en el proyecto.

**Restricción absoluta:** el contrato público JSON-RPC 2.0 no podía cambiar en absoluto.

---

## 2. Qué se construyó

### Entry-point (`infrastructure/entry-points/reactive-web`)

```
src/main/java/co/com/bancolombia/api/
├── Handler.java                       MODIFICADO  495 → 112 líneas (solo transporte HTTP)
├── JsonRpcDispatcher.java             CREADO      enrutamiento de métodos JSON-RPC
├── AgentCardProvider.java             CREADO      tarjeta A2A estática, construida una vez
└── mapper/
    ├── JsonRpcPayloadMapper.java      CREADO      params crudo → modelo A2A
    └── JsonRpcResponseFactory.java    CREADO      sobres + códigos de error como constantes

src/test/java/co/com/bancolombia/api/
├── AgentCardProviderTest.java         CREADO      5 pruebas
├── JsonRpcDispatcherTest.java         CREADO      7 pruebas
└── mapper/
    ├── JsonRpcPayloadMapperTest.java  CREADO      20 pruebas (13 de ellas degeneradas)
    └── JsonRpcResponseFactoryTest.java CREADO     12 pruebas
```

### Reparto de responsabilidades resultante

| Clase | Única razón para cambiar |
|---|---|
| `Handler` | Cambia el transporte HTTP (rutas, cabeceras, códigos de estado) |
| `JsonRpcDispatcher` | Cambia el catálogo de métodos JSON-RPC del agente |
| `JsonRpcPayloadMapper` | Cambia la forma del payload de entrada |
| `JsonRpcResponseFactory` | Cambia la forma del sobre o los códigos de error |
| `AgentCardProvider` | Cambian las capacidades publicadas del agente |

---

## 3. Decisiones aplicadas

Ninguna DP bloqueaba esta fase. **DP-05 no se tocó**: sigue abierta y corresponde a la Fase 07.

Dos decisiones de diseño tomadas dentro del alcance permitido, ambas obligadas por la
prohibición de modificar las suites del contrato:

1. **Las clases nuevas no son beans de Spring.** `RouterRestTest`, `JsonRpcErrorContractTest`,
   `RouterRestLegacyToggleTest` y `RouterRestLegacySunsetTest` declaran
   `@ContextConfiguration(classes = {RouterRest.class, Handler.class})`. Registrar cualquier
   colaborador nuevo como `@Component` habría roto las cuatro suites por bean ausente, y tocarlas
   estaba prohibido. Por eso `JsonRpcPayloadMapper`, `JsonRpcResponseFactory` y `AgentCardProvider`
   son utilidades estáticas sin estado, y `JsonRpcDispatcher` lo construye el propio `Handler`.
2. **Los códigos JSON-RPC se leyeron del código vigente, no se inventaron**, y quedaron fijados por
   una prueba explícita (`givenContractCodes_thenValuesAreStable`).

---

## 4. Métricas obtenidas

| Métrica | Antes (Fase 05) | Después |
|---|---:|---:|
| Líneas `Handler.java` | 495 | **112** |
| Clases productivas > 300 líneas | 1 | **0** |
| Responsabilidades en `Handler` | 4 | **1** |
| Argumentos del constructor de `Handler` | 5 | **4** |
| Literales numéricos de código de error en el entry-point | 6 | **0** |
| Tests del módulo `reactive-web` | 18 | **62** |
| Tests totales del proyecto | 207 | **251** |
| Cobertura `JsonRpcPayloadMapper` | n/a | **100%** (362/362) |
| Cobertura `JsonRpcResponseFactory` | n/a | **100%** (122/122) |
| Cobertura `AgentCardProvider` | n/a | **100%** (125/125) |
| Cobertura `JsonRpcDispatcher` | n/a | **100%** (218/218) |
| Cobertura `Handler` | 86,1% | 86,1% (sin cambio) |

La clase más larga del proyecto pasa a ser `PgVectorPlanningAdapter` con **240 líneas**.

---

## 5. Validación de los criterios de aceptación

| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | `Handler` ≤ 150 líneas | Conteo del archivo | ✅ **112** |
| 2 | Cero clases > 300 líneas | Recuento sobre todo `src/main` | ✅ **0** |
| 3 | Las tres clases pedidas creadas | Listado del árbol | ✅ + `JsonRpcDispatcher` |
| 4 | Códigos de error como constantes, con los mismos valores | `JsonRpcResponseFactoryTest` | ✅ |
| 5 | **Las 4 suites del contrato, verdes y sin modificar** | `git diff` sobre las 4 rutas | ✅ **salida vacía** |
| 6 | Cobertura de las clases nuevas ≥ 90% | `jacocoMergedReport` | ✅ **100%** en las 4 |
| 7 | `UseCasesConfigWiringTest` verde | `.\gradlew.bat build` | ✅ |
| 8 | `.\gradlew.bat build` verde | Ejecutado | ✅ `BUILD SUCCESSFUL` |

### Sobre el criterio 5 (control de sanidad de la fase)

```powershell
git --no-pager diff --stat -- RouterRestTest.java JsonRpcErrorContractTest.java `
                              RouterRestLegacyToggleTest.java RouterRestLegacySunsetTest.java
# → sin salida
```

`git status` confirma que en `src/main` el **único archivo modificado** es `Handler.java`; todo lo
demás es alta. `domain/` no registra ningún cambio.

---

## 6. Comandos ejecutados

```powershell
.\gradlew.bat :reactive-web:test                # línea base: 18 pruebas verdes, antes de tocar nada
.\gradlew.bat :reactive-web:test                # tras la refactorización: 62 pruebas verdes
.\gradlew.bat build                             # BUILD SUCCESSFUL
.\gradlew.bat jacocoMergedReport --no-configuration-cache
git --no-pager diff --stat -- <las 4 suites>    # salida vacía
```

**Resultado:** `BUILD SUCCESSFUL` · **251/251** pruebas del proyecto en verde.

---

## 7. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **Se creó `JsonRpcDispatcher`**, no previsto en la lista de archivos | Con solo las tres clases planificadas, `Handler` quedaba en ~190 líneas: por encima del límite de 150. El enrutamiento de métodos JSON-RPC es una responsabilidad distinta del transporte HTTP, y el plan maestro §4 ya lo anticipaba como `JsonRpcHandler`. Se implementa como colaborador interno, no como bean, para no tocar las suites del contrato |
| **`mapEnvelopeErrorToResponse` se movió como `fromEnvelopeError(Throwable)`**, devolviendo `JsonRpcResponse` en vez de `Mono<ServerResponse>` | El Paso B pedía llevar el mapeo de errores a la factoría. Devolver el sobre y no la respuesta HTTP evita que la factoría dependa de `ServerResponse`: la decisión de transporte se queda en `Handler` |
| **Se eliminó el campo `JsonMapper` de `Handler`** | Se inyectaba por constructor y **no se usaba en ninguna línea** (verificado con `Select-String`). Es código muerto; conservarlo contradecía el objetivo de la fase. No afecta al contrato: ninguna prueba ni configuración construye `Handler` a mano |
| **Se eliminó la constante `ROLE_AGENT`** | Igualmente muerta: declarada y nunca referenciada |
| **La Agent Card no se externalizó a `resources/agent-card.json`** | El Paso C lo marcaba como *«valorar»*, no obligatorio. Se deja anotado para la Fase 07: hoy la tarjeta ya está aislada en una sola clase, que era el objetivo de D-11 |
| **`successWithTask` usa `LinkedHashMap` en vez de `HashMap`** | El original usaba `HashMap` con una única clave; el orden es irrelevante y así ambas fábricas de sobres son homogéneas. Sin efecto observable en el JSON |

---

## 8. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| `domain/model` sin Spring, persistencia ni serialización | ✅ no se tocó `domain/` |
| `domain/usecase` sin anotaciones de Spring | ✅ no se tocó `domain/` |
| El entry-point no contiene lógica de negocio | ✅ solo traduce y delega |
| SRP: una razón de cambio por clase | ✅ ver tabla de §2 |
| Sin secretos hardcodeados (S2068) | ✅ la Agent Card solo declara *placeholders* `{tenant}` / `{client-id}` |
| `Optional<T>` en retornos opcionales (S2259) | ✅ sin retornos opcionales nuevos |
| S107 (listas largas de parámetros) | ✅ el constructor de `Handler` baja de 5 a 4 |
| Complejidad cognitiva ≤ 15 | ✅ el método más complejo, `dispatch`, tiene 3 guardas y un `switch` |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| **Sin números mágicos (S109)** | ✅ los 6 códigos JSON-RPC son constantes con nombre |
| Campos finales en los beans (Rule_2.7) | ✅ los 5 campos de `Handler` son `final`; no se añaden violaciones nuevas |
| Tests GIVEN/WHEN/THEN | ✅ 44 pruebas nuevas, todas con `@DisplayName` GIVEN/WHEN/THEN |
| Build verde | ✅ |

---

## 9. Hallazgos

| Hallazgo | Atender en |
|---|---|
| **`Handler` recibía un `JsonMapper` por constructor que nunca usaba.** Sobrevivió al menos seis fases porque el compilador no advierte de un campo asignado y jamás leído. Refuerza el hallazgo de la Fase 03: verificar el código muerto con `grep`, no con el build | Práctica permanente |
| Las suites del entry-point fijan `Handler` como único bean del contexto. Cualquier fase futura que quiera introducir un colaborador como `@Component` deberá negociar primero el cambio de esas suites | Fase 07 |
| `Handler` se queda en 86,1%: lo no cubierto es el camino de error de `handleListTasks` y el `onErrorResume` del endpoint legacy | Fase 07 |
| La Agent Card sigue siendo Java compilado: publicar una capacidad nueva exige recompilar. Ya está aislada, externalizarla a `resources/agent-card.json` es ahora un cambio de una sola clase | Fase 07 (opcional) |
| `ArchitectureTest` mantiene sus advertencias preexistentes (`Rule_2.7` ×5, `Rule_2.2` ×1). Las clases nuevas **no** añaden violaciones | Fase 07 |

---

## 10. Estado al cerrar

- **Siguiente fase generada:** `FASE-07-endurecimiento-y-cierre.md`
- **Decisiones abiertas:** DP-05 (**bloquea** la Fase 07: debe preguntarse al usuario antes de empezar)
- **Pendiente de ratificación:**
  1. Eliminación de la «Regla de Mínimos de 5 puntos» (Fase 01).
  2. Regla del pronombre relativo en las palabras clave genéricas (Fase 03).
- **Objetivo del plan maestro alcanzado:** **cero clases de más de 300 líneas** en el proyecto.

