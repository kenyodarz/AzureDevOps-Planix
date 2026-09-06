# FASE 06 — Separación del Entry-Point

> **Estado:** 🟢 COMPLETADA (2026-08-29) · **Depende de:** FASE 05 (🟢 completada) · **Riesgo:** Alto
> **Commit al cerrar:** `refactor(agent_api): separar validacion y mapeo de errores del entry point`
> **Resultado:** `docs/resultados/RESULTADO-FASE-06.md`

> ⚠️ **Riesgo alto:** es la única fase que toca el **contrato público** de la API. Un error aquí es
> visible para los consumidores del agente, no solo para el equipo.

---

## 1. CONTEXTO

### 1.1 De dónde venimos

Las fases 01 a 05 dejaron el dominio limpio:

- `AgentChatUseCase`: 644 → **89 líneas**, constructor de 4 argumentos tipados.
- Seis `ChatFlowHandler` + `ChatFlowDispatcher` con fail-fast; **un único** `onErrorResume`.
- `IntentResolver`, `AzureDevOpsScope` y `CorporateKnowledge` al **100%** de cobertura.
- **207 tests** en verde. `domain/model` 96,1%, `domain/usecase` 95,4%.
- `UseCasesConfigWiringTest` verifica que el contexto de Spring arranca de verdad.

### 1.2 Problema que resuelve esta fase

**Deudas D-09 y D-10.** `Handler.java` tiene **494 líneas** y es la **única** clase de más de 300
líneas que queda en el proyecto. Concentra cuatro responsabilidades sin relación entre sí:

| Responsabilidad | Métodos | Líneas aprox. |
|---|---|---|
| **Mapeo payload → dominio** | `asString`, `asInteger`, `asBoolean`, `asMap`, `asStringList`, `asParts`, `toStringKeyedMap`, `mapSendMessageRequest`, `mapConfiguration`, `mapTaskId` | ~160 |
| **Respuestas y errores JSON-RPC** | `jsonRpcSuccess`, `jsonRpcSuccessWithTask`, `jsonRpcError`, `safeId`, `mapEnvelopeErrorToResponse`, `isParseError` | ~55 |
| **Agent Card estática** | `handleAgentCard` (líneas 414-483) | ~70 |
| **Enrutamiento y despacho** | `handleJsonRpc`, `handleSendMessage`, `dispatch*`, `handleTaskOperation`, `handleListTasks`, `cancelTask` | ~120 |

Consecuencias: cambiar el mapeo de un campo obliga a abrir la misma clase que el enrutamiento HTTP
y que la tarjeta del agente. La `AgentCard` está **hardcodeada en Java**, de modo que publicar una
capacidad nueva exige recompilar.

### 1.3 Estado esperado al terminar

1. `Handler` ≤ **150 líneas**, con una sola responsabilidad: enrutar y delegar.
2. **Cero** clases de más de 300 líneas en todo el proyecto (objetivo del plan maestro).
3. Existen `JsonRpcPayloadMapper`, `JsonRpcResponseFactory` y un proveedor de `AgentCard`.
4. El contrato JSON-RPC —métodos, códigos de error, forma del sobre— **no cambia en absoluto**.
5. `RouterRestTest` (10), `JsonRpcErrorContractTest` (5), `RouterRestLegacyToggleTest` y
   `RouterRestLegacySunsetTest` siguen verdes **sin modificarse**.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `infrastructure/entry-points/reactive-web/.../api/Handler.java` | MODIFICAR (adelgazar) |
| `.../api/mapper/JsonRpcPayloadMapper.java` | CREAR |
| `.../api/mapper/JsonRpcResponseFactory.java` | CREAR |
| `.../api/AgentCardProvider.java` | CREAR |
| `.../api/mapper/JsonRpcPayloadMapperTest.java` | CREAR |
| `.../api/mapper/JsonRpcResponseFactoryTest.java` | CREAR |
| `.../api/AgentCardProviderTest.java` | CREAR |
| `.../api/RouterRestTest.java` | **NO TOCAR** |
| `.../api/JsonRpcErrorContractTest.java` | **NO TOCAR** |
| `.../api/RouterRestLegacyToggleTest.java` | **NO TOCAR** |
| `.../api/RouterRestLegacySunsetTest.java` | **NO TOCAR** |
| `domain/**` | **NO TOCAR** |

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§3-S Single Responsibility:** «Cada clase debe tener una única razón para cambiar.»
- **§2 entry-points:** el entry-point traduce entre el mundo exterior y el dominio; **no** contiene
  lógica de negocio.
- **§4 Cognitive Complexity ≤ 15** y **S107** (listas largas de parámetros).
- **§5:** cobertura ≥ 90% en las clases nuevas.
- **Rule_2.7 de ArchUnit:** los beans deben tener **solo campos finales**. Hay 5 violaciones
  registradas desde la Fase 00; comprobar si alguna vive en el entry-point.

### 1.6 Decisiones pendientes que bloquean

| ID | Estado | Acción si sigue ABIERTA |
|---|---|---|
| — | — | **Ninguna bloquea esta fase.** DP-05 solo afecta a la Fase 07 |

> ⚠️ Antes de empezar, comprobar en `ESTADO.md` si el usuario ratificó o revirtió las dos entradas
> de «Ratificación pendiente» (Fases 01 y 03). Ninguna afecta al entry-point, pero conviene saberlo.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### Paso A — `JsonRpcPayloadMapper`

Mover **sin cambiar una línea de lógica** los diez métodos de mapeo. Todos son estáticos y sin
estado, así que la clase es `final` con constructor privado:

```java
public final class JsonRpcPayloadMapper {

    public static SendMessageRequest toSendMessageRequest(Object paramsObject) { ... }
    public static String toTaskId(Object paramsObject) { ... }
    // + los helpers asString, asInteger, asBoolean, asMap, asStringList, asParts,
    //   toStringKeyedMap y mapConfiguration como privados
}
```

> ⚠️ **Conservar la tolerancia actual a nulos y a tipos inesperados.** Estos métodos defienden al
> agente de payloads mal formados; endurecerlos cambiaría el comportamiento observable ante una
> petición inválida, que es justo lo que `JsonRpcErrorContractTest` verifica.

#### Paso B — `JsonRpcResponseFactory`

```java
public final class JsonRpcResponseFactory {

    public static JsonRpcResponse success(String id, SendMessageResponse response) { ... }
    public static JsonRpcResponse successWithTask(String id, Task task) { ... }
    public static JsonRpcResponse error(String id, int code, String message) { ... }
    public static String safeId(JsonRpcRequest request) { ... }
}
```

Los **códigos de error** JSON-RPC pasan a constantes con nombre en esta clase
(`PARSE_ERROR = -32700`, `INVALID_REQUEST = -32600`, `METHOD_NOT_FOUND = -32601`,
`INVALID_PARAMS = -32602`, `INTERNAL_ERROR = -32603`, o los que use hoy el código).
**Leer los valores del código actual; no inventarlos.** Verificar con `JsonRpcErrorContractTest`,
que es la fuente de verdad del contrato.

#### Paso C — `AgentCardProvider`

`handleAgentCard()` construye ~70 líneas de tarjeta en Java. Extraerlas a `AgentCardProvider`, que
devuelve el objeto `AgentCard` ya armado.

> 💡 **Valorar** externalizar la tarjeta a `resources/agent-card.json` y deserializarla, igual que se
> hizo con los prompts en la Fase 01. Ventaja: publicar una capacidad deja de requerir recompilar.
> **No es obligatorio en esta fase.** Si se hace, añadir una prueba que valide que el JSON del
> recurso produce exactamente la misma tarjeta que hoy; si no, dejarlo anotado para la Fase 07.

#### Paso D — Adelgazar `Handler`

Queda con: constructor, los `handle*` públicos, los `dispatch*` y `handleTaskOperation`. Todo lo
demás delega. Comprobar que los campos sean **finales** (Rule_2.7).

#### Paso E — Pruebas

- `JsonRpcPayloadMapperTest`: payload completo, payload mínimo, y **casos degenerados** —`null`,
  mapa vacío, tipos inesperados, listas heterogéneas—. Es la clase con más riesgo de regresión.
- `JsonRpcResponseFactoryTest`: forma del sobre de éxito y de error, y `safeId` con petición nula.
- `AgentCardProviderTest`: campos obligatorios de la tarjeta presentes y no vacíos.
- Las 4 suites del entry-point deben seguir verdes **sin tocarlas**.

### 2.2 Qué NO hacer

- ❌ **No cambiar el contrato JSON-RPC**: ni métodos, ni códigos de error, ni forma del sobre.
- ❌ No cambiar las rutas ni las cabeceras de deprecación del endpoint legacy.
- ❌ No modificar `RouterRestTest`, `JsonRpcErrorContractTest`, `RouterRestLegacyToggleTest` ni
  `RouterRestLegacySunsetTest`.
- ❌ No tocar nada de `domain/`.
- ❌ No endurecer la validación de payloads: mover, no mejorar.
- ❌ No resolver DP-05 aquí (es de la Fase 07).

### 2.3 Criterios de aceptación

- [ ] `Handler` ≤ 150 líneas.
- [ ] **Cero** clases de más de 300 líneas en el proyecto.
- [ ] `JsonRpcPayloadMapper`, `JsonRpcResponseFactory` y `AgentCardProvider` creados.
- [ ] Los códigos de error JSON-RPC son constantes con nombre, con **los mismos valores** de hoy.
- [ ] Las 4 suites del entry-point verdes **sin modificarse** (verificar con `git diff` vacío).
- [ ] Cobertura de las clases nuevas ≥ 90%.
- [ ] `UseCasesConfigWiringTest` sigue verde: el contexto arranca.
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL`.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] El entry-point no contiene lógica de negocio
- [ ] Sin secretos hardcodeados (S2068)
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109) — **especialmente los códigos JSON-RPC**
- [ ] Campos finales en los beans (Rule_2.7)
- [ ] Tests GIVEN/WHEN/THEN
- [ ] `.\gradlew.bat build` verde

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `ESTADO.md`, `DECISIONES_PENDIENTES.md` y `RESULTADO-FASE-05.md` | Contexto al día |
| 2 | Ejecutar `.\gradlew.bat :reactive-web:test` **antes de tocar nada** | Línea base verde registrada |
| 3 | Crear `JsonRpcPayloadMapper` moviendo los 10 métodos (Paso A) | Mapeo aislado |
| 4 | Escribir `JsonRpcPayloadMapperTest` con los casos degenerados (Paso E) | Verde antes de seguir |
| 5 | Crear `JsonRpcResponseFactory` con los códigos como constantes (Paso B) | Errores aislados |
| 6 | Escribir `JsonRpcResponseFactoryTest` | Verde |
| 7 | Crear `AgentCardProvider` (Paso C) | Tarjeta fuera del handler |
| 8 | Adelgazar `Handler` y verificar campos finales (Paso D) | ≤ 150 líneas |
| 9 | Ejecutar `.\gradlew.bat :reactive-web:test` | Las 4 suites verdes, **sin haberlas tocado** |
| 10 | `git diff` sobre las 4 suites del entry-point | **Salida vacía** |
| 11 | Ejecutar `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| 12 | Medir cobertura y contar clases > 300 líneas | Métricas reales; el contador debe dar **0** |
| 13 | **Escribir `docs/resultados/RESULTADO-FASE-06.md`** con `_PLANTILLA_RESULTADO.md` | Trazabilidad |
| 14 | Actualizar `docs/plan/ESTADO.md`: 🟢 COMPLETADA + métricas + bitácora | Tablero al día |
| 15 | **Generar `docs/fases/FASE-07-endurecimiento-y-cierre.md`** con `_PLANTILLA_FASE.md` | Checkpoint creado |
| 16 | Commit: `refactor(agent_api): separar validacion y mapeo de errores del entry point` | Cambios versionados |

> ⚠️ **Los pasos 13 y 15 son innegociables.** Sin ellos la fase se considera **incompleta**.
>
> 💡 **El paso 10 es el control de sanidad de esta fase.** Si el `git diff` sobre las suites del
> entry-point no está vacío, se cambió el contrato público. Revierte y vuelve a mover el código sin
> tocarlo.

### 3.1 Contenido mínimo del MD de la Fase 07

**CONTEXTO**
- Venimos de la Fase 06: cero clases de más de 300 líneas; el entry-point solo enruta.
- Es la fase de **cierre**: se saldan las deudas acumuladas y se resuelve la última decisión abierta.

**INSTRUCCIONES**
1. **Resolver DP-05** (bloqueante): preguntar al usuario si `chat()` async + `AgentResponseGateway` +
   `NoOpAgentResponseAdapter` se conservan como punto de extensión, se eliminan, o se ponen tras un
   feature flag. **No decidir por cuenta propia.**
2. Corregir las **5 violaciones de `Rule_2.7`** (campos no finales en beans) y la de `Rule_2.2`.
   El plugin avisa: *«This will cause a build error in future»*.
3. Sustituir o corregir **`UseCasesConfigTest`**, que descarta `UnsatisfiedDependencyException` y
   por tanto da una señal falsa (hallazgo de la Fase 05).
4. Dar pruebas a los ~15 modelos A2A de `domain/model`, que lastran la cobertura del módulo.
5. Cubrir el camino `chat()` asíncrono de `AgentChatUseCase` (hoy al 75,9%), según lo que decida
   DP-05.
6. Investigar el workaround `--no-configuration-cache` de `jacocoMergedReport` (incompatibilidad
   del plugin `pitest`).
7. Revisión final contra el checklist completo de `spring-rules.md` §7 y actualización del
   `README.md` del módulo con la arquitectura resultante.

**NO HACER:** no iniciar refactorizaciones nuevas; es una fase de cierre, no de cambio estructural.

**ORDEN DE EJECUCIÓN:** preguntar DP-05 y **esperar respuesta** → aplicar la decisión → corregir
ArchUnit → arreglar el test de configuración → pruebas de los modelos A2A → cobertura final →
build verde → `RESULTADO-FASE-07.md` → **informe de cierre del plan** → commit.

**Commit de cierre:** `refactor(agent): endurecer arquitectura y cerrar deudas del plan`

