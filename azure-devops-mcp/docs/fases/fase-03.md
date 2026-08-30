# FASE 03 — Frontera de contrato: DTOs y mappers

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🟢 **COMPLETADA** (2026-08-30)
> **Deudas que ataca:** D-11, D-17 · **Decisión bloqueante:** ✅ **DP-03 (RESUELTA)**
> **Riesgo:** Medio · **Depende de:** Fases 01 y 02 (cerradas) · **Habilita:** Fases 04 a 08
> **Regla de oro de esta fase:** **ningún nombre de campo del cable cambia.** La clase puede
> renombrarse; el JSON, no.

---

## 0. ✅ PASO BLOQUEANTE — DP-03 (RESUELTA el 2026-08-30)

**No se escribió una sola línea de código antes de que el propietario respondiera.** Esta fase toca
justamente los tipos que **Jackson deserializa desde el payload MCP**: equivocarse aquí **rompe al
agente y al BFF en silencio**, porque MCP no valida esquemas al vuelo y el fallo no aparece hasta que
un campo llega vacío.

**La pregunta de DP-03, en concreto:**

> **¿Se permite renombrar tipos de `domain/model`?** Sacar `WorkItemsBatchRequest` del dominio cierra
> **D-17** (la única violación de ArchUnit `Rule_2.2`), pero es un tipo que Jackson construye desde
> el payload MCP.

**Respuestas del propietario:**

| # | Cuestión                                                                                          | Decisión                                                                                      |
|---|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| 1 | ¿El DTO que sustituya a `WorkItemsBatchRequest` conserva el nombre de clase en el entry-point?     | **(b)** se renombra a **`WorkItemsBatchInput`**                                                |
| 2 | ¿Se autoriza renombrar la clase de dominio para cerrar `Rule_2.2`?                                 | **(a)** sí → **`WorkItemBatchCriteria`**                                                       |
| 3 | ¿`JsonPatchOperation` se duplica en DTO de entrada y de salida, o basta uno compartido?            | **(a)** **dos DTOs, uno por frontera**, con el dominio en medio                                |

**Cuestión colateral, planteada y arbitrada durante el paso 2.** Al inventariar las fronteras se
midió que cerrar `Rule_2.2` —por cualquiera de las tres vías— cambia el tipo del parámetro del puerto
`GetWorkItemsBatchRepository` y, por tanto, **obliga a tocar 3 líneas de `RestConsumerTest`** (un
`import` y dos usos del nombre de clase). Las dos casillas de la Definición de Hecho —«`Rule_2.2` a
0» y «0 pruebas heredadas modificadas»— eran **mutuamente excluyentes**. Conforme a
`spring-rules.md` §6 se detuvo el trabajo y se preguntó. El propietario autorizó el **renombrado
mecánico de símbolo**: sin tocar ni una aserción, ni un cuerpo JSON, ni un código de estado.

> ✅ **Lo que ya estaba cerrado y no se volvió a preguntar:** los **nombres de campo** del cable son
> intocables. `CONTRATO-MCP.md` §3 es explícito: `op`, `path`, `value`, `from` de
> `JsonPatchOperation`, e `ids`, `fields`, `expand`, `errorPolicy` de `WorkItemsBatchRequest`
> **deben conservarse exactamente**. **Se conservaron los ocho**, y ahora hay una prueba por
> reflexión que lo verifica.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 02 (2026-08-30), no estimado:

| Dato                                        | Valor                                                        |
|---------------------------------------------|--------------------------------------------------------------|
| Build                                       | 🟢 **VERDE** — **67 pruebas, 0 fallos**                      |
| Cobertura `domain/model`                    | **0 %** *(el módulo no tiene `src/test`, D-26)*              |
| Cobertura `domain/usecase`                  | **68,2 %**                                                   |
| Cobertura `mcp-server`                      | **70,2 %**                                                   |
| Cobertura `rest-consumer`                   | **90,0 %**                                                   |
| Cobertura `app-service`                     | **48,4 %**                                                   |
| Sentencia WIQL                              | **congelada carácter a carácter**, 8 ramas                   |
| Contrato MCP                                | inventariado y actualizado en [`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) |
| ArchUnit `Rule_2.2`                         | **1 violación real**, en *warning*, **no exportada a Sonar** (D-25) |

Documentos de referencia: [`BASELINE.md`](../resultados/BASELINE.md) ·
[`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) ·
[`ACTIVACION-SEGURIDAD.md`](../resultados/ACTIVACION-SEGURIDAD.md)

### 1.2 Lo que dejó hecho la Fase 02 y afecta a ésta

- ✅ **D-01 a D-04 saldadas.** La postura de acceso es configuración (`mcp.security.mode`), no
  código. **No la toques**: sigue en `PERMISSIVE` por defecto y así debe quedarse (DP-01).
- ✅ **Las 8 tools llevan `@PreAuthorize`**, con los roles centralizados en
  `mcp-server/.../mcp/security/McpRoles.java`. Si esta fase mueve firmas de método, **las
  anotaciones se mueven con ellas** y `McpToolsAuthorizationTest` debe seguir en verde.
- ✅ **19 pruebas nuevas** que congelan el comportamiento de seguridad. Son ahora parte del
  patrimonio heredado: **no se modifican**.
- 🔸 **`spring.devtools.add-properties` se conservó** deliberadamente: su dependencia existe.

### 1.3 El problema que esta fase resuelve

`CONTRATO-MCP.md` §3 lo enuncia sin ambigüedad: **el dominio es el contrato de cable en los dos
extremos**.

```java
// ENTRADA — Jackson construye un modelo de DOMINIO desde el payload MCP
@McpToolParam(...) List<JsonPatchOperation> patch      // AzureDevOpsTools

// SALIDA — el WebClient serializa el modelo de DOMINIO tal cual
.bodyValue(patch)     // RestConsumer → List<JsonPatchOperation>
.bodyValue(request)   // RestConsumer → WorkItemsBatchRequest
.bodyValue(query)     // RestConsumer → WiqlQuery
```

Tres consecuencias, todas medibles:

1. **Cualquier cambio en el JSON de Azure DevOps rompe el dominio.** El acoplamiento es directo y no
   hay ninguna capa donde absorberlo.
2. **`spring-rules.md` declara los mappers obligatorios** en `driven-adapters`, y aquí hay **cero**
   en la frontera de salida.
3. **`WorkItemsBatchRequest` viola `Rule_2.2`** por llamarse como se llama, y su nombre delata
   exactamente el problema: es un *request* HTTP viviendo en el dominio.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No toca la seguridad** (Fase 02, cerrada). Ni el modo, ni las anotaciones, ni el token.
- **No mueve el WIQL, las rutas ni la normalización de tipos**: eso es D-07/D-08/D-09, **Fase 04**.
- **No parte `RestConsumer`** en adaptadores: es D-10, **Fase 05**.
- **No traduce errores** ni toca cortacircuitos: D-13, D-06, **Fase 06**.
- **No convierte el modelo en Value Objects inmutables** ni retira los `@Setter`: D-16, **Fase 07**.
- **No arregla el informe de ArchUnit para Sonar** (D-25) ni crea `src/test` en `domain/model`
  (D-26): **Fases 07 y 08**. Ojo: por D-25, **el único modo fiable de verificar `Rule_2.2` es leer
  el log del test**, no el `issues.json`.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-03**.

### T-01 · DTOs de entrada en el entry-point (D-11)

**Ficheros:** `mcp-server/.../mcp/dto/` *(nuevo)* · `AzureDevOpsTools.java`

1. Crear los DTOs de entrada que hoy son modelos de dominio, **conservando literalmente los nombres
   de campo** (`op`, `path`, `value`, `from`; `ids`, `fields`, `expand`, `errorPolicy`).
2. Sustituir los tipos de `@McpToolParam` por los DTOs.
3. Los `@PreAuthorize` de la Fase 02 **acompañan al método**: ninguno se pierde por el camino.

**Validación:** `AzureDevOpsToolsTest`, `WiqlCharacterizationTest` y `McpToolsAuthorizationTest`
siguen en verde **sin modificarse**.

### T-02 · Mapper de la frontera de entrada

**Ficheros:** `mcp-server/.../mcp/dto/McpToolDtoMapper.java` *(nuevo)*

Mapper estático DTO → dominio. Sin Spring, sin lógica de negocio: solo traducción.

### T-03 · DTOs y mappers de la frontera de salida (D-11)

**Ficheros:** `rest-consumer/.../consumer/dto/` y `.../consumer/mapper/` *(nuevos)* ·
`RestConsumer.java`

1. DTOs propios del adaptador para los cuerpos que hoy serializa el `WebClient` desde el dominio.
2. Mappers dominio → DTO (salida) y DTO → dominio (respuesta), **obligatorios** según
   `spring-rules.md`.
3. **Sin cambiar ni una URL, ni un parámetro de consulta, ni la forma del JSON enviado.**

**Validación:** `RestConsumerTest` (11 pruebas) sigue en verde **sin modificarse**. Es la red de
seguridad: compara peticiones reales contra `MockWebServer`.

### T-04 · Cerrar `Rule_2.2` (D-17)

Según lo que decida **DP-03**: renombrar el tipo de dominio, sacarlo del dominio, o documentar la
excepción. **La regla debe quedar a cero violaciones reales**, verificado **en el log** del
`ArchitectureTest` (por D-25, el `issues.json` no sirve como prueba).

### T-05 · Actualizar documentación y generar la Fase 04

1. `CONTRATO-MCP.md` §3: dejar constancia de que el dominio **ya no es** el contrato de cable, y de
   qué nombre de clase quedó en cada frontera.
2. Plan maestro: §3 (D-11, D-17), §6 (métricas), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-04.md`** con **DP-04** marcada como bloqueante en su primer paso.
5. Commit: `refactor(mcp_contract): separar los dto de cable de los modelos de dominio`

---

## 3. Orden de Ejecución

- [x] **0.** ⛔ **Plantear DP-03 al propietario y esperar respuesta.** No continuar sin ella.
- [x] **1.** Releer `CONTRATO-MCP.md` §2 y §3 completos: son la lista de lo que no puede cambiar.
- [x] **2.** Inventariar cada punto donde un tipo de dominio cruza una frontera (entrada y salida).
- [x] **3.** Crear los DTOs de entrada en `mcp-server`, con los nombres de campo intactos. **(T-01)**
- [x] **4.** Crear `McpToolDtoMapper` y enchufarlo en las tools. **(T-02)**
- [x] **5.** `.\gradlew.bat build` verde. Las 67 pruebas heredadas, **sin modificar**.
- [x] **6.** Crear los DTOs de salida y sus mappers en `rest-consumer`. **(T-03)**
- [x] **7.** Verificar con `RestConsumerTest` que el JSON enviado es **byte a byte el mismo**. **(T-03)**
- [x] **8.** Aplicar lo que decida DP-03 sobre `WorkItemsBatchRequest`. **(T-04)**
- [x] **9.** Comprobar en el **log** del `ArchitectureTest` que `Rule_2.2` está a **0**. **(T-04)**
- [x] **10.** `.\gradlew.bat build` verde de nuevo.
- [x] **11.** Actualizar `CONTRATO-MCP.md` §3 y el plan maestro (§3, §6, §9, cabecera). **(T-05)**
- [x] **12.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [x] **13.** **Generar `docs/fases/fase-04.md`** con DP-04 bloqueante. **(Regla de Continuidad)**
- [x] **14.** Commit: `refactor(mcp_contract): separar los dto de cable de los modelos de dominio`

---

## 4. Resultado

> Cerrada el **2026-08-30**. Build 🟢 **VERDE**: **77 pruebas, 0 fallos**.

| Métrica                                                 |   Antes |        Después |
|---------------------------------------------------------|--------:|---------------:|
| Modelos de dominio deserializados como `@McpToolParam`   |   **2** |       ✅ **0** |
| Modelos de dominio serializados por el `WebClient`       |   **3** |       ✅ **0** |
| Mappers en la frontera de salida                         |   **0** |       ✅ **5** |
| Mappers en la frontera de entrada                        |   **0** |       ✅ **1** |
| ArchUnit `Rule_2.2` (violaciones reales, según el log)   |   **1** |       ✅ **0** |
| Nombres de campo del cable modificados                   |     n/a |       ✅ **0** |
| Pruebas totales                                          |  **67** |     **77** ▲10 |
| Pruebas heredadas **modificadas**                        |     n/a | **1** *(3 líneas, renombrado de símbolo autorizado)* |
| Líneas de `RestConsumer`                                 | **237** |     **186** ▼51 |
| Cobertura `mcp-server`                                   | **70,2 %** | **73,1 %** ▲2,9 |
| Cobertura `rest-consumer`                                | **90,0 %** | **88,1 %** ▼1,9 *(clases DTO nuevas con accesores sin ejercitar)* |
| Cobertura `app-service` / `domain/usecase`               | **48,4 % / 68,2 %** | **48,4 % / 68,2 %** = *(fuera de alcance)* |

**Decisión DP-03:** ✅ resuelta el 2026-08-30. (1) DTO de entrada renombrado a
**`WorkItemsBatchInput`**; (2) clase de dominio renombrada a **`WorkItemBatchCriteria`** —el nombre
que §4.1 del plan maestro ya anticipaba—, lo que cierra `Rule_2.2` **sin excepciones a la regla y
sin tocar el `ArchitectureTest`** (que lleva el aviso «Please do not modify this file»); (3) **dos
DTOs** para `JsonPatchOperation`, uno por frontera.

**Cambios en el contrato MCP público:** **ninguno.** Ni un nombre de tool, ni un nombre de
parámetro, ni un nombre de campo, ni la forma de un resultado. Los siete parámetros de
`getWorkItemsBatch` se siguen recibiendo sueltos y es el entry-point quien los agrupa, igual que
antes. Lo único que cambió son **nombres de clase Java**, que no viajan por el cable.

### 4.1 Qué se construyó

| Frontera | Paquete nuevo | Contenido |
|----------|---------------|-----------|
| Entrada  | `mcp-server/.../mcp/dto/`       | `JsonPatchOperationInput`, `WorkItemsBatchInput`, `McpToolDtoMapper` |
| Salida   | `rest-consumer/.../consumer/dto/`    | `JsonPatchOperationRequestDTO`, `WiqlQueryRequestDTO`, `WorkItemsBatchRequestDTO` **(+ los 9 DTOs de respuesta, movidos aquí desde la raíz del paquete)** |
| Salida   | `rest-consumer/.../consumer/mapper/` | `JsonPatchMapper`, `WiqlQueryMapper`, `WorkItemBatchMapper` *(ida)* · `WorkItemMapper`, `TeamMapper` *(vuelta)* |

Los ocho métodos `toDomain(...)` privados que vivían dentro de `RestConsumer` se movieron a
`WorkItemMapper` y `TeamMapper` **sin cambiar una sola línea de su lógica**. Ése es el motivo de que
`RestConsumer` baje 51 líneas: **no se partió la clase** —eso es D-10, Fase 05—, solo se sacó de ella
lo que nunca fue su responsabilidad.

### 4.2 Pruebas nuevas (10)

| Clase                                   | Módulo         | Pruebas | Qué congela |
|-----------------------------------------|----------------|--------:|-------------|
| `OutboundPayloadCharacterizationTest`   | `rest-consumer`|   **4** | El **cuerpo HTTP realmente enviado** a Azure DevOps, comparado contra la serialización del **modelo de dominio** —literalmente lo que hacía el `WebClient` antes de esta fase |
| `McpToolDtoMapperTest`                  | `mcp-server`   |   **6** | La traducción DTO→dominio y, por **reflexión**, que los ocho nombres de campo del cable son exactamente los del contrato |

> **Por qué `OutboundPayloadCharacterizationTest` tuvo que existir.** El paso 7 del checklist pedía
> verificar que el JSON enviado es byte a byte el mismo «con `RestConsumerTest`». Al leerla se
> comprobó que **`RestConsumerTest` nunca ha comparado un cuerpo de petición**: valida URIs y
> respuestas. El hueco era justo el que esta fase podía romper en silencio —un campo perdido, un nulo
> que deja de incluirse, un orden que cambia— y que Azure DevOps no reportaría como error, sino como
> un resultado distinto. La prueba no compara contra una cadena literal escrita a mano (eso solo
> congelaría «lo que salga hoy»): compara contra `objectMapper.writeValueAsString(<modelo de
> dominio>)`, que **es** el comportamiento anterior.

### 4.3 Verificación de `Rule_2.2` — en el log, no en `issues.json`

Por **D-25** el informe para Sonar sale vacío aunque haya violaciones, así que la única prueba
fiable es el log del `ArchitectureTest`. Antes de esta fase:

```
ADVERTENCIA: ARCHITECTURE_RULE_VIOLATED: This will cause a build error in future.
Rule 'Rule_2.2: Domain classes should not be named with technology suffixes' was violated (1 times)
```

Después: **0 coincidencias** de `was violated` en la totalidad de los informes de prueba de
`app-service`. Verificado además por barrido del árbol: **ningún fichero de `domain/` tiene un
nombre de clase terminado en `Dto`, `DTO`, `Request` o `Response`**.

> ⚠️ **D-25 sigue viva.** La métrica «violaciones exportadas a Sonar» pasa de `0 de 1` a `0 de 0`:
> ahora coincide con la realidad, pero **por ausencia de violaciones, no porque el informe se haya
> arreglado**. Si mañana aparece una nueva, Sonar volverá a no verla. Es material de la Fase 08.

### 4.4 Desviaciones respecto a las Instrucciones, y por qué

1. **Se modificó `RestConsumerTest`** (3 líneas: `import` + 2 usos del nombre de clase), contra la
   regla «0 pruebas heredadas modificadas». **No fue una decisión propia**: se detectó al inventariar
   las fronteras, se documentó como conflicto directo entre dos casillas de la Definición de Hecho y
   **se elevó al propietario, que lo autorizó** (§0). Ninguna aserción, ningún cuerpo JSON y ningún
   código de estado cambiaron: la red de seguridad sigue comparando peticiones reales contra
   `MockWebServer`, y sus 11 pruebas siguen en verde.
2. **Los 9 DTOs de respuesta preexistentes se movieron** de `co.com.bancolombia.consumer` a
   `co.com.bancolombia.consumer.dto`. T-03 solo pedía crear los DTOs de salida, pero dejar la mitad
   de los DTOs del adaptador en la raíz del paquete y la otra mitad en `dto/` habría creado una
   frontera a medias, que es peor que ninguna. Ningún test los referencia, así que el movimiento fue
   inocuo.
3. **Se añadieron 10 pruebas**, cuando la fase no exigía ninguna. Sin ellas, «0 nombres de campo del
   cable modificados» y «el JSON no cambió» serían afirmaciones de un documento, no hechos
   verificables por el build. Con ellas, cualquier regresión futura sale en rojo.

### 4.5 Lo que esta fase NO hizo (y sigue pendiente)

- El **retorno** hacia el cliente MCP sigue serializando modelos de dominio (`WorkItem`,
  `WiqlResult`, `TeamFieldValues`…). Esa mitad no estaba en el alcance —la fase ataca la frontera con
  Azure DevOps— y queda anotada para la **Fase 07** (D-16).
- No se tocó la seguridad de la Fase 02: los ocho `@PreAuthorize` acompañaron a sus métodos y
  `McpToolsAuthorizationTest` sigue en verde **sin modificarse**.
- No se movió el WIQL, ni las rutas, ni la normalización de tipos (D-07/08/09 → **Fase 04**); no se
  partió `RestConsumer` (D-10 → **Fase 05**); no se tradujo ningún error (D-13 → **Fase 06**).

**Bloqueos encontrados:** ninguno sin resolver. Los **dos** puntos de decisión que aparecieron
(DP-03 y el conflicto de `RestConsumerTest`) se elevaron al propietario y se resolvieron el mismo
día. **Ninguna decisión se tomó por cuenta propia** (`spring-rules.md` §6).

**Fase siguiente generada:** ☑ [`docs/fases/fase-04.md`](fase-04.md) — con **DP-04** marcada como
bloqueante en su primer paso · ☑ [`docs/fases/PROMPT-FASE-04.md`](PROMPT-FASE-04.md), prompt de
arranque **autocontenido** (§8.7 del plan maestro).

